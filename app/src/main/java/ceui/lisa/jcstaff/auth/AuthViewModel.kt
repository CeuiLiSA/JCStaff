package ceui.lisa.jcstaff.auth

import android.app.Application
import android.net.Uri
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

    private val authRepository = AuthRepository(application)

    private val _loginState = MutableStateFlow<LoginState>(LoginState.Idle)
    val loginState: StateFlow<LoginState> = _loginState.asStateFlow()

    val authState: StateFlow<AuthState> = combine(
        authRepository.isLoggedIn,
        authRepository.currentUser
    ) { isLoggedIn, user ->
        if (isLoggedIn) AuthState.Authenticated(user) else AuthState.NotAuthenticated
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AuthState.Loading
    )

    init {
        viewModelScope.launch {
            authRepository.initialize()
        }
    }

    fun getLoginUrl(): String = PixivClient.getLoginUrl()

    fun getSignupUrl(): String = PixivClient.getSignupUrl()

    fun handleCallback(uri: Uri) {
        viewModelScope.launch {
            _loginState.value = LoginState.Loading

            authRepository.loginWithCode(uri)
                .onSuccess { account ->
                    _loginState.value = LoginState.Success(account.user)
                }
                .onFailure { error ->
                    _loginState.value = LoginState.Error(error.message ?: "Login failed")
                }
        }
    }

    fun refreshToken() {
        viewModelScope.launch {
            authRepository.refreshAccessToken()
        }
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
            _loginState.value = LoginState.Idle
        }
    }

    fun resetLoginState() {
        _loginState.value = LoginState.Idle
    }
}