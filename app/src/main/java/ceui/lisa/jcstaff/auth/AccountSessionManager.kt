package ceui.lisa.jcstaff.auth

import android.content.Context
import android.util.Log
import ceui.lisa.jcstaff.cache.ApiCacheManager
import ceui.lisa.jcstaff.cache.AppDatabase
import ceui.lisa.jcstaff.cache.BrowseHistoryManager
import ceui.lisa.jcstaff.core.LoadTaskManager
import ceui.lisa.jcstaff.core.ObjectStore
import ceui.lisa.jcstaff.core.ScrollPositionStore
import ceui.lisa.jcstaff.core.SettingsStore
import ceui.lisa.jcstaff.network.AccountResponse
import ceui.lisa.jcstaff.network.PixivClient

/**
 * 账号会话生命周期协调器
 *
 * 负责：
 * - 初始化用户会话（per-user DB、DataStore、缓存等）
 * - 拆卸当前会话
 * - 切换账号
 * - 添加新账号并切换
 * - 移除账号
 */
object AccountSessionManager {

    private const val TAG = "AccountSession"

    private var currentAuthRepository: AuthRepository? = null

    fun getCurrentAuthRepository(): AuthRepository? = currentAuthRepository

    /**
     * 初始化用户会话
     */
    suspend fun initializeSession(context: Context, userId: Long) {
        Log.d(TAG, "Initializing session for user $userId")

        // 1. 创建 per-user AuthRepository 并初始化 token
        val authRepo = AuthRepository(context, userId)
        currentAuthRepository = authRepo
        authRepo.initialize()

        // 2. 初始化 per-user database (close old first)
        AppDatabase.closeInstance()
        AppDatabase.getInstanceForUser(context, userId)

        // 3. 初始化 per-user 缓存管理器
        ApiCacheManager.reset()
        ApiCacheManager.initialize(context, userId)

        // 4. 初始化 per-user 浏览历史
        BrowseHistoryManager.reset()
        BrowseHistoryManager.initialize(context, userId)

        // 5. 初始化 per-user 设置
        SettingsStore.initialize(context, userId)

        // 6. 初始化 per-user 加载任务管理器
        LoadTaskManager.init(context, userId)

        // 7. 清除内存缓存
        ObjectStore.clear()
        ScrollPositionStore.clear()

        Log.d(TAG, "Session initialized for user $userId")
    }

    /**
     * 拆卸当前会话
     */
    fun teardownCurrentSession() {
        Log.d(TAG, "Tearing down current session")

        // 关闭数据库
        AppDatabase.closeInstance()

        // 重置各管理器
        ApiCacheManager.reset()
        BrowseHistoryManager.reset()
        SettingsStore.reset()
        LoadTaskManager.resetInMemoryState()

        // 清除内存缓存
        ObjectStore.clear()
        ScrollPositionStore.clear()

        // 重置网络客户端
        PixivClient.resetClient()

        currentAuthRepository = null

        Log.d(TAG, "Session torn down")
    }

    /**
     * 切换账号
     */
    suspend fun switchAccount(context: Context, userId: Long) {
        Log.d(TAG, "Switching to account $userId")
        teardownCurrentSession()
        AccountRegistry.setActiveAccount(userId)
        initializeSession(context, userId)
    }

    /**
     * 添加新账号并切换到该账号
     */
    suspend fun addAccountAndSwitch(context: Context, accountResponse: AccountResponse) {
        val user = accountResponse.user ?: return
        val entry = AccountEntry(
            userId = user.id,
            userName = user.name ?: "",
            userAccount = user.account ?: "",
            avatarUrl = user.profile_image_urls?.findAvatarUrl()
        )

        // 注册到账号列表
        AccountRegistry.addAccount(entry)

        // 为新账号创建 AuthRepository 并保存 token
        val newAuthRepo = AuthRepository(context, user.id)
        newAuthRepo.saveAccount(accountResponse)

        // 切换到新账号
        switchAccount(context, user.id)
    }

    /**
     * 移除账号
     * 返回 true 如果还有其他账号, false 如果没有账号了
     */
    suspend fun removeAccount(context: Context, userId: Long): Boolean {
        Log.d(TAG, "Removing account $userId")

        val accounts = AccountRegistry.getAll()
        val activeId = AccountRegistry.getActiveUserId()

        // 清除该账号的数据文件
        AccountCleanup(context, userId).deleteAccountData()

        // 从注册表中移除
        AccountRegistry.removeAccount(userId)

        val remaining = accounts.filter { it.userId != userId }

        if (remaining.isEmpty()) {
            // 没有账号了
            teardownCurrentSession()
            return false
        }

        // 如果移除的是当前活跃账号，切换到另一个
        if (activeId == userId) {
            switchAccount(context, remaining.first().userId)
        }

        return true
    }
}
