# AGENTS.md — Hain

> **This is the central file of the project. Read it first, every session, in every harness**
> (Claude Code, Codex, Gemini CLI, Aider, OpenCode, Cursor, Continue, Cline …).
> `CLAUDE.md`, `GEMINI.md` and `.cursorrules` only point here.

Hain is a **private, local-first ChatGPT alternative for Android** (German + English).
The LLM runs on the phone via llama.cpp. No account, no cloud, no API key required.
Mascot: **Pim**, a small green sprout.

---

## 1. Session protocol (do this every time)

1. Read this file completely.
2. Read `docs/workflow/STATUS.md` → section **"Now"** tells you where to continue.
3. Pick **exactly one** task from `docs/workflow/TASKS.md`:
   - the first task with status `todo` whose `depends` are all `done`,
   - unless STATUS.md names a specific task, or the human tells you one.
4. Set the task to `doing (<harness/model>)` in the Overview table of TASKS.md (status lives only there).
5. Read only the spec files the task lists under `read`. Do not read the whole repo.
6. Implement. Touch only the files listed under `files` (create them if missing).
   If you must touch another file, say why in the handoff.
7. Run the task's `check` commands. All must pass.
8. Set the task to `done` (or `blocked`), then update `docs/workflow/STATUS.md`
   ("Now", "Last sessions") and append one entry to `docs/workflow/HANDOFF_LOG.md`.
9. Commit: `T-XXX: <short title>` (one task = one commit).

**Never start a second task in the same session unless the first is `done` and committed.**
If a task is too big for you, split it: add `T-XXXa`, `T-XXXb` to TASKS.md, mark the parent `split`, do `a`.

## 2. Hard rules

- **Own code, no forks.** Do not copy app code from other chat apps (ChatterUI, PocketPal, MLC Chat, Maid, SmolChat …). Libraries via Gradle/CMake are fine.
- **llama.cpp is a git submodule** in `third_party/llama.cpp`, pinned to a commit. Never edit files inside it.
- **No Material 3 look.** Use `androidx.compose.foundation` + Compose Unstyled primitives + our own styling in `:core-ui` (see `docs/specs/UI_LIBRARIES.md`). Only libraries listed there; ask before adding others. `material3` may be a dependency only for nothing visible (prefer: not at all). No FAB, no Material You dynamic colour.
- **Local first.** Nothing leaves the phone except: model downloads, explicit tool calls (search, news, weather, wikipedia, maps). Every network call goes through `NetworkGate` (see `docs/specs/TOOLS.md`) so offline mode and the "where did my data go" log work.
- **No telemetry, no analytics SDKs, no Google Play Services / ML Kit / Firebase.**
- **All user-visible strings** in `strings.xml` (`values/` = English, `values-de/` = German). No hard-coded text in Kotlin. Both files must have the same keys.
- **Licence of the app: GPL-3.0.** Only add dependencies with GPL-3.0-compatible licences. List every new one in `docs/specs/LICENSES.md`.
- **Model output is untrusted.** Parse tool calls and cards strictly; on parse errors show plain text. Never `eval`, never execute shell, never open URLs without user tap.
- **Teen-safe.** Users can be 13+. Image generation has a fixed safety filter. Sensitive memories need consent.
- **Do not change** `AGENTS.md` sections 1–3, `docs/decisions/*` or `docs/concept/*` unless the human asks. Propose changes in the handoff instead.
- When unsure about a product decision: **stop, write the question into STATUS.md → "Questions for human", mark task `blocked`.** Do not guess.

## 3. Definition of done (every task)

- Builds: `./gradlew assembleDebug`
- Unit tests green: `./gradlew testDebugUnitTest`
- Lint green: `./gradlew ktlintCheck detekt` (after T-003 adds them)
- New logic has unit tests (pure Kotlin where possible; JVM tests, not instrumented).
- No new hard-coded strings; DE + EN strings added.
- TASKS.md, STATUS.md, HANDOFF_LOG.md updated.

---

## 4. Project map

