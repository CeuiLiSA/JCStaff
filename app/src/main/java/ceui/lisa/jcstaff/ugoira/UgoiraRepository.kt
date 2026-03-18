package ceui.lisa.jcstaff.ugoira

import android.content.Context
import android.graphics.BitmapFactory
import android.util.Log
import ceui.lisa.jcstaff.R
import ceui.lisa.jcstaff.network.PixivClient
import ceui.lisa.jcstaff.network.UgoiraMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipFile

/**
 * Ugoira processing state
 */
sealed class UgoiraState {
    object Idle : UgoiraState()
    object FetchingMetadata : UgoiraState()
    data class Downloading(val progress: Int) : UgoiraState()
    object Extracting : UgoiraState()
    data class Encoding(val progress: Int) : UgoiraState()
    data class Done(val data: UgoiraData) : UgoiraState()
    data class Error(val errorResId: Int, val errorCode: Int? = null) : UgoiraState()
}

/**
 * Exception with string resource ID
 */
class UgoiraException(val errorResId: Int, val errorCode: Int? = null) : Exception()

/**
 * Ugoira data (output file)
 */
data class UgoiraData(
    val gifFile: File
)

/**
 * Ugoira repository
 */
object UgoiraRepository {

    private const val TAG = "UgoiraRepository"
    private const val UGOIRA_DIR = "ugoira"

    private val fileCache = java.util.concurrent.ConcurrentHashMap<Pair<Long, UgoiraExportFormat>, UgoiraData>()

    // ── GIF (existing API, unchanged for callers) ──────────────────────────

    suspend fun getOrCreateGif(
        context: Context,
        illustId: Long,
        stateFlow: MutableStateFlow<UgoiraState>? = null
    ): UgoiraData? = getOrCreate(context, illustId, UgoiraExportFormat.GIF, stateFlow)

    fun getGifFile(illustId: Long): File? = getFile(illustId, UgoiraExportFormat.GIF)

    // ── Generic multi-format API ───────────────────────────────────────────

    suspend fun getOrCreate(
        context: Context,
        illustId: Long,
        format: UgoiraExportFormat,
        stateFlow: MutableStateFlow<UgoiraState>? = null
    ): UgoiraData? = withContext(Dispatchers.IO) {
        val cacheKey = Pair(illustId, format)

        // Memory cache
        fileCache[cacheKey]?.let { cached ->
            if (cached.gifFile.exists()) {
                stateFlow?.value = UgoiraState.Done(cached)
                return@withContext cached
            }
        }

        val ugoiraDir = File(context.filesDir, UGOIRA_DIR)
        if (!ugoiraDir.exists()) ugoiraDir.mkdirs()

        val outputFile = File(ugoiraDir, "$illustId.${format.extension}")

        // Disk cache
        if (outputFile.exists()) {
            val data = UgoiraData(outputFile)
            fileCache[cacheKey] = data
            stateFlow?.value = UgoiraState.Done(data)
            return@withContext data
        }

        val framesDir = File(ugoiraDir, "frames_$illustId")

        try {
            // Step 1: Fetch metadata
            stateFlow?.value = UgoiraState.FetchingMetadata
            val response = PixivClient.pixivApi.getUgoiraMetadata(illustId)
            val metadata = response.ugoira_metadata
            if (metadata == null) {
                stateFlow?.value = UgoiraState.Error(R.string.ugoira_error_no_metadata)
                return@withContext null
            }

            // Step 2: Download zip (if frames not already extracted)
            if (!framesDir.exists() || framesDir.listFiles().isNullOrEmpty()) {
                val zipUrl = metadata.getZipUrl()
                if (zipUrl == null) {
                    stateFlow?.value = UgoiraState.Error(R.string.ugoira_error_no_zip_url)
                    return@withContext null
                }
                val zipFile = File(ugoiraDir, "tmp_$illustId.zip")
                downloadZip(zipUrl, zipFile, stateFlow)

                // Step 3: Extract
                stateFlow?.value = UgoiraState.Extracting
                extractZip(zipFile, framesDir)
                zipFile.delete()
            }

            // Step 4: Encode to the requested format
            stateFlow?.value = UgoiraState.Encoding(0)
            when (format) {
                UgoiraExportFormat.GIF -> encodeGif(framesDir, metadata, outputFile) { p ->
                    stateFlow?.value = UgoiraState.Encoding(p)
                }
                UgoiraExportFormat.WEBP -> encodeWebP(framesDir, metadata, outputFile) { p ->
                    stateFlow?.value = UgoiraState.Encoding(p)
                }
                UgoiraExportFormat.MP4 -> encodeMp4(framesDir, metadata, outputFile) { p ->
                    stateFlow?.value = UgoiraState.Encoding(p)
                }
            }

            val data = UgoiraData(outputFile)
            fileCache[cacheKey] = data
            Log.d(TAG, "${format.name} created: ${outputFile.absolutePath}")
            stateFlow?.value = UgoiraState.Done(data)
            data
        } catch (e: UgoiraException) {
            Log.e(TAG, "Failed to process ugoira", e)
            stateFlow?.value = UgoiraState.Error(e.errorResId, e.errorCode)
            null
        } catch (e: Exception) {
            Log.e(TAG, "Failed to process ugoira: ${e.message}", e)
            stateFlow?.value = UgoiraState.Error(R.string.ugoira_error_processing)
            null
        }
    }

