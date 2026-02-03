import SwiftUI
import ComposeApp

struct ContentView: View {
    init() {
        // Initialize GoogleSignInBridge early to register Kotlin callbacks
        _ = GoogleSignInBridge.shared
    }

    var body: some View {
        ComposeView()
            .ignoresSafeArea(.all)
    }
}

struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.MainViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}
