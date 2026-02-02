package com.heisthunt.app.location

import com.heisthunt.shared.models.Location

expect class LocationService {
    fun startLocationUpdates(
        intervalMillis: Long = 5000L,
        onLocationUpdate: (Location) -> Unit,
        onError: (String) -> Unit
    )

    fun stopLocationUpdates()

    suspend fun getCurrentLocation(): Result<Location>

    fun isLocationEnabled(): Boolean
}
