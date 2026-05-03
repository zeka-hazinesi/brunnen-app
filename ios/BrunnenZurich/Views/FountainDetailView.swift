import SwiftUI
import MapKit

struct FountainDetailView: View {
    let fountain: Fountain

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                if let url = fountain.photoUrl.flatMap(URL.init(string:)) {
                    AsyncImage(url: url) { phase in
                        switch phase {
                        case .success(let image):
                            image.resizable().scaledToFill()
                        default:
                            Theme.surface
                        }
                    }
                    .frame(height: 220)
                    .clipped()
                }

                VStack(alignment: .leading, spacing: 12) {
                    Text(fountain.name).font(.title2.bold())

                    HStack(spacing: 8) {
                        statusBadge
                        if let q = fountain.quartier {
                            Text(q).font(.caption).foregroundColor(Theme.textSecondary)
                        }
                    }

                    if let baujahr = fountain.baujahr {
                        infoRow("Baujahr", String(baujahr))
                    }
                    if let nr = fountain.brunnenNummer {
                        infoRow("Brunnen-Nr.", nr)
                    }
                    if let wasser = fountain.wasserart {
                        infoRow("Wasserart", wasser)
                    }
                    if let loc = fountain.location {
                        infoRow("Lage", loc)
                    }

                    Map(initialPosition: .region(MKCoordinateRegion(
                        center: fountain.coordinate,
                        span: MKCoordinateSpan(latitudeDelta: 0.005, longitudeDelta: 0.005)
                    ))) {
                        Marker(fountain.name, coordinate: fountain.coordinate)
                            .tint(Theme.waterTeal)
                    }
                    .frame(height: 200)
                    .clipShape(RoundedRectangle(cornerRadius: 14))
                }
                .padding(20)
            }
        }
        .ignoresSafeArea(edges: .top)
    }

    private var statusBadge: some View {
        let (text, color): (String, Color) = {
            if fountain.isAbgestellt { return ("Abgestellt", .gray) }
            if fountain.isTrinkwasser { return ("Trinkwasser", Theme.waterTeal) }
            return ("Kein Trinkwasser", Theme.errorRed)
        }()
        return Text(text)
            .font(.caption.bold())
            .padding(.horizontal, 10).padding(.vertical, 4)
            .background(color.opacity(0.15))
            .foregroundColor(color)
            .clipShape(Capsule())
    }

    private func infoRow(_ label: String, _ value: String) -> some View {
        HStack {
            Text(label).foregroundColor(Theme.textSecondary)
            Spacer()
            Text(value).foregroundColor(Theme.textPrimary)
        }
        .font(.subheadline)
    }
}
