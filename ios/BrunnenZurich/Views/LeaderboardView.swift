import SwiftUI

struct LeaderboardView: View {
    @EnvironmentObject var auth: AuthService
    @StateObject private var service = LeaderboardService()
    @State private var tab: Scope = .publicScope
    @State private var addingFriend = false
    @State private var friendNickname = ""
    @State private var friendError: String?

    enum Scope: String, CaseIterable, Identifiable {
        case publicScope = "Public"
        case friendsScope = "Freunde"
        var id: String { rawValue }
    }

    var body: some View {
        NavigationStack {
            VStack(spacing: 0) {
                Picker("", selection: $tab) {
                    ForEach(Scope.allCases) { s in Text(s.rawValue).tag(s) }
                }
                .pickerStyle(.segmented)
                .padding()

                List(entries) { entry in
                    HStack {
                        Text("\(rank(of: entry)).").bold().foregroundColor(Theme.textSecondary).frame(width: 32)
                        Text(entry.nickname).fontWeight(entry.uid == auth.profile?.uid ? .bold : .regular)
                        Spacer()
                        Text("\(entry.points)").bold().foregroundColor(Theme.waterTeal)
                    }
                }
                .listStyle(.plain)
            }
            .navigationTitle("Rangliste")
            .toolbar {
                if tab == .friendsScope {
                    Button { addingFriend = true } label: { Image(systemName: "person.badge.plus") }
                }
            }
        }
        .task(id: tab) { await reload() }
        .alert("Freund hinzufügen", isPresented: $addingFriend) {
            TextField("Nickname", text: $friendNickname).textInputAutocapitalization(.never)
            Button("Hinzufügen") {
                Task {
                    guard let uid = auth.profile?.uid else { return }
                    do { try await service.addFriend(currentUid: uid, nickname: friendNickname) }
                    catch { friendError = error.localizedDescription }
                    friendNickname = ""
                    await reload()
                }
            }
            Button("Abbrechen", role: .cancel) { friendNickname = "" }
        } message: {
            Text(friendError ?? "Lade einen Freund per Nickname ein.")
        }
    }

    private var entries: [UserProfile] {
        tab == .publicScope ? service.publicEntries : service.friendEntries
    }

    private func rank(of entry: UserProfile) -> Int {
        (entries.firstIndex(of: entry) ?? 0) + 1
    }

    private func reload() async {
        switch tab {
        case .publicScope: await service.loadPublic()
        case .friendsScope:
            if let p = auth.profile { await service.loadFriends(p) }
        }
    }
}
