package com.heisthunt.app.network

import com.heisthunt.app.storage.SecureStorage
import com.heisthunt.app.storage.StorageKeys
import com.heisthunt.shared.models.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class TokenStorage(private val secureStorage: SecureStorage) {
    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    var accessToken: String?
        get() = secureStorage.getString(StorageKeys.ACCESS_TOKEN)
        set(value) {
            if (value != null) {
                secureStorage.saveString(StorageKeys.ACCESS_TOKEN, value)
            } else {
                secureStorage.remove(StorageKeys.ACCESS_TOKEN)
            }
        }

    var refreshToken: String?
        get() = secureStorage.getString(StorageKeys.REFRESH_TOKEN)
        set(value) {
            if (value != null) {
                secureStorage.saveString(StorageKeys.REFRESH_TOKEN, value)
            } else {
                secureStorage.remove(StorageKeys.REFRESH_TOKEN)
            }
        }

    val isLoggedIn: Boolean get() = accessToken != null && userId != null
    val userId: String? get() = secureStorage.getString(StorageKeys.USER_ID)

    fun saveTokens(accessToken: String, refreshToken: String, user: User) {
        this.accessToken = accessToken
        this.refreshToken = refreshToken
        secureStorage.saveString(StorageKeys.USER_ID, user.id)
        secureStorage.saveString(StorageKeys.USER_EMAIL, user.email)
        secureStorage.saveString(StorageKeys.USER_NICKNAME, user.nickname)
        _currentUser.value = user
    }

    fun updateTokens(accessToken: String, refreshToken: String) {
        this.accessToken = accessToken
        this.refreshToken = refreshToken
    }

    fun clear() {
        secureStorage.clear()
        _currentUser.value = null
    }

    fun loadUser() {
        val userId = secureStorage.getString(StorageKeys.USER_ID)
        val email = secureStorage.getString(StorageKeys.USER_EMAIL)
        val nickname = secureStorage.getString(StorageKeys.USER_NICKNAME)

        if (userId != null && email != null && nickname != null) {
            _currentUser.value = User(
                id = userId,
                email = email,
                nickname = nickname,
                createdAt = kotlinx.datetime.Clock.System.now(),
                updatedAt = kotlinx.datetime.Clock.System.now()
            )
        }
    }
}
