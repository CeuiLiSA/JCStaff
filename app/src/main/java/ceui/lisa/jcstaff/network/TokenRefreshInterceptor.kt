package ceui.lisa.jcstaff.network

import android.util.Log
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import java.io.IOException

/**
 * Token 自动刷新拦截器
 *
 * 工作流程：
 * 1. 发起请求
 * 2. 如果响应是 400/401 且包含 token 过期错误
 * 3. 调用 TokenManager 刷新 token（自动处理并发）
 * 4. 重试原始请求（HeaderInterceptor 会自动使用新 token）
 *
 * 注意：这是 Application Interceptor，位于 HeaderInterceptor 之前
 */
class TokenRefreshInterceptor : Interceptor {

    companion object {
        private const val TAG = "TokenRefresh"
        private const val MAX_RETRY = 1
    }

    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        var response = chain.proceed(originalRequest)
        var retryCount = 0

        while (retryCount < MAX_RETRY) {
            // 只处理 400 和 401 错误
            if (response.code != 400 && response.code != 401) {
                return response
            }

            // 读取响应体检查是否是 token 过期错误（限制 1MB 防止 OOM）
            val responseBody = response.peekBody(1024 * 1024).string()
            val contentType = response.body?.contentType()

            if (!TokenManager.isTokenExpiredError(responseBody)) {
                return response
            }

            Log.d(TAG, "Token expired, attempting refresh (retry $retryCount)")

            // 关闭原响应
            response.close()

            // 尝试刷新 token
            val newToken = runBlocking { TokenManager.refreshTokenSuspend() }

            if (newToken == null) {
                Log.e(TAG, "Token refresh failed")
                // 刷新失败，用保存的 body 和 contentType 构建新响应
                return response.newBuilder()
                    .body(responseBody.toResponseBody(contentType))
                    .build()
            }

            Log.d(TAG, "Token refreshed, retrying request: ${originalRequest.url.encodedPath}")

            // 重试原始请求
            // HeaderInterceptor 会调用 TokenManager.getAccessToken() 获取新 token
            response = chain.proceed(originalRequest)
            retryCount++
        }

        return response
    }
}
