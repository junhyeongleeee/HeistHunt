package com.heisthunt.app.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.alexzhirkevich.qrose.options.*
import io.github.alexzhirkevich.qrose.rememberQrCodePainter

@Composable
fun QRCodeDisplay(
    roomCode: String,
    modifier: Modifier = Modifier,
    size: Dp = 256.dp
) {
    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        // Display QR code using QRose painter
        val painter = rememberQrCodePainter(
            data = roomCode,
            options = QrOptions {
                // Customize QR code appearance
                shapes {
                    ball = QrBallShape.circle()
                    darkPixel = QrPixelShape.roundCorners()
                    frame = QrFrameShape.roundCorners(.25f)
                }
                colors {
                    dark = QrBrush.solid(Color(0xFF0F172A))
                    ball = QrBrush.solid(Color(0xFF3B82F6))
                    frame = QrBrush.solid(Color(0xFF3B82F6))
                }
            }
        )

        Image(
            painter = painter,
            contentDescription = "Room QR Code: $roomCode",
            modifier = Modifier.fillMaxSize()
        )
    }
}
