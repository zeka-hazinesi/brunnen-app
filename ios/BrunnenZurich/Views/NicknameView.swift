import SwiftUI
import FirebaseAuth

struct NicknameView: View {
    let user: FirebaseAuth.User
    @EnvironmentObject var auth: AuthService
    @State private var nickname = ""
    @State private var error: String?
    @State private var saving = false

    var body: some View {
        VStack(spacing: 24) {
            Spacer()
            Image(systemName: "person.crop.circle.badge.plus")
                .font(.system(size: 64))
                .foregroundColor(Theme.waterTeal)
            Text("Wähle deinen Nickname")
                .font(.title2.bold())
                .foregroundColor(Theme.textPrimary)
            Text("Wird im Leaderboard angezeigt und für Freundschaftsanfragen genutzt.")
                .multilineTextAlignment(.center)
                .foregroundColor(Theme.textSecondary)
                .padding(.horizontal, 32)

            TextField("Nickname", text: $nickname)
                .textInputAutocapitalization(.never)
                .autocorrectionDisabled()
                .padding()
                .background(Theme.surface)
                .clipShape(RoundedRectangle(cornerRadius: 14))
                .padding(.horizontal, 32)

            Button {
                Task {
                    saving = true
                    defer { saving = false }
                    do { try await auth.setNickname(nickname, for: user) }
                    catch { self.error = error.localizedDescription }
                }
            } label: {
                Text(saving ? "Speichern..." : "Weiter")
                    .fontWeight(.semibold)
                    .frame(maxWidth: .infinity, minHeight: 52)
                    .background(Theme.waterTeal)
                    .foregroundColor(.white)
                    .clipShape(RoundedRectangle(cornerRadius: 14))
            }
            .disabled(saving || nickname.trimmingCharacters(in: .whitespaces).isEmpty)
            .padding(.horizontal, 32)

            if let error {
                Text(error).font(.footnote).foregroundColor(Theme.errorRed)
            }
            Spacer()
        }
    }
}
