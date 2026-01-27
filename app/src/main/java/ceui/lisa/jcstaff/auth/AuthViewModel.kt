package ceui.lisa.jcstaff.auth

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.net.toUri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import ceui.lisa.jcstaff.network.PixivClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val context: Context = application

    /**
     * 是否是"添加账号"流程（vs 首次登录）
     */
    private var isAddAccountFlow = false

    private val _loginState = MutableStateFlow<LoginState>(LoginState.Idle)
    val loginState: StateFlow<LoginState> = _loginState.asStateFlow()

    // AccountRegistry must be initialized synchronously before flow properties are accessed
    init {
        AccountRegistry.initialize(application)
    }

    /**
     * 所有已注册账号
     */
    val allAccounts: StateFlow<List<AccountEntry>> = AccountRegistry.allAccounts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /**
     * 当前活跃用户 ID
     */
    val activeUserId: StateFlow<Long?> = AccountRegistry.activeUserId
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    /**
     * 认证状态：基于当前活跃会话的 AuthRepository
     */
    private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    init {
        viewModelScope.launch {
            // 执行迁移
            AccountMigration.migrateIfNeeded(application)

            // 获取活跃用户
            val activeId = AccountRegistry.getActiveUserId()
            if (activeId != null) {
                // 初始化会话
                AccountSessionManager.initializeSession(application, activeId)
                refreshAuthState()
            } else {
                _authState.value = AuthState.NotAuthenticated
            }
        }
    }

    /**
     * 刷新认证状态（从当前会话的 AuthRepository 读取）
     */
    private suspend fun refreshAuthState() {
        val authRepo = AccountSessionManager.getCurrentAuthRepository()
        if (authRepo == null) {
            _authState.value = AuthState.NotAuthenticated
            return
        }

        try {
            authRepo.isLoggedIn.combine(authRepo.currentUser) { isLoggedIn, user ->
                if (isLoggedIn) AuthState.Authenticated(user) else AuthState.NotAuthenticated
            }.collect { state ->
                _authState.value = state
            }
        } catch (e: Exception) {
            _authState.value = AuthState.NotAuthenticated
        }
    }

    fun getLoginUrl(): String = PixivClient.getLoginUrl()

    fun getSignupUrl(): String = PixivClient.getSignupUrl()

    /**
     * 使用 Chrome Custom Tab 启动登录流程
     */
    fun launchLogin(context: Context, isSignup: Boolean = false) {
        isAddAccountFlow = false
        val url = if (isSignup) getSignupUrl() else getLoginUrl()
        val customTabsIntent = CustomTabsIntent.Builder()
            .setShowTitle(true)
            .build()
        customTabsIntent.launchUrl(context, url.toUri())
    }

    /**
     * 启动添加账号流程
     */
    fun launchAddAccount(context: Context) {
        isAddAccountFlow = true
        // 重置 PKCE 以确保新的 code challenge
        PixivClient.resetPkce()
        val url = getLoginUrl()
        val customTabsIntent = CustomTabsIntent.Builder()
            .setShowTitle(true)
            .build()
        customTabsIntent.launchUrl(context, url.toUri())
    }

    fun handleCallback(uri: Uri) {
        viewModelScope.launch {
            _loginState.value = LoginState.Loading

            if (isAddAccountFlow) {
                handleAddAccountCallback(uri)
            } else {
                handleFirstLoginCallback(uri)
            }
        }
    }

    private suspend fun handleFirstLoginCallback(uri: Uri) {
        // 使用临时 AuthRepository 进行登录
        val tempRepo = AuthRepository(context, null)
        tempRepo.loginWithCode(uri)
            .onSuccess { account ->
                // 将登录结果注册为新账号并切换
                AccountSessionManager.addAccountAndSwitch(context, account)
                _loginState.value = LoginState.Success(account.user)
                refreshAuthState()
            }
            .onFailure { error ->
                _loginState.value = LoginState.Error(error.message ?: "Login failed")
            }
    }

    private suspend fun handleAddAccountCallback(uri: Uri) {
        // 使用临时 AuthRepository 进行登录
        val tempRepo = AuthRepository(context, null)
        tempRepo.loginWithCode(uri)
            .onSuccess { account ->
                // 添加新账号并切换
                AccountSessionManager.addAccountAndSwitch(context, account)
                _loginState.value = LoginState.Success(account.user)
                refreshAuthState()
            }
            .onFailure { error ->
                _loginState.value = LoginState.Error(error.message ?: "Login failed")
            }
        isAddAccountFlow = false
    }

    /**
     * 切换到指定账号
     */
    fun switchAccount(userId: Long) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            AccountSessionManager.switchAccount(context, userId)
            refreshAuthState()
        }
    }

    /**
     * 移除指定账号
     */
    fun removeAccount(userId: Long) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            val hasRemaining = AccountSessionManager.removeAccount(context, userId)
            if (hasRemaining) {
                refreshAuthState()
            } else {
                _authState.value = AuthState.NotAuthenticated
                _loginState.value = LoginState.Idle
            }
        }
    }

    fun refreshToken() {
        viewModelScope.launch {
            AccountSessionManager.getCurrentAuthRepository()?.refreshAccessToken()
        }
    }

    fun logout() {
        viewModelScope.launch {
            val activeId = AccountRegistry.getActiveUserId()
            if (activeId != null) {
                removeAccount(activeId)
            }
        }
    }

    fun resetLoginState() {
        _loginState.value = LoginState.Idle
    }
}
