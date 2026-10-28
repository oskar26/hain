# Source for docs/workflow/TASKS.md. Planner edits this, then: python3 tools/tasks_src.py > /tmp/t.md && mv /tmp/t.md docs/workflow/TASKS.md  (keeps existing statuses)
# (Implementers edit TASKS.md status lines directly; planner re-syncs.)
T=[]
def t(id,title,type,size,lane,depends,read,files,do,accept,check):
    T.append(locals())
G="./gradlew assembleDebug"
U=lambda m,f="": f"./gradlew :{m}:testDebugUnitTest"+(f' --tests "*{f}*"' if f else "")

M={}
def ms(k,name): M[len(T)]=(k,name)

ms("M0","Device check & setup (human)")
t("T-000","Benchmark LFM3 on own phone","plan","S","H","-","LLM.md §1, MODELS.md",
  "docs/workflow/STATUS.md (results table)",
  ["Install Termux or use llama.cpp Android build; download LFM3-1.7B Q4_K_M and LFM3-Omni-2B GGUF.",
   "Run `llama-bench -p 512 -n 128` for both; note prefill/decode tok/s and RAM.",
   "Ask 10 German questions from KONZEPT.md tone; note quality 1–5."],
  ["numbers in STATUS.md 'Benchmarks'"],"manual")
t("T-001","Create Git repo, add llama.cpp submodule","scaffold","XS","A","-","AGENTS.md §6-7",
  ".gitmodules, third_party/llama.cpp, LICENSE (GPL-3.0 text)",
  ["`git init`, add GPL-3.0 LICENSE file.",
   "`git submodule add https://github.com/ggml-org/llama.cpp third_party/llama.cpp`, checkout latest release tag that includes LFM3 text support.",
   "Write the commit hash into STATUS.md 'Pinned versions'."],
  ["submodule present, hash recorded"],"git submodule status")

ms("M1","Project skeleton")
t("T-002","Gradle multi-module skeleton","scaffold","M","A","T-001","ARCHITECTURE.md §1-2",
  "settings.gradle.kts, build.gradle.kts, gradle/libs.versions.toml, every module's build.gradle.kts + empty package dirs",
  ["Create all modules from ARCHITECTURE.md §1 with namespace `app.hain.<module>`.",
   "Use latest stable AGP/Kotlin/Compose BOM; versions only in libs.versions.toml.",
   "minSdk 29, targetSdk 36, abiFilters arm64-v8a, Java 17.",
   "Do NOT add material3 dependency."],
  ["all modules sync and build"],G)
t("T-003","Lint & static checks","scaffold","XS","A","T-002","AGENTS.md §3",
  "build.gradle.kts, config/detekt.yml, .editorconfig",
  ["Add ktlint gradle plugin and detekt with default config, Compose rules allowed (function naming)."],
  ["`./gradlew ktlintCheck detekt` passes on empty project"],"./gradlew ktlintCheck detekt")
t("T-004","Hilt + App class + MainActivity","scaffold","S","A","T-002","ARCHITECTURE.md",
  "app/src/main/AndroidManifest.xml, app/.../HainApp.kt, MainActivity.kt",
  ["@HiltAndroidApp HainApp, single-activity, edge-to-edge, setContent { HainTheme { Text placeholder } }."],
  ["app starts on device/emulator"],G)
t("T-005","Strings DE/EN + checker script","strings","S","A","T-002","I18N.md",
  "app/src/main/res/values/strings.xml, values-de/strings.xml, res/xml/locales_config.xml, tools/check_strings.py",
  ["Add app_name and 10 base strings (send, stop, new_chat, settings, copy, share, retry, cancel, ok, delete) in both languages.",
   "check_strings.py: exits 1 if key sets differ across all modules' values/ and values-de/."],
  ["script passes"],"python3 tools/check_strings.py")
t("T-006","CI workflow","scaffold","XS","A","T-003,T-005","TESTING.md",".github/workflows/ci.yml",
  ["GitHub Actions: checkout with submodules, JDK 17, NDK, run assemble, unit tests, ktlint, detekt, check_strings."],
  ["workflow file valid (actionlint if available)"],"cat .github/workflows/ci.yml")

ms("M2","Design system & Pim")
t("T-010","Design tokens","ui","S","B","T-002","DESIGN_SYSTEM.md §2",
  "core-ui/src/main/kotlin/app/hain/ui/theme/{HainColors,HainType,HainShapes,HainSpacing,HainTheme}.kt, core-ui/src/main/res/font/*",
  ["Exact hex values from §2 for light and dark.","CompositionLocals for tokens; HainTheme(darkTheme) provides them.","Bundle Inter, Atkinson Hyperlegible, JetBrains Mono (OFL) font files."],
  ["previews light+dark compile"],U("core-ui"))
