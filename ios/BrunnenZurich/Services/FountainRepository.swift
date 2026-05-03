import Foundation
import CoreLocation

final class FountainRepository {
    static let shared = FountainRepository()

    private(set) var fountains: [Fountain] = []
    private var loaded = false

    private init() {}

    func loadIfNeeded() {
        guard !loaded else { return }
        loaded = true
        guard let url = Bundle.main.url(forResource: "brunnen", withExtension: "json"),
              let data = try? Data(contentsOf: url),
              let json = try? JSONSerialization.jsonObject(with: data) as? [String: Any],
              let features = json["features"] as? [[String: Any]] else {
            return
        }

        fountains = features.compactMap { feature in
            guard let geometry = feature["geometry"] as? [String: Any],
                  let coords = geometry["coordinates"] as? [Double], coords.count >= 2,
                  let props = feature["properties"] as? [String: Any],
                  let id = props["objectid"] as? Int else { return nil }

            let wasserart = (props["wasserart"] as? String) ?? ""
            let isTrinkwasser = !wasserart.isEmpty &&
                wasserart.lowercased() != "kein trinkwasser"

            let baujahr: Int? = {
                if let h = props["historisches_baujahr"] as? Int { return h }
                return props["baujahr"] as? Int
            }()

            return Fountain(
                id: id,
                name: (props["standort"] as? String) ?? "Unbekannter Brunnen",
                location: props["ortsbezeichnung"] as? String,
                quartier: props["quartier"] as? String,
                baujahr: baujahr,
                brunnenNummer: props["brunnennummer"] as? String,
                wasserart: wasserart.isEmpty ? nil : wasserart,
                isTrinkwasser: isTrinkwasser,
                latitude: coords[1],
                longitude: coords[0],
                photoUrl: (props["foto"] as? String).flatMap { $0.isEmpty ? nil : $0 },
                isAbgestellt: ((props["abgestellt"] as? String) ?? "nein").lowercased() == "ja"
            )
        }
    }

    func sortedByDistance(from location: CLLocation) -> [(fountain: Fountain, distance: CLLocationDistance)] {
        fountains
            .map { ($0, location.distance(from: $0.clLocation)) }
            .sorted { $0.1 < $1.1 }
    }
}
