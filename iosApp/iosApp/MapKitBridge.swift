import Foundation
import UIKit
import MapKit
import ComposeApp

@objc public class MapKitBridge: NSObject {

    @objc public static let shared = MapKitBridge()

    private override init() {
        super.init()
        // Register callbacks with Kotlin
        registerCallbacks()
    }

    private func registerCallbacks() {
        print("🗺️ Registering MapKit callbacks")

        // Set create map view callback
        GoogleMapsHolder.shared.createMapViewCallback = { [weak self] () -> UIView in
            print("🗺️ createMapViewCallback called")
            return self?.createMapView() ?? UIView()
        }

        // Set update my location callback
        GoogleMapsHolder.shared.updateMyLocationCallback = { latitude, longitude, accuracy, roleColor in
            print("🗺️ updateMyLocationCallback: \(latitude), \(longitude)")
            MapKitViewWrapper.shared.updateMyLocation(
                latitude: latitude.doubleValue,
                longitude: longitude.doubleValue,
                accuracy: accuracy.floatValue,
                roleColor: UInt32(roleColor.uint32Value)
            )
        }

        // Set update player location callback
        GoogleMapsHolder.shared.updatePlayerLocationCallback = { userId, latitude, longitude, roleColor, isDisconnected in
            print("🗺️ updatePlayerLocationCallback: \(userId)")
            MapKitViewWrapper.shared.updatePlayerLocation(
                userId: userId as String,
                latitude: latitude.doubleValue,
                longitude: longitude.doubleValue,
                roleColor: UInt32(roleColor.uint32Value),
                isDisconnected: isDisconnected.boolValue
            )
        }

        // Set update safe circle callback
        GoogleMapsHolder.shared.updateSafeCircleCallback = { latitude, longitude, radiusMeters in
            print("🗺️ updateSafeCircleCallback: \(latitude), \(longitude), \(radiusMeters)")
            MapKitViewWrapper.shared.updateSafeCircle(
                latitude: latitude.doubleValue,
                longitude: longitude.doubleValue,
                radiusMeters: radiusMeters.doubleValue
            )
        }

        // Set clear player marker callback
        GoogleMapsHolder.shared.clearPlayerMarkerCallback = { userId in
            print("🗺️ clearPlayerMarkerCallback: \(userId)")
            MapKitViewWrapper.shared.clearPlayerMarker(userId: userId)
        }

        // Set clear all markers callback
        GoogleMapsHolder.shared.clearAllMarkersCallback = {
            print("🗺️ clearAllMarkersCallback")
            MapKitViewWrapper.shared.clearAllMarkers()
        }

        print("✅ MapKit callbacks registered successfully")
    }

    private func createMapView() -> UIView {
        print("🗺️ Creating MapKit UIView")
        return MapKitViewWrapper.shared.createMapView()
    }
}
