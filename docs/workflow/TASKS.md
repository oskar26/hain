# TASKS.md — backlog

Generated from `tools/tasks_src.py` (planner). Status lives **only in the Overview table** (last column). Implementers change only that cell for their task.
Lanes A–F can run in parallel; H = human/strong model. `native` tasks need a strong model (WORKFLOW.md).

Status values: `todo` · `doing (<model>)` · `review` · `done` · `blocked: <why>` · `split`

## Overview

> **Unique IDs (05.11., release 0.2.0):** every ID in this file is used once. Earlier double uses were renamed:
> the thinking picker is **T-084p** (T-084 = packs screen), the polish-branch tasks are **T-088p**/**T-089p**
> (T-088 = app icon, T-089 = website), and the planned mini-apps forest (M12) moves from T-200–T-226 to
> **T-300–T-326**, because T-210–T-238 are taken. HANDOFF_LOG.md is append-only and keeps the old names in
> older entries. Commit 6c07e89 is titled "T-230" but belongs to T-075–T-077 (see M7). New IDs: check here first.

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
| T-057 | Branches, message queue, stopped/error states | feature | M | E | T-051 | review (subset 2d-2: stop at once, edit own message, ask again on any answer; redesign 03.11.: Gemini-style actions; 8b 04.11.: versions via parent_id/selected_at + "‹ 2/3 ›", waiting messages while Pim answers, DB v4; follow-up suggestions open) |
| T-058 | ChatViewport + scroll-to-bottom pill | ui | S | E | T-052 | review (8b: follow only at the bottom, frosted pill with "Neue Antwort", animateItem; shared element composer→bubble not done) |
| T-059 | ToolTimeline + SourceChips | ui | S | E | T-052,T-062 | review |
| T-053 | Drawer: chat list, search, rename, delete, pin | ui | M | E | T-052,T-015 | review (redesign 03.11.: glass context menu with scale+fade, marked row, spring drawer + list motion) |
| T-054 | Export & share chat | feature | S | E | T-053 | review (04.11., 0.1.8: Markdown + own PDF, SAF/share sheet, all chats as ZIP) |
| T-055 | Model picker in top bar | ui | S | E | T-052,T-081 | review (2f-2, `hark/picker-perf`: brain picker in the top bar; language step + localeConfig done in 2f) |
| T-056 | Private chat + app lock | feature | S | E | T-053 | review (04.11., 0.1.8: private chat in memory only, BiometricPrompt + PIN, delay, FLAG_SECURE) |
| T-080 | Fill model catalogue with real URLs + sha256 | plan | S | F | - | review (brain now QAD-Q4_0, day 2) |
| T-081 | Catalog + DeviceCheck | feature | S | F | T-002 | review (DeviceCheck pure rules + Packs screen) |
| T-082 | PackDownloader (WorkManager) | feature | M | F | T-081 | review |
| T-083 | Onboarding (welcome, language, download brain) | ui | M | F | T-082,T-016 | review (tour + success + skip download; language → 2f) |
| T-084 | Packs screen in settings | ui | S | F | T-082 | review (packs screen 2c) |
| T-084p | Thinking picker in the composer (brain button + glass popover, same `AnswerMode` as Settings → Answers) | ui | S | B | T-100 | review (03.11., `hark/redesign-3d`; was a second "T-084", renamed 05.11.) |
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
| T-068a | Repeating alarms (`days`: Mo-Fr, täglich, montags …) → `AlarmClock.EXTRA_DAYS` | feature | S | A | T-068 | review (04.11., 0.1.4) |
| T-068b | Answer after a phone action says "vorbereitet" (`ActionWording`), card hint "tipp auf Stellen" | fix | S | A | T-068 | review (04.11., 0.1.4) |
| T-068c | Setting `confirm_actions`: off = card counts down 3 s with "Rückgängig", then starts | feature | S | A | T-068,T-100 | review (04.11., 0.1.4) |
| T-068d | Intent extras tested with Robolectric (`PhoneIntentsTest`) | test | S | A | T-068 | review (04.11., 0.1.4) |
| T-060a | Brain download through the NetworkGate (`NetworkGate.stream`, log + online dot) | feature | S | A | T-060 | review (04.11., 0.1.4) |
| T-060b | Setting `web_search` on / ask first / off (`SearchConsent`) | feature | S | A | T-060,T-066 | review (04.11., 0.1.4) |
| T-064a | WeatherCard + WikiCard in core-cards under the answer (incl. T-065a) | ui | S | A | T-064,T-065 | review (04.11., 0.1.4) |
| T-064b | Weather without a place = last known location (setting `weather_location`, asks first) | feature | S | A | T-064 | review (04.11., 0.1.4) |
| T-064c | Favicons at the source chips (needs image fetch through the NetworkGate) | ui | S | A | T-059,T-060 | review (04.11., 0.1.10.1) |
| T-068e | ConfirmCard into core-cards (needs a chat-free card state) | refactor | S | A | T-068 | review (04.11., 0.1.10.1) |
| T-180 | Tool inventory (name, description, parameters, typical errors) in TOOLS.md §0 | docs | S | C | T-062 | review (4d) |
| T-181 | Tool eval: 52 questions DE/EN + host harness (`tools/eval`), baseline before 4d | test | M | C | T-062 | review (4d) |
| T-182 | Tool definitions for the small brain (`tools.json`: one sentence when/when not, flat params, enums; compact results, errors with a hint) | feature | M | C | T-181 | review (4d) |
| T-183 | Robust tool calls (repair broken calls, name/parameter aliases, dates relative to now, missing values → ask, duplicates once, retry) | feature | M | C | T-182 | review (4d) |
| T-184 | Tool prompts DE/EN in `assets/prompts/tools.<lang>.txt`, turn hint, follow-up + link hint, token budget | feature | S | C | T-182 | review (4d) |
| T-185 | Tool eval after 4d: before/after table in TOOLS.md §8 | test | S | C | T-184 | review (4d) |
| T-186 | Unit tests: repair parser, fixer/aliases, specs, prompt templates, follow-ups, engine dedupe | test | S | C | T-183 | review (4d) |
| T-070 | Feed parser RSS2/Atom/RDF | feature | S | B | T-002 | review |
| T-071 | Standard feed list + repository | feature | S | B | T-070,T-060 | review |
| T-072 | news tool + NewsCard | feature | S | B | T-071,T-062 | review |
| T-073 | News screen (3-mode switch, own feeds) | ui | M | B | T-072 | review |
| T-074 | Article summary on demand | feature | S | B | T-073,T-067 | review |
| T-075 | News list with pictures: 19 standard feeds loaded one by one, sources mixed, NewsImages (gate, disk cache), top story + cards, frosted bar, pull to refresh | ui | M | B | T-073 | review (04.11., 0.1.10.3, `hark/news-reader`) |
| T-076 | Reading view in the app (Readability, own code) with text settings, article pictures/charts; tablet list-detail from 840 dp | ui | M | B | T-075 | review (04.11., 0.1.10.3) |
| T-077 | "Paywall" mark only for articles found cut off (Paywall.kt, `markRead`), never bypassed | feature | S | B | T-076 | review (04.11., 0.1.10.3) |
| T-078 | Ask Pim about the open article (context cut to the brain's budget), "Im Chat weiterführen"; replaces T-074 summary | feature | S | B | T-076 | review (04.11., 0.1.10.3) |
| T-079 | "Dein Briefing heute" (stories picked on the phone, sentences by the brain), optional morning note (WorkManager, off by default) | feature | M | B | T-075 | review (04.11., 0.1.10.3) |
| T-110 | Embedder (LFM2.5-Embedding via llama.cpp) | native | S | C | T-030 | review (04.11., 7b, draft PR #46) |
| T-111 | Memory tables + DAO | feature | S | C | T-020 | review (04.11., draft PR #38) |
| T-112 | Explicit remember/forget + remember tool | feature | S | C | T-111,T-062 | review (04.11., draft PR #38) |
| T-113 | Retrieval & ranking | feature | S | C | T-111 | review (04.11., draft PR #38) |
| T-114 | Background extraction worker | feature | M | C | T-112,T-113,T-110 | review (04.11., 7b, draft PR #46) |
| T-115 | Memory screen | ui | M | C | T-113 | review (04.11., draft PR #38) |
| T-116 | Memory chips under answers, topics tab, "Suche nach Bedeutung" tile | ui | S | C | T-115,T-110 | review (04.11., 7b, draft PR #46) |
| T-117 | Wire memory part 2 (embed pack, extraction, chips) into the app | feature | S | C | T-110,T-114,T-116 | review (04.11., 7b, draft PR #46) |
| T-120 | PDF text layer + page render | feature | S | D | T-002 | review (Hark, PR #49) |
| T-121 | PP-OCRv5 Latin via ONNX Runtime | native | M | D | T-120 | review (Hark, PR #49) |
| T-122 | GermanOcrFixer | feature | S | D | T-002 | review (Hark, PR #49) |
| T-123 | Chunking + doc search tool + attach in chat | feature | M | D | T-121,T-122,T-062 | review (Hark, PR #49) |
| T-130 | Card parser | feature | S | E | T-002 | done |
| T-131 | Card renderer + GBNF | ui | M | E | T-130,T-014 | done |
| T-132 | Card charts: bar + line (own Compose canvas, TalkBack sentence) in parser, renderer, card.gbnf, CARDS.md | ui | S | E | T-131 | review (04.11., 0.1.10.1) |
| T-133 | Card grammar during the answer: card.gbnf only inside a ```card block (on after the fence line, off after the closing ```), lenient parser as fallback | native | S | E | T-131,T-030 | review (04.11., 0.1.10.1) |
| T-140 | STT Moonshine + mic button | feature | M | F | T-082 | review (Hark, hark/day-10) |
| T-141 | TTS (system voice, Pim pitch) + read aloud | feature | M | F | T-082 | review (Hark, hark/day-10) |
| T-145 | Pocket TTS on Android (ONNX, all language packs) | native | M | F | T-082 | todo |
| T-142 | Voice mode screen | ui | M | F | T-140,T-141,T-016 | todo |
| T-143 | Default assistant + screen context | feature | M | F | T-052 | todo |
| T-144 | Widget + quick settings tile + share target | feature | S | F | T-052 | review (8b: share text/links → new chat, Glance widget "Frag Pim", tile "Neuer Chat"; images/PDF share + tile→voice open) |
| T-100 | Settings DataStore + screens | feature | M | B | T-015 | review (level 1 as icon tiles + sub pages, reduce-motion switch) |
| T-101 | View/edit system prompt, custom instructions | ui | S | B | T-100,T-045 | review (custom instructions only) |
| T-160 | Image edit kit | feature | M | D | T-082 | todo |
| T-090 | German/English eval set + runner | test | M | H | T-042,T-062 | todo |
| T-150 | Multimodal: Omni/VL image input via mtmd | native | M | C | T-030 | todo |
| T-163 | Pim animated (Compose: blink, breathe, squash, eye tracking) | ui | M | B | T-061,T-019 | review (no eye tracking) |
| T-170 | Image generation pack (SD1.5+DMD2, MNN) | native | M | D | T-160 | todo |
| T-087 | Lichtung as the only style (remove Morgennebel, style switch, 3D icons) | ui | S | B | T-086 | review |
| T-088p | Speed: time out of the system prompt, TTFT log (`HainLlm`) – polish branch (logged as "T-088" on 04.11., renamed) | perf | S | B | T-045 | review |
| T-089p | Pim alive everywhere, leaf anchored to the head, frame tests – polish branch (logged as "T-089" on 04.11., renamed) | ui | M | B | T-163 | review |
| T-091 | Start page: big draggable Pim, 60+ rotating suggestions per language | ui | S | B | T-089p | review |
| T-096 | Premium haptics `HainHaptics` (primitives only, levels Off/Subtle/Normal/Strong) | ui | S | B | T-100 | review |
| T-097 | Pim normal size + 5 s easter egg (grows to ≤ 5×, trembling, rising haptics, burst) | ui | S | B | T-091,T-096 | review |
| T-098 | Own 2048 mini game behind Pim (swipe, merge pop, best score, undo, won/lost) | ui | M | B | T-097 | review |
| T-099 | Subtle haptics everywhere (menus, delete confirm, pull) + HAPTICS.md checklist | ui | S | B | T-096 | review |
| T-190 | Accessibility & performance pass | fix | M | H | T-052,T-100 | review (2d-2: non-debuggable CI APK, draw-only animations, throttled streaming; 2f-2 `hark/picker-perf`: R8 + shrinkResources for release, hand-written baseline profile + ProfileInstaller, CI release smoke test with start time) |
| T-195 | Trademark check name 'Hain' | plan | XS | H | - | todo |
| T-196 | Trademark policy TRADEMARKS.md (DE+EN) + README sections "Name & Pim" and "Support (coming soon)" | docs | XS | H | - | review (04.11., draft PR) |
| T-106 | Website: hero screenshots fixed (second phone frame without empty area, both frames sized to their image) | fix | XS | B | T-089 | review (04.11., draft PR) |
| T-107 | Website: English app screenshots on the EN page (EN variants of the Roborazzi style tests) | docs | S | B | T-106 | review (04.11., draft PR #35) |
| T-220 | Keine erfundenen Quellen ohne Werkzeug (Prompt-Regel + `CitationCleaner`) | fix | S | B | T-062 | review (04.11., draft PR) |
| T-199 | Beta release (GitHub + F-Droid metadata) | scaffold | S | H | T-190 | todo |
| T-210 | Frosted glass action cards (⋯ menu, long-press menu, thinking popover: card blurs what is behind it, light dimming around) | ui | S | B | T-104 | review (04.11., extras PR) |
| T-215 | Version display: `hain.versionName` in gradle.properties (0.1.3.1), BuildConfig commit + build date, docs/workflow/VERSIONING.md | scaffold | XS | A | T-002 | review (04.11., extras PR) |
| T-214 | "Über Hain" page: centred Pim header, website, GitHub, version, licence, open-source licences, update rows | ui | S | B | T-215 | review (04.11., extras PR) |
| T-213 | Pim in the chat more often and bigger: thinking row 40 dp, 24 dp Pim at the newest answer (peeks in while tools run, cheers when done, fright on errors), empty chat 96 dp | ui | S | B | T-163 | review (04.11., extras PR) |
| T-211 | Developer mode (7× tap on the version): own GGUF from Hugging Face, sampling, context, thinking, own system prompt, speed + debug info | feature | M | C | T-214,T-032 | review (04.11., extras PR) |
| T-212 | Update check: version.json on nightly + v* releases, optional daily check via NetworkGate (purpose UPDATE), one-time ask card, calm banner → release page | feature | S | C | T-214,T-215 | review (04.11., extras PR) |
| T-216 | Zurück-Pfeil zeigt nach links: Einstellungs-Unterseiten nutzen `HainIcons.back` (Lucide chevron-left, autoMirrored) statt chevron-right | fix | XS | B | T-214 | review (04.11., extras PR) |
| T-240 | Großes Gehirn: Jan-v3.5-4B Q4_K_M als optionale Stufe für starke Handys (Katalog `tier`/`recommend_ram_gb`, Gehirn-Wähler, Modelle & Pakete, DeviceCheck, ADR-24) | feature | M | C | T-055,T-084 | review (05.11., 0.1.10.6, Draft-PR #54) |
| T-241 | Qwen3-Chatformat fürs Große Gehirn (`BrainFormat` aus der GGUF-Architektur, `QwenPromptBuilder` gegen das Jinja der Jan-GGUF, JSON-Tool-Calls, kein Denken, Sampling laut Modellkarte) | native | M | C | T-240 | review (05.11., Draft-PR #54) |
| T-242 | Schneller Start: KV-Snapshot des festen System-Prompts pro Modell + Prompt-Hash im Cache, Logs zu Aufwärmen und erstem Token | native | M | C | T-032 | review (05.11., Draft-PR #54) |
| T-243 | Host-Test Jan-v3.5-4B mit Hains Prompt (Werkzeug, Argumente, unnötige Aufrufe, TTFT kalt/Snapshot, RAM) → docs/research/jan-host-test.md | plan | S | H | T-241 | review (05.11., Draft-PR #54) |
| T-244 | Doku 0.1.10.6 (zuerst 0.1.10.4): README, Website (EN + /de), AGENTS, MODELS, LLM §2b/§2c, LICENSES, Version | docs | S | B | T-240 | review (05.11., Draft-PR #54) |
| T-250 | Unique task IDs: T-084p thinking picker, T-088p/T-089p polish branch, mini-apps forest T-300–T-326 | docs | XS | B | - | review (05.11., 0.1.10.5, PR #55) |
| T-251 | Tablets: Models & packs and Memory in two columns from 840 dp (`HainGrid`/`HainColumns`), "Über dich" + settings next to the memory list; phones unchanged | ui | S | B | T-235 | review (05.11., 0.1.10.5, PR #55) |
| T-252 | Screenshot samples without real people or places: German shots use the same Paris example as the English ones, memory/tablet shots with invented facts; shots re-recorded | docs | S | B | T-107 | review (05.11., 0.1.10.5, PR #55) |
| T-253 | Share a PDF, photo or text file from another app into Hain: new chat with the file attached (paper clip way, only `content://`) | feature | S | B | T-144,T-123 | review (05.11., 0.1.10.5, PR #55) |
| T-254 | Accessibility: thinking button and pack buttons get 48 × 48 dp touch targets | fix | XS | B | T-238 | review (05.11., 0.1.10.5, PR #55) |
| T-255 | RELEASE_0.2_CHECKLIST.md: one line per feature with its screen and website text, README/website plan and signing for the 0.2.0 step | docs | S | B | - | review (05.11., 0.1.10.5, PR #55) |
| T-256 | Fix: reading view (and news settings, ask sheet) centred short blocks like headings and list items instead of starting them at the left edge | fix | XS | B | T-076 | review (05.11., 0.1.10.5, PR #55) |
| T-257 | Fix: memory tab "Vorschläge (1)" wrapped to two lines on phones; now "Vorschläge 1", at most "9+" | fix | XS | B | T-115 | review (05.11., 0.1.10.5, PR #55) |
| T-258 | Fix: a deleted Wikipedia pack stayed open, so its space came back only after a restart; replaced pack files are opened again | fix | XS | B | T-237 | review (05.11., 0.1.10.5, PR #55) |
| T-259 | Fix: after turning the phone the chat and news view models read the old activity (old language, offline switch, private flag); now one `HostState` view model | fix | S | B | T-079 | review (05.11., 0.1.10.5, PR #55) |

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
- files: feature-chat/.../export/ (ChatMarkdown, ChatZip, MarkdownBlocks, PdfLayout, ChatPdf, ChatExport, ExportMenu) + tests
- do:
  1. One chat as Markdown and as PDF (own, android.graphics.pdf.PdfDocument: title, date, roles, paragraphs, lists, code, page break, footer, Lichtung green); save via SAF or share sheet (FileProvider `${applicationId}.exports`). Single answer share stays in the answer row (T-057).
  2. All chats as Markdown ZIP (Settings → Privacy). Private chats never exported. (04.11.: JSON export dropped in favour of PDF, per Oskar's sprint list.)
- accept:
  - exporter unit tests (Markdown, ZIP, Markdown → PDF blocks, PDF page breaks)
- check: `./gradlew :feature-chat:testDebugUnitTest --tests "*export*"`

### T-055 · Model picker in top bar

- type: `ui` · size: `S` · lane: `E` · depends: T-052,T-081
- read: MODELS.md
- files: feature-chat/.../api/ModelPicker.kt, core-models/.../api/BrainChoice.kt, core-common/.../api/BrainChoiceStore.kt, app/.../BrainPicking.kt
- do:
  1. Lists installed brains; switch = unload/load with progress toast.
  2. (2f-2) Tap on "Hain · Auf diesem Handy" opens a compact frosted-glass menu: brains of the catalogue with a plain name (by size: Flinkes / Ausgewogenes / Schlaues Gehirn), model + size, "Empfohlen für dein Handy" (DeviceCheck: biggest brain that fits GOOD, else smallest TIGHT), check on the active one. A pick loads in the background (thin progress line under the top bar + subtitle %), a running answer stops first. Not installed / "Mehr Gehirne" → Wissenspakete. Choice in DataStore `datastore/brain.preferences_pb` (`brain_id`).
- accept:
  - UI test with fake catalog (`ModelPickerTest`, Robolectric), `BrainChoiceTest`, `BrainChoiceStoreTest`, `ChatViewModelTest` (switch)
- check: `./gradlew :feature-chat:testDebugUnitTest :core-models:testDebugUnitTest :core-common:testDebugUnitTest`

### T-056 · Private chat + app lock

- type: `feature` · size: `S` · lane: `E` · depends: T-053
- read: DATA.md; SETTINGS.md
- files: feature-chat/... (ChatStore, ChatViewModel, api/PrivateChat.kt), core-common/api/lock/AppLockGate.kt, app/.../lock/AppLock.kt + LockScreen.kt
- do:
  1. Private chat: only in memory, never in DB/search/memory, clearly marked, deleted on leave; tools work.
  2. BiometricPrompt app lock (BIOMETRIC_WEAK | DEVICE_CREDENTIAL) after background with delay now/1 min/5 min; FLAG_SECURE switch.
- accept:
  - `PrivateChatTest` (no DB writes, not searchable, deleted on leave), `AppLockGateTest` (timing logic), manual check on the phone
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

> **Task-IDs News-Reader (04.11.):** T-230–T-237 belegt der parallele PR #52 (`hark/ui-polish`). Der News-Reader läuft deshalb unter den freien IDs T-075–T-079. Der schon gepushte Commit 6c07e89 trägt irrtümlich „T-230“ im Titel; inhaltlich ist er die Datenschicht von T-075/T-076/T-077 (Feeds, Readability, Paywall, Seitenbilder).

### T-075 · News list with pictures

- type: `ui` · size: `M` · lane: `B` · depends: T-073
- read: NEWS.md §1, §2, §6, §8; DESIGN_LANGUAGE.md
- files: core-tools/.../news/ (standard_feeds.json, NewsRepository, FeedParser, PageImage), feature-news/.../api/ (NewsImages, NewsCards, PullRefresh, NewsScreen, NewsViewModel, NewsPrefs, NewsSettingsPage)
- do:
  1. 19 standard feeds, each loaded on its own (15 s limit), sources take turns (`mixed()`), no dates in the future.
  2. Pictures from the feed, the feed's HTML or the page's og:image; loaded through NetworkGate (purpose IMAGES), disk cache 48 MB, memory cache; setting "Bilder in der Liste".
  3. Frosted top bar, mode chips in one line, top story, cards with picture right, pull to refresh, settings page.
- accept:
  - NewsRepositoryTest, FeedParserTest, NewsImagesTest, NewsViewModelTest; screenshots
- check: `./gradlew :core-tools:testDebugUnitTest :feature-news:testDebugUnitTest`

### T-076 · Reading view in the app

- type: `ui` · size: `M` · lane: `B` · depends: T-075
- read: NEWS.md §5, §8
- files: core-tools/.../news/ (HtmlTree, Readability), feature-news/.../api/ (ArticleReader, ReaderScreen, NewsScreen)
- do:
  1. Page through the gate (purpose NEWS, only on tap) → readable blocks with pictures; reading view with text size, serif/sans, line spacing, colours; "Auf Website öffnen".
  2. From 840 dp list left, article right.
- accept:
  - ReadabilityTest (fixtures), NewsViewModelTest (open/close); screenshots phone + tablet
- check: `./gradlew :feature-news:testDebugUnitTest`

### T-077 · Paywall mark only when true

- type: `feature` · size: `S` · lane: `B` · depends: T-076
- files: core-tools/.../news/Paywall.kt, NewsRepository.markRead
- do:
  1. No paywall flag per feed; after reading, an article found cut off (JSON-LD `isAccessibleForFree=false`, typical wall texts) gets the mark. Never bypass.
- accept:
  - PaywallTest, NewsViewModelTest (cut-off article gets the mark)
- check: `./gradlew :core-tools:testDebugUnitTest :feature-news:testDebugUnitTest`

### T-078 · Ask Pim about the article

- type: `feature` · size: `S` · lane: `B` · depends: T-076
- files: feature-news/.../api/ArticleAsk.kt, ReaderScreen.kt (Pim bubble + sheet), app/.../NewsBrain.kt
- do:
  1. Article text cut to the brain's context (`budgetChars`: context − 1,100 tokens, 3 chars per token, 1,500–24,000 chars) at a paragraph end; thinking off; answer only from the article.
  2. Quick questions (Zusammenfassen, Einfach erklärt, Hintergrund), own question, "Im Chat weiterführen". Replaces the T-074 summary.
- accept:
  - ArticleAskTest, NewsViewModelTest
- check: `./gradlew :feature-news:testDebugUnitTest`

### T-079 · Daily briefing

- type: `feature` · size: `M` · lane: `B` · depends: T-075
- files: feature-news/.../api/Briefing.kt, BriefingDaily.kt, NewsCards.kt (BriefingCard), app/.../HainApp.kt, MainActivity.kt
- do:
  1. Stories of the last 24 h clustered by shared words, more sources = more important, max 2 per source, 5–7 points; one sentence each by the brain (only when it is already loaded on open, "Neu erzeugen" loads it), else the story's first sentence; kept once a day in `files/news/briefing.json`.
  2. Optional morning note around 07:00 (WorkManager, network required, off by default, POST_NOTIFICATIONS asked when switched on); tap opens News.
- accept:
  - BriefingTest
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

### T-116 · Memory chips under answers, topics tab, "Suche nach Bedeutung" tile

- type: `ui` · size: `S` · lane: `C` · depends: T-115,T-110
- read: MEMORY.md §2.1, §3.5, §5
- files: feature-memory/.../AnswerChips.kt, MemoryTopicsTab.kt, MemoryPage.kt, MemoryScreen.kt; feature-chat/.../ChatExtras.kt (`answerFooter`), ChatScreen.kt; feature-settings/.../PacksScreen.kt; strings DE/EN
- do:
  1. Chips "Gemerkt ✓ · Rückgängig", "Vergessen ✓ · Rückgängig", "N Erinnerungen genutzt" (tap shows which) under finished answers, 48 dp targets, TalkBack labels.
  2. Tab "Themen" (facts by kind + own note), "Ersetzt: …" under suggestions, hint tile "Suche nach Bedeutung" with download/progress; packs screen describes kind `embed`.
- accept:
  - `MemoryListTest` (topics, replaced, chips, tile state), `AnswerFooterTest`; screenshots via screens.yml
- check: `./gradlew :feature-memory:testDebugUnitTest :feature-chat:testDebugUnitTest`

### T-117 · Wire memory part 2 (embed pack, extraction, chips) into the app

- type: `feature` · size: `S` · lane: `C` · depends: T-110,T-114,T-116
- read: MEMORY.md §7
- files: core-models/.../catalog.json, Models.kt; app/.../AppModule.kt, HainApp.kt, MainActivity.kt; feature-chat/.../ChatEngine.kt (`memoryHint`), ChatViewModel.kt (`onAnswered`)
- do:
  1. Catalogue entry kind `embed` (min_ram_gb 3, verified sha256/size), `TextEmbedder` + `MemoryTurn` singletons, `PackEmbedder` on the store.
  2. `ChatEngine.memoryHint` (suspend, Dispatchers.Default), `MemoryExtraction.current` in `HainApp`, `schedule(chatId)` after each finished answer, `yieldToChat` when the user writes.
- accept:
  - `CatalogParserTest`, `ChatEngineTest.memoryLineFollowsTheLanguageHintOffTheMainThread`, `ChatViewModelTest.aFinishedAnswerTellsTheMemory…`
- check: `./gradlew assembleDebug testDebugUnitTest`

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
- done (Häppchen 10, 04.11.): instead of sherpa-onnx (no Maven AAR) Moonshine Streaming Small runs directly on ONNX
  Runtime 1.28.0 (`MoonshineStt`: frontend → encoder → adapter → cross K/V → decoder with self K/V, greedy;
  `MoonshineTokenizer` decodes ▁ + byte fallback). Packs `moonshine-small-de` / `-en` in catalog.json (7 files each,
  pinned commit + SHA-256, own folder, chain download in `DefaultModelRepository`, HEAD preflight through the
  NetworkGate). `DictationController` (mic → level → resample 16 kHz → silence trim → text, max 60 s, engine kept
  90 s). Mic in the composer (tap/tap, hold/let go, slide off = cancel, TalkBack click), recording bar (✕, level
  bars, clock, Fertig), RECORD_AUDIO asked on first tap after a card. Whole utterance after stop instead of live
  partials; Silero VAD not used (energy trim). Host check: Kotlin pipeline on JVM ORT = reference text DE+EN.
  Device test open.

### T-141 · TTS Piper + read aloud

- type: `feature` · size: `M` · lane: `F` · depends: T-082
- read: VOICE.md
- files: core-voice/.../tts/*.kt
- done (Häppchen 10, 04.11.): `SpeechText` (Markdown → sentences, unit-tested), `ReadAloudController` (2 sentences
  ahead, stop, one answer at a time) behind `SpeechSynth`; engine = Android system TTS (`SystemSpeech`, pitch 1.1225 +
  rate 1.15, started lazily), "Vorlesen" in the answer row. Pocket TTS (Pim's own voice) is T-145 (was T-143, renumbered in the integration PR: the ID was taken twice): public ONNX
  exports are English-only with voice `alba` baked in (soniqo/Pocket-TTS-100M-ONNX-INT8, exporter
  csukuangfj/pocket-tts-onnx-export) or need Arm's closed engine (Arm/pocket-tts-de-mix-precision); anna + DE needs an
  own export. Open from the card: Pim/Neutral setting, amplitude envelope, DeviceCheck RTF fallback, language packs.
- do:
  1. Text prep (unit-tested), sentence pipelining, speed. Pim voice per VOICE.md: Pocket TTS voice anna +2 st, speed 1.15 in every language, English pack always installed, setup language picker downloads the chosen pack, system TTS fallback by DeviceCheck RTF, pitch via AudioTrack PlaybackParams, setting Pim/Neutral, amplitude envelope flow for Pim speaking motion; compare with assets/voice/samples.
- accept:
  - text prep tests + device
- check: `./gradlew :core-voice:testDebugUnitTest`

### T-145 · Pocket TTS on Android (ONNX, all language packs)

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
  2. (2f-2) Release build with R8 (`isMinifyEnabled` + `isShrinkResources`, keep rules for JNI/llama, Room, SQLCipher, kotlinx.serialization, WorkManager in `app/proguard-rules.pro`), baseline profile `app/src/main/baselineProfiles/baseline-prof.txt` + `androidx.profileinstaller`; CI job `release-smoke` (screens.yml, `tools/ci/release_smoke.sh`) installs the release APK on an emulator, checks it starts, checks the installed profile and measures cold start before/after. See SPEED.md §5.
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

### T-220 · Keine erfundenen Quellen ohne Werkzeug

- type: `fix` · size: `S` · lane: `B` · depends: T-062
- read: TOOLS.md §3b, LLM.md §9
- files: prompts/system_*.txt (+ Kopie in core-llm/src/main/assets/prompts), app/.../SystemPromptSource.kt, feature-chat/.../internal/{CitationCleaner,ChatEngine}.kt, Tests
- warum: Auf Oskars Handy (LFM3-1.7B) schrieb Pim ohne jedes Werkzeug „… 200+ Jahre alt werden [1].“ plus eine Zeile „[1] Wikipedia-Artikel … (falls verfügbar) – hier kein aktuelles Werkzeug nötig …“ und begann mit „**Fakt:**“.
- do:
  1. Prompt (DE+EN, kurz wegen 2048 Kontext): Quellen [n] nur, wenn in dieser Antwort ein Werkzeug benutzt wurde; sonst keine [n], keine Quellenliste, kein Wort über Werkzeuge, keine Etiketten wie „Fakt:“. In `system_full`, `system_short`, `TOOLS_*` und im Hinweis vor der Antwort (`TOOL_NUDGE_*`).
  2. `CitationCleaner` (deterministisch, auch wenn das Modell den Prompt ignoriert), angewendet im `ChatEngine` auf den Live-Text und die fertige Antwort: ohne Quellen fallen Marker (`[1]`, `[2, 3]`, `[1-3]`, `【1】`), Quellenzeilen (`[1] …`) und -blöcke (`Quellen:`/`Sources:` + Liste) und Sätze „kein Werkzeug nötig für diese Frage“ weg; mit Quellen bleiben nur Marker mit vorhandener Nummer. „Fakt:“/„Fact:“ am Zeilenanfang fällt immer weg. Codeblöcke, Inline-Code und Markdown-Links bleiben unberührt.
- accept:
  - Oskars Beispiel wird zu reinem Text ohne [1], Quellenzeile, Werkzeug-Satz und „Fakt:“
  - Antworten mit Quellen behalten gültige Marker; Code bleibt gleich
- check: `./gradlew :feature-chat:testDebugUnitTest :core-llm:testDebugUnitTest ktlintCheck detekt` (CI)

### T-216 · Zurück-Pfeil zeigt nach links

- type: `fix` · size: `XS` · lane: `B` · depends: T-214
- files: core-ui/src/main/res/drawable/ic_chevron_left.xml, core-ui/.../HainIcons.kt, feature-settings/.../SettingsScreen.kt
- warum: In den Einstellungs-Unterseiten (Aussehen, Entwickler, Über Hain, …) zeigte der Zurück-Knopf oben links nach rechts (`HainIcons.chevronRight`).
- do:
  1. Lucide `chevron-left` als Vector-Drawable (gleicher Strich wie die anderen, `autoMirrored` für RTL), `HainIcons.back`.
  2. Zurück-Knopf in `SettingsScreen` nutzt `HainIcons.back`; Hub behält das ✕. Andere `chevronRight`-Stellen (Zeilen-Chevron, gedrehter „nach unten“-Knopf im Chat) sind keine Zurück-Knöpfe und bleiben.
- accept:
  - Alle Unterseiten zeigen einen nach links zeigenden Pfeil
- check: `ktlintCheck detekt` + Screens (CI)

## 0.1.10.2 · UI-Feinschliff (`hark/ui-polish`, PR #52)

### T-230 · Chat-Kopfzeile als Milchglas

- type: `polish` · size: `S` · lane: `B` · depends: T-233
- files: feature-chat/.../api/{ChatChrome,ChatScreen}.kt, core-ui/.../components/{Scaffolding,Buttons,Messages}.kt, core-ui/.../effects/ChromeGlass.kt, `TopBarTitleTest`
- do: Kopfzeile schwebt als Milchglas über dem Chat (Chat scrollt darunter), Titel mittig auf der ganzen Leiste, kompakter; Debug-Werte in einer Zeile.
- accept: Titel bleibt mittig bei 1–3 Knöpfen; ohne Blur (Android < 12, Akkusparen) ruhige Seitenfarbe
- check: Screens (CI)

### T-231 · Ein Pim-Status für Warten, Denken, Nachschlagen, Schreiben

- type: `polish` · size: `S` · lane: `B`
- files: feature-chat/.../api/{PimStatus,ToolTimeline,ChatScreen}.kt, Strings, `PimStatusTest`
- do: ein einziges Status-Element mit Pim statt mehrerer Zeilen; Phase wechselt weich.
- check: `PimStatusTest` + Screens (CI)

### T-232 · Nachrichtenfeld als Milchglas

- type: `polish` · size: `S` · lane: `B` · depends: T-230
- files: core-ui/.../components/Composer.kt, feature-chat/.../api/ChatChrome.kt
- do: Eingabefeld und die Zeilen darüber als Milchglas über dem Chat, ruhiges Lichtung-Licht hinter dem Chat.
- check: Screens (CI)

### T-233 · Stärkeres Milchglas für Menüs, Popover, Sprung-Knopf, Seitenleiste

- type: `polish` · size: `S` · lane: `B`
- files: core-ui/.../effects/{HainGlass,MenuBackdrop}.kt, feature-chat/.../api/{ChatListDrawer,ChatScreen}.kt, `HainGlassTest`, `MenuBackdropTest`
- do: einheitlich stärkerer Blur; im Akkusparmodus kein Blur.
- check: Screens (CI)

### T-236 · Nachrichten ohne Thema, ehrliche Fehlschritte

- type: `fix` · size: `S` · lane: `B`
- files: core-tools/.../news/{NewsQuery,NewsTool}.kt, core-tools/.../api/ToolRouter.kt, feature-chat/.../internal/ChatEngine.kt, feature-chat/.../api/ToolTimeline.kt, Tests
- do: „Was gibt’s Neues?“ ohne Thema bringt die Top-Schlagzeilen; ein fehlgeschlagener Werkzeug-Schritt sagt kurz warum; dieselbe Suche wird nicht wiederholt.
- check: `NewsQueryTest`, `ToolRouterTest`, `RobustCallsTest` (CI)

### T-237 · Wissenspakete: Wikipedia offline (Kiwix ZIM)

- type: `feature` · size: `M` · lane: `B` · depends: T-084
- files: core-tools/.../wiki/zim/*, core-tools/.../wiki/WikipediaTool.kt, core-models/src/main/assets/catalog.json, core-models/.../api/KnowledgePacks.kt, app/.../MainActivity.kt, gradle/libs.versions.toml, docs/specs/LICENSES.md
- do: eigener ZIM-Leser (Header, Pfad-Index, Cluster), zstd über zstd-jni (BSD-2-Clause, nur die Android-AAR); Pakete DE + EN im Katalog; das Werkzeug `wikipedia` liest zuerst offline, sonst online über das NetworkGate.
- note: zstd-jni ist auf **1.5.7-12** gepinnt – ab 1.5.7-13 verlangt die AAR `minCompileSdk=37` (wir bauen gegen 36, KI-04); 1.5.7-12 ist 16-KB-ausgerichtet.
- check: `ZimWikiTest`, `CatalogParserTest` (CI)

### T-234 · Einstellungen aufgeräumt: „Modelle & Pakete“

- type: `polish` · size: `M` · lane: `B` · depends: T-237
- files: feature-settings/.../api/{SettingsScreen,PacksScreen,SettingsActions}.kt, app/.../MainActivity.kt, Strings, Tests
- do: erste Ebene nur Gruppen; „Modelle & Pakete“ mit Unterseiten Wissen, Gehirne, Multimedia, Sprache, Extras; eigener Eintrag „Nachrichten“.
- check: `PackCategoryTest` + Screens (CI)

### T-235 · Tablet und Breitbild

- type: `feature` · size: `M` · lane: `B` · depends: T-234
- files: core-ui/.../layout/HainWindowSize.kt (neu), feature-chat/.../api/{ChatSidePanel (neu),ChatListDrawer,ChatScreen}.kt, feature-settings/.../api/SettingsScreen.kt, Screens-Tests
- do:
  1. `HainWindowSize` (COMPACT < 600 dp, MEDIUM < 840 dp, EXPANDED ab 840 dp; Grenzen wie Androids WindowSizeClass, eigener Code ohne Material).
  2. Ab 840 dp bleibt die Chat-Liste als ruhige Seitenleiste (320 dp, Haarlinie) links offen; kein Menü-Knopf, kein Scrim, kein Gleiten.
  3. Einstellungen als List-Detail: links die Gruppen (offene Gruppe mit grünem Rand, TalkBack „ausgewählt“), rechts die Seite (max. 720 dp breit). Gedächtnis, Pakete und Nachrichten öffnen weiter als eigene Seiten.
- accept: Handy unverändert; Drehen/Split-Screen wechselt sauber; Tablet-Screens 1280 × 800 dp
- check: `HainWindowSizeTest` + `TabletScreens`, `TabletSettingsScreens` (CI)

### T-238 · Feinschliff über alle Screens

- type: `polish` · size: `S` · lane: `B` · depends: T-235
- do: Abstände, Touch-Ziele (≥ 48 dp) und Lichtung-Tokens über alle Screens angeglichen, maßvoll (siehe PR).
- check: Screens (CI)

### T-240 · Großes Gehirn (Jan-v3.5-4B) als optionale Stufe

- type: `feature` · size: `M` · lane: `C` · depends: T-055, T-084
- files: core-models/src/main/assets/catalog.json, core-models/.../api/{Models,BrainChoice}.kt, core-models/.../internal/CatalogParser.kt, feature-chat/.../api/ModelPicker.kt, feature-settings/.../api/PacksScreen.kt, Strings, docs/decisions/ADR-24-grosses-gehirn-jan.md, Tests
- do: Katalog-Eintrag `jan-v3.5-4b-q4_k_m` (`tier: strong`, `min_ram_gb` 8, `recommend_ram_gb` 12, Revision 2b0fcab0 gepinnt, sha256 geprüft); angezeigt nur, wo DeviceCheck nicht `TOO_SMALL` ist oder das Paket schon da ist; „Empfohlen für dein Handy“ erst ab 12 GB; Name „Großes Gehirn · für starke Handys“. LFM3 bleibt Standard, keine Migration.
- note: zuerst Underdog Poodle 4B; am 05.11. 08:02 von Oskar durch Jan ersetzt (Poodle-Basismodell seit 05.11. 05:45 MEZ CC BY-NC 4.0).
- check: `BrainChoiceTest`, `CatalogParserTest` (CI)

### T-241 · Qwen3-Chatformat für Jan

- type: `native` · size: `M` · lane: `C` · depends: T-240
- files: core-llm/.../api/BrainFormat.kt, core-llm/.../internal/{QwenPromptBuilder,StreamSplitter,LlamaEngine}.kt, NewsBrain, MemoryExtraction, ChatEngine, prompts/jan-v3.5-4b_chat_template.jinja, tools/render_golden_qwen.py, golden/qwen/Q-01…Q-06
- do: Format beim Laden aus `general.architecture` (`qwen3` → QWEN); Prompt wie das Jinja der Jan-GGUF, einzige Abweichung: Werkzeug-Block vor dem System-Text (fester Teil bleibt cachebar, LLM.md §2b); Tool-Calls als JSON (Parser-Reparaturpfad, Name beim Streamen); Jan denkt nie; Sampling 0,7 / top-p 0,8 / top-k 20.
- check: `QwenPromptBuilderTest` (Goldens), `StreamSplitterTest`, `DevTuningTest` (CI)

### T-242 · Schneller Start mit Snapshot des System-Prompts

- type: `native` · size: `M` · lane: `C` · depends: T-032
- files: core-llm/src/main/cpp/{hain_llm.cpp,hain_session.h}, core-llm/.../internal/{NativeLlm,LlamaEngine}.kt
- do: KV-Zustand bis zum Ende des festen Teils als Datei in `cacheDir/llm-state/` (pro Modell, Kontextgröße, Prompt-Hash), beim nächsten Start laden; Logs: Aufwärmen, wiederverwendete Token, Snapshot-Ladezeit, Zeit bis zum ersten Token.
- check: CI (nativer Build), Gerätetest offen

### T-243 · Host-Test Jan-v3.5-4B

- type: `plan` · size: `S` · lane: `H` · depends: T-241
- do: Hains Prompt (System + Werkzeuge, beide Reihenfolgen) gegen Jan auf dem Sandbox-Host: 6 Werkzeug- + 4 Plauder-Prompts, TTFT kalt und mit Snapshot, Spitzen-RAM. Ergebnis: docs/research/jan-host-test.md.
- check: Bericht

### T-244 · Doku 0.1.10.6 (zuerst 0.1.10.4)

- type: `docs` · size: `S` · lane: `B` · depends: T-240
- do: README („New in“, Feature-Zeile, Pakettabelle), Website EN + /de („Schon da“), AGENTS, MODELS, LLM §2b/§2c, LICENSES, `hain.versionName=0.1.10.6` (nach dem Merge von #55; vorher 0.1.10.4).
- check: `tools/check_strings.py`, CI

## 0.1.10.5 · Rest für 0.2 (`hark/v02-rest`, PR #55)

> Oskar 05.11. 07:33: 0.2.0 erst, wenn alle 0.2-Funktionen drin sind. Dieser Lauf schließt die Punkte aus Oskars
> Liste vom 04.11. 22:18 (Tablet, Teilen, Bugs, Screenshots) – ohne Versionssprung, ohne Release.

### T-250 · Eindeutige Task-IDs

- type: `docs` · size: `XS` · lane: `B`
- files: docs/workflow/TASKS.md, STATUS.md, ROADMAP.md
- do: doppelte IDs umbenannt (T-084p Denk-Stufe im Nachrichtenfeld, T-088p/T-089p aus dem Polish-Branch), Mini-Apps-Wald M12 von T-200–T-226 nach T-300–T-326; HANDOFF_LOG bleibt unverändert (append-only).
- check: jede ID genau einmal in der Übersicht

### T-251 · Tablet: Pakete und Gedächtnis in zwei Spalten

- type: `ui` · size: `S` · lane: `B` · depends: T-235
- files: core-ui/.../layout/HainColumns.kt (neu, `HainGrid`), feature-settings/.../api/PacksScreen.kt, feature-memory/.../api/{MemoryPage,MemoryList}.kt, `TabletMemoryScreens`, `TabletSettingsScreens`
- do: ab 840 dp (EXPANDED) Kacheln in zwei Spalten, Seite max. 1080 dp mittig; Gedächtnis: links die Liste, rechts „Über dich“ + Einstellungen (Tab „Über dich“ entfällt dort). Handys und Tablets hochkant bleiben einspaltig.
- accept: gleiche Kachelhöhe pro Zeile, ungerade letzte Kachel eine Spalte breit
- check: `HainColumnsTest`, `MemoryListTest`, Screens `tablet_packs_*`, `tablet_memory_*` (CI)

### T-252 · Screenshot-Beispiele ohne echte Daten

- type: `docs` · size: `S` · lane: `B` · depends: T-107
- files: feature-chat/.../screens/{ChatScreens,ChatStyleScreens}.kt, feature-memory/.../screens/MemoryScreens.kt, core-ui/.../screens/MenuBlurScreens.kt, README.md, docs/design/screens/*, website/public/screenshots/*
- do: keine echten Personen, Orte oder Adressen in Beispielen; deutsche Shots nutzen dasselbe Paris-Beispiel wie die englischen, Gedächtnis-Beispiele erfunden; Screens neu aufgenommen, Website-WebP neu erzeugt.
- check: Screens (CI)

### T-253 · Teilen aus anderen Apps: Datei in Hain

- type: `feature` · size: `S` · lane: `B` · depends: T-144, T-123
- files: app/src/main/AndroidManifest.xml, app/.../entry/LaunchRequest.kt, app/.../MainActivity.kt, feature-chat/.../api/ChatScreen.kt, `LaunchRequestTest`
- do: Teilen-Ziel für PDF, Bilder und Text-/Markdown-Dateien; neuer Chat, die Datei wird wie über die Büroklammer gelesen (Fortschritt, ggf. „Texterkennung laden?“), eine mitgeschickte Bildunterschrift landet im Eingabefeld. Nur `content://`-Adressen, nie `file://` fremder Apps; nichts wird von selbst gesendet.
- check: `LaunchRequestTest` (CI); Gerätetest offen

### T-254 · Touch-Ziele 48 dp

- type: `fix` · size: `XS` · lane: `B` · depends: T-238
- files: feature-chat/.../api/ThinkingPicker.kt, feature-settings/.../api/PacksScreen.kt
- do: Denk-Stufen-Knopf mindestens 48 × 48 dp (Inhalt mittig), Knöpfe in Modelle & Pakete mindestens 48 dp hoch.
- check: Screens (CI)

### T-255 · Release-Checkliste 0.2.0

- type: `docs` · size: `S` · lane: `B`
- files: docs/workflow/RELEASE_0.2_CHECKLIST.md (neu)
- do: eine Zeile pro Funktion mit Screen (Test → Datei), Status des Shots (da/DE/neu) und Website-Text EN/DE; Plan für README und Website beim 0.2.0-Schritt; Signatur und Tag nur nach Oskars Freigabe.
- check: Status „da“ nur, wenn ein DE- und ein EN-Test existiert

### T-256 · Fix: Leseansicht-Ausrichtung

- type: `fix` · size: `XS` · lane: `B` · depends: T-076
- files: feature-news/.../api/{ReaderScreen,NewsSettingsPage}.kt
- do: `wrapContentWidth` + `widthIn` zentrierte kurze Blöcke (Überschriften, Listenpunkte); danach wieder `fillMaxWidth`, damit alles links beginnt wie die Absätze (auch Frage-Sheet und Nachrichten-Einstellungen).
- check: Screens `news_reader_*` (CI)

### T-257 · Fix: Vorschläge-Tab

- type: `fix` · size: `XS` · lane: `B` · depends: T-115
- files: feature-memory/.../api/{MemoryList,MemoryPage}.kt, `MemoryListTest`
- do: „Vorschläge (1)“ brach im Vier-Tab-Schalter auf dem Handy in zwei Zeilen um → „Vorschläge 1“ mit schmalem geschütztem Leerzeichen, höchstens „9+“.
- check: `MemoryListTest`, Screens `memory_*` (CI)

### T-258 · Fix: gelöschtes Wikipedia-Paket bleibt offen

- type: `fix` · size: `XS` · lane: `B` · depends: T-237
- files: core-tools/.../wiki/zim/ZimWiki.kt, `ZimWikiTest`
- do: offene ZIM-Archive merken sich Größe + Änderungszeit; beim nächsten Nachschlagen werden Archive gelöschter oder ersetzter Pakete geschlossen, so wird der Platz sofort frei und ein neues Paket wird neu geöffnet.
- check: `ZimWikiTest` (CI)

### T-259 · Fix: View-Models lesen nach dem Drehen die alte Activity

- type: `fix` · size: `S` · lane: `B` · depends: T-079
- files: app/.../HostState.kt (neu), app/.../MainActivity.kt, `HostStateTest`
- warum: Chat- und News-View-Model überleben das Drehen, ihre Lambdas lasen aber Felder der alten Activity (`current`, `privateChatOpen`, `resources`), die niemand mehr aktualisierte – News nutzte die alte Sprache, der Chat alte Schalter (Offline-Modus, Websuche) und ein altes „privater Chat“.
- do: `HostState` ist selbst ein ViewModel (gleiche Instanz für jede Activity des Screens) und hält Einstellungen, Privat-Flag und Handy-Sprache; die Activity schreibt hinein, die Lambdas lesen daraus. News-View-Model und Vorlesen fassen die Activity gar nicht mehr an, Standort fürs Wetter über `applicationContext`.
- offen: das Chat-View-Model hält weiter einige Activity-Objekte (DocLibrary, BrainPicking) – Werte stimmen jetzt, größerer Umbau nach 0.2.
- check: `HostStateTest` (CI)

## 0.1.10.7 · Neues Standard-Gehirn, Werkzeuge nur wenn passend, Tempo (`hark/v0110-7`, PR #56)

### T-260 · LFM3-Omni 2B als Standard-Gehirn

- type: `feat` · size: `M` · lane: `B`
- do: Katalog `lfm3-omni-2b-q4_0` (Rev. d508b88, sha256 geprüft, LFM Open License v1.0) als `default`; LFM3 1.7B → `tier: legacy` (nur gelistet, solange installiert, nie empfohlen); Hinweis „Neues holen“ / „Altes entfernen“; `BrainFormat.LFM_OMNI` (kein Denken, fast greedy, keine Wiederholungsstrafe); Omni-Goldens; Extraktion nutzt das gewählte Gehirn.
- check: `BrainChoiceTest`, `CatalogParserTest`, Goldens (CI)

### T-261 · Ehrliche Paket-Beschreibungen

- type: `copy` · size: `XS` · lane: `B`
- do: jede Paket-Zeile sagt in 1–2 Sätzen, was sie bringt und wann sie sich lohnt (Omni, Jan 4B ab 8 GB, Legacy, STT, OCR, Wikipedia). EN + DE.
- check: `tools/check_strings.py`

### T-262 · Gedächtnis nur aus eigenen Aussagen

- type: `fix` · size: `S` · lane: `B`
- do: `SelfFacts`-Muster + nur belegte Fakten des Gehirns; Chip „Merken: wohnt in Köln“; Ein-Wert-Fakten im System-Prompt; leere Labels fallen weg.
- check: Unit-Tests (CI)

### T-263 · Werkzeuge nur, wenn sie passen

- type: `fix` · size: `S` · lane: `B`
- do: `ToolGuard` verwirft Wetter/News (und Wetter-Websuchen), nach denen die Nachricht nicht fragt; einmal Hinweis, dann ohne Werkzeuge; Ort fürs Wetter: Chat → Standort → Gedächtnis.
- check: Unit-Tests; Host-Test Omni: Wetter ohne Anlass 5/10 → 0/10, richtiges Werkzeug 7/10 → 8/10

### T-264 · Tempo

- type: `perf` · size: `S` · lane: `B`
- do: Threads = Performance-Kerne (`CpuCores`, 2…6), `buffer(Channel.UNLIMITED)` im `callbackFlow` (keine verlorenen Tokens), Live-Text höchstens alle 40 ms; LLM.md §2d.
- check: `CpuCoresTest`, `ChatEngineTest` (CI); t/s am Gerät offen

### T-265 · News-Briefing: Fortschritt + „Jetzt erstellen“

- type: `feat` · size: `S` · lane: `B`
- do: Karte zeigt „Feeds laden 3/19 → Artikel lesen → Zusammenfassen“; „Jetzt erstellen“ vor dem ersten Briefing; Neu-Erzeugen lädt zuerst frische Feeds.
- check: `BriefingTest` (CI)

### T-266 · Einstellungen aufgeräumt

- type: `ui` · size: `S` · lane: `B`
- do: Privatsphäre in Gruppen (Online gehen / Sicherheit / Deine Daten), eigene Suche + volle Adressen im Netzwerk-Log in den Entwickler-Bereich, 16 dp Zeilenabstand; Einstellungen → Nachrichten öffnet die News-Einstellungen, Zurück führt zurück.
- check: Screenshots (CI)

### T-267 · Fixes: Wecker-Tage, „15 Minuten früher“, Fett-Prompt

- type: `fix` · size: `S` · lane: `B`
- do: `AlarmHints` übernimmt Wiederhol-Tage und „15 Minuten früher“ (relativ zum letzten Wecker) aus den Worten des Nutzers; Prompt-Zeile zu Fettdruck ohne Beispielwort „fett“.
- offen: #44, wenn das Modell gar kein `set_alarm` ruft.
- check: `AlarmHintsTest` (CI)

### T-268 · Doku + Version 0.1.10.7

- type: `docs` · size: `XS` · lane: `B`
- do: `hain.versionName=0.1.10.7` (versionCode setzt die CI), README, Website EN + /de, LICENSES, MODELS, STATUS, TASKS, HANDOFF_LOG, PR-Text.
- check: `tools/check_strings.py`, CI

## Website 0.2 (`hark/web-0.2`, PR #57, Basis `hark/v0110-7`)

### T-269 · Website 0.2: ehrliche Status-Kennzeichen, Feature-Showcase, Roadmap

- type: `docs` · size: `M` · lane: `B`
- do: Website EN + /de, als wäre 0.2 veröffentlicht: Status „Seit 0.1 / Neu in 0.2 / Geplant“ statt „Schon da / Kommt bald“ (Seit 0.1 = Stand 0.1.3.1), Hero „Version 0.2“ + Download `releases/latest` + Installationshinweis, ein Abschnitt pro Funktion mit Screenshot (19), „Außerdem in 0.2“, „Was noch kommt“ aus KONZEPT.md / ROADMAP.md / RELEASE_0.2_CHECKLIST.md (ohne Termine), Lizenzen GPL-3.0 + LFM Open License v1.0 + Apache-2.0. Neue Screenshot-Tests `WebShowcaseScreens`, `WebSettingsScreens`, `NewsScreens.web*` (DE + EN, hell + dunkel), `website/scripts/web_shots.sh`. README-Feature-Tabelle mit denselben Kennzeichen.
- offen: kein Screenshot für Widget/Schnellkachel/Teilen, Diktat im Aufnahmezustand und privaten Chat (nur Text bzw. Einstellungsseite).
- check: Website-Build, Screens (CI)

## 0.1.10.8 (`hark/v0110-7b`, Basis main 0a44391)

### T-270 · LFM3-1.7B wieder Standard, Omni optional

- type: `feat` · size: `S` · lane: `A`
- do: LFM3 1.7B (QAD Q4_0) ist wieder das Standard-Gehirn; LFM3-Omni 2B wird `tier: optional` (unter Modelle & Pakete und im Gehirn-Menü immer angeboten, nie empfohlen oder selbst gewählt). Upgrade-Banner und „Legacy“-Kennzeichen entfernt, ehrlicher Pakettext.
- check: `BrainChoiceTest`, `CatalogParserTest`, CI

### T-271 · Werkzeug-Leaks im Antworttext

- type: `fix` · size: `M` · lane: `A`
- do: `RawToolText` erkennt rohe Werkzeugaufrufe im Fließtext (JSON, `<|tool_call|>`, Funktionsschreibweise) und entfernt sie; `AnswerParts.fenceLooseCards` – Karten nur im ```card-Block; `RememberTool.allowed` – Merken nur bei eigener Aussage oder ausdrücklicher Bitte; Nachfrage statt leerer Antwort bei verworfenem/leerem Aufruf; Merk-Regel im System-Prompt nur bei Bitte; SelfFacts „ich lerne …“; Rechner verträgt Suffix „um/bitte“.
- check: `RawToolTextTest`, `RawToolTextEngineTest`, `AnswerPartsTest`, `RememberToolTest`, `SelfFactsTest`, `CalculatorTest`, CI

### T-272 · Chat-UI: Composer-Unterkante, Aktionszeile, Versionszähler, Werkzeugschritte

- type: `fix` · size: `S` · lane: `A`
- do: Seitenfarbe blendet unter dem Eingabefeld bis zum Rand ein (kein Chat mehr unter der Navigationsleiste); Aktionszeile unter Antworten immer einzeilig (`ActionRowFit`: nur Icon-Knöpfe werden schmaler, min. 34 dp), Pfeile des Versionszählers 40 dp; „2/3“ ohne Überblendung; Werkzeugschritte einzeilig mit „…“.
- check: `ActionRowFitTest`, Roborazzi (Aktionszeile 360 dp, Composer-Unterkante, Werkzeugschritt), CI

### T-273 · Denk-Auswahl ehrlich je Gehirn

- type: `fix` · size: `XS` · lane: `A`
- do: `ChatUiState.brainThinks` aus `BrainFormat.mayThink`; ThinkingPicker zeigt bei Gehirnen ohne lautes Denken (LFM3-Omni) Untertitel, die nur den Recherche-Unterschied nennen (EN + DE).
- check: `tools/check_strings.py`, CI

### T-274 · Quellen-Design: Chips im Text, kompakte Quellenzeile

- type: `feat` · size: `M` · lane: `A`
- do: Keine „[1]“ mehr im Fließtext: `CitationMarks.toLinks` macht aus gültigen Markern Links, `AssistantBlock` zeichnet sie als kleine hochgestellte runde Chips (Hain-Grün `accentSoft`/`accent`, Zahl), Tipp öffnet die Quelle (TalkBack: „Quelle 1: Titel“). Quellen unter der Antwort als eine Zeile mit übereinanderliegenden Favicons + „3 Quellen“, ausklappbar zur Liste (Nummer, Icon, Titel, Seite). Kopieren/Teilen/Vorlesen ohne Marker (`CitationMarks.strip`). EN + DE.
- check: `CitationMarksTest`, Roborazzi `SourceRowScreens` (Chips hell/dunkel 360 dp, Liste offen), CI

### T-275 · Recherche-Prompts ohne Meta-Rede

- type: `fix` · size: `XS` · lane: `A`
- do: `THOROUGH_*`/`FINAL_*` (DE + EN): Frage direkt beantworten, Wichtigstes zuerst, nur Fakten aus den Ergebnissen mit [1], keine Rede über Suche/Quellen/Widersprüche; fehlt etwas, kurz sagen was.
- check: `ResearchPromptTest`, `ResearchLoopTest`, CI

### T-276 · Doku + Version 0.1.10.8

- type: `docs` · size: `XS` · lane: `B`
- do: `hain.versionName=0.1.10.8`, README (Tabelle, „Neu in 0.1.10.8“, Modell-Tabelle), Website EN + /de (Gehirn-Abschnitt, Lizenzen), LICENSES, MODELS, STATUS, TASKS, HANDOFF_LOG, PR-Text.
- check: `tools/check_strings.py`, Website-Build, CI
