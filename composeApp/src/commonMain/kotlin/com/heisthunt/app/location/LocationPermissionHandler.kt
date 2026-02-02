package com.heisthunt.app.location

import androidx.compose.runtime.Composable

@Composable
expect fun RequestLocationPermission(
    onPermissionGranted: () -> Unit,
    onPermissionDenied: () -> Unit
)
