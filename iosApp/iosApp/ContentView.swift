import SwiftUI
import ComposeApp

// Hosts the shared Compose UI. `MainViewControllerKt.MainViewController()` is the
// Kotlin function in composeApp/src/iosMain — the same `App` composable Android runs.
struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.MainViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

struct ContentView: View {
    var body: some View {
        ComposeView().ignoresSafeArea(.keyboard)
    }
}
