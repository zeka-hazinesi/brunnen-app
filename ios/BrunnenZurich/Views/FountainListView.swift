import SwiftUI
import CoreLocation

struct FountainListView: View {
    @EnvironmentObject var location: LocationService
    @State private var sorted: [(fountain: Fountain, distance: CLLocationDistance)] = []
    @State private var lastSortLocation: CLLocation?
    @State private var query = ""

    private var filtered: [(fountain: Fountain, distance: CLLocationDistance)] {
        guard !query.isEmpty else { return sorted }
        let q = query.lowercased()
        return sorted.filter {
            $0.fountain.name.lowercased().contains(q) ||
            ($0.fountain.quartier?.lowercased().contains(q) ?? false)
        }
    }

    var body: some View {
        NavigationStack {
            List(filtered, id: \.fountain.id) { item in
                NavigationLink(value: item.fountain) {
                    row(item)
                }
            }
            .listStyle(.plain)
            .navigationTitle("Brunnen")
            .searchable(text: $query, prompt: "Name oder Quartier")
            .navigationDestination(for: Fountain.self) { f in
                FountainDetailView(fountain: f)
            }
        }
        .onAppear {
            FountainRepository.shared.loadIfNeeded()
            if location.authorizationStatus == .notDetermined { location.requestPermission() }
            else { location.start() }
            recompute()
        }
        .onChange(of: location.location) { _, newValue in
            guard let newValue else { return }
            if let last = lastSortLocation, last.distance(from: newValue) < 25 { return }
            lastSortLocation = newValue
            recompute()
        }
    }

    private func row(_ item: (fountain: Fountain, distance: CLLocationDistance)) -> some View {
        let f = item.fountain
        return HStack(spacing: 12) {
            AsyncImage(url: f.photoUrl.flatMap(URL.init(string:))) { phase in
                switch phase {
                case .success(let image): image.resizable().scaledToFill()
                default:
                    ZStack {
                        Theme.waterTeal.opacity(0.1)
                        Image(systemName: "drop.fill").foregroundColor(Theme.waterTeal)
                    }
                }
            }
            .frame(width: 56, height: 56)
            .clipShape(RoundedRectangle(cornerRadius: 10))

            VStack(alignment: .leading, spacing: 2) {
                Text(f.name).font(.subheadline.bold()).lineLimit(1)
                HStack(spacing: 8) {
                    if let baujahr = f.baujahr { Text(String(baujahr)) }
                    if f.isAbgestellt { Text("• abgestellt").foregroundColor(.gray) }
                    else if f.isTrinkwasser { Text("• Trinkwasser").foregroundColor(Theme.waterTeal) }
                    else { Text("• kein Trinkwasser").foregroundColor(Theme.errorRed) }
                }
                .font(.caption).foregroundColor(Theme.textSecondary)
            }
            Spacer()
            Text(distanceText(item.distance))
                .font(.caption.monospacedDigit())
                .foregroundColor(Theme.textSecondary)
        }
        .padding(.vertical, 4)
    }

    private func distanceText(_ d: CLLocationDistance) -> String {
        if d.isInfinite || d == .greatestFiniteMagnitude { return "—" }
        if d < 1000 { return "\(Int(d.rounded())) m" }
        return String(format: "%.1f km", d / 1000)
    }

    private func recompute() {
        let loc = location.location ?? CLLocation(latitude: 47.3769, longitude: 8.5417)
        sorted = FountainRepository.shared.sortedByDistance(from: loc)
    }
}
