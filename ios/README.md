# Brunnen Zürich – iOS

Native SwiftUI port der Android-App. Vier Tabs (Kompass, Karte, Brunnenliste, Rangliste), Google-Login via Firebase Auth, Check-ins/Leaderboard via Firestore, MapKit für die Karte, 1287 Brunnen aus `brunnen.json`.

## Voraussetzungen

- Xcode 15+
- iOS 16+ Deployment Target
- [XcodeGen](https://github.com/yonaskolb/XcodeGen): `brew install xcodegen`
- Firebase Projekt (kann dasselbe sein wie für Android)

## Setup

1. **Firebase iOS App registrieren** (Bundle ID `app.brunnen.zurich`).
2. `GoogleService-Info.plist` aus der Firebase Console laden und nach `BrunnenZurich/Resources/GoogleService-Info.plist` legen.
3. In `project.yml` zwei Werte ersetzen:
   - `REPLACE_WITH_REVERSED_CLIENT_ID` → Wert von `REVERSED_CLIENT_ID` aus dem plist (z. B. `com.googleusercontent.apps.123-abc`).
   - `REPLACE_WITH_GID_CLIENT_ID` → Wert von `CLIENT_ID` aus dem plist.
4. `xcodegen generate` im Projekt-Root ausführen.
5. `BrunnenZurich.xcodeproj` öffnen, Signing Team setzen, Run.

## Architektur

```
BrunnenZurich/
├── App/                  Einstieg, Theme, Root-Navigation
├── Models/               Fountain, UserProfile, CheckIn
├── Services/             FountainRepository, LocationService, AuthService,
│                         CheckInService, LeaderboardService
├── Views/                Login, Nickname, Compass, Map, List, Detail, Leaderboard
└── Resources/            brunnen.json (1287 Brunnen, GeoJSON)
```

## Mapping zur Android-App

| Android | iOS |
| --- | --- |
| Jetpack Compose | SwiftUI |
| osmdroid | MapKit |
| FusedLocationProvider | CoreLocation `CLLocationManager` |
| Rotation Vector Sensor | `CLLocationManager.startUpdatingHeading` |
| FirebaseAuth + Credential Manager | FirebaseAuth + GoogleSignIn-iOS |
| Cloud Firestore | Cloud Firestore (gleiches Schema) |

Firestore-Collections sind kompatibel mit dem Android-Client (`users`, `nicknames`, `users/{uid}/checkIns`).

## Offene TODOs

- App Icon einsetzen (`Assets.xcassets/AppIcon.appiconset`).
- Foto-Upload beim Check-in (Storage), wenn entschieden.
- Firestore Security Rules vom Android-Projekt mitnutzen (`../brunnen-app/firestore.rules`).
