package ceui.lisa.jcstaff.network

import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * Token 管理器
 *
 * 设计要点：
 * 1. 单例模式，全局管理 token
 * 2. 使用 Mutex 确保同一时刻只有一个刷新操作
 * 3. 使用 Deferred 让多个等待者共享同一个刷新结果
 * 4. 支持外部注入刷新逻辑，解耦 PixivClient
 */
object TokenManager {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutex = Mutex()

    @Volatile
    private var currentToken: String? = null

    @Volatile
    private var refreshToken: String? = null

    @Volatile
    private var pendingRefresh: Deferred<String?>? = null

    // 刷新 token 的回调，由外部注入
    private var refreshCallback: (suspend (String) -> TokenResult)? = null

    /**
     * Token 刷新结果
     */
    data class TokenResult(
        val accessToken: String?,
        val refreshToken: String?,
        val success: Boolean,
        val error: String? = null
    )

    /**
     * 设置 token 信息
     */
    fun setTokens(accessToken: String?, refreshToken: String?) {
        this.currentToken = accessToken
        this.refreshToken = refreshToken
    }

    /**
     * 获取当前 access token
     */
    fun getAccessToken(): String? = currentToken

    /**
     * 设置刷新回调
     */
    fun setRefreshCallback(callback: suspend (String) -> TokenResult) {
        this.refreshCallback = callback
    }

    /**
     * 清除所有 token
     */
    fun clear() {
        currentToken = null
        refreshToken = null
        pendingRefresh = null
    }

    /**
     * 刷新 token（挂起版本）
     */
    suspend fun refreshTokenSuspend(): String? {
        // 快速路径：检查是否有正在进行的刷新
        pendingRefresh?.let { pending ->
            return pending.await()
        }

        return mutex.withLock {
            // 双重检查：获取锁后再次检查
            pendingRefresh?.let { pending ->
                return@withLock pending.await()
            }

            val currentRefreshToken = refreshToken
            if (currentRefreshToken == null) {
                return@withLock null
            }

            val callback = refreshCallback
            if (callback == null) {
                return@withLock null
            }

            // 启动刷新操作
            val deferred = scope.async {
                try {
                    val result = callback(currentRefreshToken)
                    if (result.success && result.accessToken != null) {
                        currentToken = result.accessToken
                        refreshToken = result.refreshToken
                        result.accessToken
                    } else {
                        null
                    }
                } catch (e: Exception) {
                    null
                } finally {
                    // 刷新完成后清除 pending 状态
                    pendingRefresh = null
                }
            }

            pendingRefresh = deferred
            deferred.await()
        }
    }

    /**
     * 检查错误是否是 token 过期
     */
    fun isTokenExpiredError(responseBody: String?): Boolean {
        if (responseBody == null) return false
        return responseBody.contains("invalid_grant") ||
               responseBody.contains("Invalid refresh token") ||
               responseBody.contains("Error occurred at the OAuth process")
    }
}
