package com.heisthunt.app.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.LocationManager
import android.os.Looper
import com.google.android.gms.location.*
import com.heisthunt.shared.models.Location
import kotlinx.datetime.Clock
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

actual class LocationService(private val context: Context) {
    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
    private var locationCallback: LocationCallback? = null

    @SuppressLint("MissingPermission")
    actual fun startLocationUpdates(
        intervalMillis: Long,
        onLocationUpdate: (Location) -> Unit,
        onError: (String) -> Unit
    ) {
        // Use mock location if enabled
        if (MockLocationService.isEnabled) {
            println("🧪 Using MOCK location service")
            // Simulate location updates with mock data
            val handler = android.os.Handler(Looper.getMainLooper())
            val runnable = object : Runnable {
                override fun run() {
                    val mockLocation = MockLocationService.getCurrentLocation()
                    onLocationUpdate(mockLocation)
                    handler.postDelayed(this, intervalMillis)
                }
            }
            handler.post(runnable)
            return
        }

        if (!isLocationEnabled()) {
            onError("Location services are disabled. Please enable GPS.")
            return
        }

        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            intervalMillis
        )
            .setMinUpdateIntervalMillis(intervalMillis / 2)
            .setWaitForAccurateLocation(false)
            .build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { androidLocation ->
                    val location = Location(
                        latitude = androidLocation.latitude,
                        longitude = androidLocation.longitude,
                        accuracy = androidLocation.accuracy,
                        timestamp = Clock.System.now()
                    )
                    onLocationUpdate(location)
                }
            }

            override fun onLocationAvailability(availability: LocationAvailability) {
                // 실내에서도 게임 가능 - GPS 신호 없음 알림 제거
                // 마지막 알려진 위치를 계속 사용함
                if (!availability.isLocationAvailable) {
                    println("GPS signal weak, but continuing with last known location")
                }
            }
        }

        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback!!,
                Looper.getMainLooper()
            )
            println("Location updates started with interval: ${intervalMillis}ms")
        } catch (e: SecurityException) {
            onError("Location permission denied")
            println("Location permission error: ${e.message}")
        } catch (e: Exception) {
            onError("Failed to start location updates: ${e.message}")
            println("Location update error: ${e.message}")
        }
    }

    actual fun stopLocationUpdates() {
        locationCallback?.let {
            fusedLocationClient.removeLocationUpdates(it)
            locationCallback = null
            println("Location updates stopped")
        }
    }

    @SuppressLint("MissingPermission")
    actual suspend fun getCurrentLocation(): Result<Location> {
        // Use mock location if enabled
        if (MockLocationService.isEnabled) {
            println("🧪 Returning MOCK location")
            return Result.success(MockLocationService.getCurrentLocation())
        }

        return try {
            val task = fusedLocationClient.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                null
            )

            suspendCoroutine { continuation ->
                task.addOnSuccessListener { androidLocation ->
                    if (androidLocation != null) {
                        val location = Location(
                            latitude = androidLocation.latitude,
                            longitude = androidLocation.longitude,
                            accuracy = androidLocation.accuracy,
                            timestamp = Clock.System.now()
                        )
                        continuation.resume(Result.success(location))
                    } else {
                        continuation.resume(Result.failure(Exception("Location not available")))
                    }
                }.addOnFailureListener { exception ->
                    continuation.resume(Result.failure(exception))
                }
            }
        } catch (e: SecurityException) {
            Result.failure(Exception("Location permission denied"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    actual fun isLocationEnabled(): Boolean {
        // If mock location is enabled, always return true
        if (MockLocationService.isEnabled) {
            return true
        }
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }
}
