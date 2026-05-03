import SwiftUI
import CoreLocation

private let checkInRadius: CLLocationDistance = 20
private let checkInDuration = 10
private let pointsPerCheckIn = 10

private enum CheckInPhase: Equatable {
    case idle
    case verifying(Int)
    case success
}

struct CompassView: View {
    @EnvironmentObject var location: LocationService
    @EnvironmentObject var auth: AuthService
    @State private var nearest: Fountain?
    @State private var showProfile = false
    @State private var distance: CLLocationDistance = .greatestFiniteMagnitude
    @State private var bearing: CLLocationDirection = 0
    @State private var visited: Set<Int> = []
    @State private var phase: CheckInPhase = .idle
    @State private var message: String?
    @State private var verifyTask: Task<Void, Never>?

    var body: some View {
        Group {
            switch location.authorizationStatus {
            case .notDetermined, .denied, .restricted:
                permissionPrompt
            default:
                if location.location == nil {
                    loading
                } else {
                    compassContent
                }
            }
        }
        .onAppear {
            FountainRepository.shared.loadIfNeeded()
            if location.authorizationStatus == .notDetermined {
                location.requestPermission()
            } else {
                location.start()
            }
            Task { visited = (try? await CheckInService.shared.visitedFountainIds()) ?? [] }
        }
        .onChange(of: location.location) { _, newValue in
            guard let loc = newValue else { return }
            recompute(loc)
        }
        .onChange(of: visited) { _, _ in
            if let loc = location.location { recompute(loc) }
        }
        .sheet(isPresented: $showProfile) {
            ProfileSheet().environmentObject(auth)
        }
    }

    private var permissionPrompt: some View {
        VStack(spacing: 16) {
            Image(systemName: "location.fill").font(.system(size: 44)).foregroundColor(Theme.waterTeal)
            Text("Standort benötigt").font(.title3.bold())
            Text("Die App braucht deinen Standort, um den nächsten Brunnen zu finden.")
                .multilineTextAlignment(.center).foregroundColor(Theme.textSecondary)
            Button("Standort freigeben") { location.requestPermission() }
                .buttonStyle(.borderedProminent).tint(Theme.waterTeal)
        }
        .padding(32)
    }

    private var loading: some View {
        VStack(spacing: 12) {
            ProgressView().tint(Theme.waterTeal)
            Text("Standort wird ermittelt...").foregroundColor(Theme.textSecondary)
        }
    }

    private var compassContent: some View {
        let isNearby = distance <= checkInRadius
        let relativeAngle = bearing - location.heading

        return VStack(spacing: 16) {
            HStack {
                Spacer()
                Button { showProfile = true } label: {
                    HStack(spacing: 6) {
                        Image(systemName: "person.crop.circle.fill")
                        if let p = auth.profile {
                            Text("\(p.points)").bold().monospacedDigit()
                        }
                    }
                    .padding(.horizontal, 12).padding(.vertical, 6)
                    .background(.white)
                    .clipShape(Capsule())
                    .shadow(color: .black.opacity(0.06), radius: 3, y: 1)
                }
                .foregroundColor(Theme.waterTeal)
            }
            if let f = nearest {
                fountainCard(f, isNearby: isNearby)
            }

            ZStack {
                CompassDial(angle: relativeAngle, isNearby: isNearby)
                    .frame(width: 280, height: 280)
                VStack(spacing: 0) {
                    Text(distanceText).font(.system(size: 48, weight: .bold))
                        .foregroundColor(isNearby ? Theme.successGreen : Theme.textPrimary)
                    Text(unitText).font(.subheadline).foregroundColor(Theme.textSecondary)
                }
            }
            .padding(.vertical, 8)

            checkInArea(isNearby: isNearby)
            Spacer(minLength: 0)
        }
        .padding(.horizontal, 24)
        .padding(.top, 16)
    }

