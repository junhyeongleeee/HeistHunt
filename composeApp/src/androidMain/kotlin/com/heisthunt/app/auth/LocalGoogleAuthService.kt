package com.heisthunt.app.auth

import androidx.compose.runtime.compositionLocalOf

val LocalGoogleAuthService = compositionLocalOf<GoogleAuthService> {
    error("GoogleAuthService not provided")
}
