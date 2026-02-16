import SwiftUI
import GoogleSignIn

@main
struct iOSApp: App {
    init() {
        // Reset app state for UI testing
        if ProcessInfo.processInfo.arguments.contains("RESET_APP_STATE") {
            print("🧪 UI Testing: Resetting app state...")
            resetAppState()
        }

        // Initialize bridges on app launch
        _ = GoogleSignInBridge.shared
        _ = MapKitBridge.shared
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }

    private func resetAppState() {
        // Clear UserDefaults
        if let bundleID = Bundle.main.bundleIdentifier {
            UserDefaults.standard.removePersistentDomain(forName: bundleID)
            UserDefaults.standard.synchronize()
        }

        // Sign out from Google
        GIDSignIn.sharedInstance.signOut()

        print("✅ App state reset complete")
    }
}
