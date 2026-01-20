package ceui.lisa.jcstaff.network

import android.util.Log
import ceui.lisa.jcstaff.BuildConfig
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * API 缓存拦截器
 *
 * 仅在 Debug 环境下生效，同一 API 在 15 分钟内只发起一次请求
 * 缓存 key 基于 URL + HTTP Method，忽略 Header 变化
 *
 * 设计要点：
 * 1. 使用 ConcurrentHashMap 保证线程安全
 * 2. 缓存响应体的字节数组，避免 ResponseBody 只能读取一次的问题
 * 3. LRU 策略：缓存满时清理最旧的条目
 */
class ApiCacheInterceptor : Interceptor {

    companion object {
        private const val TAG = "ApiCache"
        private val CACHE_DURATION_MS = TimeUnit.MINUTES.toMillis(15)
        private const val MAX_CACHE_SIZE = 100

        // 不缓存的路径（如 OAuth 相关）
        private val EXCLUDE_PATHS = setOf(
            "/auth/token"
        )
    }

    data class CacheEntry(
        val responseBody: ByteArray,
        val contentType: String?,
        val code: Int,
        val message: String,
        val timestamp: Long = System.currentTimeMillis()
    ) {
        fun isExpired(): Boolean {
            return System.currentTimeMillis() - timestamp > CACHE_DURATION_MS
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is CacheEntry) return false
            return responseBody.contentEquals(other.responseBody) &&
                    contentType == other.contentType &&
                    code == other.code &&
                    timestamp == other.timestamp
        }

        override fun hashCode(): Int {
            var result = responseBody.contentHashCode()
            result = 31 * result + (contentType?.hashCode() ?: 0)
            result = 31 * result + code
            result = 31 * result + timestamp.hashCode()
            return result
        }
    }

    private val cache = ConcurrentHashMap<String, CacheEntry>()

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val url = request.url.toString()
        val path = request.url.encodedPath

        // 只在 Debug 环境下启用缓存
        if (!BuildConfig.DEBUG) {
            return chain.proceed(request)
        }

        // 只缓存 GET 请求
        if (request.method != "GET") {
            Log.d(TAG, "⏭️ SKIP [${request.method}] $path (non-GET)")
            return chain.proceed(request)
        }

        // 排除特定路径
        if (EXCLUDE_PATHS.any { path.contains(it) }) {
            Log.d(TAG, "⏭️ SKIP $path (excluded)")
            return chain.proceed(request)
        }

        val cacheKey = buildCacheKey(request)

        // 检查缓存
        cache[cacheKey]?.let { entry ->
            if (!entry.isExpired()) {
                val ageSeconds = (System.currentTimeMillis() - entry.timestamp) / 1000
                val remainingSeconds = (CACHE_DURATION_MS - (System.currentTimeMillis() - entry.timestamp)) / 1000
                Log.d(TAG, "✅ HIT $path")
                Log.d(TAG, "   ├─ Age: ${ageSeconds}s")
                Log.d(TAG, "   ├─ Expires in: ${remainingSeconds}s")
                Log.d(TAG, "   └─ Size: ${entry.responseBody.size} bytes")
                return buildCachedResponse(request, entry)
            } else {
                Log.d(TAG, "⏰ EXPIRED $path (%.1f min old)".format(
                    (System.currentTimeMillis() - entry.timestamp) / 60000.0
                ))
                cache.remove(cacheKey)
            }
        }

        Log.d(TAG, "❌ MISS $path")

        // 发起请求
        val startTime = System.currentTimeMillis()
        val response = chain.proceed(request)
        val duration = System.currentTimeMillis() - startTime

        Log.d(TAG, "🌐 FETCH $path (${duration}ms, ${response.code})")

        // 只缓存成功的响应
        if (response.isSuccessful) {
            cacheResponse(cacheKey, response)?.let { cachedResponse ->
                return cachedResponse
            }
        } else {
            Log.d(TAG, "   └─ Not cached (code: ${response.code})")
        }

        return response
    }

    private fun buildCacheKey(request: Request): String {
        // Key = Method + URL（包含 query parameters）
        return "${request.method}:${request.url}"
    }

    private fun buildCachedResponse(request: Request, entry: CacheEntry): Response {
        val contentType = entry.contentType?.toMediaType()
        val body = entry.responseBody.toResponseBody(contentType)

        return Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(entry.code)
            .message(entry.message)
            .header("X-Cache", "HIT")
            .header("X-Cache-Age", "${(System.currentTimeMillis() - entry.timestamp) / 1000}s")
            .body(body)
            .build()
    }

    private fun cacheResponse(cacheKey: String, response: Response): Response? {
        return try {
            // 读取响应体
            val bodyBytes = response.body?.bytes() ?: return null
            val contentType = response.body?.contentType()?.toString()

            // 存入缓存
            val entry = CacheEntry(
                responseBody = bodyBytes,
                contentType = contentType,
                code = response.code,
                message = response.message
            )

            // 控制缓存大小
            if (cache.size >= MAX_CACHE_SIZE) {
                evictOldest()
            }

            cache[cacheKey] = entry

            val path = response.request.url.encodedPath
            Log.d(TAG, "💾 STORED $path")
            Log.d(TAG, "   ├─ Size: ${bodyBytes.size} bytes")
            Log.d(TAG, "   └─ Cache entries: ${cache.size}/$MAX_CACHE_SIZE")

            // 重建响应（因为原响应体已被读取）
            buildCachedResponse(response.request, entry)
        } catch (e: Exception) {
            Log.e(TAG, "❗ Cache store failed: ${e.message}")
            null
        }
    }

    private fun evictOldest() {
        // 找出最旧的条目并移除
        cache.entries
            .minByOrNull { it.value.timestamp }
            ?.let { oldest ->
                cache.remove(oldest.key)
            }
    }

    /**
     * 清除所有缓存
     */
    fun clearCache() {
        val size = cache.size
        cache.clear()
        Log.d(TAG, "🗑️ CLEAR ALL ($size entries removed)")
    }

    /**
     * 清除指定 URL 的缓存
     */
    fun invalidate(url: String) {
        val removed = cache.keys.filter { it.contains(url) }
        removed.forEach { cache.remove(it) }
        if (removed.isNotEmpty()) {
            Log.d(TAG, "🗑️ INVALIDATE $url (${removed.size} entries)")
        }
    }

    /**
     * 获取缓存统计信息
     */
    fun getStats(): String {
        val totalSize = cache.values.sumOf { it.responseBody.size }
        val oldestAge = cache.values.minOfOrNull { System.currentTimeMillis() - it.timestamp }?.let { it / 1000 } ?: 0
        val newestAge = cache.values.maxOfOrNull { System.currentTimeMillis() - it.timestamp }?.let { it / 1000 } ?: 0
        return """
            |📊 Cache Stats:
            |   ├─ Entries: ${cache.size}/$MAX_CACHE_SIZE
            |   ├─ Total size: ${totalSize / 1024} KB
            |   ├─ Oldest: ${oldestAge}s ago
            |   └─ Newest: ${newestAge}s ago
        """.trimMargin()
    }
}
