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
            if (tokenStorage.isLoggedIn) {
                // Try to validate token
                val result = authRepository.validateToken()
                if (result.isSuccess) {
                    _uiState.value = AuthUiState(
                        isLoggedIn = true,
                        user = authRepository.getCurrentUser()
                    )
                } else {
                    // Token expired, logout
                    logout()
                }
            }
        }
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
