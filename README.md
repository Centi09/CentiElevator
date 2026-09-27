# CentiElevator

Modernes, ressourcenschonendes Aufzugssystem fuer Paper- und Purpur-Server (1.21+).

## Features

- **Tageslichtsensor-Steuerung**: Nutzt standardmaessig flache Tageslichtsensoren (`DAYLIGHT_DETECTOR`) als Aufzugsplatte.
- **Smart-Toggle (Gelb / Grau)**:
  - **Gelb (Tag-Modus)**: Aufzug ist aktiv und betriebsbereit.
  - **Grau (Nacht-Modus)**: Etage ist ausser Betrieb und wird beim Fahren automatisch uebersprungen.
- **Einfache Bedienung**:
  - **Springen (Leertaste)**: Faehrt zur naechsten aktiven Etage nach oben.
  - **Ducken (Shift)**: Faehrt zur naechsten aktiven Etage nach unten.
- **Live-Actionbar**: Zeigt die aktuelle Etage und Gesamtzahl an (z.B. `[▲] Etage 2/4`).
- **Erstickungsschutz**: Prueft vor dem Teleportieren automatisch, ob ueber dem Ziel-Sensor genuegend freier Platz fuer den Kopf ist.
- **Spam-Schutz**: Konfigurierbarer Cooldown (Standard: 350ms) verhindert versehentliches Durchfahren mehrerer Etagen.
- **100% Konfigurierbar**: Bloecke, Sounds, Partikel und Nachrichten frei in der `config.yml` anpassbar.

## Befehle & Permissions

| Befehl | Permission | Beschreibung |
|---|---|---|
| `/centielevator reload` | `centielevator.admin` | Laedt die Konfiguration neu |
| `/ce reload` | `centielevator.admin` | Kurzform fuer den Reload-Befehl |
| - | `centielevator.use` | Erlaubt Spielern das Benutzen von Aufzuegen (Standard: true) |

## Installation

1. Lade die neueste `CentiElevator-1.0.0.jar` aus den Releases herunter.
2. Platziere die Datei im `/plugins` Ordner deines Paper- oder Purpur-Servers.
3. Server starten oder neustarten.
4. Platziere zwei Tageslichtsensoren senkrecht uebereinander und teste den Aufzug.

## Lizenz

Entwickelt von Centi09. Alle Rechte vorbehalten.  
Website: https://centi09.de
