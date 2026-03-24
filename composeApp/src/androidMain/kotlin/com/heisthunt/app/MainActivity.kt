package com.heisthunt.app

import android.app.AlertDialog
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.heisthunt.app.auth.AccountUtils
import com.heisthunt.app.auth.GoogleAuthService

class MainActivity : ComponentActivity() {
    private lateinit var googleAuthService: GoogleAuthService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        googleAuthService = GoogleAuthService(this)

        setContent {
            App(
                onGoogleLogin = suspend {
                    android.util.Log.d("MainActivity", "Google Sign-In started")
                    val result = googleAuthService.signIn()
                    android.util.Log.d("MainActivity", "Google Sign-In result: success=${result.success}, error=${result.error}")
                    if (result.success && result.idToken != null) {
                        android.util.Log.d("MainActivity", "Google Sign-In success, returning idToken")
                        result.idToken
                    } else {
                        when {
                            result.error?.contains("No Google account found") == true -> {
                                showAddAccountDialog()
                            }
                            result.error?.contains("cancelled") == true -> {
                                Toast.makeText(this, "로그인이 취소되었습니다", Toast.LENGTH_SHORT).show()
                            }
                            else -> {
                                Toast.makeText(this, "로그인 실패: ${result.error}", Toast.LENGTH_LONG).show()
                            }
                        }
                        null
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
