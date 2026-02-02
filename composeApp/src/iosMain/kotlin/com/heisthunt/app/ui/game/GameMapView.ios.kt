package com.heisthunt.app.ui.game

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.heisthunt.shared.models.Location
import com.heisthunt.shared.models.PlayerLocation
import com.heisthunt.shared.models.PlayerRole

@Composable
actual fun GameMapView(
    myLocation: Location?,
    playerLocations: List<PlayerLocation>,
    myRole: PlayerRole,
    safeRadiusMeters: Double,
    gameCenterLocation: Location?,
    modifier: Modifier
) {
    // iOS placeholder - will implement with MapKit later
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Map view not yet implemented for iOS",
            color = Color.White
        )
    }
}
