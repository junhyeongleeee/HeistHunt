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
        GoogleSignInCallback.shared.signInCallback = { [weak self] onComplete in
            self?.performSignIn()
        }

        // Set sign-out callback
        GoogleSignInCallback.shared.signOutCallback = { [weak self] in
            self?.performSignOut()
        }

        // Set get current user callback
        GoogleSignInCallback.shared.getCurrentUserCallback = { [weak self] in
            return self?.getCurrentUser()
        }
    }

    private func performSignIn() {
        // Get root view controller
        guard let scene = UIApplication.shared.connectedScenes.first as? UIWindowScene,
              let rootViewController = scene.windows.first?.rootViewController else {
            GoogleSignInCallback.shared.onSignInResult(
                success: false,
                userId: nil,
                email: nil,
                displayName: nil,
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
                    error: "Failed to get user profile"
                )
                return
            }

            let userId = user.userID ?? ""
            let email = profile.email
            let displayName = profile.name

            print("✅ Google Sign-In Success: \(email)")
            GoogleSignInCallback.shared.onSignInResult(
                success: true,
                userId: userId,
                email: email,
                displayName: displayName,
                error: nil
            )
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
            error: nil
        )
    }
}
