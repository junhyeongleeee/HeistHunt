package com.heisthunt.app.location

import com.heisthunt.shared.models.Location

actual class LocationService {
    actual fun startLocationUpdates(
        intervalMillis: Long,
        onLocationUpdate: (Location) -> Unit,
        onError: (String) -> Unit
    ) {
        throw NotImplementedError("iOS location tracking not yet implemented")
    }

    actual fun stopLocationUpdates() {
        throw NotImplementedError("iOS location tracking not yet implemented")
    }

    actual suspend fun getCurrentLocation(): Result<Location> {
        return Result.failure(NotImplementedError("iOS location tracking not yet implemented"))
    }

    actual fun isLocationEnabled(): Boolean {
        return false
    }
}
