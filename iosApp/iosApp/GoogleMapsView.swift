import Foundation
import UIKit
import GoogleMaps
import ComposeApp

@objc public class GoogleMapsViewWrapper: NSObject {
    @objc public static let shared = GoogleMapsViewWrapper()

    private var mapView: GMSMapView?
    private var playerMarkers: [String: GMSMarker] = [:]
    private var safeCircle: GMSCircle?
    private var accuracyCircle: GMSCircle?
    private var initialLocationSet = false

    private override init() {
        super.init()
    }

    @objc public func createMapView() -> UIView {
        let camera = GMSCameraPosition.camera(withLatitude: 37.7749, longitude: -122.4194, zoom: 16.0)
        let mapView = GMSMapView.map(withFrame: .zero, camera: camera)
        mapView.isMyLocationEnabled = false
        mapView.settings.compassButton = true
        mapView.settings.myLocationButton = false
        mapView.settings.zoomGestures = true
        mapView.settings.scrollGestures = true
        mapView.settings.tiltGestures = false
        mapView.settings.rotateGestures = true

        self.mapView = mapView
        self.initialLocationSet = false
        return mapView
    }

    @objc public func updateMyLocation(latitude: Double, longitude: Double, accuracy: Float, roleColor: UInt32) {
        guard let mapView = mapView else { return }

        let position = CLLocationCoordinate2D(latitude: latitude, longitude: longitude)

        // Only center camera on first location fix - user can freely pan after that
        if !initialLocationSet {
            let camera = GMSCameraPosition.camera(withTarget: position, zoom: 16.0)
            mapView.camera = camera
            initialLocationSet = true
            print("🗺️ Initial camera position set to my location")
        }

        // Remove old accuracy circle
        accuracyCircle?.map = nil

        // Add accuracy circle
        if accuracy > 0 {
            let circle = GMSCircle()
            circle.position = position
            circle.radius = CLLocationDistance(accuracy)
            circle.strokeWidth = 2
            circle.strokeColor = UIColor(rgb: roleColor).withAlphaComponent(0.3)
            circle.fillColor = UIColor(rgb: roleColor).withAlphaComponent(0.1)
            circle.map = mapView
            accuracyCircle = circle
        }

        // Update or create my location marker
        if let myMarker = playerMarkers["__my_location__"] {
            myMarker.position = position
        } else {
            let marker = GMSMarker()
            marker.position = position
            marker.icon = createMarkerIcon(color: UIColor(rgb: roleColor), size: CGSize(width: 24, height: 24))
            marker.map = mapView
            playerMarkers["__my_location__"] = marker
        }
    }

    @objc public func updatePlayerLocation(userId: String, latitude: Double, longitude: Double, roleColor: UInt32, isDisconnected: Bool) {
        guard let mapView = mapView else { return }

        let position = CLLocationCoordinate2D(latitude: latitude, longitude: longitude)
        let color = isDisconnected ? UIColor.gray : UIColor(rgb: roleColor)

        if let marker = playerMarkers[userId] {
            marker.position = position
            marker.icon = createMarkerIcon(color: color, size: CGSize(width: 20, height: 20))
            marker.opacity = isDisconnected ? 0.5 : 1.0
        } else {
            let marker = GMSMarker()
            marker.position = position
            marker.icon = createMarkerIcon(color: color, size: CGSize(width: 20, height: 20))
            marker.opacity = isDisconnected ? 0.5 : 1.0
            marker.map = mapView
            playerMarkers[userId] = marker
        }
    }

    @objc public func updateSafeCircle(latitude: Double, longitude: Double, radiusMeters: Double) {
        guard let mapView = mapView else { return }

        // Remove old circle
        safeCircle?.map = nil

        // Add new circle
        let circle = GMSCircle()
        circle.position = CLLocationCoordinate2D(latitude: latitude, longitude: longitude)
        circle.radius = radiusMeters
        circle.strokeWidth = 3
        circle.strokeColor = UIColor(red: 0.937, green: 0.267, blue: 0.267, alpha: 1.0) // #EF4444
        circle.fillColor = UIColor(red: 0.937, green: 0.267, blue: 0.267, alpha: 0.1)
        circle.map = mapView
        safeCircle = circle
    }

    @objc public func updateJailMarker(latitude: Double, longitude: Double) {
        guard let mapView = mapView else { return }
        let position = CLLocationCoordinate2D(latitude: latitude, longitude: longitude)

        if let jailMarker = playerMarkers["__jail__"] {
            jailMarker.position = position
        } else {
            let marker = GMSMarker()
            marker.position = position
            marker.title = "JAIL"
            marker.snippet = "경찰 시작 위치"
            marker.icon = GMSMarker.markerImage(with: UIColor.orange)
            marker.map = mapView
            playerMarkers["__jail__"] = marker
        }
    }

    @objc public func clearPlayerMarker(userId: String) {
        playerMarkers[userId]?.map = nil
        playerMarkers.removeValue(forKey: userId)
    }

    @objc public func clearAllMarkers() {
        playerMarkers.forEach { $0.value.map = nil }
        playerMarkers.removeAll()
        safeCircle?.map = nil
        accuracyCircle?.map = nil
    }

    private func createMarkerIcon(color: UIColor, size: CGSize) -> UIImage {
        let renderer = UIGraphicsImageRenderer(size: size)
        return renderer.image { context in
            color.setFill()
            let circle = UIBezierPath(ovalIn: CGRect(origin: .zero, size: size))
            circle.fill()

            UIColor.white.setStroke()
            circle.lineWidth = 2
            circle.stroke()
        }
    }
}

extension UIColor {
    convenience init(rgb: UInt32) {
        let red = CGFloat((rgb >> 16) & 0xFF) / 255.0
        let green = CGFloat((rgb >> 8) & 0xFF) / 255.0
        let blue = CGFloat(rgb & 0xFF) / 255.0
        self.init(red: red, green: green, blue: blue, alpha: 1.0)
    }
}
