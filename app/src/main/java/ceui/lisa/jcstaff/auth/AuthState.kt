package ceui.lisa.jcstaff.auth

import ceui.lisa.jcstaff.network.User

sealed interface AuthState {
    data object Loading : AuthState
    data class Authenticated(val user: User?) : AuthState
    data object NotAuthenticated : AuthState
}

sealed interface LoginState {
    data object Idle : LoginState
    data object Loading : LoginState
    data class Success(val user: User?) : LoginState
    data class Error(val message: String) : LoginState
}