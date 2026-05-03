import Foundation
import FirebaseFirestore

@MainActor
final class LeaderboardService: ObservableObject {
    @Published var publicEntries: [UserProfile] = []
    @Published var friendEntries: [UserProfile] = []

    private let db = Firestore.firestore()

    func loadPublic(limit: Int = 100) async {
        do {
            let snap = try await db.collection("users")
                .order(by: "points", descending: true)
                .limit(to: limit)
                .getDocuments()
            publicEntries = snap.documents.compactMap(Self.profile(from:))
        } catch {
            publicEntries = []
        }
    }

    func loadFriends(_ profile: UserProfile) async {
        let ids = profile.friends + [profile.uid]
        guard !ids.isEmpty else { friendEntries = []; return }
        do {
            // Firestore `in` queries are limited to 30; chunk if needed.
            var results: [UserProfile] = []
            for chunk in ids.chunked(into: 30) {
                let snap = try await db.collection("users")
                    .whereField("uid", in: chunk)
                    .getDocuments()
                results.append(contentsOf: snap.documents.compactMap(Self.profile(from:)))
            }
            friendEntries = results.sorted { $0.points > $1.points }
        } catch {
            friendEntries = []
        }
    }

    func addFriend(currentUid: String, nickname: String) async throws {
        let trimmed = nickname.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty else { return }
        let snap = try await db.collection("users")
            .whereField("nickname", isEqualTo: trimmed)
            .limit(to: 1)
            .getDocuments()
        guard let friendUid = snap.documents.first?.data()["uid"] as? String,
              friendUid != currentUid else { return }
        try await db.collection("users").document(currentUid)
            .updateData(["friends": FieldValue.arrayUnion([friendUid])])
    }

    private static func profile(from doc: QueryDocumentSnapshot) -> UserProfile? {
        let data = doc.data()
        guard let uid = data["uid"] as? String else { return nil }
        return UserProfile(
            uid: uid,
            email: data["email"] as? String ?? "",
            nickname: data["nickname"] as? String ?? "",
            avatarUrl: data["avatarUrl"] as? String,
            points: data["points"] as? Int ?? 0,
            checkInCount: data["checkInCount"] as? Int ?? 0,
            friends: data["friends"] as? [String] ?? []
        )
    }
}

extension Array {
    func chunked(into size: Int) -> [[Element]] {
        stride(from: 0, to: count, by: size).map { Array(self[$0..<Swift.min($0 + size, count)]) }
    }
}
