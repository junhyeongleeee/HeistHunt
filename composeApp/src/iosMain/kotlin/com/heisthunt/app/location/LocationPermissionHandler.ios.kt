package com.heisthunt.app.location

import androidx.compose.runtime.Composable

@Composable
actual fun RequestLocationPermission(
    onPermissionGranted: () -> Unit,
    onPermissionDenied: () -> Unit
) {
    // iOS placeholder - will implement later with CoreLocation
    onPermissionDenied()
}
