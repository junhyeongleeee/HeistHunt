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
    fun onSignInResult(success: Boolean, userId: String?, email: String?, displayName: String?, idToken: String?, error: String?) {
        println("🟢 [Kotlin] onSignInResult called: success=$success, email=$email, idToken=${idToken?.take(20)}...")
        val result = GoogleSignInResult(
            success = success,
            userId = userId,
            email = email,
            displayName = displayName,
            idToken = idToken,
            error = error
        )
        if (currentContinuation != null) {
            println("🟢 [Kotlin] Resuming continuation with result")
            currentContinuation?.resume(result)
            currentContinuation = null
        } else {
            println("❌ [Kotlin] ERROR: No continuation to resume!")
        }
    }

    suspend fun performSignIn(): GoogleSignInResult = suspendCoroutine { continuation ->
        println("🟡 [Kotlin] performSignIn: Setting up suspension")
        currentContinuation = continuation
        if (signInCallback != null) {
            println("🟡 [Kotlin] Invoking signInCallback")
            signInCallback?.invoke {
                GoogleSignInResult(success = false, error = "Callback not completed")
            }
        } else {
            println("❌ [Kotlin] ERROR: signInCallback is null!")
            continuation.resume(GoogleSignInResult(success = false, error = "signInCallback not set"))
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
