package com.heisthunt.app.network

import com.heisthunt.shared.models.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object TokenStorage {
    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    private var _accessToken: String? = null
    private var _refreshToken: String? = null

    val accessToken: String? get() = _accessToken
    val refreshToken: String? get() = _refreshToken
    val isLoggedIn: Boolean get() = _accessToken != null
    val userId: String? get() = _currentUser.value?.id

    fun saveTokens(accessToken: String, refreshToken: String, user: User) {
        _accessToken = accessToken
        _refreshToken = refreshToken
        _currentUser.value = user
    }

    fun updateTokens(accessToken: String, refreshToken: String) {
        _accessToken = accessToken
        _refreshToken = refreshToken
    }

    fun clear() {
        _accessToken = null
        _refreshToken = null
        _currentUser.value = null
    }
}