for i,(n,c) in enumerate([("HainText, HainSurface, HainButton, HainIconButton","basic"),("HainTextField, Composer","input"),
    ("UserBubble, AssistantBlock (markdown via renderer core), ReasoningBlock","chat"),("HainChip, ToolChip, HainCard, SegmentedControl","chips"),
    ("SettingsRow, HainSwitch, TopBar, Drawer, BottomSheet, Toast, EmptyState, ProgressBar","layout")]):
    t(f"T-01{i+1}",f"Components: {n}","ui","S" if i!=2 else "M","B","T-018,T-019","DESIGN_SYSTEM.md §3, §6; UI_LIBRARIES.md",
      f"core-ui/src/main/kotlin/app/hain/ui/components/<one file per component>.kt",
      [f"Start by copying the closest Composables UI component (MIT, keep header, add NOTICE line) and restyle; build {n} on foundation + Compose Unstyled primitives where one exists (sheet, switch, text field, drawer, tooltip); our styling, HainShapes, HainMotion; each has @Preview light+dark.","Composer: send/stop morph, glass over content (hainGlass). AssistantBlock: streaming markdown via rememberStreamingMarkdownState + WordFadeIn. ReasoningBlock: shimmer title while thinking, 'Gedacht für N s' after.","No material3 imports (grep must find none)."],
      ["previews compile","`grep -r material3 core-ui/src` returns nothing"],U("core-ui")+" && ! grep -r material3 core-ui/src")
t("T-018","Add UI libraries + thin wrappers","scaffold","S","B","T-010","UI_LIBRARIES.md §2; LICENSES.md",
  "gradle/libs.versions.toml, core-ui/build.gradle.kts, core-ui/.../shape/HainShapes.kt, core-ui/.../effects/{HainGlass,HainShimmer}.kt",
  ["Add Roborazzi screenshot testing (record/verify tasks), Showkase (debugImplementation only), konfetti-compose. Add Compose Unstyled modules (bottom-sheet, modal-bottom-sheet, dialog, dropdown-menu, tooltip, toggle-switch, slider, tab-group, text-field, scrollbars, focus-ring), Haze + haze-blur, Kyant0 capsule + backdrop, compose-shimmer, markdown renderer core + -code, Coil 3, Lottie Compose. Latest stable versions.",
   "HainShapes uses ContinuousRoundedRectangle/ContinuousCapsule.","`Modifier.hainGlass(state)` = Haze blur on API 31+, solid surface fallback below; `Modifier.hainShimmer()`.","No material3 in the dependency tree of core-ui."],
  ["builds","`./gradlew :core-ui:dependencies | grep material3` finds nothing"],G+" && ! ./gradlew -q :core-ui:dependencies | grep -q material3")
t("T-019","Motion system HainMotion + MotionGovernor","ui","S","B","T-018","UI_LIBRARIES.md §3",
  "core-ui/.../motion/{HainMotion,MotionGovernor,WordFadeIn,Haptics}.kt + test",
  ["Spring/tween constants from §3 table.","`LocalReduceMotion`; all helpers return snap() when true.","WordFadeIn: animates only newly appended words (max 30 in flight).","MotionGovernor: frame-time watcher (Choreographer) disables blur after 2 slow frames."],
  ["unit test: reduce motion returns snap; previews"],U("core-ui"))
t("T-016","Pim v2 drawn in Compose (static states)","ui","S","B","T-010","DESIGN_SYSTEM.md §4",
  "core-ui/.../pim/Pim.kt, core-ui/.../pim/PimGeometry.kt",
  ["Port geometry from assets/mascot/v2/motion/pim_motion.json (`geometry`) to Canvas paths (body, stem, leaf+vein, eyes pill/wide/happy/closed/squeeze, fx); `PimPose` data class = rig params of DESIGN_SYSTEM §4; padding like viewBox -12 -18 144 144.",
   "`Pim(state: PimState, size: Dp, modifier)`; all states from DESIGN_SYSTEM.md §4 as static drawings; legacy PNG never used."],
  ["preview shows 4 poses"],U("core-ui"))
