package ceui.lisa.jcstaff.core

import android.content.Context
import coil.ImageLoader
import coil.disk.DiskCache
import coil.memory.MemoryCache
import okhttp3.Interceptor
import okhttp3.MediaType
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.ResponseBody
import okio.Buffer
import okio.BufferedSource
import okio.ForwardingSource
import okio.Source
import okio.buffer
import java.util.concurrent.ConcurrentHashMap

/**
 * 进度监听器
 */
typealias ProgressListener = (url: String, bytesRead: Long, contentLength: Long) -> Unit

/**
 * 进度追踪管理器
 * 与 LoadTaskManager 集成，自动同步进度
 */
object ProgressManager {
    private val listeners = ConcurrentHashMap<String, ProgressListener>()

    fun addListener(url: String, listener: ProgressListener) {
        listeners[url] = listener
    }

    fun removeListener(url: String) {
        listeners.remove(url)
    }

    internal fun updateProgress(url: String, bytesRead: Long, contentLength: Long) {
        // 通知本地监听器
        listeners[url]?.invoke(url, bytesRead, contentLength)

        // 同步到 LoadTaskManager（全局进度管理）
        LoadTaskManager.updateProgress(url, bytesRead, contentLength)
    }
}

/**
 * 进度追踪拦截器
 */
class ProgressInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val originalResponse = chain.proceed(request)
        val url = request.url.toString()

        return originalResponse.newBuilder()
            .body(originalResponse.body?.let { ProgressResponseBody(url, it) })
            .build()
    }
}

/**
 * 带进度追踪的 ResponseBody
 */
class ProgressResponseBody(
    private val url: String,
    private val responseBody: ResponseBody
) : ResponseBody() {

    private var bufferedSource: BufferedSource? = null

    override fun contentType(): MediaType? = responseBody.contentType()

    override fun contentLength(): Long = responseBody.contentLength()

    override fun source(): BufferedSource {
        if (bufferedSource == null) {
            bufferedSource = source(responseBody.source()).buffer()
        }
        return bufferedSource!!
    }

    private fun source(source: Source): Source {
        return object : ForwardingSource(source) {
            var totalBytesRead = 0L

            override fun read(sink: Buffer, byteCount: Long): Long {
                val bytesRead = super.read(sink, byteCount)
                totalBytesRead += if (bytesRead != -1L) bytesRead else 0
                ProgressManager.updateProgress(url, totalBytesRead, contentLength())
                return bytesRead
            }
        }
    }
}

/**
 * 创建带进度追踪的 ImageLoader
 */
fun createProgressImageLoader(context: Context): ImageLoader {
    val okHttpClient = OkHttpClient.Builder()
        .addNetworkInterceptor(ProgressInterceptor())
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .addHeader("Referer", "https://app-api.pixiv.net/")
                .build()
            chain.proceed(request)
        }
        .build()

    return ImageLoader.Builder(context)
        .okHttpClient(okHttpClient)
        .memoryCache {
            MemoryCache.Builder(context)
                .maxSizePercent(0.25)
                .build()
        }
        .diskCache {
            DiskCache.Builder()
                .directory(context.cacheDir.resolve("image_cache"))
                .maxSizePercent(0.02)
                .build()
        }
        .crossfade(true)
        .build()
}