    private func fountainCard(_ f: Fountain, isNearby: Bool) -> some View {
        HStack(spacing: 14) {
            ZStack {
                RoundedRectangle(cornerRadius: 12).fill(Theme.waterTeal.opacity(0.12))
                Image(systemName: "drop.fill").foregroundColor(Theme.waterTeal)
            }
            .frame(width: 44, height: 44)
            VStack(alignment: .leading, spacing: 2) {
                Text(f.name).font(.subheadline.bold()).lineLimit(1).foregroundColor(Theme.textPrimary)
                if let q = f.quartier {
                    Text(q).font(.caption).foregroundColor(Theme.textSecondary)
                }
            }
            Spacer()
            Image(systemName: "location.north.line.fill")
                .foregroundColor(isNearby ? Theme.successGreen : Theme.textSecondary.opacity(0.4))
        }
        .padding(16)
        .background(.white)
        .clipShape(RoundedRectangle(cornerRadius: 18))
        .shadow(color: .black.opacity(0.04), radius: 4, y: 2)
    }

    @ViewBuilder
    private func checkInArea(isNearby: Bool) -> some View {
        switch phase {
        case .idle:
            if isNearby {
                Button(action: startCheckIn) {
                    Text("Einchecken").fontWeight(.bold)
                        .frame(maxWidth: .infinity, minHeight: 56)
                        .background(Theme.successGreen)
                        .foregroundColor(.white)
                        .clipShape(RoundedRectangle(cornerRadius: 16))
                }
            } else {
                Text("Nähere dich einem Brunnen auf \(Int(checkInRadius))m")
                    .font(.subheadline)
                    .foregroundColor(Theme.textSecondary)
                    .padding(.horizontal, 20).padding(.vertical, 14)
                    .frame(maxWidth: .infinity)
                    .background(.white.opacity(0.7))
                    .clipShape(RoundedRectangle(cornerRadius: 14))
            }
            if let message {
                Text(message).font(.footnote).foregroundColor(Theme.errorRed)
                    .multilineTextAlignment(.center)
            }
        case .verifying(let countdown):
            VStack(spacing: 8) {
                Text("VERIFIZIERUNG").font(.caption.bold()).tracking(1.5).foregroundColor(.orange)
                Text("\(countdown)s").font(.system(size: 44, weight: .bold))
                Text("Bleib in der Nähe des Brunnens").font(.subheadline).foregroundColor(Theme.textSecondary)
            }
            .frame(maxWidth: .infinity).padding(24)
            .background(Color(red: 1.0, green: 0.984, blue: 0.92))
            .clipShape(RoundedRectangle(cornerRadius: 18))
        case .success:
            VStack(spacing: 6) {
                Text("Eingecheckt!").font(.headline).foregroundColor(Theme.successGreen)
                Text("+\(pointsPerCheckIn) Punkte!").font(.title.bold()).foregroundColor(Theme.successGreen)
            }
            .frame(maxWidth: .infinity).padding(24)
            .background(Color(red: 0.94, green: 0.992, blue: 0.957))
            .clipShape(RoundedRectangle(cornerRadius: 18))
        }
    }

    private var distanceText: String {
        if distance < 1000 { return "\(Int(distance.rounded()))" }
        return String(format: "%.1f", distance / 1000)
    }
    private var unitText: String { distance < 1000 ? "m" : "km" }

    private func recompute(_ loc: CLLocation) {
        let sorted = FountainRepository.shared.sortedByDistance(from: loc)
            .first { !visited.contains($0.fountain.id) }
        guard let next = sorted else { return }
        nearest = next.fountain
        distance = next.distance
        bearing = bearingFrom(loc.coordinate, to: next.fountain.coordinate)
    }

    private func startCheckIn() {
        guard let fountain = nearest else { return }
        message = nil
        verifyTask?.cancel()
        verifyTask = Task {
            for i in stride(from: checkInDuration, through: 1, by: -1) {
                phase = .verifying(i)
                try? await Task.sleep(nanoseconds: 1_000_000_000)
                if Task.isCancelled { return }
                if distance > checkInRadius {
                    phase = .idle
                    message = "Check-in abgebrochen — du hast den Radius verlassen."
                    return
                }
            }
            do {
                try await CheckInService.shared.recordCheckIn(fountain: fountain, points: pointsPerCheckIn)
                visited.insert(fountain.id)
                phase = .success
                try? await Task.sleep(nanoseconds: 3_000_000_000)
                phase = .idle
            } catch {
                phase = .idle
                message = error.localizedDescription
            }
        }
    }

