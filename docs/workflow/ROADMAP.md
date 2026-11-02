# Hain – Plan bis 06.11.2026 (Ziel: v0.2 auf Oskars Handy, möglichst viel aus KONZEPT.md §5)

Repo `oskar26/hain`, Sandbox-Klon `/workspace/work/hain-gh`. Zwei Läufe pro Tag (07:00 und 16:00 Europe/Berlin). Pro Häppchen ein Branch `hark/day-<N>`, Draft-PR, pro Task ein Commit `T-XXX: <Titel>`, Docs (TASKS/STATUS/HANDOFF_LOG) im selben PR. Nie Ready/Merge ohne Oskar.
Status-Spalte pflegen: offen → in Arbeit → PR #x → gemerged. Reihenfolge = Abhängigkeiten; Häppchen 11 (Release) ist immer das letzte, am 06.11. abends. Reicht die Zeit nicht, fällt Häppchen 10 (Sprache) weg, nie 11.

| # | Häppchen | Tasks (TASKS.md) | Status |
|---|---|---|---|
| 1 | **Chats bleiben gespeichert**: verschlüsselte Room-DB (SQLCipher, Key im Keystore), Repository, Drawer: Suche, Umbenennen, Löschen, Anpinnen, Gruppen | T-020, T-053 | PR #4 |
| 2 | **Einstellungen**: DataStore + Settings-Screens (Antwortmodus, Theme, Gut lesbar, Bewegung reduzieren, Antwortsprache), eigene Anweisungen + Rollen, Packs-Screen, DeviceCheck, Modell-Picker, Sprachschritt im Onboarding, `localeConfig`; **Gehirn auf LFM3-1.7B-QAD-Q4_0 umstellen** (Oskars Entscheidung 01.11.: Datei `LFM3-1.7B-QAD-Q4_0.gguf`, 1.049.615.680 Bytes, sha256 `43a7d09a0e9cba91b63eb7052488b9564569bddfd65754d2e426465e572a7452`, HF LiquidAI/LFM3-1.7B-GGUF; Katalog, MODELS.md, KONZEPT-Tabelle, README; Sampling temp 0.1, top_k 50, repeat 1.1; altes Q4_K_M beim Update löschen bzw. neu laden anbieten) | T-100, T-101, T-084, T-081, T-055, T-083, T-080 | 2a: gemerged (#6) · 2b: gemerged (#7) · 2c: PR (Packs-Screen, DeviceCheck) · 2d offen (Modell-Picker, Sprachschritt) |
| 3 | **Werkzeuge I**: NetworkGate + Netzwerk-Log-Screen + Online-Punkt, ToolRouter + Tool-Loop, Rechner/Einheiten, Wetter, Wikipedia, ToolTimeline/SourceChips, fehlende Komponenten Chip/Card/Segmented | T-060, T-069, T-062, T-063, T-064, T-065, T-059, T-014 | offen |
| 4 | **Werkzeuge II**: Such-Router (SearXNG/DDG html/Wikipedia), read_page, Handy-Aktionen (Wecker, Timer, Kalender, Navigation) mit ConfirmCard | T-066, T-067, T-068 | offen |
| 5 | **Nachrichten**: Standard-Feeds DE/EN inkl. junge Welt, Repository, news-Tool + NewsCard, News-Screen mit 3 Modi, Artikel-Zusammenfassung | T-071, T-072, T-073, T-074 | offen |
| 6 | **Antwort-Karten** (Basic OpenUI): Card-Parser, Renderer + GBNF (Liste, Tabelle, Checkliste, Fortschritt, Knöpfe) | T-130, T-131 | offen |
| 7 | **Gedächtnis**: Tabellen + DAO, merken/vergessen + remember-Tool, Retrieval (erst Stichwort, Embedder wenn Zeit), Gedächtnis-Seite | T-111, T-112, T-113, T-115 (T-110 optional) | offen |
| 8 | **Privat & Teilen**: Privater Chat + App-Sperre (Fingerabdruck), Export Markdown/PDF, Teilen-Ziel + Widget + Schnellkachel, Nachricht bearbeiten/Verzweigungen, Scroll-Pill | T-056, T-054, T-144, T-057, T-058 | offen |
| 9 | **Dokumente**: PDF-Textebene + Seiten rendern, Chunking + Dokumentsuche + Anhängen im Chat (OCR T-121/T-122 wenn Zeit) | T-120, T-123 | offen |
| 10 | **Sprache** (Stretch): Diktat Moonshine + Mikro-Knopf, Vorlesen | T-140, T-141 | offen |
| 11 | **Feinschliff + Release**: Pim-Icon + animierter Pim, Barrierefreiheit/Performance, Version 0.2.0, Release-Notes, README-Statusspalte, alle Docs | T-061, T-163, T-190 | offen |

Später (nach 06.11.): Mini-Apps-Wald (M12, T-200–T-226), Bildbearbeitung/-erzeugung, Omni/Sehen, Pocket-TTS-Pim-Stimme, Standard-Assistent + Bildschirm-Kontext (T-143), Beta F-Droid (T-199), Markenprüfung (T-195).

Regeln: Own code, keine Forks; Libraries nur aus UI_LIBRARIES.md bzw. GPL-3.0-kompatibel + LICENSES.md; kein Material 3; keine Telemetrie/Play Services; alle Strings EN+DE; llama.cpp-Submodul nie editieren. README.md Feature-Tabelle (Spalte State) bei jedem Häppchen nachziehen.
Gate lokal: `./gradlew assembleDebug testDebugUnitTest ktlintCheck detekt -Phain.native=false` + `python3 tools/check_strings.py`; nativer APK-Build läuft in GitHub Actions (`apk.yml`).


Hinweis Tag 1: ~820 Dateien aus `*/build/` sind im Repo eingecheckt → bei Gelegenheit (z. B. Häppchen 11 oder separat) aus dem Index nehmen + .gitignore (mit Oskar abstimmen). README-Statusspalte: README liegt noch in Draft-PR #3, daher an Tag 1 nicht nachgezogen.
