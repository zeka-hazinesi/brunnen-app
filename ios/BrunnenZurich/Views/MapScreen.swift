import SwiftUI
import MapKit

struct MapScreen: View {
    @EnvironmentObject var location: LocationService
    @State private var selected: Fountain?
    @State private var recenterToken = UUID()

    var body: some View {
        ZStack(alignment: .bottomTrailing) {
            FountainMap(
                fountains: FountainRepository.shared.fountains,
                selected: $selected,
                recenterToken: recenterToken,
                userLocation: location.location?.coordinate
            )
            .ignoresSafeArea(edges: .top)

            Button { recenterToken = UUID() } label: {
                Image(systemName: "location.fill")
                    .font(.system(size: 18, weight: .semibold))
                    .foregroundColor(Theme.waterTeal)
                    .padding(14)
                    .background(.white)
                    .clipShape(Circle())
                    .shadow(color: .black.opacity(0.15), radius: 4, y: 2)
            }
            .padding(.trailing, 16)
            .padding(.bottom, 24)
        }
        .onAppear {
            FountainRepository.shared.loadIfNeeded()
            if location.authorizationStatus == .notDetermined { location.requestPermission() }
            else { location.start() }
        }
        .sheet(item: $selected) { f in
            FountainDetailView(fountain: f)
                .presentationDetents([.medium, .large])
        }
    }
}

private final class FountainAnnotation: NSObject, MKAnnotation {
    let fountain: Fountain
    var coordinate: CLLocationCoordinate2D { fountain.coordinate }
    var title: String? { fountain.name }

    init(_ fountain: Fountain) {
        self.fountain = fountain
    }
}

struct FountainMap: UIViewRepresentable {
    let fountains: [Fountain]
    @Binding var selected: Fountain?
    let recenterToken: UUID
    let userLocation: CLLocationCoordinate2D?

    func makeCoordinator() -> Coordinator { Coordinator(self) }

    func makeUIView(context: Context) -> MKMapView {
        let map = MKMapView()
        map.delegate = context.coordinator
        map.showsUserLocation = true
        map.pointOfInterestFilter = .excludingAll
        map.register(FountainMarkerView.self,
                     forAnnotationViewWithReuseIdentifier: FountainMarkerView.reuseID)
        map.register(MKMarkerAnnotationView.self,
                     forAnnotationViewWithReuseIdentifier: MKMapViewDefaultClusterAnnotationViewReuseIdentifier)
        map.setRegion(MKCoordinateRegion(
            center: CLLocationCoordinate2D(latitude: 47.3769, longitude: 8.5417),
            span: MKCoordinateSpan(latitudeDelta: 0.06, longitudeDelta: 0.06)
        ), animated: false)
        map.addAnnotations(fountains.map(FountainAnnotation.init))
        return map
    }

    func updateUIView(_ uiView: MKMapView, context: Context) {
        if context.coordinator.lastRecenter != recenterToken, let coord = userLocation {
            context.coordinator.lastRecenter = recenterToken
            uiView.setRegion(MKCoordinateRegion(
                center: coord,
                span: MKCoordinateSpan(latitudeDelta: 0.005, longitudeDelta: 0.005)
            ), animated: true)
        }
    }

    final class Coordinator: NSObject, MKMapViewDelegate {
        let parent: FountainMap
        var lastRecenter: UUID?
        init(_ parent: FountainMap) { self.parent = parent }

        func mapView(_ mapView: MKMapView, viewFor annotation: MKAnnotation) -> MKAnnotationView? {
            if annotation is MKUserLocation { return nil }
            if let cluster = annotation as? MKClusterAnnotation {
                let view = mapView.dequeueReusableAnnotationView(
                    withIdentifier: MKMapViewDefaultClusterAnnotationViewReuseIdentifier,
                    for: cluster
                ) as! MKMarkerAnnotationView
                view.markerTintColor = UIColor(red: 0.094, green: 0.624, blue: 0.671, alpha: 1)
                view.glyphText = "\(cluster.memberAnnotations.count)"
                view.displayPriority = .defaultHigh
                return view
            }
            guard let f = annotation as? FountainAnnotation else { return nil }
            let view = mapView.dequeueReusableAnnotationView(
                withIdentifier: FountainMarkerView.reuseID, for: f
            ) as! FountainMarkerView
            view.configure(for: f.fountain)
            return view
        }

        func mapView(_ mapView: MKMapView, didSelect view: MKAnnotationView) {
            if let cluster = view.annotation as? MKClusterAnnotation {
                let rect = cluster.memberAnnotations.reduce(MKMapRect.null) { acc, ann in
                    let p = MKMapPoint(ann.coordinate)
                    return acc.union(MKMapRect(x: p.x, y: p.y, width: 0, height: 0))
                }
                mapView.setVisibleMapRect(rect.insetBy(dx: -rect.size.width * 0.5,
                                                      dy: -rect.size.height * 0.5),
                                          animated: true)
                mapView.deselectAnnotation(cluster, animated: false)
                return
            }
            if let f = view.annotation as? FountainAnnotation {
                parent.selected = f.fountain
                mapView.deselectAnnotation(f, animated: false)
            }
        }
    }
}

private final class FountainMarkerView: MKMarkerAnnotationView {
    static let reuseID = "FountainMarker"

    override init(annotation: MKAnnotation?, reuseIdentifier: String?) {
        super.init(annotation: annotation, reuseIdentifier: reuseIdentifier)
        clusteringIdentifier = "fountain"
        displayPriority = .defaultLow
        glyphImage = UIImage(systemName: "drop.fill")
    }

    required init?(coder: NSCoder) { fatalError() }

    func configure(for fountain: Fountain) {
        if fountain.isAbgestellt {
            markerTintColor = .systemGray
        } else if fountain.isTrinkwasser {
            markerTintColor = UIColor(red: 0.094, green: 0.624, blue: 0.671, alpha: 1)
        } else {
            markerTintColor = UIColor(red: 0.863, green: 0.231, blue: 0.275, alpha: 1)
        }
    }
}
