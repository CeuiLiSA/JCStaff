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
 * Ugoira data (GIF file)
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

    private val gifCache = mutableMapOf<Long, UgoiraData>()

    suspend fun getOrCreateGif(
        context: Context,
        illustId: Long,
        stateFlow: MutableStateFlow<UgoiraState>? = null
    ): UgoiraData? = withContext(Dispatchers.IO) {
        // Memory cache
        gifCache[illustId]?.let { cached ->
            if (cached.gifFile.exists()) {
                Log.d(TAG, "Using memory cached GIF for $illustId")
                stateFlow?.value = UgoiraState.Done(cached)
                return@withContext cached
            }
        }

        val ugoiraDir = File(context.filesDir, UGOIRA_DIR)
        if (!ugoiraDir.exists()) ugoiraDir.mkdirs()

        val gifFile = File(ugoiraDir, "${illustId}.gif")

        // Check disk cache
        if (gifFile.exists()) {
            val data = UgoiraData(gifFile)
            gifCache[illustId] = data
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

            val zipUrl = metadata.getZipUrl()
            if (zipUrl == null) {
                stateFlow?.value = UgoiraState.Error(R.string.ugoira_error_no_zip_url)
                return@withContext null
            }

            // Step 2: Download zip
            val zipFile = File(ugoiraDir, "tmp_$illustId.zip")
            downloadZip(zipUrl, zipFile, stateFlow)

            // Step 3: Extract
            stateFlow?.value = UgoiraState.Extracting
            extractZip(zipFile, framesDir)

            // Delete zip file
            zipFile.delete()

            // Step 4: Encode to GIF
            stateFlow?.value = UgoiraState.Encoding(0)
            encodeGif(framesDir, metadata, gifFile) { progress ->
                stateFlow?.value = UgoiraState.Encoding(progress)
            }

            // Delete frame files
            framesDir.deleteRecursively()

            val data = UgoiraData(gifFile)
            gifCache[illustId] = data
            Log.d(TAG, "GIF created: ${gifFile.absolutePath}")
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

    private fun encodeGif(
        framesDir: File,
        metadata: UgoiraMetadata,
        outputFile: File,
        onProgress: (Int) -> Unit = {}
    ) {
        val encoder = AnimatedGifEncoder()
        encoder.setRepeat(0) // Loop forever
        encoder.setQuality(10) // Quality (1-20, 1 is best but slowest)
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
            val progress = (processedFrames * 100 / totalFrames)
            onProgress(progress)
        }

        encoder.finish()
    }

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
                zip.getInputStream(entry).use { input ->
                    FileOutputStream(outputFile).use { output ->
                        input.copyTo(output)
                    }
                }
            }
        }

        Log.d(TAG, "Zip extracted to: ${outputDir.absolutePath}")
    }

    fun getGifFile(illustId: Long): File? {
        return gifCache[illustId]?.gifFile
    }

    fun clearCache(context: Context, illustId: Long) {
        gifCache.remove(illustId)
        val ugoiraDir = File(context.filesDir, UGOIRA_DIR)
        File(ugoiraDir, "tmp_$illustId.zip").delete()
        File(ugoiraDir, "frames_$illustId").deleteRecursively()
        File(ugoiraDir, "${illustId}.gif").delete()
    }

    fun clearAllCache(context: Context) {
        gifCache.clear()
        val ugoiraDir = File(context.filesDir, UGOIRA_DIR)
        ugoiraDir.deleteRecursively()
    }
}
