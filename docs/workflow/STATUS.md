# STATUS.md — where the project stands

> Update at the end of every session: "Now", "Last sessions", and questions. Keep it short; history goes to HANDOFF_LOG.md.

## Now

- **Phase:** M1 Project skeleton in progress (2026-10-28).
- **Next tasks:**
  - Human: **T-000** (benchmark LFM3 on the phone) and **T-080** (model URLs + sha256) — can run any time; answer **Q-05** for T-004.
  - Agents: lanes can run in parallel:
    A T-005…T-006 · B T-010, T-018, T-019, T-011…T-017 · C T-020, T-030 · D T-040…T-045 (pure Kotlin, ideal for small models) · F T-081.
- **First milestone goal (M4):** app opens, downloads LFM3-1.7B, streams a German answer, keeps chat history.

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
| AGP / Kotlin / Compose BOM | AGP 9.0.1 / Kotlin 2.1.20 / Compose BOM 2026.03.01 |
| SQLite build (FTS5 + sqlite-vec) | _decide in T-020_ |
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
| Q-05 | T-004 prerequisites: Hilt dependencies/plugin are not yet in `libs.versions.toml` or root build scripts, and `HainTheme` is scheduled for T-010 in `:core-ui`. Should T-004 add Hilt via KSP and use a simple Compose placeholder without `HainTheme`, or should T-004 depend on T-010? | open |

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