t("T-017","Lucide icons as vector drawables","ui","S","B","T-010","DESIGN_SYSTEM.md §2",
  "core-ui/src/main/res/drawable/ic_*.xml, core-ui/.../HainIcons.kt",
  ["Convert needed Lucide SVGs (menu, plus, mic, send, square(stop), copy, volume-2, refresh-cw, share-2, settings, search, newspaper, brain, image, file-text, wifi-off, check, x, chevron-right, trash-2, download, globe) with vd-tool or Android Studio converter."],
  ["all icons render in a preview grid"],G)

ms("M3","LLM core (native + prompt)")
t("T-020","Encrypted Room DB skeleton","feature","M","C","T-002","DATA.md",
  "core-data/src/main/kotlin/app/hain/data/{HainDatabase,entities/*,dao/*}.kt",
  ["Entities chat, message (+FTS5), chat_summary only (others later).","SQLCipher via net.zetetic:sqlcipher-android + SupportOpenHelperFactory; key from KeyProvider (Keystore-wrapped).",
   "Decide sqlite build with FTS5+sqlite-vec; record decision in STATUS.md."],
  ["DAO tests insert/query/FTS search pass (Robolectric)"],U("core-data"))
t("T-030","JNI bridge: load/generate/cancel","native","M","C","T-001,T-002","LLM.md §1; reference: SmolChat-Android smollm module (Apache-2.0, see UI_LIBRARIES.md §2b)",
  "core-llm/src/main/cpp/{CMakeLists.txt,hain_llm.cpp}, core-llm/build.gradle.kts, core-llm/.../internal/NativeLlm.kt",
  ["Implement JNI table from LLM.md §1 incl. UTF-8 buffering and cancel flag.","16 KB page alignment flag.","Prefix KV reuse can be a follow-up (T-033)."],
  ["instrumented test loads tiny GGUF and generates 8 tokens"],"./gradlew :core-llm:connectedDebugAndroidTest (device)")
t("T-031","Copy prompts into assets","scaffold","XS","C","T-002","prompts/README.md",
  "core-llm/build.gradle.kts",
  ["Gradle Copy task from /prompts/*.txt to core-llm/src/main/assets/prompts/ before preBuild (generated dir, not committed)."],
  ["APK contains assets/prompts/system_full.en.txt"],G+" && unzip -l app/build/outputs/apk/debug/app-debug.apk | grep system_full")
t("T-032","LlmEngine implementation","feature","S","C","T-030","ARCHITECTURE.md §3,§5; LLM.md §6",
  "core-llm/.../api/LlmEngine.kt, internal/LlamaEngine.kt, di/LlmModule.kt, core-testing/.../FakeLlmEngine.kt",
  ["Single-thread dispatcher; callbackFlow for tokens; state flow; SamplingParams presets from LLM.md §6.","FakeLlmEngine emits scripted tokens with delay."],
  ["unit test with FakeLlmEngine-based consumer passes"],U("core-llm"))
t("T-033","Prefix KV cache reuse","native","S","C","T-030","LLM.md §1",
  "core-llm/src/main/cpp/hain_llm.cpp",["Keep token list of KV; on new prompt find common prefix, llama_memory_seq_rm the rest, decode only suffix."],
  ["2nd turn prefill time < 30 % of first (log)"],"manual on device")
t("T-040","Data classes ChatMessage/ToolSchema/ToolCall","feature","XS","D","T-002","ARCHITECTURE.md §3",
  "core-llm/.../api/{ChatMessage,ToolSchema,ToolCall,ParsedOutput}.kt",["Exactly as in ARCHITECTURE.md; ToolSchema holds name, description, parameters JsonObject."],["compiles"],G)
t("T-041","Golden prompt files + render script","test","S","D","T-040","LLM.md §2",
  "tools/render_golden.py, core-llm/src/test/resources/golden/G-01..G-06*.txt",
  ["Python: jinja2 render prompts/lfm3-1.7b_chat_template.jinja with 6 scenarios (described in LLM.md §2), bos_token='<|startoftext|>'.",
   "Write outputs + scenario JSON inputs (G-xx.json) next to them."],["6 txt + 6 json files"],"python3 tools/render_golden.py && ls core-llm/src/test/resources/golden")
t("T-042","PromptBuilder (pure Kotlin)","feature","M","D","T-041","LLM.md §2",
  "core-llm/.../internal/LfmPromptBuilder.kt, test/.../LfmPromptBuilderTest.kt",
  ["Implement rules 1–8; test loads each G-xx.json, builds, compares to G-xx.txt byte-for-byte."],["all 6 golden tests pass"],U("core-llm","LfmPromptBuilder"))
