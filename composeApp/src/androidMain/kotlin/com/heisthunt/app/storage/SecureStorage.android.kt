package com.heisthunt.app.storage

import android.content.Context

actual class SecureStorage(private val context: Context) {
    private val sharedPreferences = context.getSharedPreferences(
        "heisthunt_secure_prefs",
        Context.MODE_PRIVATE
    )

    actual fun saveString(key: String, value: String) {
        sharedPreferences.edit().putString(key, value).apply()
    }

    actual fun getString(key: String): String? {
        return sharedPreferences.getString(key, null)
    }

    actual fun remove(key: String) {
        sharedPreferences.edit().remove(key).apply()
    }

    actual fun clear() {
        sharedPreferences.edit().clear().apply()
    }
}
