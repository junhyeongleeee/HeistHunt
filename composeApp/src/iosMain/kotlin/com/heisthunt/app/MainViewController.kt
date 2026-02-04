package com.heisthunt.app

import androidx.compose.ui.window.ComposeUIViewController
import com.heisthunt.app.auth.GoogleAuthService
import com.heisthunt.app.di.AppModule
import com.heisthunt.app.storage.SecureStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext

fun MainViewController() = ComposeUIViewController {
    val secureStorage = SecureStorage()
    AppModule.initialize(secureStorage)

    // Create Google Auth Service
    val googleAuthService = GoogleAuthService()

    App(
        onGoogleLogin = suspend {
            println("🔵 iOS Google Sign-In button clicked")
            val result = googleAuthService.signIn()

            if (result.success && result.email != null) {
                println("✅ iOS Google Sign-In success: ${result.email}")

                // Authenticate with backend server
                try {
                    val authResult = withContext(Dispatchers.IO) {
                        AppModule.authRepository.googleLogin(result.email)
                    }

                    authResult.fold(
                        onSuccess = { user ->
                            println("✅ Backend authentication success: ${user.nickname}")
                            true
                        },
                        onFailure = { error ->
                            println("❌ Backend authentication failed: ${error.message}")
                            false
                        }
                    )
                } catch (e: Exception) {
                    println("❌ Exception during backend authentication: ${e.message}")
                    false
                }
            } else {
                println("❌ iOS Google Sign-In failed: ${result.error}")
                false
            }
        }
    )
}
