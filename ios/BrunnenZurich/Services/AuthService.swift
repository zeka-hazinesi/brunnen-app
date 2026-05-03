import Foundation
import FirebaseAuth
import FirebaseCore
import FirebaseFirestore
import GoogleSignIn
import UIKit

enum AuthState {
    case loading
    case signedOut
    case needsNickname(FirebaseAuth.User)
    case signedIn(UserProfile)
}

@MainActor
final class AuthService: ObservableObject {
    @Published var state: AuthState = .loading
    @Published var profile: UserProfile?

    private let db = Firestore.firestore()
    private var listener: ListenerRegistration?

    func bootstrap() async {
        if let user = Auth.auth().currentUser {
            await resolveProfile(for: user)
        } else {
            state = .signedOut
        }
    }

    func signInWithGoogle() async throws {
        guard let clientID = FirebaseApp.app()?.options.clientID else {
            throw NSError(domain: "Auth", code: -1,
                          userInfo: [NSLocalizedDescriptionKey: "Missing Firebase clientID"])
        }
        GIDSignIn.sharedInstance.configuration = GIDConfiguration(clientID: clientID)

        guard let root = UIApplication.shared.connectedScenes
            .compactMap({ ($0 as? UIWindowScene)?.keyWindow?.rootViewController })
            .first else {
            throw NSError(domain: "Auth", code: -2,
                          userInfo: [NSLocalizedDescriptionKey: "No root view controller"])
        }

        let result = try await GIDSignIn.sharedInstance.signIn(withPresenting: root)
        guard let idToken = result.user.idToken?.tokenString else {
            throw NSError(domain: "Auth", code: -3,
                          userInfo: [NSLocalizedDescriptionKey: "Missing Google id token"])
        }
        let credential = GoogleAuthProvider.credential(
            withIDToken: idToken,
            accessToken: result.user.accessToken.tokenString
        )
        let authResult = try await Auth.auth().signIn(with: credential)
        await resolveProfile(for: authResult.user)
    }

    func signOut() {
        try? Auth.auth().signOut()
        GIDSignIn.sharedInstance.signOut()
        listener?.remove()
        listener = nil
        profile = nil
        state = .signedOut
    }

    func setNickname(_ nickname: String, for user: FirebaseAuth.User) async throws {
        let trimmed = nickname.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty else { return }

        let existing = try await db.collection("users")
            .whereField("nickname", isEqualTo: trimmed)
            .getDocuments()
        if existing.documents.contains(where: { $0.documentID != user.uid }) {
            throw NSError(domain: "Auth", code: 409,
                          userInfo: [NSLocalizedDescriptionKey: "Nickname bereits vergeben"])
        }

        try await db.collection("users").document(user.uid).setData([
            "uid": user.uid,
            "email": user.email ?? "",
            "nickname": trimmed,
            "avatarUrl": user.photoURL?.absoluteString ?? "",
            "points": 0,
            "checkInCount": 0,
            "friends": [],
        ], merge: true)
        await resolveProfile(for: user)
    }

    private func resolveProfile(for user: FirebaseAuth.User) async {
        let ref = db.collection("users").document(user.uid)
        do {
            let snap = try await ref.getDocument()
            if let data = snap.data(), let nickname = data["nickname"] as? String, !nickname.isEmpty {
                let profile = UserProfile(
                    uid: user.uid,
                    email: data["email"] as? String ?? user.email ?? "",
                    nickname: nickname,
                    avatarUrl: data["avatarUrl"] as? String,
                    points: data["points"] as? Int ?? 0,
                    checkInCount: data["checkInCount"] as? Int ?? 0,
                    friends: data["friends"] as? [String] ?? []
                )
                self.profile = profile
                self.state = .signedIn(profile)
                attachProfileListener(uid: user.uid)
            } else {
                self.state = .needsNickname(user)
            }
        } catch {
            self.state = .needsNickname(user)
        }
    }

    private func attachProfileListener(uid: String) {
        listener?.remove()
        listener = db.collection("users").document(uid).addSnapshotListener { [weak self] snap, _ in
            guard let self, let data = snap?.data() else { return }
            let updated = UserProfile(
                uid: uid,
                email: data["email"] as? String ?? "",
                nickname: data["nickname"] as? String ?? "",
                avatarUrl: data["avatarUrl"] as? String,
                points: data["points"] as? Int ?? 0,
                checkInCount: data["checkInCount"] as? Int ?? 0,
                friends: data["friends"] as? [String] ?? []
            )
            Task { @MainActor in
                self.profile = updated
                self.state = .signedIn(updated)
            }
        }
    }
}
