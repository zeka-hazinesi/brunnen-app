import SwiftUI

struct RootView: View {
    @EnvironmentObject var auth: AuthService

    var body: some View {
        Group {
            switch auth.state {
            case .loading:
                ProgressView().tint(Theme.waterTeal)
            case .signedOut:
                LoginView()
            case .needsNickname(let user):
                NicknameView(user: user)
            case .signedIn:
                MainTabView()
            }
        }
        .task { await auth.bootstrap() }
    }
}

struct MainTabView: View {
    var body: some View {
        TabView {
            CompassView()
                .tabItem { Label("Kompass", systemImage: "location.north.line.fill") }
            MapScreen()
                .tabItem { Label("Karte", systemImage: "map.fill") }
            FountainListView()
                .tabItem { Label("Brunnen", systemImage: "drop.fill") }
            LeaderboardView()
                .tabItem { Label("Rangliste", systemImage: "trophy.fill") }
        }
        .tint(Theme.waterTeal)
    }
}
