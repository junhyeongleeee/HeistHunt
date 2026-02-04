import SwiftUI

@main
struct iOSApp: App {
    init() {
        // Initialize bridges on app launch
        _ = GoogleSignInBridge.shared
        _ = MapKitBridge.shared
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
