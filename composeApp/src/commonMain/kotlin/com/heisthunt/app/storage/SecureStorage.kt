package com.heisthunt.app.storage

expect class SecureStorage {
    fun saveString(key: String, value: String)
    fun getString(key: String): String?
    fun remove(key: String)
    fun clear()
}

object StorageKeys {
    const val ACCESS_TOKEN = "access_token"
    const val REFRESH_TOKEN = "refresh_token"
    const val USER_ID = "user_id"
    const val USER_EMAIL = "user_email"
    const val USER_NICKNAME = "user_nickname"
}
