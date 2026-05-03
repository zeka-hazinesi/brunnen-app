import SwiftUI

struct LoginView: View {
    @EnvironmentObject var auth: AuthService
    @State private var error: String?
    @State private var loading = false

    var body: some View {
        ZStack {
            LinearGradient(colors: [Theme.waterTeal, Theme.waterDark],
                           startPoint: .topLeading, endPoint: .bottomTrailing)
                .ignoresSafeArea()

            VStack(spacing: 32) {
                Spacer()
                Image(systemName: "drop.fill")
                    .font(.system(size: 96, weight: .bold))
                    .foregroundColor(.white)
                Text("Brunnen Zürich")
                    .font(.system(size: 36, weight: .bold))
                    .foregroundColor(.white)
                Text("1287 Brunnen entdecken,\neinchecken, sammeln.")
                    .multilineTextAlignment(.center)
                    .foregroundColor(.white.opacity(0.9))
                Spacer()

                Button {
                    Task {
                        loading = true
                        defer { loading = false }
                        do { try await auth.signInWithGoogle() }
                        catch { self.error = error.localizedDescription }
                    }
                } label: {
                    HStack {
                        if loading {
                            ProgressView().tint(Theme.waterDark)
                        } else {
                            Image(systemName: "person.crop.circle.fill")
                            Text("Mit Google anmelden").fontWeight(.semibold)
                        }
                    }
                    .frame(maxWidth: .infinity, minHeight: 52)
                    .background(.white)
                    .foregroundColor(Theme.waterDark)
                    .clipShape(RoundedRectangle(cornerRadius: 14))
                }
                .disabled(loading)

                if let error {
                    Text(error)
                        .font(.footnote)
                        .foregroundColor(.white)
                        .multilineTextAlignment(.center)
                }
            }
            .padding(32)
        }
    }
}