t("T-043","ToolCallParser","feature","S","D","T-040","LLM.md §3, §5",
  "core-llm/.../internal/XmlToolCallParser.kt, test/.../XmlToolCallParserTest.kt",["Implement; 8 tests from §5 table."],["8 tests pass"],U("core-llm","XmlToolCallParser"))
t("T-044","StreamSplitter","feature","S","D","T-043","LLM.md §4",
  "core-llm/.../internal/StreamSplitter.kt, test/.../StreamSplitterTest.kt",
  ["State machine; tests feed outputs char-by-char and in random chunks (seeded) and compare to expected deltas."],["tests pass"],U("core-llm","StreamSplitter"))
t("T-045","ThinkingPolicy + SystemPrompt.fill","feature","S","D","T-040","LLM.md §7, §9; I18N.md",
  "core-llm/.../internal/{ThinkingPolicy,SystemPromptFiller}.kt + tests",["As specified; tests DE+EN dates, missing values removed, emergency numbers."],["tests pass"],U("core-llm"))

ms("M4","Chat MVP (first usable app)")
t("T-050","ChatEngine turn loop","feature","M","E","T-032,T-042,T-044,T-045,T-020","ARCHITECTURE.md §4; LLM.md §8",
  "feature-chat/.../internal/engine/ChatEngine.kt + test",
  ["Steps 1–6 of §4 with ToolRouter and MemoryStore as interfaces (use empty fakes for now).","Max 3 tool rounds, timeout handling."],
  ["test with FakeLlmEngine scripted tool call → fake tool → final answer"],U("feature-chat","ChatEngine"))
t("T-051","ChatViewModel + UI state","feature","S","E","T-050","ARCHITECTURE.md",
  "feature-chat/.../ChatViewModel.kt, ChatUiState.kt + test",["Send, stop, regenerate, edit last user message, streaming state."],["VM tests with fakes pass"],U("feature-chat"))
t("T-052","Chat screen","ui","M","E","T-051,T-011,T-012,T-013","DESIGN_SYSTEM.md §5",
  "feature-chat/.../ui/ChatScreen.kt",["Messages list (LazyColumn, auto-scroll unless user scrolled up), composer, reasoning block, empty state with Pim + 4 chips."],
  ["Compose UI test: type, send, see fake answer"],U("feature-chat"))
t("T-057","Branches, message queue, stopped/error states","feature","M","E","T-051","UI_LIBRARIES.md §1",
  "feature-chat/.../ui/{BranchPicker,QueuedMessage,MessageStates}.kt, ChatViewModel.kt + test",
  ["Edit/regenerate creates sibling via parent_id; BranchPicker '2 / 3' switches.","Send while generating → queued chip, auto-send after Done.","Stopped label + Weiter; error card with retry."],["VM tests for branch switch and queue"],U("feature-chat"))
t("T-058","ChatViewport + scroll-to-bottom pill","ui","S","E","T-052","UI_LIBRARIES.md §1",
  "feature-chat/.../ui/{ChatViewport,ScrollToBottomPill}.kt",
  ["Stick to bottom while streaming unless user scrolled up > 48 dp; pill appears with fade/slide; tap animates scroll.","animateItem() for inserts; shared element composer→user bubble (SharedTransitionLayout)."],["Compose UI test for stick/unstick"],U("feature-chat"))
t("T-059","ToolTimeline + SourceChips","ui","S","E","T-052,T-062","UI_LIBRARIES.md §1, §3",
  "feature-chat/.../ui/{ToolTimeline,SourceChips}.kt",["Chip per tool call: running (shimmer) → done (✓ bounce) / failed; orange dot if online; sources as chips with favicon (Coil, cached)."],["previews + UI test"],U("feature-chat"))
t("T-053","Drawer: chat list, search, rename, delete, pin","ui","M","E","T-052,T-015","DESIGN_SYSTEM.md §5",
  "feature-chat/.../ui/ChatListDrawer.kt, ChatListViewModel.kt",["Groups Today/Yesterday/7 days/Older; FTS search over messages.","Open animation: chat scales to 0.94 with continuous corners + dim + drawer parallax (UI_LIBRARIES.md §3)."],["UI test"],U("feature-chat"))
t("T-054","Export & share chat","feature","S","E","T-053","DATA.md",
  "feature-chat/.../export/ChatExporter.kt + test",["Markdown and JSON export via SAF; share sheet with plain text."],["exporter unit tests"],U("feature-chat","ChatExporter"))
t("T-055","Model picker in top bar","ui","S","E","T-052,T-081","MODELS.md",
  "feature-chat/.../ui/ModelPicker.kt",["Lists installed brains; switch = unload/load with progress toast."],["UI test with fake catalog"],U("feature-chat"))
