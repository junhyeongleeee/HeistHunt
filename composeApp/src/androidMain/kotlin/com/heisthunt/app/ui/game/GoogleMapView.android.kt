package com.heisthunt.app.ui.game

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import com.heisthunt.shared.models.Location
import com.heisthunt.shared.models.PlayerLocation
import com.heisthunt.shared.models.PlayerRole
import kotlinx.coroutines.launch

@Composable
actual fun GameMapView(
    myLocation: Location?,
    playerLocations: List<PlayerLocation>,
    myRole: PlayerRole,
    safeRadiusMeters: Double,
    gameCenterLocation: Location?,
    modifier: Modifier
) {
    // Default to a location if none provided (will be overridden when real location arrives)
    val defaultLocation = LatLng(37.7749, -122.4194) // San Francisco
    val myLatLng = myLocation?.let { LatLng(it.latitude, it.longitude) } ?: defaultLocation
    val centerLatLng = gameCenterLocation?.let { LatLng(it.latitude, it.longitude) } ?: myLatLng

    // Camera position state - follows user location
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(myLatLng, 16f)
    }

    // Update camera when location changes
    LaunchedEffect(myLocation) {
        myLocation?.let {
            cameraPositionState.position = CameraPosition.fromLatLngZoom(
                LatLng(it.latitude, it.longitude),
                cameraPositionState.position.zoom
            )
        }
    }

    val coroutineScope = rememberCoroutineScope()

    Box(modifier = modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(
                isMyLocationEnabled = false, // We'll draw our own marker
                mapType = MapType.NORMAL
            ),
            uiSettings = MapUiSettings(
                zoomControlsEnabled = true,
                myLocationButtonEnabled = false,
                compassEnabled = true,
                mapToolbarEnabled = false,
                scrollGesturesEnabled = true,
                zoomGesturesEnabled = true,
                tiltGesturesEnabled = false,
                rotationGesturesEnabled = true
            )
        ) {
            // Safe radius circle
            gameCenterLocation?.let { center ->
                Circle(
                    center = LatLng(center.latitude, center.longitude),
                    radius = safeRadiusMeters,
                    strokeColor = Color(0xFFEF4444), // red-500
                    strokeWidth = 3f,
                    fillColor = Color(0x1AEF4444), // red-500/10
                    clickable = false
                )
            }

            // My location marker (larger, distinctive)
            myLocation?.let { loc ->
                val markerColor = when (myRole) {
                    PlayerRole.POLICE -> Color(0xFF3B82F6) // blue-500
                    PlayerRole.THIEF -> Color(0xFFDC2626) // red-600
                }

                // Accuracy circle (if available)
                loc.accuracy?.let { accuracy ->
                    Circle(
                        center = LatLng(loc.latitude, loc.longitude),
                        radius = accuracy.toDouble(),
                        strokeColor = markerColor.copy(alpha = 0.3f),
                        strokeWidth = 2f,
                        fillColor = markerColor.copy(alpha = 0.1f),
                        clickable = false
                    )
                }

                // My marker
                Marker(
                    state = MarkerState(position = LatLng(loc.latitude, loc.longitude)),
                    title = "ME",
                    snippet = "${myRole.name} - Accuracy: ${loc.accuracy?.toInt() ?: "?"}m",
                    icon = createCustomMarkerIcon(markerColor, isMe = true)
                )
            }

            // Other players' markers
            playerLocations.forEach { playerLocation ->
                // Don't draw a marker for myself
                if (myLocation?.let {
                    playerLocation.location.latitude == it.latitude &&
                    playerLocation.location.longitude == it.longitude
                } == true) {
                    return@forEach
                }

                val markerColor = when (playerLocation.role) {
                    PlayerRole.POLICE -> Color(0xFF3B82F6) // blue-500
                    PlayerRole.THIEF -> Color(0xFFDC2626) // red-600
                }

                Marker(
                    state = MarkerState(
                        position = LatLng(
                            playerLocation.location.latitude,
                            playerLocation.location.longitude
                        )
                    ),
                    title = playerLocation.userId.take(8),
                    snippet = playerLocation.role.name,
                    icon = createCustomMarkerIcon(markerColor, isMe = false)
                )
            }
        }

        // My Location FAB (bottom right)
        myLocation?.let { loc ->
            SmallFloatingActionButton(
                onClick = {
                    coroutineScope.launch {
                        cameraPositionState.animate(
                            update = com.google.android.gms.maps.CameraUpdateFactory.newLatLngZoom(
                                LatLng(loc.latitude, loc.longitude),
                                17f
                            ),
                            durationMs = 500
                        )
                    }
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
                    .padding(bottom = 80.dp), // Account for action footer
                containerColor = Color.White,
                contentColor = Color(0xFF1E293B)
            ) {
                // Using a simple text icon instead of missing Material icon
                androidx.compose.material3.Text(
                    text = "📍",
                    fontSize = 20.sp
                )
            }
        }
    }
}

private fun createCustomMarkerIcon(color: Color, isMe: Boolean): com.google.android.gms.maps.model.BitmapDescriptor {
    // For now, use default marker with hue
    // TODO: Create custom bitmap markers with proper design
    val hue = when {
        color == Color(0xFF3B82F6) -> com.google.android.gms.maps.model.BitmapDescriptorFactory.HUE_BLUE
        color == Color(0xFFDC2626) -> com.google.android.gms.maps.model.BitmapDescriptorFactory.HUE_RED
        else -> com.google.android.gms.maps.model.BitmapDescriptorFactory.HUE_ORANGE
    }

    return if (isMe) {
        // Use default marker for "me" (can be enhanced later)
        com.google.android.gms.maps.model.BitmapDescriptorFactory.defaultMarker(hue)
    } else {
        // Use smaller default marker for others
        com.google.android.gms.maps.model.BitmapDescriptorFactory.defaultMarker(hue)
    }
}
