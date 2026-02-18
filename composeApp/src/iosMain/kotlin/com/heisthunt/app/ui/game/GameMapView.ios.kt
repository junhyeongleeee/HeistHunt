package com.heisthunt.app.ui.game

import androidx.compose.foundation.layout.*
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.interop.UIKitView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.heisthunt.shared.models.Location
import com.heisthunt.shared.models.PlayerLocation
import com.heisthunt.shared.models.PlayerRole
import kotlinx.cinterop.ExperimentalForeignApi

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun GameMapView(
    myLocation: Location?,
    playerLocations: List<PlayerLocation>,
    myRole: PlayerRole,
    safeRadiusMeters: Double,
    gameCenterLocation: Location?,
    disconnectedPlayerIds: Set<String>,
    modifier: Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        // Google Maps UIView
        UIKitView(
            factory = {
                println("🗺️ Creating Google Maps view")
                GoogleMapsHolder.createMapView()
            },
            update = { _ ->
                println("🗺️ Updating Google Maps view")

                // Update my location if available
                myLocation?.let { loc ->
                    val roleColor = when (myRole) {
                        PlayerRole.POLICE -> 0x3B82F6u // blue-500
                        PlayerRole.THIEF -> 0xDC2626u // red-600
                    }

                    println("🗺️ Updating my location: ${loc.latitude}, ${loc.longitude}")
                    GoogleMapsHolder.updateMyLocation(
                        latitude = loc.latitude,
                        longitude = loc.longitude,
                        accuracy = loc.accuracy ?: 0f,
                        roleColor = roleColor
                    )
                }

                // Update safe circle and jail marker if available
                gameCenterLocation?.let { center ->
                    println("🗺️ Updating safe circle at: ${center.latitude}, ${center.longitude}, radius: $safeRadiusMeters")
                    GoogleMapsHolder.updateSafeCircle(
                        latitude = center.latitude,
                        longitude = center.longitude,
                        radiusMeters = safeRadiusMeters
                    )
                    GoogleMapsHolder.updateJailMarker(
                        latitude = center.latitude,
                        longitude = center.longitude
                    )
                }

                // Update other players' markers (server already excludes my own location)
                println("🗺️ Updating ${playerLocations.size} player markers")
                playerLocations.forEach { player ->
                    val isDisconnected = disconnectedPlayerIds.contains(player.userId)

                    val roleColor = if (isDisconnected) {
                        0x64748Bu // slate-500 (gray for disconnected)
                    } else {
                        when (player.role) {
                            PlayerRole.POLICE -> 0x3B82F6u // blue-500
                            PlayerRole.THIEF -> 0xDC2626u // red-600
                        }
                    }

                    println("🗺️ Adding marker for ${player.userId}: ${player.location.latitude}, ${player.location.longitude}, disconnected: $isDisconnected")
                    GoogleMapsHolder.updatePlayerLocation(
                        userId = player.userId,
                        latitude = player.location.latitude,
                        longitude = player.location.longitude,
                        roleColor = roleColor,
                        isDisconnected = isDisconnected
                    )
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // My Location FAB (bottom right) - matching Android
        myLocation?.let { loc ->
            SmallFloatingActionButton(
                onClick = {
                    // Re-center camera on my location
                    val roleColor = when (myRole) {
                        PlayerRole.POLICE -> 0x3B82F6u
                        PlayerRole.THIEF -> 0xDC2626u
                    }
                    println("🗺️ Recenter button clicked")
                    GoogleMapsHolder.updateMyLocation(
                        latitude = loc.latitude,
                        longitude = loc.longitude,
                        accuracy = loc.accuracy ?: 0f,
                        roleColor = roleColor
                    )
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
                    .padding(bottom = 80.dp), // Account for action footer
                containerColor = androidx.compose.ui.graphics.Color.White,
                contentColor = androidx.compose.ui.graphics.Color(0xFF1E293B)
            ) {
                Text(
                    text = "📍",
                    fontSize = 20.sp
                )
            }
        }
    }
}