t("T-056","Private chat + app lock","feature","S","E","T-053","DATA.md; SETTINGS.md",
  "feature-chat/..., app/.../lock/AppLock.kt",["Private chat flag (no memory, not listed after close if setting).","BiometricPrompt app lock on resume."],["manual + VM test"],G)

ms("M5","Models & onboarding")
t("T-080","Fill model catalogue with real URLs + sha256","plan","S","F","-","MODELS.md",
  "core-models/src/main/assets/catalog.json",["Human/strong model: copy exact HF file URLs and sha256 from model pages. Never invent."],["every default entry has url+sha256"],"python3 -m json.tool core-models/src/main/assets/catalog.json")
t("T-081","Catalog + DeviceCheck","feature","S","F","T-002","MODELS.md",
  "core-models/.../{ModelCatalogImpl,DeviceCheckImpl}.kt + tests",["Parse catalog.json; DeviceCheck rules as unit-tested pure function on DeviceProfile."],["tests pass"],U("core-models"))
t("T-082","PackDownloader (WorkManager)","feature","M","F","T-081","MODELS.md",
  "core-models/.../download/*.kt + test (MockWebServer)",["Range resume, sha256 verify, atomic rename, foreground notification, Wi-Fi constraint."],["resume + checksum tests pass"],U("core-models"))
t("T-083","Onboarding (welcome, language, download brain)","ui","M","F","T-082,T-016","DESIGN_SYSTEM.md §5",
  "feature-onboarding/...",["3 steps with Pim; shows size, Wi-Fi hint; continues to chat when brain installed."],["UI test with fake downloader"],U("feature-onboarding"))
t("T-084","Packs screen in settings","ui","S","F","T-082","MODELS.md",
  "feature-settings/.../PacksScreen.kt",["List packs, size, install/delete, storage bar, custom GGUF import (SAF)."],["UI test"],U("feature-settings"))

ms("M6","Tools & internet")
t("T-060","NetworkGate + network log","feature","S","A","T-020","TOOLS.md §1",
  "core-tools/.../net/{NetworkGateImpl,NetworkLogDao}.kt + test",["Offline check, purpose switches, log entry, UA rules."],["tests with MockWebServer"],U("core-tools","NetworkGate"))
t("T-061","Pim launcher icon + VectorDrawable export","ui","S","B","T-016","DESIGN_SYSTEM.md §4",
  "core-ui/src/main/res/drawable/pim_*.xml",["Export idle Pim as adaptive launcher icon + notification icon (monochrome eyes+leaf) from assets/mascot/v2/static/pim_idle.svg (no vein, no fx)."],["previews"],G)
t("T-062","ToolRouter + schemas","feature","S","A","T-040,T-060","TOOLS.md §2, §6-7",
  "core-tools/.../ToolRouterImpl.kt, schemas/*.json + test",["Registry, enabled set from settings, error mapping, 12 s timeout."],["tests"],U("core-tools","ToolRouter"))
t("T-063","Calculator + units","feature","S","A","T-062","TOOLS.md §5",
  "core-tools/.../calc/*.kt + tests",["Parser, decimal comma, units table; ≥ 30 test cases."],["tests pass"],U("core-tools","Calc"))
t("T-064","Weather tool + WeatherCard","feature","M","A","T-062,T-014","TOOLS.md §4; CARDS.md §1",
  "core-tools/.../weather/*.kt, core-cards/.../WeatherCard.kt + tests (fixtures)",["Geocode + forecast, WMO codes DE/EN."],["tests pass"],U("core-tools","Weather"))
t("T-065","Wikipedia tool + WikiCard","feature","S","A","T-062","TOOLS.md §2",
  "core-tools/.../wiki/*.kt + tests",["opensearch → summary REST, lang de/en, UA header."],["fixture tests"],U("core-tools","Wiki"))
t("T-066","SearchRouter: SearXNG + DDG html + fallback","feature","M","A","T-062","TOOLS.md §3",
  "core-tools/.../search/*.kt + tests with saved HTML/JSON fixtures",["Classifier, order, back-off, uddg decoding."],["tests pass"],U("core-tools","Search"))
t("T-067","read_page with Readability4K","feature","S","A","T-066","TOOLS.md §3",
  "core-tools/.../search/PageReader.kt + test",["Fetch, extract, trim by tokenCount (inject a TokenCounter interface)."],["tests"],U("core-tools","PageReader"))
