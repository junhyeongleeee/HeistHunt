package com.heisthunt.app.ui.game

import kotlinx.cinterop.ExperimentalForeignApi
import platform.UIKit.UIView

/**
 * Holder for Google Maps callbacks set by Swift bridge
 */
@OptIn(ExperimentalForeignApi::class)
object GoogleMapsHolder {
    var createMapViewCallback: (() -> UIView)? = null
    var updateMyLocationCallback: ((Double, Double, Float, UInt) -> Unit)? = null
    var updatePlayerLocationCallback: ((String, Double, Double, UInt, Boolean) -> Unit)? = null
    var updateSafeCircleCallback: ((Double, Double, Double) -> Unit)? = null
    var clearPlayerMarkerCallback: ((String) -> Unit)? = null
    var clearAllMarkersCallback: (() -> Unit)? = null
    var updateJailMarkerCallback: ((Double, Double) -> Unit)? = null

    fun createMapView(): UIView {
        return createMapViewCallback?.invoke()
            ?: error("GoogleMapsHolder.createMapViewCallback not initialized")
    }

    fun updateMyLocation(latitude: Double, longitude: Double, accuracy: Float, roleColor: UInt) {
        updateMyLocationCallback?.invoke(latitude, longitude, accuracy, roleColor)
    }

    fun updatePlayerLocation(
        userId: String,
        latitude: Double,
        longitude: Double,
        roleColor: UInt,
        isDisconnected: Boolean
    ) {
        updatePlayerLocationCallback?.invoke(userId, latitude, longitude, roleColor, isDisconnected)
    }

    fun updateSafeCircle(latitude: Double, longitude: Double, radiusMeters: Double) {
        updateSafeCircleCallback?.invoke(latitude, longitude, radiusMeters)
    }

    fun clearPlayerMarker(userId: String) {
        clearPlayerMarkerCallback?.invoke(userId)
    }

    fun clearAllMarkers() {
        clearAllMarkersCallback?.invoke()
    }

    fun updateJailMarker(latitude: Double, longitude: Double) {
        updateJailMarkerCallback?.invoke(latitude, longitude)
    }
}
