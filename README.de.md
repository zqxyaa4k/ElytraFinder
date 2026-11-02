# ElytraFinder

[English](README.md) · [Deutsch](README.de.md)

**Autor:** zqxyaa4k

Eine clientseitige Fabric-Mod für Minecraft **26.2**, die mithilfe eines Welt-Seeds und der aktuellen Spielerposition im Ende nahe Endstädte mit Schiffen sucht.

## Download

**[Neueste ElytraFinder-JAR bei GitHub Releases herunterladen](https://github.com/zqxyaa4k/ElytraFinder/releases/latest)**

Jedes GitHub-Release enthält die fertige, direkt installierbare Mod-JAR. Du musst den Quellcode nicht selbst kompilieren.

Die JAR eines normalen Entwicklungs-Builds findest du im Repository unter **Actions**: Öffne den neuesten erfolgreichen **Build**-Durchlauf und lade das Artefakt herunter, dessen Name mit `ElytraFinder-JAR-` beginnt.

## Sprachauswahl

Beim ersten Öffnen fragt ElytraFinder dich, ob du **Deutsch** oder **English** verwenden möchtest. Deine Auswahl wird lokal gespeichert und gilt für Hauptfenster, HUD und gespeicherte Seeds. Über die Schaltfläche **DE / EN** im Finder-Menü kannst du die Sprache jederzeit wechseln.

## Funktionen

- Sucht Endstädte mit Schiffen rund um die aktuelle X/Z-Position im Ende.
- Zeigt die fünf nächstgelegenen Treffer in einem Live-HUD und sortiert deren Entfernung laufend anhand der Spielerposition.
- Prüft den Schiffsbereich auf eine Elytra in einem Gegenstandsrahmen, sobald die benötigten Chunks geladen sind. Bis dahin kann der Status unbekannt sein.
- Speichert benannte Welt-Seeds lokal und merkt sich den zuletzt verwendeten Seed.
- Öffnet sich mit **J** (die Taste kann in den Minecraft-Steuerungseinstellungen geändert werden).
- Bietet Teleport-Buttons **nur im Singleplayer**. Im Multiplayer sind diese deaktiviert.

## Voraussetzungen

- Minecraft 26.2
- Java 25 oder neuer
- Fabric Loader 0.20.1 oder neuer
- Fabric API 0.161.0+26.2

Die Mod läuft clientseitig. Für die Suche muss ein numerischer Welt-Seed eingetragen sein und der Spieler muss sich im Ende befinden, damit die aktuelle Position genutzt werden kann. Im Multiplayer muss der Seed bekannt sein; die Mod liest keinen versteckten Server-Seed aus.

## Installation

1. Fabric Loader für Minecraft 26.2 installieren.
2. Die passende Fabric-API-Version installieren.
3. Die ElytraFinder-JAR von [GitHub Releases](https://github.com/zqxyaa4k/ElytraFinder/releases/latest) herunterladen.
4. Die JAR-Datei in den Minecraft-Ordner `mods` legen.
5. Minecraft mit dem Fabric-Profil starten.

## Selbst bauen

```powershell
.\gradlew.bat build
```

Die fertige JAR-Datei liegt danach in `build/libs/`. Dieser Ordner wird von Gradle automatisch erzeugt und absichtlich nicht ins Quellcode-Repository eingecheckt. GitHub Actions erstellt die JAR automatisch; bei Versions-Tags wird sie an ein GitHub-Release angehängt.

## Release erstellen (Maintainer)

Nachdem der Quellcode auf den Branch `main` hochgeladen wurde, löst ein Versions-Tag den automatischen Build und die Veröffentlichung aus:

```bash
git tag v1.0.0
git push origin v1.0.0
```

Der Release-Workflow baut die Mod mit Java 25 und hängt die fertige JAR-Datei an das GitHub-Release an.

## Lizenz und Drittanbieter-Code

Der ursprüngliche ElytraFinder-Code steht unter der [MIT-Lizenz](LICENSE). Der Endstadt-Generator enthält Code mit Herkunftshinweis auf die StructureFinder contributors. Deren separate MIT-Lizenzdatei [`LICENSE-StructureFinder.txt`](LICENSE-StructureFinder.txt) und die Quellenhinweise in den Quelldateien bleiben erhalten.

Dieses Projekt ist unabhängig und steht nicht in Verbindung mit Mojang oder Microsoft.