t("T-068","Intent tools (alarm, timer, calendar, navigate) + ConfirmCard","feature","M","A","T-062,T-014","TOOLS.md §2; CARDS.md §1",
  "core-tools/.../intents/*.kt, core-cards/.../ConfirmCard.kt + tests",["Build intents (unit test the Intent extras), card with confirm button, undo when auto."],["tests"],U("core-tools","Intent"))
t("T-069","Online indicator & network log screen","ui","S","A","T-060","DESIGN_SYSTEM.md; SETTINGS.md",
  "feature-settings/.../NetworkLogScreen.kt, ToolChip orange dot",["List from network_log, filter by purpose, clear."],["UI test"],U("feature-settings"))

ms("M7","News (ADR-14)")
t("T-070","Feed parser RSS2/Atom/RDF","feature","S","B","T-002","NEWS.md §3",
  "core-tools/.../news/FeedParser.kt + fixtures for all 14 standard feeds",["Save one real response per standard feed as fixture (human/strong model fetches), parse all."],["14 fixture tests pass"],U("core-tools","FeedParser"))
t("T-071","Standard feed list + repository","feature","S","B","T-070,T-060","NEWS.md §2-3",
  "core-tools/src/main/assets/news/standard_feeds.json, news/NewsRepository.kt + test",["Modes, conditional GET, dedup, 7-day retention, Correctiv filter, health status."],["tests"],U("core-tools","NewsRepository"))
t("T-072","news tool + NewsCard","feature","S","B","T-071,T-062","NEWS.md §4",
  "core-tools/.../news/NewsTool.kt, core-cards/.../NewsCard.kt + test",["FTS query, 8 items, forModel format."],["tests"],U("core-tools","NewsTool"))
t("T-073","News screen (3-mode switch, own feeds)","ui","M","B","T-072","NEWS.md §1, §6; DESIGN_SYSTEM.md",
  "feature-news/...",["SegmentedControl with exactly 3 modes; own feed add/rename/delete; no per-source toggles, no labels."],["UI test asserts no toggle on standard feeds"],U("feature-news"))
t("T-074","Article summary on demand","feature","S","B","T-073,T-067","NEWS.md §5",
  "feature-news/.../SummarizeArticle.kt",["Fetch → readability → LLM 5 bullets, not stored; paywall message."],["VM test with fakes"],U("feature-news"))

ms("M8","Memory")
t("T-110","Embedder (LFM2.5-Embedding via llama.cpp)","native","S","C","T-030","MEMORY.md §3",
  "core-llm/src/main/cpp/hain_llm.cpp (embedding fn), core-memory/.../Embedder.kt",["Embedding mode context (separate small context, can coexist with brain? measure RAM; else load on demand). Record dimension in DATA.md."],["instrumented test returns vector"],"device")
t("T-111","Memory tables + DAO","feature","S","C","T-020","DATA.md; MEMORY.md",
  "core-data/... memory entities, sqlite-vec table, migration + test",["Add memory_profile/topic/fact/fts/vec; migration test."],["tests"],U("core-data"))
t("T-112","Explicit remember/forget + remember tool","feature","S","C","T-111,T-062","MEMORY.md §2",
  "core-memory/.../ExplicitMemory.kt + test, prompts/extract_facts.txt",["Regex DE/EN, chip event; write extract_facts.txt prompt (short, JSON only)."],["tests"],U("core-memory"))
t("T-113","Retrieval & ranking","feature","S","C","T-111","MEMORY.md §3",
  "core-memory/.../MemoryStoreImpl.kt, Ranking.kt + test",["Hybrid score formula, thresholds, budget trim, output block."],["ranking tests"],U("core-memory","Ranking"))
t("T-114","Background extraction worker","feature","M","C","T-112,T-113,T-110","MEMORY.md §2.3",
  "core-memory/.../MemoryExtractionWorker.kt + test with fake LLM",["Grammar JSON, cosine thresholds, labels, suggested for sensitive."],["tests"],U("core-memory"))
t("T-115","Memory screen","ui","M","C","T-113","MEMORY.md §5",
  "feature-memory/...",["Tabs, edit, delete everywhere, export/import JSON."],["UI test"],U("feature-memory"))

ms("M9","Documents & OCR")
t("T-120","PDF text layer + page render","feature","S","D","T-002","DOCS_OCR.md",
  "core-docs/.../pdf/*.kt + test pdf fixtures",["pdfbox-android text, PdfRenderer 300 dpi bitmap."],["tests"],U("core-docs"))
