package com.heisthunt.app.auth

data class GoogleSignInResult(
    val success: Boolean,
    val userId: String? = null,
    val email: String? = null,
    val displayName: String? = null,
    val error: String? = null
)

expect class GoogleAuthService {
    suspend fun signIn(): GoogleSignInResult
    suspend fun signOut()
    fun getCurrentUser(): GoogleSignInResult?
}
