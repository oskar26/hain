# STATUS.md — where the project stands

> Update at the end of every session: "Now", "Last sessions", and questions. Keep it short; history goes to HANDOFF_LOG.md.

## Now

- **Fix: keine erfundenen Quellen (04.11., `hark/fix-cites`, 0.1.3.2):** T-220 Pim schrieb ohne Werkzeug „[1]“, eine Quellenzeile, „kein Werkzeug nötig“ und „**Fakt:**“ in die Antwort. Jetzt Prompt-Regel (DE+EN: [n] nur nach einem Werkzeug) und `CitationCleaner` im `ChatEngine` (live + am Ende): ohne Quellen fallen Marker, Quellenzeilen/-blöcke, Werkzeug-Sätze und das „Fakt:“-Etikett weg, mit Quellen nur ungültige Nummern. Code und Links bleiben unberührt. `hain.versionName` gibt es auf main noch nicht (kommt mit dem Extras-PR #36) → nach dessen Merge auf 0.1.3.2 setzen.
- **Englische Screenshots (04.11., `hark/web-en`, PR #35):** T-107 die englische Website zeigt jetzt englische App-Screenshots (`<id>-<mode>-en.webp`, aus neuen EN-Varianten der Lichtung-Screenshot-Tests über `screens.yml`), /de unverändert mit den deutschen Bildern. Geprüft bei 1366×1024 und 390×844, EN + /de.
- **Website fix (04.11., `hark/web-fix`):** T-106 the second phone in the hero showed a big empty white area under its screenshot (flex item stretched to the height of the first phone). Frames now take the height of their image; checked at 1366×1024, 1024×768, 390×844, 1440 in light/dark, EN + /de. T-107 (English app screenshots) → PR #35.
- **Trademarks + support (04.11., `hark/trademarks`):** T-196 `TRADEMARKS.md` (DE+EN, "open code, protected brand": name Hain, Pim and icon are project marks, forks rename and replace them) + README sections "Name & Pim / Trademarks" and "Support / Unterstützen" (coming soon, no sponsor button yet). Actual DPMA/TMview check stays T-195 (human).

- **App-Icon „Pim im Wald“ + Menü-Blur + Website (04.11., `hark/icon-bubble`, PR #31):** T-102: neues Icon-Motiv „Pim im Wald“ (Pim als glänzende Glasfigur vor einem weichen Wald aus runden Bäumen), in 3D und flach; ersetzt „Pim als Sprechblase“. T-103: Icon wählbar im Onboarding (nach der Sprache) und unter Einstellungen → Aussehen → App-Symbol, umgesetzt mit zwei `activity-alias` (`.Launcher3D`, `.LauncherFlat`), DataStore `app_icon`; das Icon wechselt erst, wenn man die App verlässt. T-104: dezenter Blur hinter Popup-Menüs und Popovern (Haze, unter Android 12 nur Abdunkeln), Langdruck auf einen Chat in der Seitenleiste öffnet das Menü. T-105: Website mit neuem Icon, weicherem Header, Footer (Marke, „Unterstützen – kommt bald“), Screenshots ohne weißen Rest, /de-Übersetzungen, Alt-Texte. Offen: englische Seite zeigt deutsche App-Screenshots.
- **App icon v3 (04.11., `hark/icon-bubble`):** T-100i: new motif “Pim als Sprechblase” (Oskar's choice): Pim's glossy mint glass body is a round chat bubble with a small tail lower left, two dot eyes, stem + leaf growing from the top, green squircle. Same pipeline (`assets/icon/src/make_icon.py`), all launcher layers + themed + debug regenerated, website favicon/apple-touch/OG/`hain-icon.webp` updated.
- **Day 2f (04.11., `hark/day-2f`):** T-055a: first onboarding screen asks Deutsch / English (sets answer language + app language on Android 13+), manifest `localeConfig` so the system language picker lists Hain. Brain picker in the top bar → 2f-2.
- **App icon + website (03.11. night, `hark/lichtung-web`):** T-088 launcher icon = glossy glass Pim on a green squircle (own SVG, `assets/icon/src/make_icon.py`), adaptive + round + themed (monochrome vector) in `app/src/main/res`, debug build with a small “dev” pill in `app/src/debug/res`; `docs/design/ICON.md`. T-089 website in style A “Lichtung” (white, hairlines, Hanken Grotesk, Lucide line icons, light/dark, style A screenshots as WebP, feature list with “Available now / Coming soon” badges, new icon as favicon/OG/apple-touch, no location mentions).
- **Phase:** M1–M3 core + M4 MVP on main (PR #1, #2 merged 2026-11-01). **Day 1 of the v0.2 plan in review** on `hark/day-1`: chats are saved in an encrypted Room DB (T-020) and the drawer has search, rename, pin, delete and day sections (T-053).
- **APK:** every push builds one (Actions → APK). Pushes to main refresh the `nightly` pre-release with `hain-debug.apk` (shared debug key in `app/signing/`, installs as update).
- **llama.cpp pin:** `39febd2ad` (2026-10-28, has LFM3). Native build = shared libs with `GGML_BACKEND_DL` + `GGML_CPU_ALL_VARIANTS` (runtime CPU pick, no SIGILL on older phones).
- **Known build issues:** read `docs/workflow/KNOWN_ISSUES.md` before touching Gradle/JDK/lint — KI-01: Gradle daemon pinned to JDK 21 because detekt 1.23.x crashes on JDK 25; KI-02 (open): T-004 "app starts" on-device check — phone attached but USB debugging not authorized; **new KI-04: the pinned toolchain (AGP 9.0.1 / compileSdk 36, AGP-embedded Kotlin 2.2.0) caps library versions** — newest "latest stable" of several UI libraries need compileSdk 37 / Kotlin ≥ 2.3; chosen newest-compatible versions are listed in LICENSES.md. Upgrading AGP → 9.1 + compileSdk 37 (T-002-scale change) would unlock them.
- **Next tasks:**
  - Human: **T-000** (benchmark LFM3 on the phone) and **T-080** (model URLs + sha256) — can run any time.
  - Agents (v0.2 plan): settings + DataStore (T-09x), T-084 packs screen, DeviceCheck, onboarding language; then T-060/T-062 tool loop, T-071 feeds; finish T-014/T-015 components.
- **First milestone goal (M4):** app opens, downloads LFM3-1.7B, streams a German answer, keeps chat history.

- **Werkzeuge 4d (04.11.):** `hark/day-4d` (baut auf #27/4c auf): all tools reworked for the 1.7B brain. One definition file `core-tools/.../tools.json` (short when/when-not sentence, flat params, enums, wrong names + aliases), `ToolCallFixer` (aliases, enum words, relative dates, `minutes` → `seconds`, missing value → ask the user, duplicates once, repeat hint), `RepairingToolCallParser` (repairs broken/JSON/Python-style calls), one network retry, compact Wikipedia results, `say` sentence for phone actions; prompts in `assets/prompts/tools.<de|en>.txt` + follow-up/link hints; spoken sums and time/date answered by code. Host eval (52 questions, `tools/eval`): right tool 88.5 % → 96.2 %, valid args 94.3 % → 97.5 %, hallucinations 6 → 1 (0 on fact questions), prompt 1678 → 1662 tokens. Example dialogs measured worse and were left out (TOOLS.md §8).
- **3D redesign (03.11., 2e-2, `hark/redesign-3d`):** after Oskar's "where is my 3D redesign?" the design language became "calm, but with depth" (DESIGN_LANGUAGE.md): `glassTile(hue)` (white/dark glass, inner coloured light, light edge, sheen, coloured floor glow), `dome()` buttons (press 0.96 + tick, new DARK style), 22 own glossy 3D icons (`Hain3DIcon`), glossy Pim with white oval eyes, empty chat like reference 6, composer card with green rim glow, glass drawer, settings hub/pages as coloured glass, packs, tool card per answer, Wabi bubble cloud + Pim knob in the tour. T-084: brain button + glass popover in the composer instead of the Auto/Fast/Thorough bar. T-085: long-text fixes (bubble fold after 14 lines, follow-scroll only at the bottom + jump button, ellipsis in top bar/menus, composer 6 lines, FlowRow in delete confirm). Screenshots: CI workflow `screens.yml` (Roborazzi) → `docs/design/screens/`.
- **Day 4b (03.11., Werkzeuge II Rest):** `hark/day-4b`: T-068 phone actions – `set_alarm`, `set_timer`, `add_calendar_event`, `navigate` (offline tools, own DE/EN time parser relative to `ToolContext.now`) only prepare a **ConfirmCard** under the answer; the Android intent (AlarmClock / CalendarContract insert / `geo:0,0?q=`) starts only after the tap on Stellen/Starten/Eintragen, "Abbrechen" or "no app" are kept with the message. T-066 rest: DuckDuckGo back-off (10 min per page after a bot check) + own SearXNG address in Settings → Privacy (asked first). Host check with LFM3: see TOOLS.md §3c. Rest (repeating alarms `days`, `confirm_actions` auto mode with undo, ConfirmCard in core-cards) → 4c.
- **Day 4 (03.11., Werkzeuge II):** `hark/day-4`: (1) **Fix from Oskar's test of #19 – tools in every message:** each answer keeps its tool calls + short results (`ToolTrace` in `message.tool_calls_json`, column existed → no migration); `PromptHistory` puts them back as call → tool response → answer (last answer 600 chars, older 200); new rule in the system prompt (DE+EN): call the tool again for every new question, never claim a lookup without a call. Host check with real LFM3: see TOOLS.md §3b table. (2) **Research in several steps:** `web_search` without API key (SearXNG → DuckDuckGo HTML → DuckDuckGo Lite → Wikipedia), then the engine reads the 3 best pages in parallel (`ToolRouter.readPages`, 1,200 chars each) and shows "Suche ✓ / Lese 3 Seiten…"; up to 4 tool rounds and 2 searches per answer, mode Schnell = 1 search + 1 page. `read_page` for links (own HTML-to-text, no library). Everything through the NetworkGate (SEARCH / PAGE_READ in the log). Phone actions (alarm, timer, calendar, navigation + ConfirmCard) → 4b.
- **Redesign (03.11., 2d-4 points 2+3):** `hark/redesign`: one design language for the whole app (`docs/design/DESIGN_LANGUAGE.md`): Hanken Grotesk replaces Inter (Light headings, Regular text, Medium buttons, SemiBold bold), tokens for three depth levels (`hainSurface` FLAT/RAISED/FLOATING), three radii + capsule, one motion language (`HainEase`, 150/180/240 ms, calm spring, all snap with reduce motion); composer, bubbles, cards, chips, settings (tiles, `SegmentedControl`, new `HainSwitch`), onboarding wash and success screen moved onto it. T-057: Gemini-style actions (tap own message → edit/copy fade in; answer row copy/share/thumbs/again; thumbs in `hain_ratings` prefs). T-053: glass ⋯ menu (scale+fade 180 ms from the ⋯ corner, also on close), whole-row highlight, drawer + list on springs. Open: follow-up suggestions, read aloud (voice), device check.
- **Day 3 (03.11., Werkzeuge I):** `hark/day-3`: Pim can use tools. NetworkGate (only https, offline mode, User-Agent, log 7 days in memory) + network log in Settings → Privacy (filter, clear, full addresses optional) + orange "Online" dot; ToolRouter (12 s timeout, short error codes) + tool loop in `ChatEngine` (max 3 rounds, then answer without tools; tools block kept in warm-up/checkpoints so the cache stays valid); `calculate` (own parser + units, pure sums answered without the brain), `weather` (Open-Meteo, days named heute/morgen), `wikipedia` (DE/EN opensearch + summary); ToolTimeline + SourceChips under answers (stored in `sources_json`); HainChip/HainCard/SegmentedControl. Host check with real LFM3-1.7B-Q4_0: correct XML tool calls for weather/Wikipedia/calculate, no call for small talk. Rest → 3b (cards, Room log, download via gate, per-purpose switches).
- **Fix 03.11. (T-080, Antwortsprache):** `hark/fix-language`: Pim antwortete auf Deutsch geschriebene Nachrichten oft Englisch oder DE/EN gemischt (englischer System-Prompt). Jetzt deutscher System-Prompt bei Antwortsprache Deutsch bzw. Automatisch + deutschem Handy, Sprachregel am Ende, pro Zug ein kurzer Sprach-Hinweis hinter dem Verlauf (Cache bleibt gültig), Prompt für 1.7B überarbeitet (Pim, Tonfall, Länge, ehrlich). Host-Test 14/14 + 5 Zusatzfälle richtige Sprache, siehe LLM.md §9.
- **Day 2d-2 (02.11.):** Tempo + Fixes in `hark/day-2d-2`: CI-APK nicht mehr „debuggable“ (Hauptgrund für „sau langsam“), Gehirn merkt sich den Chat (Checkpoints für LFM-Hybrid, fester Zeitanker im System-Prompt, Warm-up), Stopp sofort (Abort-Callback), Ladefortschritt, Tastatur (adjustResize), Gehirn-Download überspringbar + Hinweis-Banner, eigene Nachricht kopieren/bearbeiten, „Nochmal“ auf jeder Antwort, kompaktes ⋯-Menü, Auto-Titel. Einstellungen neu, 3D-UI, R8/Baseline-Profil → 2d-3.
- **Day 2e (02.11.):** Persönlichkeit Teil 1 in `hark/day-2e`: Willkommenstour (4 Seiten, 3D-Objekte), Pim lebendig (Glanz, Atmen, Blinzeln, Hüpfen, Antippen), Erfolgs-Screen mit Konfetti + Haptik, Denk-Verlauf unten→oben, gewölbte Knöpfe mit Eindrücken; Bewegung reduzieren = System-Animationen aus. Einstellungen neu (Gruppen, Unterseiten) + weitere Micro-Animationen → 2e-2.
- **Day 2d (02.11.):** Antwortsprache Auto, Stopp beim Denken, Pim-Position, Kopfzeile Hain, Begrüßung scrollbar, ein Denk-Hinweis in `hark/day-2d`; Ladezeit, Auto-Titel, Bearbeiten → 2d-2.
- **Day 2c (02.11.):** DeviceCheck (pure rules, tested) + packs screen (list, load, remove, fit hints) in `hark/day-2c`; model picker + onboarding language → day 2d.
- **Day 2b (02.11.):** settings (DataStore, theme, readable font, answer language/mode, about me/style) in `hark/day-2b`; packs screen, DeviceCheck, picker, onboarding language → day 2c.
- **Day 2a (02.11.):** brain model is now LFM3-1.7B-QAD-Q4_0 (`hark/day-2`).

## Milestones

| M | Name | State |
|---|---|---|
| M0 | Device check & setup | todo |
| M1 | Project skeleton | todo |
| M2 | Design system & Pim | todo |
| M3 | LLM core | todo |
| M4 | Chat MVP | todo |
| M5 | Models & onboarding | todo |
| M6 | Tools & internet | todo |
| M7 | News | todo |
| M8 | Memory | todo |
| M9 | Documents & OCR | todo |
| M10 | Cards, voice, assistant | todo |
| M11 | Settings, images, polish, beta | todo |

## Implemented features

_None yet._ (List feature + task IDs here when a milestone closes.)

## Not implemented yet / open by design

- Everything in TASKS.md with status ≠ done.
- v2 ideas (not in TASKS): live camera/video, routines, Kiwix offline Wikipedia, PC companion for big models, wake word, Omni native voice, seasonal Pim.

## Pinned versions

| What | Version / commit |
|---|---|
| llama.cpp | b11963 (`39febd2adb947b70da35e95f9351b3bf58ad170a`) |
| Gradle daemon JVM | 21 via `gradle/gradle-daemon-jvm.properties` — required for detekt 1.23.x, do not remove (KI-01) |
| Hilt | 2.60.1 via KSP (added in T-004) |
| AGP / Kotlin / Compose BOM | AGP 9.0.1 / Kotlin 2.1.20 / Compose BOM 2026.03.01 |
| UI libraries (T-018) | Compose Unstyled 2.6.0 · Haze 1.7.0 · Capsule 2.1.3 · Backdrop 2.0.0-alpha03 · shimmer 1.3.2 · markdown-renderer 0.41.0 · Coil 3.4.0 · Lottie 6.7.1 · Konfetti 2.0.5 · Showkase 1.0.5 (debug) · Roborazzi 1.77.0 (test) — newest versions compatible with the pinned toolchain (KI-04) |
| SQLite build | **T-020 decision:** `net.zetetic:sqlcipher-android` 4.10.0 (SQLCipher 4, own SQLite) via Room `SupportOpenHelperFactory`, raw 32-byte key (`x'…'`, no KDF) wrapped by an AES-GCM Keystore key, wrapped key in `noBackupFilesDir/hain_db.key`. Full-text: **FTS4** (`@Fts4`, unicode61, Room-managed triggers) instead of FTS5 — Room cannot declare FTS5 tables. **sqlite-vec deferred** to M8 (needs loadable extension or own SQLCipher build; decide in T-110). |
| OCR runtime | _decide in T-121_ |

## Benchmarks (T-000)

| Device | Model | prefill tok/s | decode tok/s | RAM | German quality 1–5 |
|---|---|---|---|---|---|
| | LFM3-1.7B Q4_K_M | | | | |
| | LFM3-Omni-2B Q4_K_M | | | | |

## Questions for human

| ID | Question | Answer |
|---|---|---|
| Q-01 | Final app id / package name (`app.hain` placeholder)? | open |
| Q-02 | Piper thorsten-medium: accept (CC0 data, lessac base) or wait for Kokoro-German? | open |
| Q-03 | Repo public on GitHub from day one? | open |
| Q-04 | Pim animation: Rive (best, needs ~$9/month Cadet plan to export without splash) or Compose-drawn + Lottie (free)? Default until answered: Compose-drawn. **Recommendation 2026-10-28: Compose-drawn — Pim v2 is only 5 shapes, so Rive brings little** | open |
| Q-05 | T-004 prerequisites: Hilt dependencies/plugin are not yet in `libs.versions.toml` or root build scripts, and `HainTheme` is scheduled for T-010 in `:core-ui`. Should T-004 add Hilt via KSP and use a simple Compose placeholder without `HainTheme`, or should T-004 depend on T-010? | **answered 2026-10-28**: human said "fix the Problem of Task 4" → option A: Hilt 2.60.1 via KSP, simple `BasicText` placeholder without `HainTheme` (T-010 swaps it in later) |

## Ideas (not planned yet)

_Agents add ideas here instead of doing them._

## Last sessions

| Date | Task | Result | Harness/model |
|---|---|---|---|
| 2026-10-28 | planning | AGENTS.md, specs, backlog created | Hark (planner) |
| 2026-10-28 | planning | UI libraries + motion system (ADR-21), 6 new tasks | Hark (planner) |
| 2026-10-28 | T-001 | Git repo init, GPL-3.0 LICENSE added, llama.cpp pinned at b11963 (39febd2ad) | Antigravity (Gemini 3.8 Flash) |
| 2026-10-28 | T-002 | Gradle multi-module skeleton (18 modules), AGP 9.0.1, Kotlin 2.1.20, Compose BOM 2026.03.01, assembleDebug green | Antigravity (Gemini 3.8 Flash) |
| 2026-10-28 | T-003 | KtLint + Detekt setup, Compose naming configured, ./gradlew ktlintCheck detekt green | Antigravity (Gemini 3.8 Flash) |
| 2026-10-28 | T-004 | blocked: Hilt plugin/dependencies missing from build and HainTheme not in core-ui yet | Antigravity (Gemini 3.8 Flash) |
| 2026-10-30 | review T-001…T-004 + T-004 | review passed; T-004 done: Hilt 2.60.1 via KSP, HainApp, MainActivity, daemon pinned to JDK 21 (detekt) | ZCode (GLM-5.3) |
| 2026-10-30 | T-004 follow-up | KNOWN_ISSUES.md created (KI-01..KI-03); on-device check deferred by human (KI-02); Pixel_7 AVD repaired | ZCode (GLM-5.3) |
| 2026-11-01 | batch T-005, T-006, T-010, T-018, T-019, T-016, T-017 | all 7 tasks review/done: strings+checker, CI, tokens+fonts, UI libs+wrappers, motion system, Pim v2, Lucide icons | ZCode (GLM-5.3) |
| 2026-11-04 | T-087, T-088, T-089, T-091, T-096, T-097, T-098, T-099 | Lichtung only style, speed, living Pim, start page, premium haptics; Pim normal size, 5 s easter egg → own 2048 game, subtle haptics everywhere (review, branch hark/polish) | Hark |
| 2026-11-04 | T-196 | TRADEMARKS.md (DE+EN) + README "Name & Pim" and "Support (coming soon)" (review, branch hark/trademarks) | Hark |

## 0.1.3.1 – Extras (04.11., `hark/extras`, Draft-PR #36)

- **T-210 Frosted Glass:** ⋯-Menü, Langdruck-Menü und Denk-Auswahl sind Milchglas-Karten (`Modifier.frostedGlass`: Haze 24 dp im Karten-Hintergrund, Tönung 66 % hell / 72 % dunkel, helle Haarlinie oben, weicher Schatten). Der Rest des Bildschirms wird nur noch leicht abgedunkelt (35 % von `scrim`), der alte Vollbild-Blur von T-104 ist weg. Unter Android 12 eine deckende Karte (auch überall dort, wo Haze nicht blurren kann: `fallbackTint`). Screenshots: Karte im Overlay von `HainMenuBackdrop` + `LocalForceFrost`, weil Robolectric Popup-Fenster in Software zeichnet.
- **T-211 Entwicklermodus:** 7× auf „Version“ in „Über Hain“ (Hinweis „Noch 3×…“, Haptik), wieder ausschaltbar. Einstellungen → Entwickler: eigenes GGUF von Hugging Face (Repo oder https-Link, Liste über die HF-API durch das NetworkGate, Größe + RAM-Hinweis, Download mit dem Paket-Downloader, sha256 wenn bekannt, „Nutzen“), Sampling, Kontext 512–8192 (lädt das Modell neu), Antwort-Tokens, Seed, Denken an/aus/auto + Budget, rohes Denken, eigener System-Prompt, Token/s + Zeit bis zum ersten Token + Debug-Infos kopieren, „Auf Standard zurücksetzen“. Eigene Modelle nutzen ihr Chat-Template aus der GGUF (llama.cpp `llama_chat_apply_template`, sonst ChatML) und keine Werkzeuge.
- **T-212 Update-Prüfung:** APK-Workflow hängt `version.json` an `nightly` und an `v*`-Releases. In der App: Aus (Standard) / täglich beim Start, Kanal Stabil / Nightly, „Jetzt prüfen“; einmalige Frage-Karte nach dem 3. Chat; ruhiges Banner → Release-Seite im Browser. Anfrage nur über das NetworkGate (Zweck UPDATE, im Netzwerk-Log).
- **T-213 Pim im Chat:** Denk-Zeile 40 dp, kleiner Pim (24 dp) an der neuesten Antwort (schaut bei Werkzeugen rein, freut sich am Ende, erschrickt bei Fehlern), leerer Chat 96 dp.
- **T-214 Über Hain:** zentrierter Kopf mit Pim, Zeilen mit Icon für Website, GitHub, Version, Lizenz, Open-Source-Lizenzen, Update-Block, Entwicklermodus aus.
- **T-215 Version:** `hain.versionName=0.1.3.1` in `gradle.properties`, `BuildConfig.GIT_SHA` / `BUILD_DATE` (CI setzt sie), Schema in `docs/workflow/VERSIONING.md`.
- **T-216 Zurück-Pfeil:** Einstellungs-Unterseiten zeigen jetzt einen Pfeil nach links (`HainIcons.back`, Lucide chevron-left, spiegelt sich bei RTL) statt nach rechts.
- **Hinweis Kontext:** Der Auftrag nannte als Standard 2048, die App lädt seit T-088 aber mit 8192 (2048 beendete lange Chats). Standard bleibt 8192, im Entwicklermodus wählbar.
- **Offen:** Download großer GGUF-Dateien läuft weiter direkt im Paket-Downloader (wie das Gehirn); durch das Gate geht nur eine kurze HEAD-Anfrage, damit er im Netzwerk-Log steht. Denk-Budget = Token-Grenze einer Antwort mit Denken (kein hartes Abschneiden nur des Denkens).
