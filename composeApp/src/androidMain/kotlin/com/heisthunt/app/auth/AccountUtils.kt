package com.heisthunt.app.auth

import android.content.Context
import android.content.Intent
import android.provider.Settings

object AccountUtils {
    /**
     * Opens system settings to add a Google account
     */
    fun openAddAccountSettings(context: Context) {
        val intent = Intent(Settings.ACTION_ADD_ACCOUNT).apply {
            putExtra(Settings.EXTRA_ACCOUNT_TYPES, arrayOf("com.google"))
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}
