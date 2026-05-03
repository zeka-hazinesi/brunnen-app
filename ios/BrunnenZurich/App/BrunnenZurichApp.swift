import SwiftUI
import FirebaseCore
import GoogleSignIn

@main
struct BrunnenZurichApp: App {
    @StateObject private var auth = AuthService()
    @StateObject private var location = LocationService()

    init() {
        FirebaseApp.configure()
    }

    var body: some Scene {
        WindowGroup {
            RootView()
                .environmentObject(auth)
                .environmentObject(location)
                .preferredColorScheme(.light)
                .onOpenURL { url in
                    GIDSignIn.sharedInstance.handle(url)
                }
        }
    }
}
