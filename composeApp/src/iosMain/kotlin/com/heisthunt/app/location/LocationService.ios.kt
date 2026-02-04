package com.heisthunt.app.location

import com.heisthunt.shared.models.Location
import kotlinx.cinterop.*
import platform.CoreLocation.*
import platform.Foundation.NSTimer
import platform.darwin.NSObject
import kotlinx.datetime.Clock

actual class LocationService {
    private val locationManager = CLLocationManager()
    private var locationDelegate: LocationDelegate? = null
    private var updateTimer: NSTimer? = null
    private var currentCallback: ((Location) -> Unit)? = null
    private var errorCallback: ((String) -> Unit)? = null

    actual fun startLocationUpdates(
        intervalMillis: Long,
        onLocationUpdate: (Location) -> Unit,
        onError: (String) -> Unit
    ) {
        println("📍 iOS LocationService: Starting location updates")

        currentCallback = onLocationUpdate
        errorCallback = onError

        // Create and set delegate
        locationDelegate = LocationDelegate(
            onUpdate = { location ->
                println("📍 iOS LocationService: Location updated - lat=${location.latitude}, lon=${location.longitude}")
                onLocationUpdate(location)
            },
            onError = { error ->
                println("❌ iOS LocationService: Error - $error")
                onError(error)
            }
        )

        locationManager.delegate = locationDelegate
        locationManager.desiredAccuracy = kCLLocationAccuracyBest
        locationManager.distanceFilter = 10.0 // Update every 10 meters

        // Request permission
        locationManager.requestWhenInUseAuthorization()

        // Start location updates
        locationManager.startUpdatingLocation()

        println("✅ iOS LocationService: Location updates started")
    }

    actual fun stopLocationUpdates() {
        println("🛑 iOS LocationService: Stopping location updates")
        locationManager.stopUpdatingLocation()
        updateTimer?.invalidate()
        updateTimer = null
        locationDelegate = null
        currentCallback = null
        errorCallback = null
    }

    @OptIn(ExperimentalForeignApi::class)
    actual suspend fun getCurrentLocation(): Result<Location> {
        return try {
            val currentLocation = locationManager.location
            if (currentLocation != null) {
                val location = Location(
                    latitude = currentLocation.coordinate.useContents { latitude },
                    longitude = currentLocation.coordinate.useContents { longitude },
                    accuracy = currentLocation.horizontalAccuracy.toFloat(),
                    timestamp = Clock.System.now()
                )
                Result.success(location)
            } else {
                Result.failure(Exception("Location not available"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    actual fun isLocationEnabled(): Boolean {
        return CLLocationManager.locationServicesEnabled()
    }
}

@OptIn(ExperimentalForeignApi::class)
private class LocationDelegate(
    private val onUpdate: (Location) -> Unit,
    private val onError: (String) -> Unit
) : NSObject(), CLLocationManagerDelegateProtocol {

    override fun locationManager(manager: CLLocationManager, didUpdateLocations: List<*>) {
        val locations = didUpdateLocations as List<CLLocation>
        locations.lastOrNull()?.let { clLocation ->
            val location = Location(
                latitude = clLocation.coordinate.useContents { latitude },
                longitude = clLocation.coordinate.useContents { longitude },
                accuracy = clLocation.horizontalAccuracy.toFloat(),
                timestamp = Clock.System.now()
            )
            onUpdate(location)
        }
    }

    override fun locationManager(manager: CLLocationManager, didFailWithError: platform.Foundation.NSError) {
        onError("Location error: ${didFailWithError.localizedDescription}")
    }

    override fun locationManager(manager: CLLocationManager, didChangeAuthorizationStatus: CLAuthorizationStatus) {
        println("📍 iOS LocationService: Authorization status changed to $didChangeAuthorizationStatus")
        when (didChangeAuthorizationStatus) {
            kCLAuthorizationStatusAuthorizedWhenInUse,
            kCLAuthorizationStatusAuthorizedAlways -> {
                println("✅ iOS LocationService: Location permission granted")
                manager.startUpdatingLocation()
            }
            kCLAuthorizationStatusDenied,
            kCLAuthorizationStatusRestricted -> {
                println("❌ iOS LocationService: Location permission denied")
                onError("Location permission denied")
            }
            else -> {
                println("⚠️ iOS LocationService: Location permission status: $didChangeAuthorizationStatus")
            }
        }
    }
}
