# 🚰 Trinkbrunnen-App Zürich – Planungsstand

## Plattform

- Native Android App

## Datenquelle

- JSON-Datei mit Zürcher Trinkbrunnen
- Felder: Name, Baujahr, Trinkwasser ja/nein, Koordinaten

---

## 📱 Screens & Navigation

### Login-Screen _(erster Screen nach App-Download)_

- Google Login (einzige Login-Methode)
- Nickname-Vergabe beim ersten Login

---

### Tab 1 – Kompass _(Haupt-Screen)_

- Zeigt Richtung zum nächsten Brunnen (Kompass-Darstellung)
- Zeigt Entfernung zum nächsten Brunnen
- **Check-in Button** (nur sichtbar wenn User ≤ 15–20m vom Brunnen entfernt)
  - Nach Tap: 10 Sekunden Verifizierung
  - GPS wird kontinuierlich geprüft
  - User muss die gesamten 10 Sek. im 20m-Radius bleiben
  - Bei Verlassen des Radius → Check-in ungültig
  - Bei Erfolg → Punkte werden gutgeschrieben
- Foto beim Check-in? → **noch offen**

---

### Tab 2 – Brunnenliste

- Alle Brunnen sortiert nach Entfernung (nächster zuerst)
- Nutzt GPS-Standort des Users
- **List-Item zeigt:**
  - 📷 Thumbnail (Foto)
  - 📛 Name des Brunnens
  - 📅 Baujahr
  - 💧 Trinkwasser ja/nein
- Tap auf Brunnen → Detail-Screen _(noch auszuarbeiten)_
- Brunnen ohne Foto → User kann Foto hochladen

---

### Tab 3 – Leaderboard

- **Unter-Tab: Public** – alle User, sortiert nach gesammelten Punkten
- **Unter-Tab: Privat** – nur Freunde
  - Freunde per Nickname einladen

---

## 🏆 Gamification

- Punkte pro erfolgreichem Check-in
- Punkteanzahl pro Check-in → **noch offen**
- Jeder Brunnen soll ein Foto haben (Community-driven)

---

## 👤 Account-System

- Pflicht: Jeder User braucht einen Account
- Login: **nur via Google**
- Nickname frei wählbar (wird im Leaderboard angezeigt)
- Freunde hinzufügen per Nickname

---

## 🔧 Backend _(Empfehlung: Firebase)_

- Google Authentication
- User-Datenbank (Google ID, Nickname, Punkte, Check-ins)
- Freundschafts-System
- Foto-Storage

---

## ❓ Offene Punkte

- [ ] Foto beim Check-in verpflichtend oder optional?
- [ ] Wie viele Punkte pro Check-in?
- [ ] Gibt es seltenere/wertvollere Brunnen?
- [ ] Detail-Screen: Was soll alles angezeigt werden?
- [ ] Backend-Wahl bestätigen (Firebase?)
