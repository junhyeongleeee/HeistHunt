package com.heisthunt.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.heisthunt.app.network.TokenStorage
import com.heisthunt.app.repository.AuthRepository
import com.heisthunt.shared.models.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AuthUiState(
    val isLoading: Boolean = false,
    val user: User? = null,
    val error: String? = null,
    val isLoggedIn: Boolean = false
)

class AuthViewModel(
    private val authRepository: AuthRepository,
    private val tokenStorage: TokenStorage
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        checkAutoLogin()
    }

    private fun checkAutoLogin() {
        viewModelScope.launch {
            println("🔐 [AuthViewModel] Checking auto-login, isLoggedIn=${tokenStorage.isLoggedIn}")
            if (tokenStorage.isLoggedIn) {
                val result = authRepository.validateToken()
                if (result.isSuccess) {
                    println("✅ [AuthViewModel] Token valid, auto-login success")
                    _uiState.value = AuthUiState(
                        isLoggedIn = true,
                        user = authRepository.getCurrentUser()
                    )
                } else {
                    println("⚠️ [AuthViewModel] Token invalid, attempting refresh before logout")
                    val refreshResult = authRepository.refreshTokens()
                    if (refreshResult.isSuccess) {
                        println("✅ [AuthViewModel] Token refreshed successfully, auto-login")
                        _uiState.value = AuthUiState(
                            isLoggedIn = true,
                            user = authRepository.getCurrentUser()
                        )
                    } else {
                        println("❌ [AuthViewModel] Token refresh failed, logging out")
                        logout()
                    }
                }
            } else {
                println("ℹ️ [AuthViewModel] No stored tokens, waiting for login")
            }
        }
    }

    fun googleLogin(idToken: String) {
        viewModelScope.launch {
            println("🔵 [AuthViewModel] Google login started")
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            authRepository.googleLogin(idToken)
                .onSuccess { user ->
                    println("✅ [AuthViewModel] Google login success: ${user.nickname}")
                    _uiState.value = AuthUiState(isLoading = false, user = user, isLoggedIn = true)
                }
                .onFailure { exception ->
                    println("❌ [AuthViewModel] Google login failed: ${exception.message}")
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = exception.message ?: "Google login failed"
                    )
                }
        }
    }

    fun forceLogout() {
        println("🔒 [AuthViewModel] Force logout triggered by expired session")
        logout()
    }

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            authRepository.login(email, password)
                .onSuccess { user ->
                    _uiState.value = AuthUiState(
                        isLoading = false,
                        user = user,
                        isLoggedIn = true
                    )
                }
                .onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = exception.message ?: "Login failed"
                    )
                }
        }
    }

    fun register(email: String, password: String, nickname: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            authRepository.register(email, password, nickname)
                .onSuccess { user ->
                    _uiState.value = AuthUiState(
                        isLoading = false,
                        user = user,
                        isLoggedIn = true
                    )
                }
                .onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = exception.message ?: "Registration failed"
                    )
                }
        }
    }

    fun logout() {
        authRepository.logout()
        _uiState.value = AuthUiState()
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
