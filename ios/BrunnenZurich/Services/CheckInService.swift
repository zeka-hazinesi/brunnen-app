import Foundation
import FirebaseAuth
import FirebaseFirestore

enum CheckInError: LocalizedError {
    case notSignedIn

    var errorDescription: String? {
        switch self {
        case .notSignedIn: return "Nicht eingeloggt"
        }
    }
}

@MainActor
final class CheckInService {
    static let shared = CheckInService()

    private let db = Firestore.firestore()
    private init() {}

    func recordCheckIn(fountain: Fountain, points: Int) async throws {
        guard let user = Auth.auth().currentUser else { throw CheckInError.notSignedIn }
        let userRef = db.collection("users").document(user.uid)

        try await db.collection("checkins").addDocument(data: [
            "userId": user.uid,
            "fountainId": fountain.id,
            "fountainName": fountain.name,
            "timestamp": FieldValue.serverTimestamp(),
            "points": points,
        ])

        _ = try await db.runTransaction { txn, errPtr -> Any? in
            do {
                let snap = try txn.getDocument(userRef)
                let currentPoints = snap.data()?["points"] as? Int ?? 0
                let currentCount = snap.data()?["checkInCount"] as? Int ?? 0
                txn.updateData([
                    "points": currentPoints + points,
                    "checkInCount": currentCount + 1,
                ], forDocument: userRef)
                return nil
            } catch {
                errPtr?.pointee = error as NSError
                return nil
            }
        }
    }

    func visitedFountainIds() async throws -> Set<Int> {
        guard let user = Auth.auth().currentUser else { return [] }
        let snap = try await db.collection("checkins")
            .whereField("userId", isEqualTo: user.uid)
            .getDocuments()
        return Set(snap.documents.compactMap { $0.data()["fountainId"] as? Int })
    }
}
