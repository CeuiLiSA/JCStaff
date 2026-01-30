package ceui.lisa.jcstaff.network

import android.util.Log
import ceui.lisa.jcstaff.BuildConfig
import ceui.lisa.jcstaff.cache.ApiCacheManager
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody

/**
 * API 缓存拦截器（只写模式）
 *
 * 仅在 Debug 环境下生效，使用 Room 数据库持久化缓存
 * 此拦截器只负责将成功的响应写入缓存，不负责读取缓存
 * 缓存读取由 PagedDataLoader 通过 ApiCacheManager 控制
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
            return chain.proceed(request)
        }

        // 排除特定路径
        if (EXCLUDE_PATHS.any { path.contains(it) }) {
            return chain.proceed(request)
        }

        val cacheKey = buildCacheKey(request)

        // 发起请求
        val startTime = System.currentTimeMillis()
        val response = chain.proceed(request)
        val duration = System.currentTimeMillis() - startTime

        Log.d(TAG, "🌐 FETCH $path (${duration}ms, ${response.code})")

        // 只缓存成功的响应
        if (response.isSuccessful) {
            return cacheAndRebuildResponse(cacheKey, response)
        }

        return response
    }

    private fun buildCacheKey(request: Request): String {
        return "${request.method}:${request.url}"
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

            Log.d(TAG, "💾 STORED ${response.request.url.encodedPath}")

            // 重建响应（因为 body 只能读一次）
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
            Log.e(TAG, "❗ Cache write failed: ${e.message}")
            response
        }
    }
}