    fun getFile(illustId: Long, format: UgoiraExportFormat): File? {
        return fileCache[Pair(illustId, format)]?.gifFile
    }

    // ── Encoders ──────────────────────────────────────────────────────────

    private fun encodeGif(
        framesDir: File,
        metadata: UgoiraMetadata,
        outputFile: File,
        onProgress: (Int) -> Unit = {}
    ) {
        val encoder = AnimatedGifEncoder()
        encoder.setRepeat(0)
        encoder.setQuality(10)
        encoder.start(outputFile.absolutePath)

        val totalFrames = metadata.frames.size
        var processedFrames = 0

        for (frame in metadata.frames) {
            val fileName = frame.file ?: continue
            val file = File(framesDir, fileName)
            if (!file.exists()) continue

            val bitmap = BitmapFactory.decodeFile(file.absolutePath) ?: continue
            encoder.setDelay(frame.delay)
            encoder.addFrame(bitmap)
            bitmap.recycle()

            processedFrames++
            onProgress(processedFrames * 100 / totalFrames)
        }

        encoder.finish()
    }

    private fun encodeWebP(
        framesDir: File,
        metadata: UgoiraMetadata,
        outputFile: File,
        onProgress: (Int) -> Unit = {}
    ) {
        val frames = mutableListOf<Pair<android.graphics.Bitmap, Int>>()
        val totalFrames = metadata.frames.size

        try {
            for ((index, frame) in metadata.frames.withIndex()) {
                val fileName = frame.file ?: continue
                val file = File(framesDir, fileName)
                if (!file.exists()) continue
                val bitmap = BitmapFactory.decodeFile(file.absolutePath) ?: continue
                frames.add(Pair(bitmap, frame.delay))
                onProgress((index + 1) * 50 / totalFrames) // first 50% = loading frames
            }

            FileOutputStream(outputFile).use { out ->
                AnimatedWebPEncoder.encode(frames, out)
            }
            onProgress(100)
        } finally {
            frames.forEach { it.first.recycle() }
        }
    }

    private fun encodeMp4(
        framesDir: File,
        metadata: UgoiraMetadata,
        outputFile: File,
        onProgress: (Int) -> Unit = {}
    ) {
        val framePairs = metadata.frames.mapNotNull { frame ->
            val fileName = frame.file ?: return@mapNotNull null
            val file = File(framesDir, fileName)
            if (!file.exists()) return@mapNotNull null
            Pair(file, frame.delay)
        }
        Mp4Encoder.encode(framePairs, outputFile, onProgress)
    }

    // ── Download / Extract ────────────────────────────────────────────────

    private suspend fun downloadZip(
        url: String,
        outputFile: File,
        stateFlow: MutableStateFlow<UgoiraState>?
    ) = withContext(Dispatchers.IO) {
        stateFlow?.value = UgoiraState.Downloading(0)

        val request = Request.Builder().url(url).build()
        val response = PixivClient.imageClient.newCall(request).execute()

        if (!response.isSuccessful) {
            throw UgoiraException(R.string.ugoira_error_download_failed, response.code)
        }

        val body = response.body ?: throw UgoiraException(R.string.ugoira_error_empty_response)
        val contentLength = body.contentLength()

        body.byteStream().use { input ->
            FileOutputStream(outputFile).use { output ->
                val buffer = ByteArray(8192)
                var bytesRead: Long = 0
                var read: Int

                while (input.read(buffer).also { read = it } != -1) {
                    output.write(buffer, 0, read)
                    bytesRead += read

                    if (contentLength > 0) {
                        val progress = (bytesRead * 100 / contentLength).toInt()
                        stateFlow?.value = UgoiraState.Downloading(progress)
                    }
                }
            }
        }

        Log.d(TAG, "Zip downloaded: ${outputFile.absolutePath}")
    }

    private fun extractZip(zipFile: File, outputDir: File) {
        if (outputDir.exists()) outputDir.deleteRecursively()
        outputDir.mkdirs()

        ZipFile(zipFile).use { zip ->
            zip.entries().asSequence().forEach { entry ->
                val outputFile = File(outputDir, entry.name)
                // Validate against zip path traversal attacks
                if (!outputFile.canonicalPath.startsWith(outputDir.canonicalPath + File.separator)) {
                    throw SecurityException("Zip entry attempts path traversal: ${entry.name}")
                }
                zip.getInputStream(entry).use { input ->
                    FileOutputStream(outputFile).use { output ->
                        input.copyTo(output)
                    }
                }
            }
        }

        Log.d(TAG, "Zip extracted to: ${outputDir.absolutePath}")
    }

    // ── Cache management ──────────────────────────────────────────────────

    fun clearCache(context: Context, illustId: Long) {
        UgoiraExportFormat.entries.forEach { format ->
            fileCache.remove(Pair(illustId, format))
        }
        val ugoiraDir = File(context.filesDir, UGOIRA_DIR)
        File(ugoiraDir, "tmp_$illustId.zip").delete()
        File(ugoiraDir, "frames_$illustId").deleteRecursively()
        UgoiraExportFormat.entries.forEach { format ->
            File(ugoiraDir, "$illustId.${format.extension}").delete()
        }
    }

    fun clearAllCache(context: Context) {
        fileCache.clear()
        val ugoiraDir = File(context.filesDir, UGOIRA_DIR)
        ugoiraDir.deleteRecursively()
    }
}
