package com.heisthunt.app.auth

import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.tasks.await
import java.security.MessageDigest
import java.util.UUID

actual class GoogleAuthService(private val context: Context) {
    private val auth = FirebaseAuth.getInstance()
    private val credentialManager = CredentialManager.create(context)

    actual suspend fun signIn(): GoogleSignInResult {
        Log.d("GoogleAuthService", "signIn() called")
        return try {
            Log.d("GoogleAuthService", "Starting credential request")
            // Generate nonce for security
            val rawNonce = UUID.randomUUID().toString()
            val bytes = rawNonce.toByteArray()
            val md = MessageDigest.getInstance("SHA-256")
            val digest = md.digest(bytes)
            val hashedNonce = digest.fold("") { str, it -> str + "%02x".format(it) }

            // Configure Google ID option
            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(getWebClientId(context))
                .setNonce(hashedNonce)
                .build()

            // Build credential request
            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            // Get credential
            val result = credentialManager.getCredential(
                request = request,
                context = context
            )

            // Handle the credential
            val credential = result.credential

            when {
                credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL -> {
                    try {
                        // Extract GoogleIdTokenCredential from CustomCredential
                        val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                        val googleIdToken = googleIdTokenCredential.idToken

                        // Sign in with Firebase
                        val firebaseCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                        val authResult = auth.signInWithCredential(firebaseCredential).await()

                        val user = authResult.user
                        if (user != null) {
                            Log.d("GoogleAuthService", "✅ Sign-in success: ${user.email}")
                            GoogleSignInResult(
                                success = true,
                                userId = user.uid,
                                email = user.email,
                                displayName = user.displayName,
                                idToken = googleIdToken
                            )
                        } else {
                            GoogleSignInResult(
                                success = false,
                                idToken = null,
                                error = "Failed to get user information"
                            )
                        }
                    } catch (e: GoogleIdTokenParsingException) {
                        Log.e("GoogleAuthService", "GoogleIdToken parsing error", e)
                        GoogleSignInResult(
                            success = false,
                            idToken = null,
                            error = "Failed to parse Google ID token: ${e.message}"
                        )
                    }
                }
                else -> {
                    Log.e("GoogleAuthService", "Unexpected credential type: ${credential.type}")
                    GoogleSignInResult(
                        success = false,
                        idToken = null,
                        error = "Invalid credential type: ${credential.type}"
                    )
                }
            }
        } catch (e: GetCredentialCancellationException) {
            Log.e("GoogleAuthService", "Sign-in cancelled", e)
            GoogleSignInResult(
                success = false,
                idToken = null,
                error = "Sign-in was cancelled"
            )
        } catch (e: NoCredentialException) {
            Log.e("GoogleAuthService", "No credential found", e)
            GoogleSignInResult(
                success = false,
                idToken = null,
                error = "No Google account found. Please add a Google account to your device."
            )
        } catch (e: GetCredentialException) {
            Log.e("GoogleAuthService", "Credential error: ${e.type}, ${e.errorMessage}", e)
            GoogleSignInResult(
                success = false,
                idToken = null,
                error = "Credential error: ${e.errorMessage ?: e.message}"
            )
        } catch (e: Exception) {
            Log.e("GoogleAuthService", "Unknown error", e)
            GoogleSignInResult(
                success = false,
                idToken = null,
                error = e.message ?: "Unknown error occurred"
            )
        }
    }

    actual suspend fun signOut() {
        try {
            auth.signOut()
            // Note: Credential Manager doesn't require explicit sign-out
        } catch (e: Exception) {
            // Handle error silently
        }
    }

    actual fun getCurrentUser(): GoogleSignInResult? {
        val user = auth.currentUser ?: return null
        // Note: idToken is not available here, would need to call user.getIdToken(false).await()
        return GoogleSignInResult(
            success = true,
            userId = user.uid,
            email = user.email,
            displayName = user.displayName,
            idToken = null  // Not available in getCurrentUser
        )
    }

    private fun getWebClientId(context: Context): String {
        val resources = context.resources
        val resourceId = resources.getIdentifier(
            "default_web_client_id",
            "string",
            context.packageName
        )
        return if (resourceId != 0) {
            resources.getString(resourceId)
        } else {
            throw IllegalStateException("default_web_client_id not found. Make sure google-services.json is configured correctly.")
        }
    }
}
