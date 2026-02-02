package com.heisthunt.app

import android.app.AlertDialog
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.heisthunt.app.auth.AccountUtils
import com.heisthunt.app.auth.GoogleAuthService
import com.heisthunt.app.di.AppModule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    private lateinit var googleAuthService: GoogleAuthService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        googleAuthService = GoogleAuthService(applicationContext)

        setContent {
            App(
                onGoogleLogin = suspend {
                    android.util.Log.d("MainActivity", "Google login started")
                    val result = googleAuthService.signIn()
                    android.util.Log.d("MainActivity", "Google login result: success=${result.success}, error=${result.error}")
                    if (result.success && result.email != null) {
                        // Authenticate with backend server
                        try {
                            val authResult = withContext(Dispatchers.IO) {
                                AppModule.authRepository.googleLogin(result.email)
                            }

                            authResult.fold(
                                onSuccess = { user ->
                                    Toast.makeText(
                                        this,
                                        "로그인 성공: ${user.nickname}",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    true
                                },
                                onFailure = { error ->
                                    Toast.makeText(
                                        this,
                                        "서버 로그인 실패: ${error.message}",
                                        Toast.LENGTH_LONG
                                    ).show()
                                    false
                                }
                            )
                        } catch (e: Exception) {
                            Toast.makeText(
                                this,
                                "서버 연결 실패: ${e.message}",
                                Toast.LENGTH_LONG
                            ).show()
                            false
                        }
                    } else {
                        // Handle specific error cases
                        when {
                            result.error?.contains("No Google account found") == true -> {
                                showAddAccountDialog()
                            }
                            result.error?.contains("cancelled") == true -> {
                                Toast.makeText(
                                    this,
                                    "로그인이 취소되었습니다",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            else -> {
                                Toast.makeText(
                                    this,
                                    "로그인 실패: ${result.error}",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                        false
                    }
                }
            )
        }
    }

    private fun showAddAccountDialog() {
        AlertDialog.Builder(this)
            .setTitle("Google 계정 필요")
            .setMessage("Google 로그인을 사용하려면 기기에 Google 계정을 추가해야 합니다.\n\n계정 추가 화면으로 이동하시겠습니까?")
            .setPositiveButton("계정 추가") { _, _ ->
                AccountUtils.openAddAccountSettings(this)
            }
            .setNegativeButton("취소", null)
            .show()
    }
}
