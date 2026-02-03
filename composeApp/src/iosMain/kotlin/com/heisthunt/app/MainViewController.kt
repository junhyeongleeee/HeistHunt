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
            println("🔵 iOS Google Sign-In button clicked")
            val result = googleAuthService.signIn()

            if (result.success) {
                println("✅ iOS Google Sign-In success: ${result.email}")
                // Store user info if needed
                true
            } else {
                println("❌ iOS Google Sign-In failed: ${result.error}")
                false
            }
        }
    )
}
