package ceui.lisa.jcstaff.network

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
 * 4. 用新 token 重试请求
 *
 * 注意：这是 Application Interceptor，在所有其他拦截器之前执行
 */
class TokenRefreshInterceptor : Interceptor {

    companion object {
        private const val MAX_RETRY = 1
    }

    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        var response = chain.proceed(originalRequest)

        // 只处理 400 和 401 错误
        if (response.code != 400 && response.code != 401) {
            return response
        }

        // 读取响应体检查是否是 token 过期错误
        val responseBody = response.peekBody(Long.MAX_VALUE).string()

        if (!TokenManager.isTokenExpiredError(responseBody)) {
            return response
        }

        // 关闭原响应
        response.close()

        // 尝试刷新 token
        val newToken = TokenManager.refreshTokenSync()

        if (newToken == null) {
            // 刷新失败，返回原始错误响应
            // 需要重新创建响应因为原响应已关闭
            return response.newBuilder()
                .body(responseBody.toResponseBody(response.body?.contentType()))
                .build()
        }

        // 用新 token 重试请求
        val newRequest = originalRequest.newBuilder()
            .header("Authorization", "Bearer $newToken")
            .build()

        return chain.proceed(newRequest)
    }
}