t("T-121","PP-OCRv5 Latin via ONNX Runtime","native","M","D","T-120","DOCS_OCR.md",
  "core-docs/.../ocr/*.kt",["Decide RapidOCR-Android vs own ORT pipeline (det/cls/rec, DB postprocess, CTC decode); record in STATUS.md."],["instrumented test reads fixture image with ≥ 95 % chars"],"device")
t("T-122","GermanOcrFixer","feature","S","D","T-002","DOCS_OCR.md rules",
  "core-docs/.../fix/GermanOcrFixer.kt, assets/dict/de_words.txt + test",["Rules 1–5; dictionary from a free list (licence noted in LICENSES.md)."],["all examples pass"],U("core-docs","GermanOcrFixer"))
t("T-123","Chunking + doc search tool + attach in chat","feature","M","D","T-121,T-122,T-062","DOCS_OCR.md",
  "core-docs/.../DocStore.kt, core-tools/.../SearchDocumentsTool.kt, feature-chat attach UI",["Small docs inline, large docs via tool; page citations."],["tests"],U("core-docs"))

ms("M10","Cards, voice, assistant")
t("T-130","Card parser","feature","S","E","T-002","CARDS.md §2",
  "core-cards/.../CardParser.kt + tests",["Line parser, streaming-tolerant, fallback."],["≥ 15 tests"],U("core-cards"))
t("T-131","Card renderer + GBNF","ui","M","E","T-130,T-014","CARDS.md §3-4",
  "core-cards/.../CardRenderer.kt, assets/card.gbnf",["Renderer with core-ui; GBNF grammar file (validate with llama.cpp `llama-gbnf-validator` on PC)."],["previews, validator ok"],U("core-cards"))
t("T-140","STT Moonshine + mic button","feature","M","F","T-082","VOICE.md",
  "core-voice/.../stt/*.kt, feature-chat composer mic",["sherpa-onnx streaming, VAD, permission sheet."],["device test"],"device")
t("T-141","TTS Piper + read aloud","feature","M","F","T-082","VOICE.md",
  "core-voice/.../tts/*.kt",["Text prep (unit-tested), sentence pipelining, speed. Pim voice per VOICE.md: Pocket TTS voice anna +2 st, speed 1.15 in every language, English pack always installed, setup language picker downloads the chosen pack, system TTS fallback by DeviceCheck RTF, pitch via AudioTrack PlaybackParams, setting Pim/Neutral, amplitude envelope flow for Pim speaking motion; compare with assets/voice/samples."],["text prep tests + device"],U("core-voice"))
t("T-143","Pocket TTS on Android (ONNX, all language packs)","native","M","F","T-082","VOICE.md (Pim voice)",
  "tools/voice/export_kerstin_onnx.py, docs/specs/VOICE.md",["Export Pocket TTS (en/de/fr/es/it/pt/nl, voice anna embedding) to ONNX fp16/int8 (reference: PocketTTS-Android-Engine), streaming inference in :core:voice, measure size + RTF on T-000 devices; URLs/SHA-256 only via T-080 on the T-000 devices, compare with assets/voice/samples; write URL+SHA-256 only after a human verifies (T-080)."],
  ["ONNX output audibly matches reference samples; RTF table in VOICE.md"],"device")
t("T-142","Voice mode screen","ui","M","F","T-140,T-141,T-016","VOICE.md; DESIGN_SYSTEM.md",
  "feature-chat/.../voice/VoiceModeScreen.kt",["States, ring from RMS, barge-in."],["device"],"device")
t("T-143","Default assistant + screen context","feature","M","F","T-052","KONZEPT.md §13",
  "app/.../assist/*",["VoiceInteractionService, ROLE_ASSISTANT request, Assist API text+screenshot (setting gated)."],["device"],"device")
t("T-144","Widget + quick settings tile + share target","feature","S","F","T-052","KONZEPT.md §5",
  "app/.../widget/*, tile/*, manifest",["Glance widget 'Frag Hain', TileService opens voice mode, ACTION_SEND text/image/pdf into new chat."],["device"],"device")

ms("M11","Settings, images, polish")
t("T-100","Settings DataStore + screens","feature","M","B","T-015","SETTINGS.md",
  "core-data/.../settings/*, feature-settings/...",["All keys/defaults from SETTINGS.md; level 1 + Advanced; news only 3 modes."],["defaults unit test"],U("feature-settings"))
t("T-101","View/edit system prompt, custom instructions","ui","S","B","T-100,T-045","SETTINGS.md; LLM.md §9",
  "feature-settings/.../PromptScreen.kt",["Show filled prompt preview, edit custom, reset."],["UI test"],U("feature-settings"))
