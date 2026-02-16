import Foundation
import GoogleSignIn
import UIKit
import ComposeApp

@objc public class GoogleSignInBridge: NSObject {

    @objc public static let shared = GoogleSignInBridge()

    private override init() {
        super.init()
        // Register callbacks with Kotlin
        registerCallbacks()
    }

    private func registerCallbacks() {
        // Set sign-in callback
        GoogleSignInCallback.shared.signInCallback = { [weak self] _ in
            print("🔵 iOS Google Sign-In button clicked")
            self?.performSignIn()
        }

        // Set sign-out callback
        GoogleSignInCallback.shared.signOutCallback = { [weak self] in
            print("🔵 iOS Google Sign-Out called")
            self?.performSignOut()
        }

        // Set get current user callback
        GoogleSignInCallback.shared.getCurrentUserCallback = { [weak self] in
            return self?.getCurrentUser()
        }
    }

    private func performSignIn() {
        // UI Testing Mode: Auto Mock Login
        if ProcessInfo.processInfo.arguments.contains("UI_TESTING") {
            print("🧪 UI Testing Mode: Mock Google Sign-In")
            GoogleSignInCallback.shared.onSignInResult(
                success: true,
                userId: "test-user-123",
                email: "test@heisthunt.com",
                displayName: "Test User",
                idToken: "mock-id-token-for-testing",
                error: nil
            )
            return
        }

        // Get root view controller
        guard let scene = UIApplication.shared.connectedScenes.first as? UIWindowScene,
              let rootViewController = scene.windows.first?.rootViewController else {
            GoogleSignInCallback.shared.onSignInResult(
                success: false,
                userId: nil,
                email: nil,
                displayName: nil,
                idToken: nil,
                error: "No view controller available"
            )
            return
        }

        // Get Client ID from Info.plist
        guard let clientID = Bundle.main.object(forInfoDictionaryKey: "GIDClientID") as? String,
              !clientID.contains("YOUR_IOS_CLIENT_ID_HERE") else {
            GoogleSignInCallback.shared.onSignInResult(
                success: false,
                userId: nil,
                email: nil,
                displayName: nil,
                idToken: nil,
                error: "Please update GIDClientID in Info.plist"
            )
            return
        }

        // Configure Google Sign-In
        let config = GIDConfiguration(clientID: clientID)
        GIDSignIn.sharedInstance.configuration = config

        // Start sign-in flow
        GIDSignIn.sharedInstance.signIn(withPresenting: rootViewController) { result, error in
            if let error = error {
                GoogleSignInCallback.shared.onSignInResult(
                    success: false,
                    userId: nil,
                    email: nil,
                    displayName: nil,
                    idToken: nil,
                    error: "Sign-In Error: \(error.localizedDescription)"
                )
                return
            }

            guard let user = result?.user,
                  let profile = user.profile else {
                GoogleSignInCallback.shared.onSignInResult(
                    success: false,
                    userId: nil,
                    email: nil,
                    displayName: nil,
                    idToken: nil,
                    error: "Failed to get user profile"
                )
                return
            }

            let userId = user.userID ?? ""
            let email = profile.email
            let displayName = profile.name
            let idToken = user.idToken?.tokenString

            print("✅ [Swift] Google Sign-In Success: \(email)")
            print("🔵 [Swift] ID Token: \(idToken?.prefix(20) ?? "nil")...")
            print("🟢 [Swift] Calling GoogleSignInCallback.shared.onSignInResult")
            GoogleSignInCallback.shared.onSignInResult(
                success: true,
                userId: userId,
                email: email,
                displayName: displayName,
                idToken: idToken,
                error: nil
            )
            print("🟢 [Swift] onSignInResult call completed")
        }
    }

    private func performSignOut() {
        GIDSignIn.sharedInstance.signOut()
        print("✅ Google Sign-Out Complete")
    }

    private func getCurrentUser() -> GoogleSignInResult? {
        guard let user = GIDSignIn.sharedInstance.currentUser,
              let profile = user.profile else {
            return nil
        }

        return GoogleSignInResult(
            success: true,
            userId: user.userID ?? "",
            email: profile.email,
            displayName: profile.name,
            idToken: user.idToken?.tokenString,
            error: nil
        )
    }
}
