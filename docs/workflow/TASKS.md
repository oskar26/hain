# TASKS.md — backlog

Generated from `tools/tasks_src.py` (planner). Status lives **only in the Overview table** (last column). Implementers change only that cell for their task.
Lanes A–F can run in parallel; H = human/strong model. `native` tasks need a strong model (WORKFLOW.md).

Status values: `todo` · `doing (<model>)` · `review` · `done` · `blocked: <why>` · `split`

## Overview

| Task | Title | Type | Size | Lane | Depends | Status |
|---|---|---|---|---|---|---|
| T-000 | Benchmark LFM3 on own phone | plan | S | H | - | todo |
| T-001 | Create Git repo, add llama.cpp submodule | scaffold | XS | A | - | done |
| T-002 | Gradle multi-module skeleton | scaffold | M | A | T-001 | done |
| T-003 | Lint & static checks | scaffold | XS | A | T-002 | done |
| T-004 | Hilt + App class + MainActivity | scaffold | S | A | T-002 | done |
| T-005 | Strings DE/EN + checker script | strings | S | A | T-002 | review |
| T-006 | CI workflow | scaffold | XS | A | T-003,T-005 | review |
| T-010 | Design tokens | ui | S | B | T-002 | review (redesign 03.11.: one design language — levels, radii, motion, Hanken Grotesk; docs/design/DESIGN_LANGUAGE.md) |
| T-011 | Components: HainText, HainSurface, HainButton, HainIconButton | ui | S | B | T-018,T-019 | review (3D design language 03.11.: glassTile, dome, Hain3DIcon, glossy Pim – DESIGN_LANGUAGE.md) |
| T-012 | Components: HainTextField, Composer | ui | S | B | T-018,T-019 | review (subset) |
| T-013 | Components: UserBubble, AssistantBlock (markdown via renderer core), ReasoningBlock | ui | M | B | T-018,T-019 | review (subset) |
| T-014 | Components: HainChip, ToolChip, HainCard, SegmentedControl | ui | S | B | T-018,T-019 | review |
| T-015 | Components: SettingsRow, HainSwitch, TopBar, Drawer, BottomSheet, Toast, EmptyState, ProgressBar | ui | S | B | T-018,T-019 | review (subset) |
| T-018 | Add UI libraries + thin wrappers | scaffold | S | B | T-010 | review |
| T-019 | Motion system HainMotion + MotionGovernor | ui | S | B | T-018 | review |
| T-016 | Pim v2 drawn in Compose (static states) | ui | S | B | T-010 | review |
| T-017 | Lucide icons as vector drawables | ui | S | B | T-010 | review |
| T-020 | Encrypted Room DB skeleton | feature | M | C | T-002 | review |
| T-030 | JNI bridge: load/generate/cancel | native | M | C | T-001,T-002 | review |
| T-031 | Copy prompts into assets | scaffold | XS | C | T-002 | review |
| T-032 | LlmEngine implementation | feature | S | C | T-030 | review |
| T-033 | Prefix KV cache reuse | native | S | C | T-030 | review (2d-2: checkpoints for hybrid LFM, stop inside a prompt, warm-up) |
| T-040 | Data classes ChatMessage/ToolSchema/ToolCall | feature | XS | D | T-002 | review |
| T-041 | Golden prompt files + render script | test | S | D | T-040 | review |
| T-042 | PromptBuilder (pure Kotlin) | feature | M | D | T-041 | review |
| T-043 | ToolCallParser | feature | S | D | T-040 | review |
| T-044 | StreamSplitter | feature | S | D | T-043 | review |
| T-045 | ThinkingPolicy + SystemPrompt.fill | feature | S | D | T-040 | review |
| T-050 | ChatEngine turn loop | feature | M | E | T-032,T-042,T-044,T-045,T-020 | review (no tools yet) |
| T-051 | ChatViewModel + UI state | feature | S | E | T-050 | review |
| T-052 | Chat screen | ui | M | E | T-051,T-011,T-012,T-013 | review |
| T-057 | Branches, message queue, stopped/error states | feature | M | E | T-051 | review (subset 2d-2: stop at once, edit own message, ask again on any answer; redesign 03.11.: Gemini-style actions — tap own message → edit/copy, answer row copy/share/thumbs/again; branches/queue/follow-up suggestions open) |
| T-058 | ChatViewport + scroll-to-bottom pill | ui | S | E | T-052 | todo |
| T-059 | ToolTimeline + SourceChips | ui | S | E | T-052,T-062 | review |
| T-053 | Drawer: chat list, search, rename, delete, pin | ui | M | E | T-052,T-015 | review (redesign 03.11.: glass context menu with scale+fade, marked row, spring drawer + list motion) |
| T-054 | Export & share chat | feature | S | E | T-053 | todo |
| T-055 | Model picker in top bar | ui | S | E | T-052,T-081 | partly (language step + localeConfig done; picker open) |
| T-056 | Private chat + app lock | feature | S | E | T-053 | todo |
| T-080 | Fill model catalogue with real URLs + sha256 | plan | S | F | - | review (brain now QAD-Q4_0, day 2) |
| T-081 | Catalog + DeviceCheck | feature | S | F | T-002 | review (DeviceCheck pure rules + Packs screen) |
| T-082 | PackDownloader (WorkManager) | feature | M | F | T-081 | review |
| T-083 | Onboarding (welcome, language, download brain) | ui | M | F | T-082,T-016 | review (tour + success + skip download; language → 2f) |
| T-084 | Packs screen in settings | ui | S | F | T-082 | review (packs screen 2c; ID also used 03.11. for the thinking picker in the composer, see HANDOFF_LOG) |
| T-085 | UI fixes for long texts (bubble fold, follow-scroll, ellipsis, composer 6 lines, 200 % font, 360 dp, landscape) | ui | S | E | T-052 | review (03.11., screenshots via screens.yml) |
| T-088 | App icon: glossy glass Pim (squircle, adaptive + themed + debug "dev") | ui | S | B | T-061 | review (03.11., own SVG → assets/icon/src/make_icon.py, in app/src/{main,debug}/res) |
| T-089 | Website in style A "Lichtung" + feature status badges + new icon | docs | S | B | T-088 | review (03.11.) |
| T-100i | App icon motif "Pim als Sprechblase" (Pim's glass body as a round chat bubble, tail lower left) | ui | S | B | T-088 | review (04.11., Oskar's choice; make_icon.py, res, website copies) |
| T-102 | App icon motif "Pim im Wald" (glass Pim in front of a round-tree forest), 3D + flat look | ui | S | B | T-100i | review (04.11., Oskar's choice; make_icon.py, res for both looks, website copies) |
| T-103 | Icon choice in the app (activity-alias 3D/flat, DataStore `app_icon`, onboarding step + Settings → Look) | feature | S | B | T-102,T-100 | review (04.11.; alias switches when you leave the app) |
| T-104 | Soft background blur behind menus and popups (Haze, dimming under API 31) | ui | S | B | T-011 | review (04.11.) |
| T-105 | Website: new icon + bug pass (smoother header, phone screenshots, 390/1440 px, EN + /de) | docs | S | B | T-089 | review (04.11.) |
| T-060 | NetworkGate + network log | feature | S | A | T-020 | review |
| T-061 | Pim launcher icon + VectorDrawable export | ui | S | B | T-016 | done via T-088 (03.11.) |
| T-062 | ToolRouter + schemas | feature | S | A | T-040,T-060 | review |
| T-063 | Calculator + units | feature | S | A | T-062 | review |
| T-064 | Weather tool + WeatherCard | feature | M | A | T-062,T-014 | review |
| T-065 | Wikipedia tool + WikiCard | feature | S | A | T-062 | review |
| T-066 | SearchRouter: SearXNG + DDG html + fallback | feature | M | A | T-062 | review |
| T-067 | read_page (own HTML-to-text, no Readability4K) | feature | S | A | T-066 | review |
| T-068 | Intent tools (alarm, timer, calendar, navigate) + ConfirmCard | feature | M | A | T-062,T-014 | review |
| T-069 | Online indicator & network log screen | ui | S | A | T-060 | review |
| T-070 | Feed parser RSS2/Atom/RDF | feature | S | B | T-002 | review |
| T-071 | Standard feed list + repository | feature | S | B | T-070,T-060 | todo |
| T-072 | news tool + NewsCard | feature | S | B | T-071,T-062 | todo |
| T-073 | News screen (3-mode switch, own feeds) | ui | M | B | T-072 | todo |
| T-074 | Article summary on demand | feature | S | B | T-073,T-067 | todo |
| T-110 | Embedder (LFM2.5-Embedding via llama.cpp) | native | S | C | T-030 | todo |
| T-111 | Memory tables + DAO | feature | S | C | T-020 | todo |
| T-112 | Explicit remember/forget + remember tool | feature | S | C | T-111,T-062 | todo |
| T-113 | Retrieval & ranking | feature | S | C | T-111 | todo |
| T-114 | Background extraction worker | feature | M | C | T-112,T-113,T-110 | todo |
| T-115 | Memory screen | ui | M | C | T-113 | todo |
| T-120 | PDF text layer + page render | feature | S | D | T-002 | todo |
| T-121 | PP-OCRv5 Latin via ONNX Runtime | native | M | D | T-120 | todo |
| T-122 | GermanOcrFixer | feature | S | D | T-002 | todo |
| T-123 | Chunking + doc search tool + attach in chat | feature | M | D | T-121,T-122,T-062 | todo |
| T-130 | Card parser | feature | S | E | T-002 | todo |
| T-131 | Card renderer + GBNF | ui | M | E | T-130,T-014 | todo |
| T-140 | STT Moonshine + mic button | feature | M | F | T-082 | todo |
| T-141 | TTS Piper + read aloud | feature | M | F | T-082 | todo |
| T-143 | Pocket TTS on Android (ONNX, all language packs) | native | M | F | T-082 | todo |
| T-142 | Voice mode screen | ui | M | F | T-140,T-141,T-016 | todo |
| T-143 | Default assistant + screen context | feature | M | F | T-052 | todo |
| T-144 | Widget + quick settings tile + share target | feature | S | F | T-052 | todo |
| T-100 | Settings DataStore + screens | feature | M | B | T-015 | review (level 1 as icon tiles + sub pages, reduce-motion switch) |
| T-101 | View/edit system prompt, custom instructions | ui | S | B | T-100,T-045 | review (custom instructions only) |
| T-160 | Image edit kit | feature | M | D | T-082 | todo |
| T-090 | German/English eval set + runner | test | M | H | T-042,T-062 | todo |
| T-150 | Multimodal: Omni/VL image input via mtmd | native | M | C | T-030 | todo |
| T-163 | Pim animated (Compose: blink, breathe, squash, eye tracking) | ui | M | B | T-061,T-019 | review (no eye tracking) |
| T-170 | Image generation pack (SD1.5+DMD2, MNN) | native | M | D | T-160 | todo |
| T-087 | Lichtung as the only style (remove Morgennebel, style switch, 3D icons) | ui | S | B | T-086 | review |
| T-088p | Speed: time out of the system prompt, TTFT log (`HainLlm`) – polish branch (ID also used for the app icon) | perf | S | B | T-045 | review |
| T-089p | Pim alive everywhere, leaf anchored to the head, frame tests – polish branch (ID also used for the website) | ui | M | B | T-163 | review |
| T-091 | Start page: big draggable Pim, 60+ rotating suggestions per language | ui | S | B | T-089p | review |
| T-096 | Premium haptics `HainHaptics` (primitives only, levels Off/Subtle/Normal/Strong) | ui | S | B | T-100 | review |
| T-097 | Pim normal size + 5 s easter egg (grows to ≤ 5×, trembling, rising haptics, burst) | ui | S | B | T-091,T-096 | review |
| T-098 | Own 2048 mini game behind Pim (swipe, merge pop, best score, undo, won/lost) | ui | M | B | T-097 | review |
| T-099 | Subtle haptics everywhere (menus, delete confirm, pull) + HAPTICS.md checklist | ui | S | B | T-096 | review |
| T-190 | Accessibility & performance pass | fix | M | H | T-052,T-100 | doing (2d-2: non-debuggable CI APK, draw-only animations, throttled streaming; R8 + own baseline profile open) |
| T-195 | Trademark check name 'Hain' | plan | XS | H | - | todo |
| T-196 | Trademark policy TRADEMARKS.md (DE+EN) + README sections "Name & Pim" and "Support (coming soon)" | docs | XS | H | - | review (04.11., draft PR) |
| T-106 | Website: hero screenshots fixed (second phone frame without empty area, both frames sized to their image) | fix | XS | B | T-089 | review (04.11., draft PR) |
| T-107 | Website: English app screenshots on the EN page (EN variants of the Roborazzi style tests) | docs | S | B | T-106 | review (04.11., draft PR #35) |
| T-199 | Beta release (GitHub + F-Droid metadata) | scaffold | S | H | T-190 | todo |
| T-210 | Frosted glass action cards (⋯ menu, long-press menu, thinking popover: card blurs what is behind it, light dimming around) | ui | S | B | T-104 | review (04.11., extras PR) |

## M0 · Device check & setup (human)

### T-000 · Benchmark LFM3 on own phone

- type: `plan` · size: `S` · lane: `H` · depends: -
- read: LLM.md §1, MODELS.md
- files: docs/workflow/STATUS.md (results table)
- do:
  1. Install Termux or use llama.cpp Android build; download LFM3-1.7B Q4_K_M and LFM3-Omni-2B GGUF.
  2. Run `llama-bench -p 512 -n 128` for both; note prefill/decode tok/s and RAM.
  3. Ask 10 German questions from KONZEPT.md tone; note quality 1–5.
- accept:
  - numbers in STATUS.md 'Benchmarks'
- check: `manual`

### T-001 · Create Git repo, add llama.cpp submodule

- type: `scaffold` · size: `XS` · lane: `A` · depends: -
- read: AGENTS.md §6-7
- files: .gitmodules, third_party/llama.cpp, LICENSE (GPL-3.0 text)
- do:
  1. `git init`, add GPL-3.0 LICENSE file.
  2. `git submodule add https://github.com/ggml-org/llama.cpp third_party/llama.cpp`, checkout latest release tag that includes LFM3 text support.
  3. Write the commit hash into STATUS.md 'Pinned versions'.
- accept:
  - submodule present, hash recorded
- check: `git submodule status`

## M1 · Project skeleton

### T-002 · Gradle multi-module skeleton

- type: `scaffold` · size: `M` · lane: `A` · depends: T-001
- read: ARCHITECTURE.md §1-2
- files: settings.gradle.kts, build.gradle.kts, gradle/libs.versions.toml, every module's build.gradle.kts + empty package dirs
- do:
  1. Create all modules from ARCHITECTURE.md §1 with namespace `app.hain.<module>`.
  2. Use latest stable AGP/Kotlin/Compose BOM; versions only in libs.versions.toml.
  3. minSdk 29, targetSdk 36, abiFilters arm64-v8a, Java 17.
  4. Do NOT add material3 dependency.
- accept:
  - all modules sync and build
- check: `./gradlew assembleDebug`

### T-003 · Lint & static checks

- type: `scaffold` · size: `XS` · lane: `A` · depends: T-002
- read: AGENTS.md §3
- files: build.gradle.kts, config/detekt.yml, .editorconfig
- do:
  1. Add ktlint gradle plugin and detekt with default config, Compose rules allowed (function naming).
- accept:
  - `./gradlew ktlintCheck detekt` passes on empty project
- check: `./gradlew ktlintCheck detekt`

### T-004 · Hilt + App class + MainActivity

- type: `scaffold` · size: `S` · lane: `A` · depends: T-002
- read: ARCHITECTURE.md
- files: app/src/main/AndroidManifest.xml, app/.../HainApp.kt, MainActivity.kt
- do:
  1. @HiltAndroidApp HainApp, single-activity, edge-to-edge, setContent { HainTheme { Text placeholder } }.
- accept:
  - app starts on device/emulator
- check: `./gradlew assembleDebug`

### T-005 · Strings DE/EN + checker script

- type: `strings` · size: `S` · lane: `A` · depends: T-002
- read: I18N.md
- files: app/src/main/res/values/strings.xml, values-de/strings.xml, res/xml/locales_config.xml, tools/check_strings.py
- do:
  1. Add app_name and 10 base strings (send, stop, new_chat, settings, copy, share, retry, cancel, ok, delete) in both languages.
  2. check_strings.py: exits 1 if key sets differ across all modules' values/ and values-de/.
- accept:
  - script passes
- check: `python3 tools/check_strings.py`

### T-006 · CI workflow

- type: `scaffold` · size: `XS` · lane: `A` · depends: T-003,T-005
- read: TESTING.md
- files: .github/workflows/ci.yml
- do:
  1. GitHub Actions: checkout with submodules, JDK 17, NDK, run assemble, unit tests, ktlint, detekt, check_strings.
- accept:
  - workflow file valid (actionlint if available)
- check: `cat .github/workflows/ci.yml`

## M2 · Design system & Pim

### T-010 · Design tokens

- type: `ui` · size: `S` · lane: `B` · depends: T-002
- read: DESIGN_SYSTEM.md §2
- files: core-ui/src/main/kotlin/app/hain/ui/theme/{HainColors,HainType,HainShapes,HainSpacing,HainTheme}.kt, core-ui/src/main/res/font/*
- do:
  1. Exact hex values from §2 for light and dark.
  2. CompositionLocals for tokens; HainTheme(darkTheme) provides them.
  3. Bundle Inter, Atkinson Hyperlegible, JetBrains Mono (OFL) font files.
- accept:
  - previews light+dark compile
- check: `./gradlew :core-ui:testDebugUnitTest`

### T-011 · Components: HainText, HainSurface, HainButton, HainIconButton

- type: `ui` · size: `S` · lane: `B` · depends: T-018,T-019
- read: DESIGN_SYSTEM.md §3, §6; UI_LIBRARIES.md
- files: core-ui/src/main/kotlin/app/hain/ui/components/<one file per component>.kt
- do:
  1. Start by copying the closest Composables UI component (MIT, keep header, add NOTICE line) and restyle; build HainText, HainSurface, HainButton, HainIconButton on foundation + Compose Unstyled primitives where one exists (sheet, switch, text field, drawer, tooltip); our styling, HainShapes, HainMotion; each has @Preview light+dark.
  2. Composer: send/stop morph, glass over content (hainGlass). AssistantBlock: streaming markdown via rememberStreamingMarkdownState + WordFadeIn. ReasoningBlock: shimmer title while thinking, 'Gedacht für N s' after.
  3. No material3 imports (grep must find none).
- accept:
  - previews compile
  - `grep -r material3 core-ui/src` returns nothing
- check: `./gradlew :core-ui:testDebugUnitTest && ! grep -r material3 core-ui/src`

### T-012 · Components: HainTextField, Composer

- type: `ui` · size: `S` · lane: `B` · depends: T-018,T-019
- read: DESIGN_SYSTEM.md §3, §6; UI_LIBRARIES.md
- files: core-ui/src/main/kotlin/app/hain/ui/components/<one file per component>.kt
- do:
  1. Start by copying the closest Composables UI component (MIT, keep header, add NOTICE line) and restyle; build HainTextField, Composer on foundation + Compose Unstyled primitives where one exists (sheet, switch, text field, drawer, tooltip); our styling, HainShapes, HainMotion; each has @Preview light+dark.
  2. Composer: send/stop morph, glass over content (hainGlass). AssistantBlock: streaming markdown via rememberStreamingMarkdownState + WordFadeIn. ReasoningBlock: shimmer title while thinking, 'Gedacht für N s' after.
  3. No material3 imports (grep must find none).
- accept:
  - previews compile
  - `grep -r material3 core-ui/src` returns nothing
- check: `./gradlew :core-ui:testDebugUnitTest && ! grep -r material3 core-ui/src`

### T-013 · Components: UserBubble, AssistantBlock (markdown via renderer core), ReasoningBlock

- type: `ui` · size: `M` · lane: `B` · depends: T-018,T-019
- read: DESIGN_SYSTEM.md §3, §6; UI_LIBRARIES.md
- files: core-ui/src/main/kotlin/app/hain/ui/components/<one file per component>.kt
- do:
  1. Start by copying the closest Composables UI component (MIT, keep header, add NOTICE line) and restyle; build UserBubble, AssistantBlock (markdown via renderer core), ReasoningBlock on foundation + Compose Unstyled primitives where one exists (sheet, switch, text field, drawer, tooltip); our styling, HainShapes, HainMotion; each has @Preview light+dark.
  2. Composer: send/stop morph, glass over content (hainGlass). AssistantBlock: streaming markdown via rememberStreamingMarkdownState + WordFadeIn. ReasoningBlock: shimmer title while thinking, 'Gedacht für N s' after.
  3. No material3 imports (grep must find none).
- accept:
  - previews compile
  - `grep -r material3 core-ui/src` returns nothing
- check: `./gradlew :core-ui:testDebugUnitTest && ! grep -r material3 core-ui/src`

### T-014 · Components: HainChip, ToolChip, HainCard, SegmentedControl

- type: `ui` · size: `S` · lane: `B` · depends: T-018,T-019
- read: DESIGN_SYSTEM.md §3, §6; UI_LIBRARIES.md
- files: core-ui/src/main/kotlin/app/hain/ui/components/<one file per component>.kt
- do:
  1. Start by copying the closest Composables UI component (MIT, keep header, add NOTICE line) and restyle; build HainChip, ToolChip, HainCard, SegmentedControl on foundation + Compose Unstyled primitives where one exists (sheet, switch, text field, drawer, tooltip); our styling, HainShapes, HainMotion; each has @Preview light+dark.
  2. Composer: send/stop morph, glass over content (hainGlass). AssistantBlock: streaming markdown via rememberStreamingMarkdownState + WordFadeIn. ReasoningBlock: shimmer title while thinking, 'Gedacht für N s' after.
  3. No material3 imports (grep must find none).
- accept:
  - previews compile
  - `grep -r material3 core-ui/src` returns nothing
- check: `./gradlew :core-ui:testDebugUnitTest && ! grep -r material3 core-ui/src`

### T-015 · Components: SettingsRow, HainSwitch, TopBar, Drawer, BottomSheet, Toast, EmptyState, ProgressBar

- type: `ui` · size: `S` · lane: `B` · depends: T-018,T-019
- read: DESIGN_SYSTEM.md §3, §6; UI_LIBRARIES.md
- files: core-ui/src/main/kotlin/app/hain/ui/components/<one file per component>.kt
- do:
  1. Start by copying the closest Composables UI component (MIT, keep header, add NOTICE line) and restyle; build SettingsRow, HainSwitch, TopBar, Drawer, BottomSheet, Toast, EmptyState, ProgressBar on foundation + Compose Unstyled primitives where one exists (sheet, switch, text field, drawer, tooltip); our styling, HainShapes, HainMotion; each has @Preview light+dark.
  2. Composer: send/stop morph, glass over content (hainGlass). AssistantBlock: streaming markdown via rememberStreamingMarkdownState + WordFadeIn. ReasoningBlock: shimmer title while thinking, 'Gedacht für N s' after.
  3. No material3 imports (grep must find none).
- accept:
  - previews compile
  - `grep -r material3 core-ui/src` returns nothing
- check: `./gradlew :core-ui:testDebugUnitTest && ! grep -r material3 core-ui/src`

### T-018 · Add UI libraries + thin wrappers

- type: `scaffold` · size: `S` · lane: `B` · depends: T-010
- read: UI_LIBRARIES.md §2; LICENSES.md
- files: gradle/libs.versions.toml, core-ui/build.gradle.kts, core-ui/.../shape/HainShapes.kt, core-ui/.../effects/{HainGlass,HainShimmer}.kt
- do:
  1. Add Roborazzi screenshot testing (record/verify tasks), Showkase (debugImplementation only), konfetti-compose. Add Compose Unstyled modules (bottom-sheet, modal-bottom-sheet, dialog, dropdown-menu, tooltip, toggle-switch, slider, tab-group, text-field, scrollbars, focus-ring), Haze + haze-blur, Kyant0 capsule + backdrop, compose-shimmer, markdown renderer core + -code, Coil 3, Lottie Compose. Latest stable versions.
  2. HainShapes uses ContinuousRoundedRectangle/ContinuousCapsule.
  3. `Modifier.hainGlass(state)` = Haze blur on API 31+, solid surface fallback below; `Modifier.hainShimmer()`.
  4. No material3 in the dependency tree of core-ui.
- accept:
  - builds
  - `./gradlew :core-ui:dependencies | grep material3` finds nothing
- check: `./gradlew assembleDebug && ! ./gradlew -q :core-ui:dependencies | grep -q material3`

### T-019 · Motion system HainMotion + MotionGovernor

- type: `ui` · size: `S` · lane: `B` · depends: T-018
- read: UI_LIBRARIES.md §3
- files: core-ui/.../motion/{HainMotion,MotionGovernor,WordFadeIn,Haptics}.kt + test
- do:
  1. Spring/tween constants from §3 table.
  2. `LocalReduceMotion`; all helpers return snap() when true.
  3. WordFadeIn: animates only newly appended words (max 30 in flight).
  4. MotionGovernor: frame-time watcher (Choreographer) disables blur after 2 slow frames.
- accept:
  - unit test: reduce motion returns snap; previews
- check: `./gradlew :core-ui:testDebugUnitTest`

### T-016 · Pim v2 drawn in Compose (static states)

- type: `ui` · size: `S` · lane: `B` · depends: T-010
- read: DESIGN_SYSTEM.md §4
- files: core-ui/.../pim/Pim.kt, core-ui/.../pim/PimGeometry.kt
- do:
  1. Port geometry from assets/mascot/v2/motion/pim_motion.json (`geometry`) to Canvas paths (body, stem, leaf+vein, eyes pill/wide/happy/closed/squeeze, fx); `PimPose` data class = rig params of DESIGN_SYSTEM §4; padding like viewBox -12 -18 144 144.
  2. `Pim(state: PimState, size: Dp, modifier)`; all states from DESIGN_SYSTEM.md §4 as static drawings; legacy PNG never used.
- accept:
  - preview shows 4 poses
- check: `./gradlew :core-ui:testDebugUnitTest`

### T-017 · Lucide icons as vector drawables

- type: `ui` · size: `S` · lane: `B` · depends: T-010
- read: DESIGN_SYSTEM.md §2
- files: core-ui/src/main/res/drawable/ic_*.xml, core-ui/.../HainIcons.kt
- do:
  1. Convert needed Lucide SVGs (menu, plus, mic, send, square(stop), copy, volume-2, refresh-cw, share-2, settings, search, newspaper, brain, image, file-text, wifi-off, check, x, chevron-right, trash-2, download, globe) with vd-tool or Android Studio converter.
- accept:
  - all icons render in a preview grid
- check: `./gradlew assembleDebug`

## M3 · LLM core (native + prompt)

### T-020 · Encrypted Room DB skeleton

- type: `feature` · size: `M` · lane: `C` · depends: T-002
- read: DATA.md
- files: core-data/src/main/kotlin/app/hain/data/{HainDatabase,entities/*,dao/*}.kt
- do:
  1. Entities chat, message (+FTS5), chat_summary only (others later).
  2. SQLCipher via net.zetetic:sqlcipher-android + SupportOpenHelperFactory; key from KeyProvider (Keystore-wrapped).
  3. Decide sqlite build with FTS5+sqlite-vec; record decision in STATUS.md.
- accept:
  - DAO tests insert/query/FTS search pass (Robolectric)
- check: `./gradlew :core-data:testDebugUnitTest`

### T-030 · JNI bridge: load/generate/cancel

- type: `native` · size: `M` · lane: `C` · depends: T-001,T-002
- read: LLM.md §1; reference: SmolChat-Android smollm module (Apache-2.0, see UI_LIBRARIES.md §2b)
- files: core-llm/src/main/cpp/{CMakeLists.txt,hain_llm.cpp}, core-llm/build.gradle.kts, core-llm/.../internal/NativeLlm.kt
- do:
  1. Implement JNI table from LLM.md §1 incl. UTF-8 buffering and cancel flag.
  2. 16 KB page alignment flag.
  3. Prefix KV reuse can be a follow-up (T-033).
- accept:
  - instrumented test loads tiny GGUF and generates 8 tokens
- check: `./gradlew :core-llm:connectedDebugAndroidTest (device)`

### T-031 · Copy prompts into assets

- type: `scaffold` · size: `XS` · lane: `C` · depends: T-002
- read: prompts/README.md
- files: core-llm/build.gradle.kts
- do:
  1. Gradle Copy task from /prompts/*.txt to core-llm/src/main/assets/prompts/ before preBuild (generated dir, not committed).
- accept:
  - APK contains assets/prompts/system_full.en.txt
- check: `./gradlew assembleDebug && unzip -l app/build/outputs/apk/debug/app-debug.apk | grep system_full`

### T-032 · LlmEngine implementation

- type: `feature` · size: `S` · lane: `C` · depends: T-030
- read: ARCHITECTURE.md §3,§5; LLM.md §6
- files: core-llm/.../api/LlmEngine.kt, internal/LlamaEngine.kt, di/LlmModule.kt, core-testing/.../FakeLlmEngine.kt
- do:
  1. Single-thread dispatcher; callbackFlow for tokens; state flow; SamplingParams presets from LLM.md §6.
  2. FakeLlmEngine emits scripted tokens with delay.
- accept:
  - unit test with FakeLlmEngine-based consumer passes
- check: `./gradlew :core-llm:testDebugUnitTest`

### T-033 · Prefix KV cache reuse

- type: `native` · size: `S` · lane: `C` · depends: T-030
- read: LLM.md §1
- files: core-llm/src/main/cpp/hain_llm.cpp
- do:
  1. Keep token list of KV; on new prompt find common prefix, llama_memory_seq_rm the rest, decode only suffix.
- accept:
  - 2nd turn prefill time < 30 % of first (log)
- check: `manual on device`

### T-040 · Data classes ChatMessage/ToolSchema/ToolCall

- type: `feature` · size: `XS` · lane: `D` · depends: T-002
- read: ARCHITECTURE.md §3
- files: core-llm/.../api/{ChatMessage,ToolSchema,ToolCall,ParsedOutput}.kt
- do:
  1. Exactly as in ARCHITECTURE.md; ToolSchema holds name, description, parameters JsonObject.
- accept:
  - compiles
- check: `./gradlew assembleDebug`

### T-041 · Golden prompt files + render script

- type: `test` · size: `S` · lane: `D` · depends: T-040
- read: LLM.md §2
- files: tools/render_golden.py, core-llm/src/test/resources/golden/G-01..G-06*.txt
- do:
  1. Python: jinja2 render prompts/lfm3-1.7b_chat_template.jinja with 6 scenarios (described in LLM.md §2), bos_token='<|startoftext|>'.
  2. Write outputs + scenario JSON inputs (G-xx.json) next to them.
- accept:
  - 6 txt + 6 json files
- check: `python3 tools/render_golden.py && ls core-llm/src/test/resources/golden`

### T-042 · PromptBuilder (pure Kotlin)

- type: `feature` · size: `M` · lane: `D` · depends: T-041
- read: LLM.md §2
- files: core-llm/.../internal/LfmPromptBuilder.kt, test/.../LfmPromptBuilderTest.kt
- do:
  1. Implement rules 1–8; test loads each G-xx.json, builds, compares to G-xx.txt byte-for-byte.
- accept:
  - all 6 golden tests pass
- check: `./gradlew :core-llm:testDebugUnitTest --tests "*LfmPromptBuilder*"`

### T-043 · ToolCallParser

- type: `feature` · size: `S` · lane: `D` · depends: T-040
- read: LLM.md §3, §5
- files: core-llm/.../internal/XmlToolCallParser.kt, test/.../XmlToolCallParserTest.kt
- do:
  1. Implement; 8 tests from §5 table.
- accept:
  - 8 tests pass
- check: `./gradlew :core-llm:testDebugUnitTest --tests "*XmlToolCallParser*"`

### T-044 · StreamSplitter

- type: `feature` · size: `S` · lane: `D` · depends: T-043
- read: LLM.md §4
- files: core-llm/.../internal/StreamSplitter.kt, test/.../StreamSplitterTest.kt
- do:
  1. State machine; tests feed outputs char-by-char and in random chunks (seeded) and compare to expected deltas.
- accept:
  - tests pass
- check: `./gradlew :core-llm:testDebugUnitTest --tests "*StreamSplitter*"`

### T-045 · ThinkingPolicy + SystemPrompt.fill

- type: `feature` · size: `S` · lane: `D` · depends: T-040
- read: LLM.md §7, §9; I18N.md
- files: core-llm/.../internal/{ThinkingPolicy,SystemPromptFiller}.kt + tests
- do:
  1. As specified; tests DE+EN dates, missing values removed, emergency numbers.
- accept:
  - tests pass
- check: `./gradlew :core-llm:testDebugUnitTest`

## M4 · Chat MVP (first usable app)

### T-050 · ChatEngine turn loop

- type: `feature` · size: `M` · lane: `E` · depends: T-032,T-042,T-044,T-045,T-020
- read: ARCHITECTURE.md §4; LLM.md §8
- files: feature-chat/.../internal/engine/ChatEngine.kt + test
- do:
  1. Steps 1–6 of §4 with ToolRouter and MemoryStore as interfaces (use empty fakes for now).
  2. Max 3 tool rounds, timeout handling.
- accept:
  - test with FakeLlmEngine scripted tool call → fake tool → final answer
- check: `./gradlew :feature-chat:testDebugUnitTest --tests "*ChatEngine*"`

### T-051 · ChatViewModel + UI state

- type: `feature` · size: `S` · lane: `E` · depends: T-050
- read: ARCHITECTURE.md
- files: feature-chat/.../ChatViewModel.kt, ChatUiState.kt + test
- do:
  1. Send, stop, regenerate, edit last user message, streaming state.
- accept:
  - VM tests with fakes pass
- check: `./gradlew :feature-chat:testDebugUnitTest`

### T-052 · Chat screen

- type: `ui` · size: `M` · lane: `E` · depends: T-051,T-011,T-012,T-013
- read: DESIGN_SYSTEM.md §5
- files: feature-chat/.../ui/ChatScreen.kt
- do:
  1. Messages list (LazyColumn, auto-scroll unless user scrolled up), composer, reasoning block, empty state with Pim + 4 chips.
- accept:
  - Compose UI test: type, send, see fake answer
- check: `./gradlew :feature-chat:testDebugUnitTest`

### T-057 · Branches, message queue, stopped/error states

- type: `feature` · size: `M` · lane: `E` · depends: T-051
- read: UI_LIBRARIES.md §1
- files: feature-chat/.../ui/{BranchPicker,QueuedMessage,MessageStates}.kt, ChatViewModel.kt + test
- do:
  1. Edit/regenerate creates sibling via parent_id; BranchPicker '2 / 3' switches.
  2. Send while generating → queued chip, auto-send after Done.
  3. Stopped label + Weiter; error card with retry.
- accept:
  - VM tests for branch switch and queue
- check: `./gradlew :feature-chat:testDebugUnitTest`

### T-058 · ChatViewport + scroll-to-bottom pill

- type: `ui` · size: `S` · lane: `E` · depends: T-052
- read: UI_LIBRARIES.md §1
- files: feature-chat/.../ui/{ChatViewport,ScrollToBottomPill}.kt
- do:
  1. Stick to bottom while streaming unless user scrolled up > 48 dp; pill appears with fade/slide; tap animates scroll.
  2. animateItem() for inserts; shared element composer→user bubble (SharedTransitionLayout).
- accept:
  - Compose UI test for stick/unstick
- check: `./gradlew :feature-chat:testDebugUnitTest`

### T-059 · ToolTimeline + SourceChips

- type: `ui` · size: `S` · lane: `E` · depends: T-052,T-062
- read: UI_LIBRARIES.md §1, §3
- files: feature-chat/.../ui/{ToolTimeline,SourceChips}.kt
- do:
  1. Chip per tool call: running (shimmer) → done (✓ bounce) / failed; orange dot if online; sources as chips with favicon (Coil, cached).
- accept:
  - previews + UI test
- check: `./gradlew :feature-chat:testDebugUnitTest`

### T-053 · Drawer: chat list, search, rename, delete, pin

- type: `ui` · size: `M` · lane: `E` · depends: T-052,T-015
- read: DESIGN_SYSTEM.md §5
- files: feature-chat/.../ui/ChatListDrawer.kt, ChatListViewModel.kt
- do:
  1. Groups Today/Yesterday/7 days/Older; FTS search over messages.
  2. Open animation: chat scales to 0.94 with continuous corners + dim + drawer parallax (UI_LIBRARIES.md §3).
- accept:
  - UI test
- check: `./gradlew :feature-chat:testDebugUnitTest`

### T-054 · Export & share chat

- type: `feature` · size: `S` · lane: `E` · depends: T-053
- read: DATA.md
- files: feature-chat/.../export/ChatExporter.kt + test
- do:
  1. Markdown and JSON export via SAF; share sheet with plain text.
- accept:
  - exporter unit tests
- check: `./gradlew :feature-chat:testDebugUnitTest --tests "*ChatExporter*"`

### T-055 · Model picker in top bar

- type: `ui` · size: `S` · lane: `E` · depends: T-052,T-081
- read: MODELS.md
- files: feature-chat/.../ui/ModelPicker.kt
- do:
  1. Lists installed brains; switch = unload/load with progress toast.
- accept:
  - UI test with fake catalog
- check: `./gradlew :feature-chat:testDebugUnitTest`

### T-056 · Private chat + app lock

- type: `feature` · size: `S` · lane: `E` · depends: T-053
- read: DATA.md; SETTINGS.md
- files: feature-chat/..., app/.../lock/AppLock.kt
- do:
  1. Private chat flag (no memory, not listed after close if setting).
  2. BiometricPrompt app lock on resume.
- accept:
  - manual + VM test
- check: `./gradlew assembleDebug`

## M5 · Models & onboarding

### T-080 · Fill model catalogue with real URLs + sha256

- type: `plan` · size: `S` · lane: `F` · depends: -
- read: MODELS.md
- files: core-models/src/main/assets/catalog.json
- do:
  1. Human/strong model: copy exact HF file URLs and sha256 from model pages. Never invent.
- accept:
  - every default entry has url+sha256
- check: `python3 -m json.tool core-models/src/main/assets/catalog.json`

### T-081 · Catalog + DeviceCheck

- type: `feature` · size: `S` · lane: `F` · depends: T-002
- read: MODELS.md
- files: core-models/.../{ModelCatalogImpl,DeviceCheckImpl}.kt + tests
- do:
  1. Parse catalog.json; DeviceCheck rules as unit-tested pure function on DeviceProfile.
- accept:
  - tests pass
- check: `./gradlew :core-models:testDebugUnitTest`

### T-082 · PackDownloader (WorkManager)

- type: `feature` · size: `M` · lane: `F` · depends: T-081
- read: MODELS.md
- files: core-models/.../download/*.kt + test (MockWebServer)
- do:
  1. Range resume, sha256 verify, atomic rename, foreground notification, Wi-Fi constraint.
- accept:
  - resume + checksum tests pass
- check: `./gradlew :core-models:testDebugUnitTest`

### T-083 · Onboarding (welcome, language, download brain)

- type: `ui` · size: `M` · lane: `F` · depends: T-082,T-016
- read: DESIGN_SYSTEM.md §5
- files: feature-onboarding/...
- do:
  1. 3 steps with Pim; shows size, Wi-Fi hint; continues to chat when brain installed.
- accept:
  - UI test with fake downloader
- check: `./gradlew :feature-onboarding:testDebugUnitTest`

### T-084 · Packs screen in settings

- type: `ui` · size: `S` · lane: `F` · depends: T-082
- read: MODELS.md
- files: feature-settings/.../PacksScreen.kt
- do:
  1. List packs, size, install/delete, storage bar, custom GGUF import (SAF).
- accept:
  - UI test
- check: `./gradlew :feature-settings:testDebugUnitTest`

## M6 · Tools & internet

### T-060 · NetworkGate + network log

- type: `feature` · size: `S` · lane: `A` · depends: T-020
- read: TOOLS.md §1
- files: core-tools/.../net/{NetworkGateImpl,NetworkLogDao}.kt + test
- do:
  1. Offline check, purpose switches, log entry, UA rules.
- accept:
  - tests with MockWebServer
- check: `./gradlew :core-tools:testDebugUnitTest --tests "*NetworkGate*"`

### T-061 · Pim launcher icon + VectorDrawable export

- type: `ui` · size: `S` · lane: `B` · depends: T-016
- read: DESIGN_SYSTEM.md §4
- files: core-ui/src/main/res/drawable/pim_*.xml
- do:
  1. Export idle Pim as adaptive launcher icon + notification icon (monochrome eyes+leaf) from assets/mascot/v2/static/pim_idle.svg (no vein, no fx).
- accept:
  - previews
- check: `./gradlew assembleDebug`

### T-062 · ToolRouter + schemas

- type: `feature` · size: `S` · lane: `A` · depends: T-040,T-060
- read: TOOLS.md §2, §6-7
- files: core-tools/.../ToolRouterImpl.kt, schemas/*.json + test
- do:
  1. Registry, enabled set from settings, error mapping, 12 s timeout.
- accept:
  - tests
- check: `./gradlew :core-tools:testDebugUnitTest --tests "*ToolRouter*"`

### T-063 · Calculator + units

- type: `feature` · size: `S` · lane: `A` · depends: T-062
- read: TOOLS.md §5
- files: core-tools/.../calc/*.kt + tests
- do:
  1. Parser, decimal comma, units table; ≥ 30 test cases.
- accept:
  - tests pass
- check: `./gradlew :core-tools:testDebugUnitTest --tests "*Calc*"`

### T-064 · Weather tool + WeatherCard

- type: `feature` · size: `M` · lane: `A` · depends: T-062,T-014
- read: TOOLS.md §4; CARDS.md §1
- files: core-tools/.../weather/*.kt, core-cards/.../WeatherCard.kt + tests (fixtures)
- do:
  1. Geocode + forecast, WMO codes DE/EN.
- accept:
  - tests pass
- check: `./gradlew :core-tools:testDebugUnitTest --tests "*Weather*"`

### T-065 · Wikipedia tool + WikiCard

- type: `feature` · size: `S` · lane: `A` · depends: T-062
- read: TOOLS.md §2
- files: core-tools/.../wiki/*.kt + tests
- do:
  1. opensearch → summary REST, lang de/en, UA header.
- accept:
  - fixture tests
- check: `./gradlew :core-tools:testDebugUnitTest --tests "*Wiki*"`

### T-066 · SearchRouter: SearXNG + DDG html + fallback

- type: `feature` · size: `M` · lane: `A` · depends: T-062
- read: TOOLS.md §3
- files: core-tools/.../search/*.kt + tests with saved HTML/JSON fixtures
- do:
  1. Classifier, order, back-off, uddg decoding.
- accept:
  - tests pass
- check: `./gradlew :core-tools:testDebugUnitTest --tests "*Search*"`

### T-067 · read_page with Readability4K

- type: `feature` · size: `S` · lane: `A` · depends: T-066
- read: TOOLS.md §3
- files: core-tools/.../search/PageReader.kt + test
- do:
  1. Fetch, extract, trim by tokenCount (inject a TokenCounter interface).
- accept:
  - tests
- check: `./gradlew :core-tools:testDebugUnitTest --tests "*PageReader*"`

### T-068 · Intent tools (alarm, timer, calendar, navigate) + ConfirmCard

- type: `feature` · size: `M` · lane: `A` · depends: T-062,T-014
- read: TOOLS.md §2; CARDS.md §1
- files: core-tools/.../intents/*.kt, core-cards/.../ConfirmCard.kt + tests
- do:
  1. Build intents (unit test the Intent extras), card with confirm button, undo when auto.
- accept:
  - tests
- check: `./gradlew :core-tools:testDebugUnitTest --tests "*Intent*"`

### T-069 · Online indicator & network log screen

- type: `ui` · size: `S` · lane: `A` · depends: T-060
- read: DESIGN_SYSTEM.md; SETTINGS.md
- files: feature-settings/.../NetworkLogScreen.kt, ToolChip orange dot
- do:
  1. List from network_log, filter by purpose, clear.
- accept:
  - UI test
- check: `./gradlew :feature-settings:testDebugUnitTest`

## M7 · News (ADR-14)

### T-070 · Feed parser RSS2/Atom/RDF

- type: `feature` · size: `S` · lane: `B` · depends: T-002
- read: NEWS.md §3
- files: core-tools/.../news/FeedParser.kt + fixtures for all 14 standard feeds
- do:
  1. Save one real response per standard feed as fixture (human/strong model fetches), parse all.
- accept:
  - 14 fixture tests pass
- check: `./gradlew :core-tools:testDebugUnitTest --tests "*FeedParser*"`

### T-071 · Standard feed list + repository

- type: `feature` · size: `S` · lane: `B` · depends: T-070,T-060
- read: NEWS.md §2-3
- files: core-tools/src/main/assets/news/standard_feeds.json, news/NewsRepository.kt + test
- do:
  1. Modes, conditional GET, dedup, 7-day retention, Correctiv filter, health status.
- accept:
  - tests
- check: `./gradlew :core-tools:testDebugUnitTest --tests "*NewsRepository*"`

### T-072 · news tool + NewsCard

- type: `feature` · size: `S` · lane: `B` · depends: T-071,T-062
- read: NEWS.md §4
- files: core-tools/.../news/NewsTool.kt, core-cards/.../NewsCard.kt + test
- do:
  1. FTS query, 8 items, forModel format.
- accept:
  - tests
- check: `./gradlew :core-tools:testDebugUnitTest --tests "*NewsTool*"`

### T-073 · News screen (3-mode switch, own feeds)

- type: `ui` · size: `M` · lane: `B` · depends: T-072
- read: NEWS.md §1, §6; DESIGN_SYSTEM.md
- files: feature-news/...
- do:
  1. SegmentedControl with exactly 3 modes; own feed add/rename/delete; no per-source toggles, no labels.
- accept:
  - UI test asserts no toggle on standard feeds
- check: `./gradlew :feature-news:testDebugUnitTest`

### T-074 · Article summary on demand

- type: `feature` · size: `S` · lane: `B` · depends: T-073,T-067
- read: NEWS.md §5
- files: feature-news/.../SummarizeArticle.kt
- do:
  1. Fetch → readability → LLM 5 bullets, not stored; paywall message.
- accept:
  - VM test with fakes
- check: `./gradlew :feature-news:testDebugUnitTest`

## M8 · Memory

### T-110 · Embedder (LFM2.5-Embedding via llama.cpp)

- type: `native` · size: `S` · lane: `C` · depends: T-030
- read: MEMORY.md §3
- files: core-llm/src/main/cpp/hain_llm.cpp (embedding fn), core-memory/.../Embedder.kt
- do:
  1. Embedding mode context (separate small context, can coexist with brain? measure RAM; else load on demand). Record dimension in DATA.md.
- accept:
  - instrumented test returns vector
- check: `device`

### T-111 · Memory tables + DAO

- type: `feature` · size: `S` · lane: `C` · depends: T-020
- read: DATA.md; MEMORY.md
- files: core-data/... memory entities, sqlite-vec table, migration + test
- do:
  1. Add memory_profile/topic/fact/fts/vec; migration test.
- accept:
  - tests
- check: `./gradlew :core-data:testDebugUnitTest`

### T-112 · Explicit remember/forget + remember tool

- type: `feature` · size: `S` · lane: `C` · depends: T-111,T-062
- read: MEMORY.md §2
- files: core-memory/.../ExplicitMemory.kt + test, prompts/extract_facts.txt
- do:
  1. Regex DE/EN, chip event; write extract_facts.txt prompt (short, JSON only).
- accept:
  - tests
- check: `./gradlew :core-memory:testDebugUnitTest`

### T-113 · Retrieval & ranking

- type: `feature` · size: `S` · lane: `C` · depends: T-111
- read: MEMORY.md §3
- files: core-memory/.../MemoryStoreImpl.kt, Ranking.kt + test
- do:
  1. Hybrid score formula, thresholds, budget trim, output block.
- accept:
  - ranking tests
- check: `./gradlew :core-memory:testDebugUnitTest --tests "*Ranking*"`

### T-114 · Background extraction worker

- type: `feature` · size: `M` · lane: `C` · depends: T-112,T-113,T-110
- read: MEMORY.md §2.3
- files: core-memory/.../MemoryExtractionWorker.kt + test with fake LLM
- do:
  1. Grammar JSON, cosine thresholds, labels, suggested for sensitive.
- accept:
  - tests
- check: `./gradlew :core-memory:testDebugUnitTest`

### T-115 · Memory screen

- type: `ui` · size: `M` · lane: `C` · depends: T-113
- read: MEMORY.md §5
- files: feature-memory/...
- do:
  1. Tabs, edit, delete everywhere, export/import JSON.
- accept:
  - UI test
- check: `./gradlew :feature-memory:testDebugUnitTest`

## M9 · Documents & OCR

### T-120 · PDF text layer + page render

- type: `feature` · size: `S` · lane: `D` · depends: T-002
- read: DOCS_OCR.md
- files: core-docs/.../pdf/*.kt + test pdf fixtures
- do:
  1. pdfbox-android text, PdfRenderer 300 dpi bitmap.
- accept:
  - tests
- check: `./gradlew :core-docs:testDebugUnitTest`

### T-121 · PP-OCRv5 Latin via ONNX Runtime

- type: `native` · size: `M` · lane: `D` · depends: T-120
- read: DOCS_OCR.md
- files: core-docs/.../ocr/*.kt
- do:
  1. Decide RapidOCR-Android vs own ORT pipeline (det/cls/rec, DB postprocess, CTC decode); record in STATUS.md.
- accept:
  - instrumented test reads fixture image with ≥ 95 % chars
- check: `device`

### T-122 · GermanOcrFixer

- type: `feature` · size: `S` · lane: `D` · depends: T-002
- read: DOCS_OCR.md rules
- files: core-docs/.../fix/GermanOcrFixer.kt, assets/dict/de_words.txt + test
- do:
  1. Rules 1–5; dictionary from a free list (licence noted in LICENSES.md).
- accept:
  - all examples pass
- check: `./gradlew :core-docs:testDebugUnitTest --tests "*GermanOcrFixer*"`

### T-123 · Chunking + doc search tool + attach in chat

- type: `feature` · size: `M` · lane: `D` · depends: T-121,T-122,T-062
- read: DOCS_OCR.md
- files: core-docs/.../DocStore.kt, core-tools/.../SearchDocumentsTool.kt, feature-chat attach UI
- do:
  1. Small docs inline, large docs via tool; page citations.
- accept:
  - tests
- check: `./gradlew :core-docs:testDebugUnitTest`

## M10 · Cards, voice, assistant

### T-130 · Card parser

- type: `feature` · size: `S` · lane: `E` · depends: T-002
- read: CARDS.md §2
- files: core-cards/.../CardParser.kt + tests
- do:
  1. Line parser, streaming-tolerant, fallback.
- accept:
  - ≥ 15 tests
- check: `./gradlew :core-cards:testDebugUnitTest`

### T-131 · Card renderer + GBNF

- type: `ui` · size: `M` · lane: `E` · depends: T-130,T-014
- read: CARDS.md §3-4
- files: core-cards/.../CardRenderer.kt, assets/card.gbnf
- do:
  1. Renderer with core-ui; GBNF grammar file (validate with llama.cpp `llama-gbnf-validator` on PC).
- accept:
  - previews, validator ok
- check: `./gradlew :core-cards:testDebugUnitTest`

### T-140 · STT Moonshine + mic button

- type: `feature` · size: `M` · lane: `F` · depends: T-082
- read: VOICE.md
- files: core-voice/.../stt/*.kt, feature-chat composer mic
- do:
  1. sherpa-onnx streaming, VAD, permission sheet.
- accept:
  - device test
- check: `device`

### T-141 · TTS Piper + read aloud

- type: `feature` · size: `M` · lane: `F` · depends: T-082
- read: VOICE.md
- files: core-voice/.../tts/*.kt
- do:
  1. Text prep (unit-tested), sentence pipelining, speed. Pim voice per VOICE.md: Pocket TTS voice anna +2 st, speed 1.15 in every language, English pack always installed, setup language picker downloads the chosen pack, system TTS fallback by DeviceCheck RTF, pitch via AudioTrack PlaybackParams, setting Pim/Neutral, amplitude envelope flow for Pim speaking motion; compare with assets/voice/samples.
- accept:
  - text prep tests + device
- check: `./gradlew :core-voice:testDebugUnitTest`

### T-143 · Pocket TTS on Android (ONNX, all language packs)

- type: `native` · size: `M` · lane: `F` · depends: T-082
- read: VOICE.md (Pim voice)
- files: tools/voice/export_kerstin_onnx.py, docs/specs/VOICE.md
- do:
  1. Export Pocket TTS (en/de/fr/es/it/pt/nl, voice anna embedding) to ONNX fp16/int8 (reference: PocketTTS-Android-Engine), streaming inference in :core:voice, measure size + RTF on T-000 devices; URLs/SHA-256 only via T-080 on the T-000 devices, compare with assets/voice/samples; write URL+SHA-256 only after a human verifies (T-080).
- accept:
  - ONNX output audibly matches reference samples; RTF table in VOICE.md
- check: `device`

### T-142 · Voice mode screen

- type: `ui` · size: `M` · lane: `F` · depends: T-140,T-141,T-016
- read: VOICE.md; DESIGN_SYSTEM.md
- files: feature-chat/.../voice/VoiceModeScreen.kt
- do:
  1. States, ring from RMS, barge-in.
- accept:
  - device
- check: `device`

### T-143 · Default assistant + screen context

- type: `feature` · size: `M` · lane: `F` · depends: T-052
- read: KONZEPT.md §13
- files: app/.../assist/*
- do:
  1. VoiceInteractionService, ROLE_ASSISTANT request, Assist API text+screenshot (setting gated).
- accept:
  - device
- check: `device`

### T-144 · Widget + quick settings tile + share target

- type: `feature` · size: `S` · lane: `F` · depends: T-052
- read: KONZEPT.md §5
- files: app/.../widget/*, tile/*, manifest
- do:
  1. Glance widget 'Frag Hain', TileService opens voice mode, ACTION_SEND text/image/pdf into new chat.
- accept:
  - device
- check: `device`

## M11 · Settings, images, polish

### T-100 · Settings DataStore + screens

- type: `feature` · size: `M` · lane: `B` · depends: T-015
- read: SETTINGS.md
- files: core-data/.../settings/*, feature-settings/...
- do:
  1. All keys/defaults from SETTINGS.md; level 1 + Advanced; news only 3 modes.
- accept:
  - defaults unit test
- check: `./gradlew :feature-settings:testDebugUnitTest`

### T-101 · View/edit system prompt, custom instructions

- type: `ui` · size: `S` · lane: `B` · depends: T-100,T-045
- read: SETTINGS.md; LLM.md §9
- files: feature-settings/.../PromptScreen.kt
- do:
  1. Show filled prompt preview, edit custom, reset.
- accept:
  - UI test
- check: `./gradlew :feature-settings:testDebugUnitTest`

### T-160 · Image edit kit

- type: `feature` · size: `M` · lane: `D` · depends: T-082
- read: IMAGE.md
- files: feature-image/...
- do:
  1. Select, erase, cut-out, upscale, undo, save. Licence rows first.
- accept:
  - device
- check: `device`

### T-090 · German/English eval set + runner

- type: `test` · size: `M` · lane: `H` · depends: T-042,T-062
- read: TESTING.md
- files: eval/de_en_questions.jsonl, tools/eval_runner.py
- do:
  1. 100 items as specified; runner against llama-server; report markdown.
- accept:
  - runner prints pass rate
- check: `python3 tools/eval_runner.py --dry-run`

### T-150 · Multimodal: Omni/VL image input via mtmd

- type: `native` · size: `M` · lane: `C` · depends: T-030
- read: LLM.md §10
- files: core-llm/src/main/cpp/*, core-llm/.../MultimodalEngine.kt
- do:
  1. Use llama.cpp mtmd API with mmproj; image attach in chat.
- accept:
  - device
- check: `device`

### T-163 · Pim animated (Compose: blink, breathe, squash, eye tracking)

- type: `ui` · size: `M` · lane: `B` · depends: T-061,T-019
- read: UI_LIBRARIES.md §2; DESIGN_SYSTEM.md §4; STATUS Q-04
- files: core-ui/.../pim/PimAnimated.kt, core-ui/src/main/res/raw/pim.riv (if Rive)
- do:
  1. Copy pim_motion.json to core-ui assets; `PimMotionPlayer` samples keyframes with the 6 easings (port `sample()`/`ease()` 1:1, unit-test against values printed by tools/pim_gen.py); base-state + micro-action layering; `PimIdleDirector` from idleSchedule; event → motion map from DESIGN_SYSTEM §4; RMS inputs for listening/speaking; tap → tap_squish + haptic; reduce_motion → blink only. Rive only if Q-04 decides so later.
- accept:
  - unit tests: sample() matches python at 5 timestamps per motion; screenshot frames at 25/50/75 % of hop, shake, sleeping match preview GIF frames visually
- check: `./gradlew :core-ui:testDebugUnitTest`

### T-170 · Image generation pack (SD1.5+DMD2, MNN)

- type: `native` · size: `M` · lane: `D` · depends: T-160
- read: IMAGE.md
- files: feature-image/.../gen/*
- do:
  1. Only ≥ 8 GB RAM; fixed safety filter.
- accept:
  - device
- check: `device`

### T-190 · Accessibility & performance pass

- type: `fix` · size: `M` · lane: `H` · depends: T-052,T-100
- read: DESIGN_SYSTEM.md
- files: various (list in handoff)
- do:
  1. TalkBack labels, font scale 200 %, contrast, baseline profile, startup < 1 s without model.
- accept:
  - checklist in STATUS.md
- check: `manual`

### T-195 · Trademark check name 'Hain'

- type: `plan` · size: `XS` · lane: `H` · depends: -
- read: ADR-20
- files: docs/workflow/STATUS.md
- do:
  1. Human: DPMA/TMview search classes 9 and 42.
- accept:
  - result noted
- check: `manual`

### T-196 · Trademark policy + support note (coming soon)

- type: `docs` · size: `XS` · lane: `H` · depends: -
- read: ADR-20; T-195
- files: TRADEMARKS.md, README.md
- do:
  1. TRADEMARKS.md, German (plain language) then English: "open code, protected brand" (modelled on the Mozilla policy). Code GPL-3.0-or-later; name "Hain", Pim (look, animations, voice) and icon/logo are project marks; forks need another name, own mascot and icon, must not look official; allowed: unchanged official builds, mentions ("based on Hain"), reports/tutorials, distro/F-Droid packaging without code changes beyond the build; contact via issues; not legal advice, registration may be planned; no location details.
  2. README: short "Name & Pim / Trademarks" section next to the licence, "Support / Unterstützen" section with "coming soon" (star, issue, tell others). No FUNDING.yml, no active sponsor button.
- accept:
  - both files render on GitHub, links work
- check: `manual`

### T-199 · Beta release (GitHub + F-Droid metadata)

- type: `scaffold` · size: `S` · lane: `H` · depends: T-190
- read: AGENTS.md
- files: fastlane/metadata/android/{de-DE,en-US}/*, .github/workflows/release.yml
- do:
  1. Signed release build, changelog, F-Droid metadata.
- accept:
  - release APK builds
- check: `./gradlew assembleRelease`