    private func bearingFrom(_ from: CLLocationCoordinate2D, to: CLLocationCoordinate2D) -> CLLocationDirection {
        let lat1 = from.latitude * .pi / 180
        let lat2 = to.latitude * .pi / 180
        let dLon = (to.longitude - from.longitude) * .pi / 180
        let y = sin(dLon) * cos(lat2)
        let x = cos(lat1) * sin(lat2) - sin(lat1) * cos(lat2) * cos(dLon)
        let deg = atan2(y, x) * 180 / .pi
        return (deg + 360).truncatingRemainder(dividingBy: 360)
    }
}

private struct CompassDial: View {
    let angle: Double
    let isNearby: Bool

    var body: some View {
        let accent = isNearby ? Theme.successGreen : Theme.waterTeal
        Canvas { ctx, size in
            let center = CGPoint(x: size.width / 2, y: size.height / 2)
            let radius = min(size.width, size.height) / 2
            ctx.stroke(Path(ellipseIn: CGRect(x: 0, y: 0, width: size.width, height: size.height)),
                       with: .color(Color(red: 0.886, green: 0.910, blue: 0.941)), lineWidth: 2)
            ctx.fill(Path(ellipseIn: CGRect(x: 2, y: 2, width: size.width - 4, height: size.height - 4)),
                     with: .color(.white.opacity(0.6)))
            for i in 0..<72 {
                let isMajor = i % 18 == 0
                let isMinor = i % 9 == 0
                let length: CGFloat = isMajor ? 18 : (isMinor ? 10 : 5)
                let width: CGFloat = isMajor ? 2.5 : 1
                let color = isMajor ? Color(red: 0.39, green: 0.45, blue: 0.55)
                                    : Color(red: 0.80, green: 0.84, blue: 0.88)
                var path = Path()
                path.move(to: CGPoint(x: center.x, y: center.y - radius + 10))
                path.addLine(to: CGPoint(x: center.x, y: center.y - radius + 10 + length))
                let rotated = path.applying(CGAffineTransform(translationX: -center.x, y: -center.y)
                    .concatenating(CGAffineTransform(rotationAngle: Double(i) * 5 * .pi / 180))
                    .concatenating(CGAffineTransform(translationX: center.x, y: center.y)))
                ctx.stroke(rotated, with: .color(color), style: StrokeStyle(lineWidth: width, lineCap: .round))
            }
            var arrow = Path()
            arrow.move(to: CGPoint(x: center.x, y: center.y - radius * 0.62))
            arrow.addLine(to: CGPoint(x: center.x - 14, y: center.y - radius * 0.30))
            arrow.addLine(to: CGPoint(x: center.x, y: center.y - radius * 0.38))
            arrow.addLine(to: CGPoint(x: center.x + 14, y: center.y - radius * 0.30))
            arrow.closeSubpath()
            let rotated = arrow.applying(CGAffineTransform(translationX: -center.x, y: -center.y)
                .concatenating(CGAffineTransform(rotationAngle: angle * .pi / 180))
                .concatenating(CGAffineTransform(translationX: center.x, y: center.y)))
            ctx.fill(rotated, with: .color(accent))
            ctx.fill(Path(ellipseIn: CGRect(x: center.x - 7, y: center.y - 7, width: 14, height: 14)),
                     with: .color(Color(red: 0.886, green: 0.910, blue: 0.941)))
            ctx.fill(Path(ellipseIn: CGRect(x: center.x - 4, y: center.y - 4, width: 8, height: 8)),
                     with: .color(accent))
        }
        .animation(.easeInOut(duration: 0.3), value: angle)
    }
}
