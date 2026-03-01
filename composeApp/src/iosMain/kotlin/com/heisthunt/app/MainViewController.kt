package com.heisthunt.app

import androidx.compose.ui.window.ComposeUIViewController
import com.heisthunt.app.auth.GoogleAuthService
import com.heisthunt.app.di.AppModule
import com.heisthunt.app.storage.SecureStorage

fun MainViewController() = ComposeUIViewController {
    val secureStorage = SecureStorage()
    AppModule.initialize(secureStorage)

    // Create Google Auth Service
    val googleAuthService = GoogleAuthService()

    App(
        onGoogleLogin = suspend {
            println("🔵 [MainViewController] Google Sign-In started")
            try {
                val result = googleAuthService.signIn()
                println("🔵 [MainViewController] Sign-In result: success=${result.success}, email=${result.email}")
                if (result.success && result.idToken != null) {
                    println("✅ [MainViewController] Google Sign-In success, returning idToken")
                    result.idToken
                } else {
                    println("❌ [MainViewController] Google Sign-In failed: ${result.error}")
                    null
                }
            } catch (e: Exception) {
                println("❌ [MainViewController] Exception in onGoogleLogin: ${e.message}")
                null
            }
        }
    )
}
