import Foundation
import UIKit
import GoogleMaps
import ComposeApp

@objc public class GoogleMapsBridge: NSObject {

    @objc public static let shared = GoogleMapsBridge()

    private override init() {
        super.init()
        // Initialize Google Maps
        initializeGoogleMaps()
        // Register callbacks with Kotlin
        registerCallbacks()
    }

    private func initializeGoogleMaps() {
        // Get API key from Info.plist
        guard let apiKey = Bundle.main.object(forInfoDictionaryKey: "GMSApiKey") as? String else {
            print("❌ GMSApiKey not found in Info.plist")
            return
        }

        print("🗺️ Initializing Google Maps with API key")
        GMSServices.provideAPIKey(apiKey)
    }

    private func registerCallbacks() {
        print("🗺️ Registering Google Maps callbacks")

        // Set create map view callback
        GoogleMapsHolder.shared.createMapViewCallback = { [weak self] in
            print("🗺️ createMapViewCallback called")
            return self?.createMapView() as Any
        }

        // Set update my location callback
        GoogleMapsHolder.shared.updateMyLocationCallback = { latitude, longitude, accuracy, roleColor in
            print("🗺️ updateMyLocationCallback: \(latitude), \(longitude)")
            GoogleMapsViewWrapper.shared.updateMyLocation(
                latitude: latitude,
                longitude: longitude,
                accuracy: accuracy,
                roleColor: UInt32(roleColor)
            )
        }

        // Set update player location callback
        GoogleMapsHolder.shared.updatePlayerLocationCallback = { userId, latitude, longitude, roleColor, isDisconnected in
            print("🗺️ updatePlayerLocationCallback: \(userId)")
            GoogleMapsViewWrapper.shared.updatePlayerLocation(
                userId: userId,
                latitude: latitude,
                longitude: longitude,
                roleColor: UInt32(roleColor),
                isDisconnected: isDisconnected
            )
        }

        // Set update safe circle callback
        GoogleMapsHolder.shared.updateSafeCircleCallback = { latitude, longitude, radiusMeters in
            print("🗺️ updateSafeCircleCallback: \(latitude), \(longitude), \(radiusMeters)")
            GoogleMapsViewWrapper.shared.updateSafeCircle(
                latitude: latitude,
                longitude: longitude,
                radiusMeters: radiusMeters
            )
        }

        // Set clear player marker callback
        GoogleMapsHolder.shared.clearPlayerMarkerCallback = { userId in
            print("🗺️ clearPlayerMarkerCallback: \(userId)")
            GoogleMapsViewWrapper.shared.clearPlayerMarker(userId: userId)
        }

        // Set clear all markers callback
        GoogleMapsHolder.shared.clearAllMarkersCallback = {
            print("🗺️ clearAllMarkersCallback")
            GoogleMapsViewWrapper.shared.clearAllMarkers()
        }

        print("✅ Google Maps callbacks registered successfully")
    }

    private func createMapView() -> UIView {
        print("🗺️ Creating Google Maps UIView")
        return GoogleMapsViewWrapper.shared.createMapView()
    }
}
