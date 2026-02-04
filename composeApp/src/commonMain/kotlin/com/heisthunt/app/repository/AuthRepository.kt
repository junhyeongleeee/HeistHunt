package com.heisthunt.app.repository

import com.heisthunt.app.network.ApiClient
import com.heisthunt.app.network.TokenStorage
import com.heisthunt.shared.dto.LoginRequest
import com.heisthunt.shared.dto.RegisterRequest
import com.heisthunt.shared.models.User

class AuthRepository(
    private val apiClient: ApiClient,
    private val tokenStorage: TokenStorage
) {

    suspend fun register(email: String, password: String, nickname: String): Result<User> {
        return try {
            val response = apiClient.register(RegisterRequest(email, password, nickname))
            val authData = response.data
            if (response.success && authData != null) {
                tokenStorage.saveTokens(authData.accessToken, authData.refreshToken, authData.user)
                apiClient.setTokens(authData.accessToken, authData.refreshToken)
                Result.success(authData.user)
            } else {
                Result.failure(Exception(response.error?.message ?: "Registration failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun login(email: String, password: String): Result<User> {
        return try {
            val response = apiClient.login(LoginRequest(email, password))
            val authData = response.data
            if (response.success && authData != null) {
                tokenStorage.saveTokens(authData.accessToken, authData.refreshToken, authData.user)
                apiClient.setTokens(authData.accessToken, authData.refreshToken)
                Result.success(authData.user)
            } else {
                Result.failure(Exception(response.error?.message ?: "Login failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun googleLogin(email: String): Result<User> {
        return try {
            val response = apiClient.googleLogin(email)
            val authData = response.data
            if (response.success && authData != null) {
                tokenStorage.saveTokens(authData.accessToken, authData.refreshToken, authData.user)
                apiClient.setTokens(authData.accessToken, authData.refreshToken)
                Result.success(authData.user)
            } else {
                Result.failure(Exception(response.error?.message ?: "Google login failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun refreshTokens(): Result<Unit> {
        return try {
            val response = apiClient.refreshTokens()
            val tokenData = response.data
            if (response.success && tokenData != null) {
                tokenStorage.updateTokens(tokenData.accessToken, tokenData.refreshToken)
                apiClient.setTokens(tokenData.accessToken, tokenData.refreshToken)
                Result.success(Unit)
            } else {
                Result.failure(Exception(response.error?.message ?: "Token refresh failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun validateToken(): Result<Unit> {
        return try {
            val response = apiClient.getMe()
            if (response.success) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Token validation failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun logout() {
        tokenStorage.clear()
        apiClient.clearTokens()
    }

    fun isLoggedIn(): Boolean = tokenStorage.isLoggedIn

    fun getCurrentUser(): User? = tokenStorage.currentUser.value
}
