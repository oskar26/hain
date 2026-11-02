# STATUS.md — where the project stands

> Update at the end of every session: "Now", "Last sessions", and questions. Keep it short; history goes to HANDOFF_LOG.md.

## Now

- **Phase:** M1–M3 core + M4 MVP on main (PR #1, #2 merged 2026-11-01). **Day 1 of the v0.2 plan in review** on `hark/day-1`: chats are saved in an encrypted Room DB (T-020) and the drawer has search, rename, pin, delete and day sections (T-053).
- **APK:** every push builds one (Actions → APK). Pushes to main refresh the `nightly` pre-release with `hain-debug.apk` (shared debug key in `app/signing/`, installs as update).
- **llama.cpp pin:** `39febd2ad` (2026-10-28, has LFM3). Native build = shared libs with `GGML_BACKEND_DL` + `GGML_CPU_ALL_VARIANTS` (runtime CPU pick, no SIGILL on older phones).
- **Known build issues:** read `docs/workflow/KNOWN_ISSUES.md` before touching Gradle/JDK/lint — KI-01: Gradle daemon pinned to JDK 21 because detekt 1.23.x crashes on JDK 25; KI-02 (open): T-004 "app starts" on-device check — phone attached but USB debugging not authorized; **new KI-04: the pinned toolchain (AGP 9.0.1 / compileSdk 36, AGP-embedded Kotlin 2.2.0) caps library versions** — newest "latest stable" of several UI libraries need compileSdk 37 / Kotlin ≥ 2.3; chosen newest-compatible versions are listed in LICENSES.md. Upgrading AGP → 9.1 + compileSdk 37 (T-002-scale change) would unlock them.
- **Next tasks:**
  - Human: **T-000** (benchmark LFM3 on the phone) and **T-080** (model URLs + sha256) — can run any time.
  - Agents (v0.2 plan): settings + DataStore (T-09x), T-084 packs screen, DeviceCheck, onboarding language; then T-060/T-062 tool loop, T-071 feeds; finish T-014/T-015 components.
- **First milestone goal (M4):** app opens, downloads LFM3-1.7B, streams a German answer, keeps chat history.

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
