package ceui.lisa.jcstaff.ugoira

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import ceui.lisa.jcstaff.network.PixivClient
import ceui.lisa.jcstaff.network.UgoiraMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipFile

/**
 * Ugoira 处理状态
 */
sealed class UgoiraState {
    object Idle : UgoiraState()
    data class FetchingMetadata(val progress: String = "获取元数据...") : UgoiraState()
    data class Downloading(val progress: Int) : UgoiraState()
    data class Extracting(val progress: String = "解压中...") : UgoiraState()
    data class Encoding(val progress: String = "处理中...") : UgoiraState()
    data class Done(val frames: UgoiraFrames) : UgoiraState()
    data class Error(val message: String) : UgoiraState()
}

/**
 * Ugoira 帧数据
 */
data class UgoiraFrames(
    val frameFiles: List<File>,
    val delays: List<Int>,
    val cacheDir: File
) {
    val totalDuration: Int get() = delays.sum()

    fun getFrameAtTime(timeMs: Long): File {
        val loopedTime = timeMs % totalDuration
        var accumulated = 0
        for (i in frameFiles.indices) {
            accumulated += delays[i]
            if (loopedTime < accumulated) {
                return frameFiles[i]
            }
        }
        return frameFiles.last()
    }
}

/**
 * Ugoira 仓库
 *
 * 负责：
 * - 获取 ugoira 元数据
 * - 下载 zip 文件
 * - 解压帧图片
 * - 提供帧序列用于播放
 */
object UgoiraRepository {

    private const val TAG = "UgoiraRepository"
    private const val UGOIRA_DIR = "ugoira"

    private val client = OkHttpClient.Builder()
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .header("Referer", "https://app-api.pixiv.net/")
                .build()
            chain.proceed(request)
        }
        .build()

    // 内存缓存
    private val framesCache = mutableMapOf<Long, UgoiraFrames>()

    /**
     * 获取或创建 ugoira 帧数据
     */
    suspend fun getOrCreateFrames(
        context: Context,
        illustId: Long,
        stateFlow: MutableStateFlow<UgoiraState>? = null
    ): UgoiraFrames? = withContext(Dispatchers.IO) {
        // 内存缓存
        framesCache[illustId]?.let { cached ->
            if (cached.frameFiles.all { it.exists() }) {
                Log.d(TAG, "Using memory cached frames for $illustId")
                stateFlow?.value = UgoiraState.Done(cached)
                return@withContext cached
            }
        }

        val ugoiraDir = File(context.filesDir, UGOIRA_DIR)
        if (!ugoiraDir.exists()) ugoiraDir.mkdirs()

        val framesDir = File(ugoiraDir, "frames_$illustId")

        try {
            // Step 1: 获取元数据
            stateFlow?.value = UgoiraState.FetchingMetadata()
            val response = PixivClient.pixivApi.getUgoiraMetadata(illustId)
            val metadata = response.ugoira_metadata
                ?: throw Exception("无法获取 ugoira 元数据")

            val zipUrl = metadata.getZipUrl()
                ?: throw Exception("无法获取 zip 下载地址")

            // 检查是否已有缓存的帧
            if (framesDir.exists() && framesDir.listFiles()?.isNotEmpty() == true) {
                val frames = loadFramesFromDir(framesDir, metadata)
                if (frames != null) {
                    framesCache[illustId] = frames
                    stateFlow?.value = UgoiraState.Done(frames)
                    return@withContext frames
                }
            }

            // Step 2: 下载 zip
            val zipFile = File(ugoiraDir, "tmp_$illustId.zip")
            downloadZip(zipUrl, zipFile, stateFlow)

            // Step 3: 解压
            stateFlow?.value = UgoiraState.Extracting()
            extractZip(zipFile, framesDir)

            // 删除 zip 文件
            zipFile.delete()

            // Step 4: 加载帧数据
            stateFlow?.value = UgoiraState.Encoding()
            val frames = loadFramesFromDir(framesDir, metadata)
                ?: throw Exception("无法加载帧数据")

            framesCache[illustId] = frames
            Log.d(TAG, "Frames loaded: ${frames.frameFiles.size} frames")
            stateFlow?.value = UgoiraState.Done(frames)
            frames
        } catch (e: Exception) {
            Log.e(TAG, "Failed to process ugoira: ${e.message}", e)
            stateFlow?.value = UgoiraState.Error(e.message ?: "处理失败")
            null
        }
    }

    private fun loadFramesFromDir(framesDir: File, metadata: UgoiraMetadata): UgoiraFrames? {
        val frameFiles = mutableListOf<File>()
        val delays = mutableListOf<Int>()

        for (frame in metadata.frames) {
            val fileName = frame.file ?: continue
            val file = File(framesDir, fileName)
            if (file.exists()) {
                frameFiles.add(file)
                delays.add(frame.delay)
            }
        }

        if (frameFiles.isEmpty()) return null

        return UgoiraFrames(
            frameFiles = frameFiles,
            delays = delays,
            cacheDir = framesDir
        )
    }

    private suspend fun downloadZip(
        url: String,
        outputFile: File,
        stateFlow: MutableStateFlow<UgoiraState>?
    ) = withContext(Dispatchers.IO) {
        stateFlow?.value = UgoiraState.Downloading(0)

        val request = Request.Builder().url(url).build()
        val response = client.newCall(request).execute()

        if (!response.isSuccessful) {
            throw Exception("下载失败: ${response.code}")
        }

        val body = response.body ?: throw Exception("响应体为空")
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

    /**
     * 将帧序列编码为 WebP（使用第一帧作为静态图）
     * 注意：Android 原生不支持编码动态 WebP，这里保存的是静态图
     */
    suspend fun saveFirstFrameAsWebp(
        context: Context,
        illustId: Long,
        outputFile: File
    ): Boolean = withContext(Dispatchers.IO) {
        val frames = framesCache[illustId] ?: return@withContext false
        val firstFrame = frames.frameFiles.firstOrNull() ?: return@withContext false

        try {
            val bitmap = BitmapFactory.decodeFile(firstFrame.absolutePath)
                ?: return@withContext false

            FileOutputStream(outputFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.WEBP_LOSSY, 90, out)
            }
            bitmap.recycle()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save WebP: ${e.message}")
            false
        }
    }

    /**
     * 获取第一帧文件（用于保存到相册）
     */
    fun getFirstFrameFile(illustId: Long): File? {
        return framesCache[illustId]?.frameFiles?.firstOrNull()
    }

    /**
     * 清理指定作品的缓存
     */
    fun clearCache(context: Context, illustId: Long) {
        framesCache.remove(illustId)
        val ugoiraDir = File(context.filesDir, UGOIRA_DIR)
        File(ugoiraDir, "tmp_$illustId.zip").delete()
        File(ugoiraDir, "frames_$illustId").deleteRecursively()
    }

    /**
     * 清理所有 ugoira 缓存
     */
    fun clearAllCache(context: Context) {
        framesCache.clear()
        val ugoiraDir = File(context.filesDir, UGOIRA_DIR)
        ugoiraDir.deleteRecursively()
    }
}
