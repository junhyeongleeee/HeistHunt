package com.heisthunt.app.camera

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Platform-specific QR code scanner
 */
@Composable
expect fun QRScannerView(
    onQRCodeDetected: (String) -> Unit,
    onError: (String) -> Unit,
    modifier: Modifier = Modifier
)
