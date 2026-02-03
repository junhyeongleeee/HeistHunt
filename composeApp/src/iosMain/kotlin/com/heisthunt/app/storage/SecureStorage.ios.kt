package com.heisthunt.app.storage

import platform.Foundation.NSUserDefaults

actual class SecureStorage {
    private val userDefaults = NSUserDefaults.standardUserDefaults

    actual fun saveString(key: String, value: String) {
        userDefaults.setObject(value, forKey = key)
    }

    actual fun getString(key: String): String? {
        return userDefaults.stringForKey(key)
    }

    actual fun remove(key: String) {
        userDefaults.removeObjectForKey(key)
    }

    actual fun clear() {
        // Clear all app-specific keys
        listOf(
            StorageKeys.ACCESS_TOKEN,
            StorageKeys.REFRESH_TOKEN,
            StorageKeys.USER_ID,
            StorageKeys.USER_EMAIL,
            StorageKeys.USER_NICKNAME
        ).forEach { key ->
            userDefaults.removeObjectForKey(key)
        }
    }
}
