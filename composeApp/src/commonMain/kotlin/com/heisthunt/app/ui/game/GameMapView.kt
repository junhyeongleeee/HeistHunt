package com.heisthunt.app.ui.game

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.heisthunt.shared.models.Location
import com.heisthunt.shared.models.PlayerLocation
import com.heisthunt.shared.models.PlayerRole

@Composable
expect fun GameMapView(
    myLocation: Location?,
    playerLocations: List<PlayerLocation>,
    myRole: PlayerRole,
    safeRadiusMeters: Double = 500.0,
    gameCenterLocation: Location? = null,
    modifier: Modifier = Modifier
)
