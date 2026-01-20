package ceui.lisa.jcstaff.network

import android.util.Log
import ceui.lisa.jcstaff.BuildConfig
import ceui.lisa.jcstaff.cache.ApiCacheManager
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody

/**
 * API 缓存拦截器
 *
 * 仅在 Debug 环境下生效，使用 Room 数据库持久化缓存
 * 同一 API 在 15 分钟内返回缓存结果
 */
class ApiCacheInterceptor : Interceptor {

    companion object {
        private const val TAG = "ApiCache"

        // 不缓存的路径
        private val EXCLUDE_PATHS = setOf(
            "/auth/token"
        )
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val path = request.url.encodedPath

        // 只在 Debug 环境下启用缓存
        if (!BuildConfig.DEBUG) {
            return chain.proceed(request)
        }

        // 只缓存 GET 请求
        if (request.method != "GET") {
            Log.d(TAG, "⏭️ SKIP [${request.method}] $path")
            return chain.proceed(request)
        }

        // 排除特定路径
        if (EXCLUDE_PATHS.any { path.contains(it) }) {
            Log.d(TAG, "⏭️ SKIP $path (excluded)")
            return chain.proceed(request)
        }

        val cacheKey = buildCacheKey(request)

        // 检查缓存
        ApiCacheManager.getSync(cacheKey)?.let { cached ->
            return buildCachedResponse(request, cached.responseBody, cached.contentType, cached.httpCode, cached.httpMessage, cached.timestamp)
        }

        Log.d(TAG, "❌ MISS $path")

        // 发起请求
        val startTime = System.currentTimeMillis()
        val response = chain.proceed(request)
        val duration = System.currentTimeMillis() - startTime

        Log.d(TAG, "🌐 FETCH $path (${duration}ms, ${response.code})")

        // 只缓存成功的响应
        if (response.isSuccessful) {
            return cacheAndRebuildResponse(cacheKey, response)
        } else {
            Log.d(TAG, "   └─ Not cached (code: ${response.code})")
        }

        return response
    }

    private fun buildCacheKey(request: Request): String {
        return "${request.method}:${request.url}"
    }

    private fun buildCachedResponse(
        request: Request,
        body: ByteArray,
        contentType: String?,
        code: Int,
        message: String,
        timestamp: Long
    ): Response {
        val mediaType = contentType?.toMediaType()
        val responseBody = body.toResponseBody(mediaType)
        val ageSeconds = (System.currentTimeMillis() - timestamp) / 1000

        return Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(code)
            .message(message)
            .header("X-Cache", "HIT")
            .header("X-Cache-Age", "${ageSeconds}s")
            .body(responseBody)
            .build()
    }

    private fun cacheAndRebuildResponse(cacheKey: String, response: Response): Response {
        return try {
            val bodyBytes = response.body?.bytes() ?: return response
            val contentType = response.body?.contentType()?.toString()

            // 存入缓存
            ApiCacheManager.putSync(
                key = cacheKey,
                responseBody = bodyBytes,
                contentType = contentType,
                httpCode = response.code,
                httpMessage = response.message
            )

            // 重建响应
            val mediaType = contentType?.toMediaType()
            Response.Builder()
                .request(response.request)
                .protocol(response.protocol)
                .code(response.code)
                .message(response.message)
                .headers(response.headers)
                .body(bodyBytes.toResponseBody(mediaType))
                .build()
        } catch (e: Exception) {
            Log.e(TAG, "❗ Cache failed: ${e.message}")
            response
        }
    }
}
