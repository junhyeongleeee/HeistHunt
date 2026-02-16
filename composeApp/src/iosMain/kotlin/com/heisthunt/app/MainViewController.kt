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
            println("🔵 [MainViewController] onGoogleLogin lambda started")
            try {
                println("🔵 [MainViewController] Calling googleAuthService.signIn()")
                val result = googleAuthService.signIn()
                println("🔵 [MainViewController] googleAuthService.signIn() returned: success=${result.success}, email=${result.email}")

                if (result.success && result.idToken != null) {
                    println("✅ [MainViewController] Google Sign-In success: ${result.email}")
                    println("🔵 [MainViewController] ID Token: ${result.idToken.take(20)}...")

                    // Authenticate with backend server
                    try {
                        println("🔵 [MainViewController] Starting backend authentication")
                        val authResult = withContext(Dispatchers.IO) {
                            AppModule.authRepository.googleLogin(result.idToken)
                        }
                        println("🔵 [MainViewController] Backend call completed")

                        val finalResult = authResult.fold(
                            onSuccess = { user ->
                                println("✅ [MainViewController] Backend authentication success: ${user.nickname}")
                                true
                            },
                            onFailure = { error ->
                                println("❌ [MainViewController] Backend authentication failed: ${error.message}")
                                false
                            }
                        )
                        println("🔵 [MainViewController] Returning: $finalResult")
                        finalResult
                    } catch (e: Exception) {
                        println("❌ [MainViewController] Exception during backend authentication: ${e.message}")
                        e.printStackTrace()
                        false
                    }
                } else {
                    println("❌ [MainViewController] Google Sign-In failed: ${result.error}")
                    false
                }
            } catch (e: Exception) {
                println("❌ [MainViewController] Exception in onGoogleLogin: ${e.message}")
                e.printStackTrace()
                false
            }
        }
    )
}