t("T-160","Image edit kit","feature","M","D","T-082","IMAGE.md",
  "feature-image/...",["Select, erase, cut-out, upscale, undo, save. Licence rows first."],["device"],"device")
t("T-090","German/English eval set + runner","test","M","H","T-042,T-062","TESTING.md",
  "eval/de_en_questions.jsonl, tools/eval_runner.py",["100 items as specified; runner against llama-server; report markdown."],["runner prints pass rate"],"python3 tools/eval_runner.py --dry-run")
t("T-150","Multimodal: Omni/VL image input via mtmd","native","M","C","T-030","LLM.md §10",
  "core-llm/src/main/cpp/*, core-llm/.../MultimodalEngine.kt",["Use llama.cpp mtmd API with mmproj; image attach in chat."],["device"],"device")
t("T-163","Pim animated (Compose: blink, breathe, squash, eye tracking)","ui","M","B","T-061,T-019","UI_LIBRARIES.md §2; DESIGN_SYSTEM.md §4; STATUS Q-04",
  "core-ui/.../pim/PimAnimated.kt, core-ui/src/main/res/raw/pim.riv (if Rive)",
  ["Copy pim_motion.json to core-ui assets; `PimMotionPlayer` samples keyframes with the 6 easings (port `sample()`/`ease()` 1:1, unit-test against values printed by tools/pim_gen.py); base-state + micro-action layering; `PimIdleDirector` from idleSchedule; event → motion map from DESIGN_SYSTEM §4; RMS inputs for listening/speaking; tap → tap_squish + haptic; reduce_motion → blink only. Rive only if Q-04 decides so later."],
  ["unit tests: sample() matches python at 5 timestamps per motion; screenshot frames at 25/50/75 % of hop, shake, sleeping match preview GIF frames visually"],U("core-ui"))
t("T-170","Image generation pack (SD1.5+DMD2, MNN)","native","M","D","T-160","IMAGE.md",
  "feature-image/.../gen/*",["Only ≥ 8 GB RAM; fixed safety filter."],["device"],"device")
t("T-190","Accessibility & performance pass","fix","M","H","T-052,T-100","DESIGN_SYSTEM.md",
  "various (list in handoff)",["TalkBack labels, font scale 200 %, contrast, baseline profile, startup < 1 s without model."],["checklist in STATUS.md"],"manual")
t("T-195","Trademark check name 'Hain'","plan","XS","H","-","ADR-20",
  "docs/workflow/STATUS.md",["Human: DPMA/TMview search classes 9 and 42."],["result noted"],"manual")
t("T-199","Beta release (GitHub + F-Droid metadata)","scaffold","S","H","T-190","AGENTS.md",
  "fastlane/metadata/android/{de-DE,en-US}/*, .github/workflows/release.yml",["Signed release build, changelog, F-Droid metadata."],["release APK builds"],"./gradlew assembleRelease")

import os,re
OLD={}
if os.path.exists("docs/workflow/TASKS.md"):
    for line in open("docs/workflow/TASKS.md",encoding="utf-8"):
        m=re.match(r"\| (T-\w+) \|.*\| ([^|]+) \|\s*$",line)
        if m: OLD[m.group(1)]=m.group(2).strip()
out=["# TASKS.md — backlog","",
"Generated from `tools/tasks_src.py` (planner). Status lives **only in the Overview table** (last column). Implementers change only that cell for their task.",
"Lanes A–F can run in parallel; H = human/strong model. `native` tasks need a strong model (WORKFLOW.md).","",
"Status values: `todo` · `doing (<model>)` · `review` · `done` · `blocked: <why>` · `split`","",
"## Overview","","| Task | Title | Type | Size | Lane | Depends | Status |","|---|---|---|---|---|---|---|"]
for x in T: out.append(f"| {x['id']} | {x['title']} | {x['type']} | {x['size']} | {x['lane']} | {x['depends']} | {OLD.get(x['id'],'todo')} |")
out.append("")
for i,x in enumerate(T):
    if i in M: out+= [f"## {M[i][0]} · {M[i][1]}",""]
    out+=[f"### {x['id']} · {x['title']}","",
          f"- type: `{x['type']}` · size: `{x['size']}` · lane: `{x['lane']}` · depends: {x['depends']}",
          f"- read: {x['read']}", f"- files: {x['files']}","- do:"]
    out+=[f"  {n+1}. {s}" for n,s in enumerate(x['do'])]
    out+=["- accept:"]+[f"  - {a}" for a in x['accept']]+[f"- check: `{x['check']}`",""]
print("\n".join(out))
