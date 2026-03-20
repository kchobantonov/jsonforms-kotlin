import SwiftUI
import UIKit
import ExamplesComposeApp

private struct ComposeRootViewController: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.MainViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {
    }
}

@main
struct ExamplesComposeIosApp: App {
    var body: some Scene {
        WindowGroup {
            ComposeRootViewController()
                .ignoresSafeArea()
        }
    }
}