```
hain/
├── AGENTS.md                ← you are here (central index + rules)
├── CLAUDE.md / GEMINI.md / .cursorrules   ← pointers to AGENTS.md
├── docs/
│   ├── workflow/
│   │   ├── STATUS.md        ← current state, where to continue, open questions
│   │   ├── TASKS.md         ← full backlog, small tasks with checks
│   │   ├── HANDOFF_LOG.md   ← append-only log, one entry per session
│   │   ├── WORKFLOW.md      ← how to work with small models, task card format, roles
│   │   └── PROMPTS_FOR_AGENTS.md ← copy-paste prompts to start a harness
│   ├── specs/               ← exact implementation specs (the "how")
│   │   ├── ARCHITECTURE.md  modules, packages, data flow, DI
│   │   ├── DESIGN_SYSTEM.md colours, type, components, Pim usage
│   │   ├── UI_LIBRARIES.md  libraries we build on, assistant-ui mapping, motion system
│   │   ├── LLM.md           JNI bridge, prompt builder, chat template, tool-call parser, sampling
│   │   ├── TOOLS.md         tool router, network gate, search, weather, wiki, intents, calculator
│   │   ├── NEWS.md          3 modes, feed list, parsing, caching
│   │   ├── MEMORY.md        4-layer memory, DB schema, retrieval
│   │   ├── DOCS_OCR.md      PDF text, PP-OCRv5, German correction, PaddleOCR-VL
│   │   ├── VOICE.md         STT (Moonshine), TTS (Piper/Kokoro), voice mode
│   │   ├── CARDS.md         OpenUI subset, GBNF grammar, renderer
│   │   ├── IMAGE.md         image edit kit, optional image generation
│   │   ├── MODELS.md        model catalogue, download manager, device check
│   │   ├── SETTINGS.md      every setting with key, type, default
│   │   ├── I18N.md          DE/EN rules
│   │   ├── DATA.md          Room schema (all tables), encryption
│   │   ├── TESTING.md       test strategy, golden files, German eval set
│   │   └── LICENSES.md      dependency licences
│   ├── decisions/ADR.md     architecture decisions (why), do not change
│   ├── concept/             product concept (German) + research reports + PDF
│   └── design/              5 UI mockups (style direction only)
├── prompts/                 system prompts + official LFM3 chat templates
├── assets/mascot/           Pim artwork
└── (code, created by tasks)  app/, core-*/, feature-*/, third_party/llama.cpp
```

## 5. Product in one screen

| Area | Version 1 |
|---|---|
| Chat | streaming, markdown, code, tables, LaTeX, edit/regenerate/stop, chat list, search, export, share |
| Models | LFM3-1.7B Q4 (default brain), switchable; optional LFM3-Omni-2B (see + speak) |
| Tools | web search (no API key), news (3 modes), weather, wikipedia, calculator/units, alarm/timer/calendar/maps intents |
| Documents | PDF text layer → PP-OCRv5 Latin → German fix → optional PaddleOCR-VL |
| Voice | Moonshine Small (dictation), Piper thorsten (DE) / cori (EN), Kokoro optional |
| Memory | About me / Topics / Facts / Chat search, user can see, edit, delete |
| Cards | OpenUI subset (8 components) via GBNF |
| Images | edit kit in base (erase, cut-out, upscale); generation = optional download |
| Trust | offline mode, network log, app lock, private chat |
| Languages | German + English (UI and answers) |

Full product text (German): `docs/concept/KONZEPT.md`.

## 6. Tech stack (fixed, see ADR)

Kotlin 2.x · Jetpack Compose (Foundation + Compose Unstyled; Haze, Capsule, shimmer, markdown renderer, Rive/Lottie — UI_LIBRARIES.md) · Hilt · Coroutines/Flow · Room + SQLite FTS5 + sqlite-vec · SQLCipher ·
WorkManager · OkHttp + jsoup + Readability4K · llama.cpp (C++, JNI, CMake, NDK r27+) · sherpa-onnx (STT/TTS) ·
PaddleOCR via ONNX Runtime (or RapidOCR-Android) · MediaPipe/LiteRT (image tools) · MNN (image gen, optional).
minSdk 29, targetSdk 36, arm64-v8a only, 16 KB page size aligned.

Package root: `app.hain` (placeholder until the name is legally checked).

## 7. Commands

```bash
bash tools/sync.sh          # keep this checkout == GitHub (pull --rebase, submodule, push); start + end of session
git submodule update --init --recursive
./gradlew assembleDebug
./gradlew testDebugUnitTest
./gradlew ktlintCheck detekt
./gradlew :core-llm:testDebugUnitTest --tests "*PromptBuilder*"
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Models are **never committed**. For local dev put GGUF files in `~/hain-models/` and push with
`adb push ~/hain-models/LFM3-1.7B-Q4_K_M.gguf /sdcard/Android/data/app.hain/files/models/` (see MODELS.md).

## 8. Glossary

- **Brain** = the main chat LLM. **Pack** = an optional downloadable model bundle.
- **Tool** = a function the LLM may call (XML `<tool_call>` format, see LLM.md).
- **Card** = a structured UI block in an answer (CARDS.md).
- **NetworkGate** = the single place every outbound request passes.
- **Pim** = the mascot. **Grove green** = brand colour `#16A36B` / dark `#22C17E`.
