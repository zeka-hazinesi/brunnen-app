import SwiftUI

struct ProfileSheet: View {
    @EnvironmentObject var auth: AuthService
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        NavigationStack {
            VStack(spacing: 24) {
                if let p = auth.profile {
                    avatar(p)
                    Text(p.nickname).font(.title2.bold())
                    Text(p.email).font(.footnote).foregroundColor(Theme.textSecondary)

                    HStack(spacing: 32) {
                        stat("Punkte", "\(p.points)")
                        stat("Check-ins", "\(p.checkInCount)")
                        stat("Freunde", "\(p.friends.count)")
                    }
                    .padding(.top, 12)
                }

                Spacer()

                Button(role: .destructive) {
                    auth.signOut()
                    dismiss()
                } label: {
                    Text("Abmelden")
                        .frame(maxWidth: .infinity, minHeight: 48)
                }
                .buttonStyle(.bordered)
            }
            .padding(24)
            .navigationTitle("Profil")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button("Schliessen") { dismiss() }
                }
            }
        }
        .presentationDetents([.medium, .large])
    }

    private func avatar(_ p: UserProfile) -> some View {
        Group {
            if let url = p.avatarUrl.flatMap(URL.init(string:)) {
                AsyncImage(url: url) { phase in
                    if case .success(let img) = phase { img.resizable().scaledToFill() }
                    else { Theme.waterTeal.opacity(0.2) }
                }
            } else {
                ZStack {
                    Theme.waterTeal.opacity(0.2)
                    Text(String(p.nickname.prefix(1)).uppercased())
                        .font(.system(size: 36, weight: .bold))
                        .foregroundColor(Theme.waterTeal)
                }
            }
        }
        .frame(width: 88, height: 88)
        .clipShape(Circle())
    }

    private func stat(_ label: String, _ value: String) -> some View {
        VStack(spacing: 4) {
            Text(value).font(.title3.bold()).foregroundColor(Theme.waterTeal)
            Text(label).font(.caption).foregroundColor(Theme.textSecondary)
        }
    }
}
