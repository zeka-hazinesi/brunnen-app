import CoreLocation

struct Fountain: Identifiable, Hashable {
    let id: Int
    let name: String
    let location: String?
    let quartier: String?
    let baujahr: Int?
    let brunnenNummer: String?
    let wasserart: String?
    let isTrinkwasser: Bool
    let latitude: Double
    let longitude: Double
    let photoUrl: String?
    let isAbgestellt: Bool

    var coordinate: CLLocationCoordinate2D {
        CLLocationCoordinate2D(latitude: latitude, longitude: longitude)
    }

    var clLocation: CLLocation {
        CLLocation(latitude: latitude, longitude: longitude)
    }
}

struct CheckIn: Identifiable, Codable {
    var id: String = UUID().uuidString
    var userId: String
    var fountainId: Int
    var fountainName: String
    var timestamp: Date
    var points: Int
}

struct UserProfile: Identifiable, Codable, Hashable {
    var id: String { uid }
    var uid: String
    var email: String
    var nickname: String
    var avatarUrl: String?
    var points: Int
    var checkInCount: Int
    var friends: [String]
}
