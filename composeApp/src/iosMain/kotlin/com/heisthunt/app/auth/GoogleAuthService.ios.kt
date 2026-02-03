package com.heisthunt.app.auth

import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.coroutines.Continuation

// Global callback holder that Swift will set
object GoogleSignInCallback {
    var signInCallback: ((suspend () -> GoogleSignInResult) -> Unit)? = null
    var signOutCallback: (() -> Unit)? = null
    var getCurrentUserCallback: (() -> GoogleSignInResult?)? = null

    private var currentContinuation: Continuation<GoogleSignInResult>? = null

    // Called from Swift when sign-in completes
    fun onSignInResult(success: Boolean, userId: String?, email: String?, displayName: String?, error: String?) {
        val result = GoogleSignInResult(
            success = success,
            userId = userId,
            email = email,
            displayName = displayName,
            error = error
        )
        currentContinuation?.resume(result)
        currentContinuation = null
    }

    suspend fun performSignIn(): GoogleSignInResult = suspendCoroutine { continuation ->
        currentContinuation = continuation
        signInCallback?.invoke {
            GoogleSignInResult(success = false, error = "Callback not completed")
        }
    }
}

actual class GoogleAuthService {

    actual suspend fun signIn(): GoogleSignInResult {
        return GoogleSignInCallback.performSignIn()
    }

    actual suspend fun signOut() {
        GoogleSignInCallback.signOutCallback?.invoke()
    }

    actual fun getCurrentUser(): GoogleSignInResult? {
        return GoogleSignInCallback.getCurrentUserCallback?.invoke()
    }
}
