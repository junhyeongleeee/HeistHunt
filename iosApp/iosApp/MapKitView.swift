import Foundation
import UIKit
import MapKit
import ComposeApp

@objc public class MapKitViewWrapper: NSObject {
    @objc public static let shared = MapKitViewWrapper()

    private var mapView: MKMapView?
    private var playerAnnotations: [String: MKPointAnnotation] = [:]
    private var myLocationAnnotation: MKPointAnnotation?
    private var safeCircle: MKCircle?
    private var accuracyCircle: MKCircle?

    private override init() {
        super.init()
    }

    @objc public func createMapView() -> UIView {
        let mapView = MKMapView(frame: .zero)
        mapView.showsUserLocation = false
        mapView.showsCompass = true
        mapView.isZoomEnabled = true
        mapView.isScrollEnabled = true
        mapView.isPitchEnabled = false
        mapView.isRotateEnabled = true
        mapView.mapType = .standard

        // Set initial region (San Francisco)
        let initialLocation = CLLocationCoordinate2D(latitude: 37.7749, longitude: -122.4194)
        let region = MKCoordinateRegion(center: initialLocation,
                                        latitudinalMeters: 1000,
                                        longitudinalMeters: 1000)
        mapView.setRegion(region, animated: false)

        mapView.delegate = self
        self.mapView = mapView
        return mapView
    }

    @objc public func updateMyLocation(latitude: Double, longitude: Double, accuracy: Float, roleColor: UInt32) {
        guard let mapView = mapView else { return }

        let location = CLLocationCoordinate2D(latitude: latitude, longitude: longitude)

        // Update camera to follow my location
        let region = MKCoordinateRegion(center: location,
                                        latitudinalMeters: 1000,
                                        longitudinalMeters: 1000)
        mapView.setRegion(region, animated: true)

        // Remove old accuracy circle
        if let oldCircle = accuracyCircle {
            mapView.removeOverlay(oldCircle)
        }

        // Add accuracy circle
        if accuracy > 0 {
            let circle = MKCircle(center: location, radius: CLLocationDistance(accuracy))
            mapView.addOverlay(circle)
            accuracyCircle = circle
        }

        // Update or create my location annotation
        if let annotation = myLocationAnnotation {
            annotation.coordinate = location
        } else {
            let annotation = MKPointAnnotation()
            annotation.coordinate = location
            annotation.title = "Me"
            mapView.addAnnotation(annotation)
            myLocationAnnotation = annotation
        }
    }

    @objc public func updatePlayerLocation(userId: String, latitude: Double, longitude: Double, roleColor: UInt32, isDisconnected: Bool) {
        guard let mapView = mapView else { return }

        let location = CLLocationCoordinate2D(latitude: latitude, longitude: longitude)

        if let annotation = playerAnnotations[userId] {
            annotation.coordinate = location
            annotation.subtitle = isDisconnected ? "Disconnected" : "Connected"
        } else {
            let annotation = MKPointAnnotation()
            annotation.coordinate = location
            annotation.title = userId
            annotation.subtitle = isDisconnected ? "Disconnected" : "Connected"
            mapView.addAnnotation(annotation)
            playerAnnotations[userId] = annotation
        }
    }

    @objc public func updateSafeCircle(latitude: Double, longitude: Double, radiusMeters: Double) {
        guard let mapView = mapView else { return }

        // Remove old circle
        if let oldCircle = safeCircle {
            mapView.removeOverlay(oldCircle)
        }

        // Add new circle
        let location = CLLocationCoordinate2D(latitude: latitude, longitude: longitude)
        let circle = MKCircle(center: location, radius: radiusMeters)
        mapView.addOverlay(circle)
        safeCircle = circle
    }

    @objc public func clearPlayerMarker(userId: String) {
        guard let mapView = mapView else { return }

        if let annotation = playerAnnotations[userId] {
            mapView.removeAnnotation(annotation)
            playerAnnotations.removeValue(forKey: userId)
        }
    }

    @objc public func clearAllMarkers() {
        guard let mapView = mapView else { return }

        // Remove all player annotations
        playerAnnotations.values.forEach { mapView.removeAnnotation($0) }
        playerAnnotations.removeAll()

        // Remove my location annotation
        if let annotation = myLocationAnnotation {
            mapView.removeAnnotation(annotation)
            myLocationAnnotation = nil
        }

        // Remove circles
        if let circle = safeCircle {
            mapView.removeOverlay(circle)
            safeCircle = nil
        }
        if let circle = accuracyCircle {
            mapView.removeOverlay(circle)
            accuracyCircle = nil
        }
    }
}

// MKMapViewDelegate
extension MapKitViewWrapper: MKMapViewDelegate {
    public func mapView(_ mapView: MKMapView, rendererFor overlay: MKOverlay) -> MKOverlayRenderer {
        if let circle = overlay as? MKCircle {
            let renderer = MKCircleRenderer(circle: circle)

            // Check if this is the safe circle or accuracy circle
            if circle === safeCircle {
                renderer.strokeColor = UIColor(red: 0.937, green: 0.267, blue: 0.267, alpha: 1.0) // #EF4444
                renderer.fillColor = UIColor(red: 0.937, green: 0.267, blue: 0.267, alpha: 0.1)
                renderer.lineWidth = 3
            } else if circle === accuracyCircle {
                renderer.strokeColor = UIColor(red: 0.23, green: 0.51, blue: 0.96, alpha: 0.3) // blue with alpha
                renderer.fillColor = UIColor(red: 0.23, green: 0.51, blue: 0.96, alpha: 0.1)
                renderer.lineWidth = 2
            }

            return renderer
        }

        return MKOverlayRenderer(overlay: overlay)
    }

    public func mapView(_ mapView: MKMapView, viewFor annotation: MKAnnotation) -> MKAnnotationView? {
        // Don't customize the user location annotation
        if annotation is MKUserLocation {
            return nil
        }

        let identifier = "PlayerAnnotation"
        var annotationView = mapView.dequeueReusableAnnotationView(withIdentifier: identifier) as? MKMarkerAnnotationView

        if annotationView == nil {
            annotationView = MKMarkerAnnotationView(annotation: annotation, reuseIdentifier: identifier)
            annotationView?.canShowCallout = true
        } else {
            annotationView?.annotation = annotation
        }

        // Set color based on whether it's me or another player
        if annotation === myLocationAnnotation {
            annotationView?.markerTintColor = UIColor(red: 0.23, green: 0.51, blue: 0.96, alpha: 1.0) // blue
            annotationView?.glyphImage = UIImage(systemName: "person.fill")
        } else {
            // Check if disconnected
            if annotation.subtitle == "Disconnected" {
                annotationView?.markerTintColor = UIColor.gray
                annotationView?.alpha = 0.5
            } else {
                annotationView?.markerTintColor = UIColor(red: 0.86, green: 0.15, blue: 0.15, alpha: 1.0) // red
                annotationView?.alpha = 1.0
            }
            annotationView?.glyphImage = UIImage(systemName: "person.fill")
        }

        return annotationView
    }
}
