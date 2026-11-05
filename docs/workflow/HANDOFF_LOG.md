# HANDOFF_LOG.md — append-only

One entry per session, newest at the bottom. Format in WORKFLOW.md.

## 2026-10-28 · planning · done · harness: Hark · model: planner
- What: central AGENTS.md, workflow, 16 spec files, ADRs, task backlog (M0–M11), prompts, concept + research.
- Files: whole repo skeleton (docs only, no code).
- Checks: n/a
- Notes/risks: model URLs/sha256 not filled (T-080, never invent). llama.cpp VL support still in review (PR #31252); Omni/audio merged 2026-10-27 (PR #31257), not yet confirmed in a release tag.
- Next: T-001, T-002; human T-000.

## 2026-10-28 · planning (UI libraries) · done · harness: Hark · model: planner
- What: assistant-ui checked (React/RN only → UX blueprint, ADR-21). New spec UI_LIBRARIES.md (libraries, assistant-ui mapping, motion system). New tasks T-018, T-019, T-057, T-058, T-059, T-163. Components T-011…T-015 now depend on T-018/T-019.
- Notes/risks: Q-04 (Rive paid export vs Compose-drawn Pim). Haze blur only API 31+, fallback solid.
- Next: unchanged (T-001, T-002).

## 2026-10-28 · planning (copy sources) · done · harness: Hark · model: planner
- What: UI_LIBRARIES.md §2b/§2c: Composables UI (MIT) as copy source for components, SmolChat-Android (Apache-2.0) as JNI reference, AI Edge Gallery as reference, Stream AI components forbidden (Stream License). Roborazzi screenshot loop added to T-018.
- Next: unchanged.

## 2026-10-28 · planning (Pim v2 + micro-details) · done · harness: Hark · model: planner
- What: Pim v2 (minimal gumdrop, Compose-drawn, ADR-22, DESIGN_SYSTEM §4, assets/mascot/v2). SmolChat UI rejected (engine code only). awesome-kotlin checked: Konfetti, Showkase added; micro-details catalogue UI_LIBRARIES §2e.
- Next: Q-04 recommendation Compose-drawn; owner to confirm.
- 2026-10-28: Pim v2 SVGs + animations generated (tools/pim_gen.py). Compose Unstyled added as backup component base (UI_LIBRARIES §2c-bis).

## 2026-10-28 · planning (Pim v2.1 motion + voice) · done · harness: Hark · model: planner
- What: leaf fixed (sits on stem tip, stem grows from body); rig with 16 params; 33 motions (9 states, 24 micro-actions) + idle director in tools/pim_gen.py → motion/pim_motion.json, static SVGs, GIFs, preview HTML. Pim voice chosen: kerstin-low +3 st (de), cori-medium +2 st (en); samples in assets/voice/. ADR-23, VOICE.md, DESIGN_SYSTEM §4, MODELS, LICENSES, T-016/T-141/T-163 updated.
- Next: owner listens to samples and confirms voice; T-080 must add kerstin-low URL + SHA-256 (human).

## 2026-10-28 · planning (Pim voice v2) · done · harness: Hark · model: planner
- What: owner rejected Piper quality, likes Kerstin's colour → Pim voice = Kokoro German Kerstin (cryptomilk, Apache-2.0) +2 st, EN Kokoro af_heart +1 st; Piper kerstin-low/cori as fallback for slow phones. New samples in assets/voice/, scripts in tools/voice/, new task T-143 (ONNX export), VOICE/MODELS/LICENSES/ADR-23 updated.
- Next: owner confirms samples; T-143 needs device RTF numbers.

## 2026-10-28 · planning (Pim voice DE) · open · harness: Hark · model: planner
- Owner: EN af_heart good, DE Kokoro-Kerstin unusable. 4 German candidates (Pocket TTS cosette/anna/eponine, Kokoro Victoria) in assets/voice/candidates_de/. Waiting for owner choice.

## 2026-10-28 · planning (Pim voice multilingual) · done · harness: Hark · model: planner
- Owner: one voice everywhere. Pocket TTS voice anna +2 st, 7 languages, English default + chosen language pack at setup. Samples assets/voice/samples/pim_<lang>.mp3. Also added assets/mascot/v2/pim_all_frames.svg(.txt) (all 1349 frames, ids <motion>_f<NNN>) via tools/pim_frames_svg.py.

## 2026-10-28 · T-001 · done · harness: Antigravity · model: Gemini 3.8 Flash
- What: Git repo initialized with branch main, GPL-3.0 LICENSE added, llama.cpp submodule added at tag b11963 (commit 39febd2adb947b70da35e95f9351b3bf58ad170a) with LFM3 text and Omni/VL support.
- Files: .gitmodules, third_party/llama.cpp, LICENSE, docs/workflow/STATUS.md, docs/workflow/TASKS.md, docs/workflow/HANDOFF_LOG.md.
- Checks: `git submodule status` passes.
- Notes/risks: Tag b11963 includes LFM3-1.7B text and LFM3-Omni-2B / audio support. Initial project docs/tools also added to git tracking.
- Next: T-002 (Gradle multi-module skeleton).

## 2026-10-28 · T-002 · done · harness: Antigravity · model: Gemini 3.8 Flash
- What: Gradle multi-module skeleton set up with 18 modules (1 app, 11 core, 6 feature) adhering to ARCHITECTURE.md §1-2. Gradle 9.1.0, AGP 9.0.1, Kotlin 2.1.20, Compose BOM 2026.03.01. Configured minSdk 29, targetSdk 36, abiFilters arm64-v8a, Java 17, and no material3 dependency.
- Files: settings.gradle.kts, build.gradle.kts, gradle.properties, gradle/libs.versions.toml, gradlew, gradlew.bat, gradle/wrapper/*, app/src/main/AndroidManifest.xml, all 18 modules' build.gradle.kts and package directories with .gitkeep files.
- Checks: `./gradlew assembleDebug` and `./gradlew testDebugUnitTest` passed.
- Notes/risks: AGP 9.0.1 natively includes Kotlin support without requiring the standalone `org.jetbrains.kotlin.android` plugin in Android modules.
- Next: T-003 (Lint & static checks) or parallel lanes B/C/D.

## 2026-10-28 · T-003 · done · harness: Antigravity · model: Gemini 3.8 Flash
- What: Lint and static checks configured across all modules. Added ktlint-gradle 14.2.0 and detekt 1.23.8 plugins to build.gradle.kts and gradle/libs.versions.toml. Added config/detekt.yml with Composable ignored in FunctionNaming, and .editorconfig ignoring Composable in ktlint function naming rule.
- Files: build.gradle.kts, config/detekt.yml, .editorconfig, gradle/libs.versions.toml, docs/workflow/TASKS.md, docs/workflow/STATUS.md, docs/workflow/HANDOFF_LOG.md.
- Checks: `./gradlew ktlintCheck detekt`, `./gradlew assembleDebug testDebugUnitTest` passed.
- Notes/risks: `.editorconfig` disables ktlint for `*.gradle.kts` to prevent false positive script formatting conflicts in Gradle build scripts while enforcing style on Kotlin sources.
- Next: T-004 (Hilt + App class + MainActivity) or parallel lanes.

## 2026-10-28 · T-004 · blocked · harness: Antigravity · model: Gemini 3.8 Flash
- What: Attempted T-004 (Hilt + App class + MainActivity). Task requires `@HiltAndroidApp HainApp`, edge-to-edge `MainActivity`, and `setContent { HainTheme { Text placeholder } }`.
- Blocker:
  1. Hilt build plugin and dependencies (e.g. `com.google.dagger:hilt-android`, compiler/KSP plugin) are not present in `gradle/libs.versions.toml`, root `build.gradle.kts`, or `app/build.gradle.kts`. T-004 lists `files: app/src/main/AndroidManifest.xml, app/.../HainApp.kt, MainActivity.kt` and does not include Gradle files.
  2. `HainTheme` is not implemented yet; it is slated for T-010 in `:core-ui` (`core-ui/src/main/kotlin/app/hain/ui/theme/HainTheme.kt`).
- Question logged: Q-05 in STATUS.md regarding how to handle Hilt dependency setup and HainTheme placeholder.
- Files: docs/workflow/TASKS.md, docs/workflow/STATUS.md, docs/workflow/HANDOFF_LOG.md.
- Next: Human answers Q-05 or unblocks T-004; in parallel lanes: T-005 (Strings DE/EN), T-010 (Design tokens), T-020 (Room skeleton), T-040 (Data classes).

## 2026-10-28 · review T-001…T-004 + T-004 · done · harness: ZCode · model: GLM-5.3-Flash
- What (review): compared the four commits against their task cards and specs. T-001 (submodule at 39febd2ad, hash recorded), T-002 (18 modules match ARCHITECTURE §1, versions only in libs.versions.toml, no material3), T-003 (ktlint 14.2.0 + detekt 1.23.8 + .editorconfig) all pass: `./gradlew assembleDebug`, `testDebugUnitTest`, `ktlintCheck detekt` green, no hard-coded user-visible strings, no material3 imports anywhere, no network code at all yet (NetworkGate untouched). Verdict: status `done` confirmed, no changes.
- What (T-004 fix): resolved Q-05 with option A per human instruction ("fix the Problem of Task 4"). Added Hilt 2.60.1 via KSP (libs.versions.toml, root + app build.gradle.kts), `@HiltAndroidApp HainApp`, `@AndroidEntryPoint MainActivity` with `enableEdgeToEdge()` and a `BasicText(stringResource(app_name))` placeholder (HainTheme still pending T-010). Manifest registers app class + launcher activity, label now `@string/app_name`.
- Files beyond the task card (justified): gradle/libs.versions.toml, build.gradle.kts, app/build.gradle.kts (Q-05 said add Hilt), app/src/main/res/values{,-de}/strings.xml (hard rule: strings in resources, DE+EN), **gradle/gradle-daemon-jvm.properties** (new, toolchainVersion=21).
- Important finding: detekt 1.23.8's embedded Kotlin CLI cannot run on a JDK 25 daemon (`IllegalArgumentException: 25.0.3` in `JavaVersion.parse` as soon as a module has a Kotlin plugin — i.e. Hilt in :app). Fixed by daemon JVM criteria `toolchainVersion=21` (JDK 21 provisioned via the foojay resolver already in settings.gradle.kts). This affects every future session: if the Gradle daemon runs on JDK 25, `detekt` fails on :app.
- Notes persisted: findings now live in docs/workflow/KNOWN_ISSUES.md (KI-01 detekt/JDK 25 + daemon pin, KI-02 T-004 on-device acceptance open, KI-03 review deviations); STATUS.md points to it ("Now" + pinned versions).
- Checks: `./gradlew assembleDebug testDebugUnitTest ktlintCheck detekt` all green. APK contains Hilt-generated components. On-device acceptance ("app starts") not runnable: `adb devices` shows no device attached.
- Notes/risks: Hilt Gradle plugin resets detekt's jvmTarget inference to the daemon JVM; do not remove the daemon-JVM pin until detekt ≥ 2.0.0-alpha (JDK 25 tested). T-002 also committed docs/workflow/AGENT_PROMPTS.md and LOCAL_MODELS.md (generated workflow docs) beyond its file list — harmless, left in.
- Next: T-005 (strings + checker), then lanes B/C/D per STATUS.md "Now".

## 2026-10-30 · T-004 follow-up (notes + device-check attempt) · done · harness: ZCode · model: GLM-5.3
- What: persisted the session findings in docs/workflow/KNOWN_ISSUES.md (KI-01 detekt/JDK-25 daemon pin, KI-02 on-device acceptance, KI-03 review deviations); STATUS.md points to it ("Now" + pinned versions). Attempted to close KI-02 with the local Pixel_7 emulator: the AVD was an unusable stub — repaired ~/.android/avd/Pixel_7.ini and config.ini (image.sysdir.1, tag.id, abi.type, skin); emulator boots and KVM works.
- Why still open: the agent sandbox reaps background processes — adb server dies between tool calls and the emulator launcher (owner of the adb bridge) gets killed, so `adb devices` never lists the running qemu. Human decided to stop ("dann höre auf"). Cleanup done: no qemu processes, AVD locks removed, adb server stopped.
- How to close KI-02: attach a real device → `adb install -r app/build/outputs/apk/debug/app-debug.apk` → `adb shell am start -n app.hain/.MainActivity` (no crash, label "Hain"). Emulator also works when started from Android Studio outside the sandbox (Pixel_7 AVD is now functional, API 36 x86_64, ARM translation covers the arm64-only APK).
- Checks: `./gradlew assembleDebug testDebugUnitTest ktlintCheck detekt` green on JDK 21 daemon (504 tasks).
- Next: T-005 (Strings DE/EN + checker), lanes B/C/D per STATUS.md "Now".

## 2026-11-01 · batch "Fundament + UI-Basis" (T-005, T-006, T-010, T-018, T-019, T-016, T-017) · done · harness: ZCode · model: GLM-5.3

Human-authorized batch mode (7 tasks, one session; AGENTS.md §1 override). Baseline was green (T-004 state); all dependencies inside/outside the batch satisfied.

| task | status | check result | commit | files outside list (reason) | open issues |
|---|---|---|---|---|---|
| T-005 | done | `python3 tools/check_strings.py` OK (pos. + neg. test proven) | 571c1b7 | TASKS.md (protocol) | locales_config.xml not yet wired into AndroidManifest (`android:localeConfig`) — manifest not in card; wire in a settings/l10n task |
| T-006 | done | YAML valid (actionlint not installed locally); `cat` check | 7508d63 | TASKS.md | CI untested on a real runner (no push from here); NDK pinned to r27 per ADR |
| T-010 | done | `:core-ui:testDebugUnitTest` 9/9 + full gate | 02b2ca6 | TASKS.md | — |
| T-018 | done | assembleDebug + `grep material3` on `:core-ui:dependencies` → 0 hits + full gate | fd0e615 | build.gradle.kts root (roborazzi plugin), LICENSES.md (hard rule), TASKS.md | version pins constrained by toolchain → KI-04; Backdrop only as alpha03 |
| T-019 | done | `:core-ui:testDebugUnitTest` (motion tests) + full gate | 25949f9, 9fe177a (preview fix) | TASKS.md | — |
| T-016 | done | `:core-ui:testDebugUnitTest` (Pim tests) + full gate | dbd9247 | TASKS.md | drawing split across PimGeometry.kt (data) / Pim.kt (render) — both files are in the card list |
| T-017 | done | assembleDebug (22 drawables compile) + full gate | d081480 | TASKS.md | converter was an own script (vd-tool not installed), output equivalent; icons from lucide-static 1.66.0 (ISC) |

**Final gate:** `clean assembleDebug testDebugUnitTest ktlintCheck detekt` green (541 tasks), `check_strings` OK, 33 core-ui unit tests / 0 failures, 0 material3 imports, 0 TODO/FIXME, 0 skipped tests.

**Self-review notes:**
- AAPT2 requires the *unprefixed* `name=` attribute on `<string>`; `android:name` is silently dropped (cost one red herring in T-005).
- Showkase requires non-private `@Preview` functions → previews are `internal` (convention for T-011+).
- `WordFadeIn` "regenerate" path snaps (non-append edits never animate); `MotionGovernor` streak resets on fast frames and never re-enables blur mid-session.
- Pim geometry/anchors are locked by tests against `pim_motion.json` values; fx glyphs (zz/!/?) are hand-drawn const paths because the JSON only covers state fx.

**Not done / deferred:** on-device install via adb — device attached but unauthorized (user must confirm the USB-debugging dialog); KI-02 check runs after that.
**Reviewer should look at first:** core-ui/build.gradle.kts (material3 exclusion at configuration level — intentional, matches card rule "no material3 in the dependency tree"), tools/check_strings.py, HainGlass/HainShimmer wrappers, PimGeometry path parser.

## 2026-11-01 · batch D "Chat-Kern" (T-040…T-045) + T-070 · review · harness: Hark · branch `hark/batch-d`

Human-authorized large batch ("sehr großer Happen"; AGENTS.md §1 override). One commit per task, draft PR, not ready-for-review without the human.

| task | status | check result | files outside list (reason) | open issues |
|---|---|---|---|---|
| T-040 | review | compiles; used by all tests below | — | — |
| T-041 | review | G-01…G-06 rendered by `tools/render_golden.py` from the official Jinja chat template (jinja2) | — | regenerate goldens if the model template changes |
| T-042 | review | `LfmPromptBuilderTest` 11/11, byte-exact vs goldens | `PyJson.kt` (JSON exactly like Python `json.dumps`, needed for byte equality); kotlinx-serialization-json 1.8.1 → libs.versions.toml, LICENSES.md | — |
| T-043 | review | `XmlToolCallParserTest` 13/13 | — | strict: malformed call → no calls, text kept, error list |
| T-044 | review | `StreamSplitterTest` 7/7 | — | tag hold-back max 12 chars |
| T-045 | review | `ThinkingPolicyTest` 6/6, `SystemPromptFillerTest` 5/5 | `EmergencyNumbers` (in SystemPromptFiller.kt) | `SystemPromptVars.of()` takes time/locale/region only; city/memory/custom via `copy()` (detekt LongParameterList) |
| T-070 | review | `FeedParserTest` on real responses of all 14 standard feeds (fixtures saved 2026-11-01) + synthetic cases | kxml2 2.3.0 (test only) → LICENSES.md | own parser on XmlPullParser, no Rome/other lib |

**Gate:** `:core-llm:testDebugUnitTest :core-tools:testDebugUnitTest ktlintCheck detekt` green (see PR). Not run: `assembleDebug` for :app with native (submodule not initialised in the Hark sandbox; no JNI change in this batch).
**Also:** `tools/sync.sh` (keep a local checkout identical to GitHub; documented in AGENTS.md §7). KI-02 title corrected to OPEN — the body and the batch log both say the device check is still pending.
**Gotcha:** JUnit4 rejects `@Test fun x() = check(...)` when `check` returns a value ("Method should be void") — use block bodies.
**Next:** T-032 (JNI bridge, native, strong model) → T-050 turn loop; T-071 feed repository.


## 2026-11-01 · batch E "M4 MVP: basic working app" · review · harness: Hark · branch `hark/batch-e`

Human asked: "mach es basic funktionsfähig, bau schon das UI, GitHub Action die immer APKs baut". One commit per task group, draft PR.

| task | status | check result | files outside list (reason) | open issues |
|---|---|---|---|---|
| T-006 | review | `apk.yml`: APK on every push/PR (artifact), nightly pre-release on main | `app/signing/hain-debug.keystore` (shared public debug key, force-added) | first real runner run = this PR |
| T-030/T-033 | review | host test (aarch64 Linux, same C++ + LFM3-1.7B Q4_K_M): "Paris." then "Wien. 😊", 2nd turn 0.6 s via prefix reuse; emoji/umlaut UTF-8 intact | — | **deviation from LLM.md §1:** shared libs + `GGML_BACKEND_DL`/`GGML_CPU_ALL_VARIANTS` instead of static `armv8.2-a+dotprod+i8mm` (avoids SIGILL); app uses `useLegacyPackaging` so backends can be dlopen'ed. Callback gets UTF-8 `byte[]`, not String. Vulkan not wired. |
| T-031 | review | `PromptAssetsTest` (assets == prompts/) | — | plain copy, test guards drift |
| T-032 | review | compile + used by ChatEngine tests (fake) | — | one model, one generation at a time; threads = cores−2 (2…6) |
| T-080/T-081/T-082 | review | `CatalogParserTest`; sha256/size from HF API (LiquidAI/LFM3-1.7B-GGUF, verified by local download) | core-models manifest (FGS dataSync + POST_NOTIFICATIONS) | no DeviceCheck/bench yet; no Wi-Fi-only setting yet; downloads bypass NetworkGate (T-060 not done) |
| T-011…T-013, T-015 (subset) | review | compile; used by screens | — | HainButton, HainIconButton, HainMenuRow, Composer, UserBubble, AssistantBlock (mikepenz markdown core, own colours), ReasoningBlock, MessageAction, TopBar, EmptyState, ProgressBar. Missing: HainTextField, chips/cards, switch, sheet, toast, previews |
| T-050…T-053 | review | `ChatEngineTest` 2/2, `ChatStoreTest` 2/2 | app wiring (MainActivity, AppModule, SystemPromptSource) | **no tools** (system prompt says so via custom instructions); chats in memory only until T-020; no model picker/settings |
| T-083 | review | compile | — | basic: welcome + download + progress + retry; no language step |

**Gate:** `assembleDebug testDebugUnitTest ktlintCheck detekt -Phain.native=false` green, 37 unit tests in touched modules, 0 failures; `check_strings` OK. Native APK built by `apk.yml` on GitHub.
**Sandbox gotchas:** NDK has no linux-aarch64 host → native only on CI (`-Phain.native=false` locally). AAPT2 is x86-64 → run via `qemu-x86_64` wrapper set in `~/.gradle/gradle.properties` (`android.aapt2FromMavenOverride`, the file must be named `aapt2`).
**Next:** device test (KI-02), T-020 Room, T-062 tools, settings, T-084.

## 2026-11-01 · day 1 "Chats bleiben gespeichert" (T-020, T-053) · review · harness: Hark · branch `hark/day-1`

v0.2 plan, day 1 of 6. One commit per task, draft PR, not ready-for-review without the human.

| task | status | check result | files outside list (reason) | open issues |
|---|---|---|---|---|
| T-020 | review | `HainDatabaseTest` 3/3 (insert/query, rename/pin/cascade delete, FTS prefix + umlaut search incl. update/delete triggers), `FtsQueryTest` 3/3, `ChatStorePersistenceTest` 3/3 | `ChatStore`/`ChatPersistence`/`RoomChatPersistence` in :feature-chat and `AppModule` (wiring: chats survive restarts); `core-data/schemas/` (exported Room schema v1) | **decisions (STATUS → Pinned versions):** SQLCipher for Android 4.10.0, raw key `x'…'` (no KDF), Keystore-wrapped key file in `noBackupFilesDir`; **FTS4 instead of FTS5** (Room cannot declare FTS5); sqlite-vec deferred to T-110. Wrapped key in a file, not DataStore (DataStore not in the build yet). DAO tests run on the bundled SQLite driver (Robolectric's native SQLite has no linux-aarch64 build); SQLCipher itself is only exercised on the device. If the DB cannot open, chats stay in memory (logged). |
| T-053 | review | `ChatGroupsTest` 2/2; compile | `HainIcons` + `ic_ellipsis`/`ic_pin`/`ic_pencil` (Lucide) in :core-ui; `DrawerActions.kt` | drawer: search (text + FTS), sections Pinned/Today/Yesterday/7 days/Older, per-chat ⋯ → pin, rename (inline), delete (inline confirm); chat scales to 0.94 while the drawer is open. No UI test yet (no Compose test setup in the repo); no continuous-corner/parallax polish. |

**Persistence model:** `ChatStore` stays the in-memory source for the UI and writes through, in order, on an IO scope. Streaming answers are saved only when finished. Ids start at the wall clock and are bumped past the DB's max id, so they stay unique across restarts.
**Gate:** `assembleDebug testDebugUnitTest ktlintCheck detekt -Phain.native=false` green (112 unit tests, 0 failures), `check_strings` OK. Native APK by `apk.yml`. Low-memory sandbox: `--max-workers=2 -Pkotlin.compiler.execution.strategy=in-process` avoids daemon crashes.
**Sandbox gotchas:** run ktlintFormat with `-Phain.native=false` too, otherwise Gradle starts downloading the NDK. `build/` outputs of some modules are tracked in git by an older commit — do not stage them (`git add` explicit paths).
**Next:** day 2 — settings + DataStore, packs screen, DeviceCheck, onboarding language.

## 2026-11-02 · day 2a "Neues Gehirn-Modell" (T-080) · review · harness: Hark · branch `hark/day-2`

Brain switched to `LFM3-1.7B-QAD-Q4_0.gguf` (1,049,615,680 bytes, sha256 43a7d09a…7452; catalog id `lfm3-1.7b-qad-q4_0`). The old Q4_K_M file is deleted automatically once the new one is complete. Sampling defaults (temp 0.1, repeat 1.1) already match. Catalog test, MODELS.md, README, AGENTS.md updated.
**Split:** settings/DataStore, packs screen, DeviceCheck, model picker, onboarding language move to day 2b (plan row 2b).

## 2026-11-02 · day 2b "Einstellungen" (T-100, T-101) · review · harness: Hark · branch `hark/day-2b`

| task | status | check result | files outside list (reason) | open issues |
|---|---|---|---|---|
| T-100 | review | `SettingsRepositoryTest` (defaults, read-back, reset, custom instructions) | `core-common` (DataStore preferences-core, Apache-2.0, in LICENSES.md) instead of core-data; `AppModule` provides the repository | Only level-1 keys so far: theme, readable font, answer language, answer mode, about me, style. Other SETTINGS.md keys come with their features. |
| T-101 | review | compile | `ChatEngine` takes an `answerMode` provider; `SystemPromptSource` takes custom instructions; drawer got a Settings row | No filled-prompt preview yet; no UI test (no Compose test setup). |

**Next:** day 2c: packs screen, DeviceCheck, model picker, onboarding language step.

## 2026-11-02 · day 2c "Wissenspakete" (T-081 DeviceCheck, packs screen) · review · harness: Hark · branch `hark/day-2c`

| task | status | check result | files outside list (reason) | open issues |
|---|---|---|---|---|
| T-081 | review | `DeviceCheckTest` (4 cases); Gate | packs screen in `feature-settings` (reads `ModelRepository`), Settings row "Wissenspakete" | Only one pack in catalog; RAM from `ActivityManager`. |

**Next:** day 2d: model picker (T-055), onboarding language step (T-083).

## 2026-11-02 · day 2d "Fixes aus dem Gerätetest" (Teil 1) · review · harness: Hark · branch `hark/day-2d`

| task | status | check result | files outside list (reason) | open issues |
|---|---|---|---|---|
| T-080 | review | `SettingsRepositoryTest` (auto language → device language) | `SystemPromptSource` passes the device language | Antwortsprache Auto = Sprache der Nutzernachricht, sonst Gerätesprache. |
| T-057 | review | `ChatEngineTest` (stopped while thinking) | `ChatViewModel` no longer copies live text into a stopped answer | Branches/queue still open. |
| T-083 | review | Gate | `PimGeometry` viewBox translate sign fixed (Pim was shifted and clipped) | Onboarding-Sprache bleibt 2f. |
| T-053 | review | Gate | Header says Hain, greeting scrolls, chips equal width, one thinking indicator | Auto-Titel (4), Ladezeit (3), Nachricht bearbeiten (6) → Häppchen 2d-2. |

**Next:** day 2d-2: Ladezeit messen, Auto-Titel, Nachricht bearbeiten; dann 2e.

## 2026-11-02 · day 2e "Persönlichkeit" (Teil 1) · review · harness: Hark · branch `hark/day-2e`

| task | status | check result | files outside list (reason) | open issues |
|---|---|---|---|---|
| T-163 | review | `JuicyPimTest` (breathe, hop, blink, tap) | `PimGeometry.drawPim` got an optional gloss layer (`PimGloss.kt`); `MainActivity` provides `LocalReduceMotion` from the system animator scale | No eye tracking yet; reduce-motion switch in settings comes with 2e-2. |
| T-083 | review | Gate | `WelcomeTour`, `SuccessScreen`, `Heroes` in feature-onboarding; `ThinkingGlow`, `Celebration` (Konfetti) in core-ui; primary `HainButton` domed + press-in | Onboarding language step stays 2f. |

**Next:** day 2e-2: Einstellungen neu (Gruppen mit Icon, Unterseiten), Micro-Animationen Senden/neue Nachricht/Drawer; dann 2d-2.

## 2026-11-02 · day 2d-2 "Tempo, Stopp, Tastatur, Gehirn später, Nachrichten-Aktionen, Menü, Auto-Titel" · review · harness: Hark · branch `hark/day-2d-2`

| task | status | check result | files outside list (reason) | open issues |
|---|---|---|---|---|
| T-190 | doing | Gate; APK check: CI APK built with `-Phain.fastDebug=true` | `app/build.gradle.kts` (debug `isDebuggable` from property), `apk.yml` | **Main "sau langsam" cause:** the phone APK was a *debuggable* debug build (Compose runs without ART optimisations, library baseline profiles ignored). CI now builds it non-debuggable, same id `app.hain.debug` + key → installs as update. R8 (`isMinifyEnabled`) and an own baseline profile are **not** on yet (need a device run to rule out missing keep rules) → 2d-3. |
| T-033 | review | host harness against host-built llama.cpp `39febd2` + real `LFM3-1.7B-QAD-Q4_0.gguf` (see below); `ByteOffsetsTest` | native logic moved to `core-llm/src/main/cpp/hain_session.h` (no JNI, testable on a PC) | LFM3 is hybrid (attention + short conv): `llama_memory_seq_rm` cannot cut the recurrent part, so **every turn re-read the whole chat**. Now: partial-state checkpoints (`llama_state_seq_*_ext`, `PARTIAL_ONLY`, like llama-server) at the end of the fixed system text, of the system prompt and of the history; warm-up reads the system prompt after loading; abort callback → stop lands inside a prompt batch; load progress callback; stats (`HainLlm` logcat: load ms, prompt/reused tokens, t/s). |
| T-050 | review | `ChatEngineTest` (checkpoints, 30-min time anchor, warm-up, title prompt, clean title) | `SystemPromptSource.current(atMillis)` | System prompt contained the time in minutes → prompt start changed every minute → full re-read. Time anchor now moves every 30 min. Auto-title is asked *after* the history (same prefix), thinking off, 24 tokens max. |
| T-057 | review (subset) | `ChatViewModelTest` 7 cases, `ChatStoreTest.truncateFrom` | `ChatStore.truncateFrom` | Stop switches the UI at once (message marked stopped, job cancelled, native flag set). Edit own message (long press → menu), ask again on any answer, streaming UI updates throttled to ~20/s, chat without brain → hint `NO_BRAIN`. Branches/queue still open. |
| T-052 | review | Gate | manifest `windowSoftInputMode="adjustResize"`; `core-ui`: `HainPopupMenu`, `UserBubble(onLongPress, menu)` | Keyboard: the window was *panned* (top bar under the status bar) **and** got `imePadding` (input in the middle). Now resize only; greeting shrinks Pim and hides chips below 420 dp height. Banner "Pim braucht noch sein Gehirn" with Laden/Später, edit bar above the composer, load progress in % in the top bar. |
| T-053 | review | Gate | — | ⋯ opens a compact popup at the row (pin, rename, delete with confirm inside the menu) instead of the inline box. |
| T-083 | review | Gate | `MainActivity` (SharedPreferences flag `onboarding_skipped`) | Download can be skipped ("Später – erst mal umschauen") or left running ("Im Hintergrund weiterladen"). |
| T-163 | review | `JuicyPimTest` | `Pim.kt` `PimCanvas(pose: () -> PimPose)` | Pim and the thinking glow read their animation values in the draw phase only (no recomposition per frame). Blink 200 ms. Glow fades in 450 ms with an eased curve. |

**Measurements (sandbox, aarch64, 2 cores, host build of the pinned llama.cpp, `LFM3-1.7B-QAD-Q4_0.gguf`; the phone will differ, ratios matter):**
- Load with Q4_0 repack (default): 2.3 s; without repack (`use_extra_bufts=false`): 0.9 s, but prompt reading ~3× and answers ~2–3× slower → repack stays, load shows progress instead.
- Warm-up (system prompt, 113 tokens): 1.8 s once after loading.
- Turn 1: 134-token prompt, **113 reused**, read in 0.29 s. Turn 2: 190 tokens, **174 reused**, 0.40 s — without checkpoints the same prompt took 2.0 s (full re-read). Switching to another chat restores the system checkpoint (85 tokens reused).
- Stop inside a long prompt (1.9k tokens): **≈ 30–40 ms** until the decode returns (before: only after the whole prompt batch, here > 20 s worst case).
- Greedy output with checkpoints vs. full re-read: same quality, can differ by a word (different batch split → tiny float differences, known llama.cpp behaviour).
- Not measured: phone start time, jank, t/s on the phone (no device in the sandbox) → please look at logcat tag `HainLlm` on the next test.

**Gate:** `assembleDebug testDebugUnitTest ktlintCheck detekt -Phain.native=false` green (135 unit tests, 0 failures), `check_strings` OK. Native APK by `apk.yml` (PR #14). **Next:** 2d-3 (Einstellungen neu, 3D-UI/Onboarding-Bilder, Pim-3D-Glanz; R8 + Baseline-Profil after a device check).


## Day 2d-3 (03.11.)
- T-100: settings are now a hub of icon tiles (Look, Answers, About you, Knowledge packs, Privacy, About Hain) with sub pages; new DataStore key `reduce_motion` feeds `LocalReduceMotion` (OR system animation scale). EN+DE strings.
- Not done (stays in 2d-3 / later): glossy 3D Pim, new onboarding pictures, R8 + baseline profile (needs device test of 2d-2 first).

## 2026-11-03 · fix "Antwortsprache Deutsch zuverlässig" (T-080) · review · harness: Hark · branch `hark/fix-language`

| task | status | check result | files outside list (reason) | open issues |
|---|---|---|---|---|
| T-080 | review | `ReplyLanguageTest` (detector, setting/device rules, German prompt ends with the rule), `ChatEngineTest` (hint after history, checkpoints kept, no hint), `PromptAssetsTest`; host run with llama.cpp `39febd2` + `LFM3-1.7B-QAD-Q4_0.gguf` (14 prompts auto + 5 extra cases, all in the right language, no mixing) | `SystemPromptSource` (template per language, settings instead of `customInstructions`), `MainActivity` (2 lines), `ChatEngine` (`turnHint`, `# Kontext` heading), `SystemPromptFiller` (`{language_rule}`) | **Cause:** the system prompt was English with the language rule in the middle; LFM3-1.7B follows the prompt language → "I'm doing great" / "Sure! Lauch (leek) …". `HainSettings.customInstructions()` is no longer used by the app (left in place, settings files belong to the parallel settings run). Content quality of the 1.7B model stays limited (e.g. invented leek dishes) – not a language issue. Sampling unchanged. Device check on the phone still open. |

## 2026-11-03 – Issue-Formulare (Hark)
- `.github/ISSUE_TEMPLATE/`: Bug, Funktionswunsch, Antwortqualität, Übersetzung, Frage (zweisprachig), leere Issues aus. Labels im Repo angelegt. README-Abschnitt Contributing verlinkt die Auswahl.

## 2026-11-03 · day 3 "Werkzeuge I" · review · harness: Hark · branch `hark/day-3`

| task | status | check result | files outside list (reason) | open issues |
|---|---|---|---|---|
| T-014 | review | compiles, previews light+dark, no material3 | – | ToolChip lives in feature-chat (ToolTimeline) |
| T-060 | review | `HttpNetworkGateTest` (log without query, detail, UA, offline + user download, https only, failures, 7 days) | API in `core-common/api/net` (settings + chat + tools need it), `AppModule` | HttpURLConnection instead of OkHttp (no new lib), log in memory → Room in 3b; model download not yet through the gate (3b) |
| T-062 | review | `ToolRouterTest`, `ChatEngineToolsTest` (loop, 3-round cap, offline schemas, quick sum) | `ChatEngine`, `ChatViewModel`, `MainActivity`, `SystemPromptSource`, prompt assets (tool rule) | – |
| T-063 | review | `CalculatorTest` + `UnitsTest` (> 50 cases) | – | currency not in v1 |
| T-064 | review | `WeatherToolTest` (fixtures, gate, days named) | – | WeatherCard → 3b |
| T-065 | review | `WikipediaToolTest` (fixtures, URLs, language) | – | WikiCard → 3b |
| T-059 | review | `ChatEngineToolsTest.stepsSurviveTheDatabase` | `ChatStore`, `RoomChatPersistence` (steps+sources in `sources_json`) | no favicons yet (needs Coil, 3b) |
| T-069 | review | compiles; strings EN+DE | `SettingsPages` (Privacy page), `SettingsActions` | UI test missing |

**Host check** (llama.cpp `39febd2`, `LFM3-1.7B-QAD-Q4_0.gguf`, temp 0.1, our prompt + tools block): "Wetter morgen in Köln" → `weather(place=Köln)`, "Wer war Ada Lovelace" → `wikipedia(topic, lang=de)`, "15 % von 80" → `calculate(15% * 80)`, Berlin weather (EN) and "mayor of Paris" → right tools, small talk and jokes → no tool. With a weather result the brain answered in German from the data but mixed up today/tomorrow → days in the result are now named (`"day":"morgen"`). "42 km in Meilen" came as `42 km in m` → example in the tool description now uses `mi`.

## 2026-11-03 · Werkzeuge II (day 4) · review · harness: Hark · model: Hark
- What: T-062 fix "tools in every message" (Oskar's device test of #19: tools only once per chat, then "laut Wikipedia …" hallucinated): tool calls + short results stored per answer and replayed in the prompt, new system-prompt rule DE+EN + short tool nudge in the language hint (host: 5 of 5 questions in a row call a real tool, before only the first). T-066 `web_search` (SearXNG/DDG HTML/DDG Lite/Wikipedia, no key) + T-067 `read_page`/`readPages` (3 pages in parallel, cut) → research in several steps with timeline steps and source chips; tool rounds 3 → 4, Schnell = 1 search + 1 page.
- Files: core-tools (internal/search/*, api/Tools.kt, ToolRouter.readPages, HainTools), feature-chat (ChatEngine, PromptHistory, ToolSteps/ToolStepsJson, ToolTimeline, strings EN+DE), app/SystemPromptSource, docs/specs/TOOLS.md §3a/§3b.
- Checks: unit tests (SearchToolTest, ToolsEveryTurnTest, ChatEngineToolsTest), ktlint, detekt, check_strings; host test with LFM3-1.7B-Q4_0 (table in TOOLS.md §3b); CI + APK on the PR.
- Notes/risks: DuckDuckGo HTML sometimes shows a bot check from server IPs → Lite fallback; public SearXNG instances often block JSON. No back-off timer yet. T-068 phone actions not started.
- Next: 4b (T-068 alarm/timer/calendar/navigation + ConfirmCard, DDG back-off, own SearXNG URL setting), then 3b.


## 2026-11-03 · redesign "Aktionen + Seitenleiste" (2d-4 points 2+3) · review · harness: Hark · branch `hark/redesign`

| task | status | check result | files outside list (reason) | open issues |
|---|---|---|---|---|
| T-010 | review | `HainTypeTest` (Hanken scale, body ≥ 16 sp), `HainShapesTest` (3 radii + capsule), `HainColorsTest` (new tokens), `HainSurfaceTest`, `HainMotionLanguageTest` (snap when reduced) | all screens that drew their own surface: `Composer`, `HainCard`, `HainChip`, `Buttons`, `Scaffolding`, `SettingsRows`/`SettingsScreen`/`PacksScreen`, `WelcomeTour`/`SuccessScreen`, `ChatScreen`/`ChatListDrawer` (move onto tokens) | task id T-010 used for the design-system part (T-083 is onboarding); Inter fonts removed; Roborazzi screenshots not recorded (Robolectric native graphics missing on the aarch64 sandbox) |
| T-057 | review | `MessageRatingsTest`, `ChatViewModelTest.thumbsToggleAndStayPerAnswer` | `MainActivity` (wires `PrefsRatings`), `HainIcons` + 2 Lucide drawables (thumbs) | follow-up suggestions skipped (extra brain call per answer); read aloud waits for voice (Häppchen 10); ratings of deleted chats stay in prefs (tiny) |
| T-053 | review | compiles; reduce motion → `snap()` for drawer, scrim, list, menu | `PopupMenu` (now `expanded` param for fade-out) | menu near the screen bottom does not flip upwards yet |

## 2026-11-03 · day 5 (2d-5) · done · harness: Hark
- What: (1) soft screen changes: start page → chat fades + slides (300 ms, `HainMotion.PAGE_MS`), settings and packs slide/fade in and fade out; "reduce motion" = no motion. (2) Answer-depth switch (Auto / Fast / Thorough) above the message box (`ModeSwitch`), stored in the same DataStore setting as Settings → Answers, applies to the next message.
- Stacked on `hark/day-4` (PR #21 open) + merged main (#20 redesign). Files: ChatScreen.kt, ModeSwitch.kt, MainActivity.kt, HainMotion.kt, strings EN/DE.
- Next: 3b / 4b / 5.

## 2026-11-03 · 3D redesign + thinking picker + long texts (2e-2) · review · harness: Hark · branch `hark/redesign-3d`

| task | status | check result | files outside list (reason) | open issues |
|---|---|---|---|---|
| T-011 | review | `HainSurfaceTest` (levels deeper, dark glass glows stronger), `HainShapesTest` (tile 24, composer 28); screenshots via `screens.yml` | `Buttons`, `Composer`, `PopupMenu` (HainPopover above anchor), `HainLevel`, `HainShapes`, `PimGloss`/`PimGeometry`, `ChatScreen`, `ChatListDrawer`, `ToolTimeline`, `SettingsScreen`/`SettingsPage`/`PacksScreen`, `WelcomeTour`/`SuccessScreen` + new `Bubbles.kt`, `GlassTile.kt`, `HainGlow.kt`, `Hain3DIcon.kt`, 22 `ic3d_*.webp` | 3D icons = own generation (LICENSES.md); no ConfirmCard/weather card yet (4b/3b) – they should use `glassTile` when they come |
| T-084 | review | compiles; same `AnswerMode` DataStore value as Settings → Answers | `ModeSwitch.kt` removed | ID collides with "Packs screen" (2c) – Oskar named it T-084 |
| T-085 | review | screenshot cases: long own message, long answer with table + code, composer 12 lines, long drawer titles, 200 % font, 360 dp, landscape | `Messages` (UserBubble fold + 85 % width), `Scaffolding` (TopBar ellipsis, min height), `PopupMenu` (2-line items) | horizontal scroll of tables/code is built into the markdown renderer (checked) |

- Screenshot tests live in `*/src/test/kotlin/**/screens/` and only run with `-Phain.screens` (Robolectric native graphics need x86): `.github/workflows/screens.yml` uploads them as artifact `screens`.
- Next: 3b, 4b (ConfirmCard on `glassTile`), 5.

## 2026-11-03 · Werkzeuge II Rest (4b) · review · harness: Hark · branch `hark/day-4b`

| task | status | check result | files outside list (reason) | open issues |
|---|---|---|---|---|
| T-068 | review | `TimeParserTest` (clock, durations, day words, weekdays, dates, ISO), `PhoneToolsTest` (4 tools, labels, whole day, bad time, JSON), `PhoneActionsTest` (engine → pending card, card states, stored), `ChatViewModelTest.confirmCardTapIsKeptOnTheMessage`; host test LFM3 (TOOLS.md §3c) | `ChatEngine`/`ChatStore`/`ToolStepsJson`/`RoomChatPersistence` (cards in `sources_json`), `ChatScreen`, `ToolTimeline`, `HainIcons` + 4 Lucide drawables, app manifest (`SET_ALARM`), `SystemPromptSource` (PHONE_DE/EN) | ConfirmCard lives in feature-chat (not core-cards, needs the chat state); intents not unit-tested (Robolectric missing on aarch64 sandbox); repeating alarms (`days`) + `confirm_actions` auto mode → 4c |
| T-066 | review | `SearchBackoffTest` (bot check → 10 min pause with fake clock, own SearXNG first), `SearxAddressTest` | `HainSettings`/`SettingsRepository` (`searxng_url`), `SettingsPages` (Privacy), `MainActivity` | back-off kept in memory (resets on app start, fine) |

- Notes: the brain answers phone requests with the right call in the host test; the tool result tells it that nothing is done yet, so it asks you to tap instead of claiming "eingestellt".
- Next: 4c (rest), then the plan order 2e-2+2f, 3b, 5 …


## 2026-11-03 · T-088 + T-089 (app icon, website "Lichtung") · review · harness: Hark · model: Hark
- What: T-088 app icon: Pim v2.1 shapes (pim_idle.svg) as a glossy glass drop with leaf (stem grows from the head) on a macOS-like squircle, gradients #5FDCA6 → #22B47A → #0B7A4F, light edge, inner glow, specular highlight, soft shadow. Own SVG + resvg, no image model. Adaptive icon (webp layers mdpi–xxxhdpi, legacy squircle + round), monochrome vector for themed icons, debug variant with a "dev" pill, manifest icon/roundIcon. T-089 website: restyled to style A "Lichtung" (tokens = HainStyle LICHTUNG light/dark), Hanken Grotesk self-hosted (Inter removed), new icon everywhere, feature list with status (12 available, 9 coming soon, from the README feature table), style A screenshots light/dark, "Made in Cologne" and the Köln examples removed, bubbles/3D Pim/grain/tilt/parallax removed. README: icon on top + two "new" bullets.
- Files: assets/icon/**, app/src/main/res/{mipmap-*,drawable/ic_launcher_monochrome.xml}, app/src/debug/res/**, app/src/main/AndroidManifest.xml, docs/design/ICON.md, website/**, README.md, docs/workflow/{TASKS,STATUS,HANDOFF_LOG}.md.
- Checks: icon checked at 48/72/96 px, masked round and squircle, safe zone; `npm ci && npm run build` green; Chromium screenshots desktop/mobile, light/dark. Gradle not run locally (parallel run) → CI + APK workflow.
- Notes/risks: T-089 is a new ID (T-087 may be used by the parallel polish run). Regenerate icons: `pip install resvg-py pillow && python3 assets/icon/src/make_icon.py`.
- Next: Oskar checks the icon on the phone (themed icon on Android 13+).

## 2026-11-04 · T-055a (language step at first start, localeConfig) · review · harness: Hark · model: Hark
- What: first onboarding screen "Wähle deine Sprache / Choose your language" (Deutsch / English). One tap sets the answer language and, on Android 13+, the app language via LocaleManager. Manifest now declares `android:localeConfig` (res/xml/locales_config.xml existed already) so the system language picker lists Hain with DE + EN.
- Files: feature-onboarding/.../LanguageStep.kt, OnboardingScreen.kt (onLanguage), MainActivity.kt (pickLanguage), AndroidManifest.xml, strings EN+DE, README.md.
- Not done: brain picker in the top bar (rest of T-055) → 2f-2.
- Next: Oskar checks first start with a fresh install.

## 2026-11-04 · Lichtung + Pim + 2048 + Haptik (Feinschliff) · review · harness: Hark · branch `hark/polish`

| task | status | check result | files outside list (reason) | open issues |
|---|---|---|---|---|
| T-087 | review | compiles in CI | Morgennebel, style switch, 3D icons, jelly Pim removed | – |
| T-088 | review | TTFT logged as `HainLlm` | `SystemPromptSource` (time moved into the hint) | host before/after numbers still to add |
| T-096 | review | `HapticsTest` | `HainSettings` (`haptics`), `SettingsPages` | – |
| T-089 | review | Pim frame tests (leaf root ≤ 1 dp from the head) | `PimMoves`, `PimGeometry`, `LivePim`, `PullPim` | – |
| T-091 | review | `SuggestionsTest` | `StartPage` | – |
| T-097 | review | `PimEggTest` (≥ 5 s threshold, ≤ 5× scale, monotonic, trembling builds), `HapticsTest` (charge denser + firmer) | `HeroPim`, `PimEgg`, `Haptics` | sizes: start 88/72/56 dp, thinking 28 dp, drawer 32 dp |
| T-098 | review | `Game2048Test` (slide, merge once, R/U/D, no-op move, spawn, game over, win, ids) | `MainActivity` (overlay, best score via `recordGame2048`) | no saved game across app restarts (only best score) |
| T-099 | review | compiles; HAPTICS.md checklist | `PopupMenu`, `PullPim`, `ChatListDrawer` | – |

- Oskar's correction 03.11. 23:26: Pim only a little bigger; the easter egg only after 5 s holding without a break, grows slowly to 5×, then bursts into the 2048 game; subtle haptics everywhere.
- Still open from the polish list: compact ⋯ menu, About page with website/GitHub, screenshot fixes (composer lines, back arrow, segments), usability pass, new screenshots → next run.

## 2026-11-04 · Day 3b (part 1): network log stays after restart · review · harness: Hark · branch `hark/day-3b`

| task | status | check result | files outside list (reason) | open issues |
|---|---|---|---|---|
| T-060 | review | `NetworkLogStoreTest` (store, load, prune, clear); DB version 2 + migration 1→2 | `AppModule` (shared `DatabaseHolder`) | brain download via gate, per-purpose switches, WeatherCard/WikiCard still open → 3b-2 |

## 2026-11-04 · T-196 (trademark policy, support coming soon) · review · harness: Hark · model: Hark · branch `hark/trademarks`
- What: new `TRADEMARKS.md`, German in plain language first, then English: code GPL-3.0-or-later and free for everyone; name "Hain", Pim (look, animations, voice) and app icon/logo are project marks; forks need another name, their own mascot and icon and must not look official; allowed without asking: unchanged official builds, mentions ("based on Hain"), reports/tutorials, distro/F-Droid packaging without code changes beyond the build; contact via GitHub issues; not legal advice, registration may be planned; no location details. README: "Name & Pim / Trademarks" and "Support / Unterstützen" (coming soon: star, issue, tell others) after the licence section; rest of README untouched.
- Files: TRADEMARKS.md, README.md, docs/workflow/{TASKS,STATUS,HANDOFF_LOG}.md.
- Not done (on purpose): no `.github/FUNDING.yml`, no sponsor button, website unchanged (other PR).
- Checks: docs only; CI on the PR.
- Next: Oskar reads the wording; T-195 (DPMA/TMview search) still open; add FUNDING.yml once a sponsor account exists.

## 2026-11-04 · T-100i (app icon "Pim als Sprechblase") · review · harness: Hark · model: Hark · branch `hark/icon-bubble`
- What: Oskar chose the motif "Pim als Sprechblase" (04.11.2026). Pim's body path in `assets/icon/src/make_icon.py` is now a round chat bubble (ellipse cx 62 cy 66 rx 36 ry 28.5 + tail at the lower left, one smooth closed path); stem + leaf moved so they grow from the top of the bubble (stem starts inside the body), eyes centred, highlight/inner glow/bounce light/contact shadow kept in the same style and re-placed. Pim scale 0.0068 → 0.0072 (tile), adaptive foreground 0.0051 → 0.0052 (tail tip at ~32 dp of the 33 dp safe radius). Monochrome vector = bubble silhouette with eyes cut out + stem + leaf.
- Files: assets/icon/** (PNGs + SVGs + make_icon.py), app/src/{main,debug}/res mipmaps, app/src/main/res/drawable/ic_launcher_monochrome.xml, website/public/{favicon.png,apple-touch-icon.png,og.png,img/hain-icon.webp} (OG: only the icon area swapped), website/src/components/ui.tsx (comment), README.md (alt text + feature bullet), docs/design/ICON.md, docs/workflow/{TASKS,STATUS,HANDOFF_LOG}.md.
- Checks: 1024/96/48 px, masked round and squircle (adaptive crop), monochrome rendered, dev pill clear of the tail; `npm ci && npm run build` green. Gradle not run locally (RAM) → CI + APK workflow.
- Next: Oskar checks the icon on the phone (launcher, themed icon on Android 13+).

## 2026-11-04 · T-102 bis T-105 (Icon „Pim im Wald“, Icon-Wahl, Menü-Blur, Website) · review · harness: Hark · model: Hark · branch `hark/icon-bubble` (PR #31)

| task | status | check result | files outside list (reason) | open issues |
|---|---|---|---|---|
| T-102 | review | Icon bei 1024/96/48 px, rund und Squircle maskiert, Safe-Zone geprüft | `assets/icon/**`, Mipmaps in `app/src/{main,debug}/res` (3D + `ic_launcher_flat*`) | Oskar prüft das Icon auf dem Handy |
| T-103 | review | `SettingsRepositoryTest` (`app_icon`) | `AndroidManifest.xml` (zwei `activity-alias`), `AppIconSwitcher`, `MainActivity`, `AppIconPicker`, `IconStep`, `SettingsPages`, Strings DE/EN | Icon wechselt erst beim Verlassen der App; der Launcher braucht evtl. ein paar Sekunden |
| T-104 | review | `MenuBackdropTest`; Screenshots `menu_blur_light/dark` über `screens.yml` | `MenuBackdrop.kt` (neu), `PopupMenu`, `Buttons`, `MainActivity`, `ChatListDrawer` (Langdruck) | Blur nur bei Popup-Menüs und Popovern, nicht bei Dialogen |
| T-105 | review | `npm ci && npm run build` grün (EN + /de vorgerendert) | `website/**`, `README.md` | Englische Seite zeigt deutsche App-Screenshots; Footer-Link „Marke“ zeigt auf `TRADEMARKS.md`, die erst mit `hark/trademarks` (T-196) auf main kommt |

- Website (T-105): Header blendet Hintergrund, Blur und Linie weich als eine Ebene ein (ohne Flackern), Screenshots ohne weißen Streifen unten (Settings kürzer), englische Labels auf /de übersetzt (Navigation, Footer, `og:locale`, `noscript`), Alt-Texte für Icon und Startseite.
- Nächster Schritt: Oskar testet APK (Icon-Wahl, Blur), danach Merge; Pages-Deploy erst nach dem Merge.

## 2026-11-04 · T-106 (Website: Hero-Screenshots repariert) · review · harness: Hark · model: Hark · branch `hark/web-fix`
- Was: Im Hero (iPad quer) hatte das zweite, kleinere Handy unter dem Screenshot eine große leere weiße Fläche. Ursache: Der Flex-Container streckte beide Rahmen (`.device`) auf die Höhe des größeren ersten Handys. Jetzt `items-start` am Container und `self-start` an jedem `Shot`, der Rahmen ist so hoch wie sein Bild. Der Screenshot selbst (Denk-Auswahl offen) bleibt unverändert, so gewollt.
- Geprüft mit Playwright (Chromium) bei 1366×1024, 1024×768, 390×844, 1440×900, hell + dunkel, EN + /de: kein waagerechter Überlauf, keine weiteren abgeschnittenen Bereiche. Vorher/Nachher-Bilder in `docs/design/web/`.
- Dateien: website/src/components/Hero.tsx, docs/design/web/*.png, docs/workflow/{TASKS,STATUS,HANDOFF_LOG}.md.
- Nicht gemacht: T-107 (englische App-Screenshots für die EN-Seite). Dafür braucht es EN-Varianten der Roborazzi-Style-Tests (Qualifier `en-…`, englische Beispieltexte in `ChatFrame`/`answerState`/`Drawer`, Onboarding, Einstellungen), einen Lauf von `screens.yml` und eine Sprachweiche in `Shot` (`<id>-<mode>-en.webp`). Zu groß für diesen Lauf.
- Hinweis: Ganzseiten-Screenshots brauchen die Klasse `cv-off` (sonst rendert `content-visibility` die Abschnitte außerhalb des Bildschirms nicht) – kein Fehler der Seite.
- Nächstes: Oskar prüft auf dem iPad; dann T-107.

## 2026-11-04 · T-107 (Website: englische App-Screenshots) · review · harness: Hark · model: Hark · branch `hark/web-en` (PR #35)
- Was: Die englische Seite zeigte deutsche App-Screenshots. Die Lichtung-Screenshot-Tests haben jetzt englische Varianten: `ChatStyleScreensEn`, `OnboardingStyleScreensEn`, `SettingsStyleScreensEn` (Qualifier `en-w392dp-h850dp-xxhdpi`, Dateien `lichtung_<screen>_<mode>_en.png`). Die Tests selbst liegen in abstrakten Basisklassen (`ChatStyleShots` usw.), die deutschen Klassen und ihre Dateinamen bleiben gleich. Beispieltexte stehen in `ShotTexts` (`DE`/`EN`, in `ChatScreens.kt`); `ChatFrame`, `Drawer` und `answerState` nehmen sie als Parameter, Standard ist Deutsch. Englisches Beispiel: Wetter in Paris + Eiffelturm, `LONG_MD_EN`.
- Website: zwölf neue Bilder `website/public/screenshots/<id>-<mode>-en.webp` (aus dem `screens`-Artefakt, auf 720 px Breite skaliert, unten wie die deutschen beschnitten, WebP q85). `Shot` bekommt `lang` und nimmt auf der EN-Seite die `-en`-Bilder, /de bleibt unverändert. `SHOT_SIZE_EN` für die höhere englische Einstellungsseite (720×1110, die App hat dort inzwischen eine kurze Einleitung oben).
- Deutsche Screenshots nicht neu erzeugt: Sie zeigen einen etwas älteren Stand (Einstellungen ohne Einleitung, andere Begrüßung). Neu aufnehmen wäre ein eigener Schritt.
- Geprüft mit Playwright (Chromium) bei 1366×1024 (hell + dunkel) und 390×844, EN + /de: alle Bilder laden, kein waagerechter Überlauf, /de zeigt weiter die deutschen Bilder. Vorher/Nachher in `docs/design/web/t107-*.png`. `npm ci && npm run build` grün. Gradle nicht lokal (zu langsam) → CI, APK, Screens.
- Dateien: feature-chat/src/test/.../screens/{ChatScreens,ChatStyleScreens}.kt, feature-onboarding/src/test/.../screens/OnboardingStyleScreens.kt, feature-settings/src/test/.../screens/SettingsStyleScreens.kt, website/src/components/{Hero,Sections}.tsx, website/src/content.ts, website/public/screenshots/*-en.webp, docs/design/web/t107-*.png, docs/workflow/{TASKS,STATUS,HANDOFF_LOG}.md.
- Nächstes: Oskar schaut sich die EN-Seite an; evtl. die deutschen Screenshots auf den aktuellen Stand bringen.

## 2026-11-04 · T-210 bis T-215 (0.1.3.1 – Extras) · review · harness: Hark · model: Hark · branch `hark/extras` (Draft-PR #36)

| task | status | check result | files outside list (reason) | open issues |
|---|---|---|---|---|
| T-210 | review | `MenuBackdropTest` (24 dp, Tönung 60–75 %, Fallback deckend, leichte Abdunklung); Screenshots `menu_frosted_light/dark` | `MenuBackdrop.kt`, `PopupMenu.kt`, `DESIGN_LANGUAGE.md` | Blur in Popups braucht Haze-Quelle im Hauptfenster (`HainMenuBackdrop`) – Dialoge außerhalb davon bleiben deckend |
| T-211 | review | `DevTuningTest`, `GgufPromptBuilderTest`, `HuggingFaceTest`, `SettingsRepositoryTest` (Dev-Werte), `DeveloperPageTest` | `hain_llm.cpp` + `NativeLlm` (Chat-Template aus GGUF), `LlamaEngine` (Kontext-Neuladen, Statistik), `ChatEngine`, `ChatViewModel`, `ModelDownloadWorker` (sha optional), `DefaultModelRepository` (Größe unbekannt), `MainActivity` | eigene Modelle ohne Werkzeuge; Download nicht durch das Gate (nur HEAD-Vorabfrage) |
| T-212 | review | `UpdateRulesTest` (JSON, Vergleich, Adressen, täglich, einmal fragen, Gate-Zweck UPDATE) | `apk.yml` (version.json, `v*`-Tags), `NetworkGate` (`Purpose.UPDATE`), `NetworkLogSection` | Kanal „Stabil“ findet erst etwas, wenn es ein `v*`-Release gibt |
| T-213 | review | `AnswerPimTest`; bestehende Pim-Tests unverändert (Blatt bleibt dran); Screenshot `chat_pim_tools` | `ChatScreen.kt`, `StartPage.kt` | – |
| T-214 | review | `AboutTapsTest`; Screenshots `lichtung_settings_about_*` | Icons `ic_github/scale/info/code/tag`, `SettingsScreen/Page/Pages/Rows` | – |
| T-215 | review | `AppInfoTest` | `app/build.gradle.kts` (`buildConfig = true`), `gradle.properties` | – |

- Gradle lokal nicht gestartet (qemu, gemeinsamer RAM): ktlint 1.5.0 CLI lokal, Rest über CI (CI, APK, Screens).
- README (Funktionstabelle + „Neu im Testbuild“), Website (EN + /de: Update-Prüfung, Entwicklermodus) und `SETTINGS.md` aktualisiert.
- Nächstes: Oskar testet die APK (Milchglas hell/dunkel, Entwicklermodus mit einem zweiten GGUF, Update-Banner mit Kanal Nightly); für „Stabil“ ein erstes `v0.1.3.1`-Tag setzen, wenn gewünscht.
- Nachtrag (Abschluss-Lauf): CI-Fehler bei `42d321f` war ein Netzwerk-Aussetzer (Robolectric lädt `android-all` von Maven Central, `IOException` in `HainDatabaseTest`/`NetworkLogStoreTest`), danach ktlint `function-signature` (`HainSettings`, `ChatEngine`) und detekt (Komplexität/Funktionszahl begründet per `@Suppress`, `LlamaEngine`-Bedingungen vereinfacht). Popup-Karten zeichnen in Robolectric in Software, daher zeigt der Screenshot-Test die Karte im neuen `overlay` von `HainMenuBackdrop` (Hauptfenster, echter Blur) mit `LocalForceFrost`. Fallback-Tönung jetzt deckend (`fallbackTint`). Pim schiebt sich bei „Weniger Bewegung“ nicht mehr aus dem Bild. Screenshots in `docs/design/screens/extras_*.png`.

## 2026-11-04 · T-220 (Keine erfundenen Quellen ohne Werkzeug) · review · harness: Hark · model: Hark · branch `hark/fix-cites`
- Fehler (Oskars Handy, LFM3-1.7B): ohne Werkzeug stand in der Antwort „… 200+ Jahre alt werden [1].“, darunter „[1] Wikipedia-Artikel zu Baum-Lebensdauer (falls verfügbar) – hier kein aktuelles Werkzeug nötig für diese allgemeine Frage.“, und vorne „**Fakt:**“. Ursache: Die Werkzeug-Regel im System-Prompt sagte „nenne die Quellen als [1], [2]“ ohne Bedingung; das kleine Modell schreibt das dann auch ohne Werkzeug.
- Prompt: `system_full.{de,en}.txt` (eine Zeile unter „Ehrlich bleiben“), `system_short.*`, `TOOLS_DE/EN` („Hast du ein Werkzeug benutzt, …; ohne Werkzeug: keine [1] …“) und `TOOL_NUDGE_DE/EN` (Hinweis vor der Antwort, nicht im Cache). Assets in `core-llm` neu kopiert (`PromptAssetsTest`).
- Nachbearbeitung: neu `CitationCleaner` (feature-chat), im `ChatEngine` auf den Live-Text (`TurnUpdate.Streaming`) und die fertige Antwort (`TurnUpdate.Finished`) angewendet, mit der Zahl der Quellen dieses Zugs. Ohne Quellen: Marker `[1]`, `[2, 3]`, `[1][2]`, `[1-3]`, `【1】`/`【1†…】` weg (samt Leerzeichen davor), Quellenzeilen `[n] …`/`[n]: url` weg, Blöcke ab `Quellen:`/`Quelle:`/`Sources:`/`References:` samt Liste weg, Sätze/Klammern über Werkzeuge nur wenn sie Werkzeug-Wort + Verneinung + „nötig/benutzt/needed/used“ + „Frage/Antwort/aktuell/allgemein/question/answer“ enthalten (so bleibt „Dafür brauchst du kein Werkzeug, eine Münze reicht.“ stehen). Mit Quellen: nur Marker mit Nummer 1…n bleiben, Quellenzeilen mit ungültiger Nummer fallen weg. Immer: „**Fakt:**“/„Fakt:“/„Fact:“ am Zeilenanfang weg. Codeblöcke (auch noch offene beim Streamen), Inline-Code und Markdown-Links werden nie verändert; ohne Änderung kommt der Text unverändert zurück.
- Tests: `CitationCleanerTest` (15 Fälle: Oskars Beispiel, alle Marker-Formen, Quellenblöcke DE/EN, Werkzeug-Sätze DE/EN, Hammer-Fall, „Fakt:“, gültige/ungültige Nummern, Codeblock/Inline-Code/Links, Streaming, Stabilität) lokal mit kotlinc 2.1 + JUnit grün; `ChatEngineTest.answerWithoutToolsLosesFakeCitationsLiveAndAtTheEnd`; in `ToolsEveryTurnTest` bekommen die Fake-Werkzeuge eine Quelle (sonst würde ihr [1] jetzt zu Recht entfernt). ktlint 1.5.0 grün. Gradle nicht lokal → CI.
- Version: Extras-PR #36 setzt `hain.versionName=0.1.3.1`; auf main gibt es die Property noch nicht, darum hier nicht gesetzt. Nach dem Merge von #36 auf 0.1.3.2 setzen.
- Dateien außerhalb der Liste: `feature-chat/src/test/.../ToolsEveryTurnTest.kt` (Fake-Quellen), README.md (Hinweis „New in the latest test build“).
- Nächstes: Oskar testet mit der APK die Baum-Frage („Wie alt werden Bäume?“) und eine Wetterfrage (Quellen-Chips + [1] müssen bleiben).

## 2026-11-04 · T-216 (Zurück-Pfeil zeigt nach links) · review · harness: Hark · model: Hark · branch `hark/extras` (Draft-PR #36)
- Fehler: Zurück-Knopf in den Einstellungs-Unterseiten war `HainIcons.chevronRight` (zeigte nach rechts). Neu `ic_chevron_left.xml` (Lucide, Strich 2.1 wie die anderen, `autoMirrored`) + `HainIcons.back`, in `SettingsScreen` verwendet. Ganze App durchsucht: sonst kein Zurück-Knopf mit chevronRight (`PacksScreen` nutzt ✕, `chevronRight` bleibt als Zeilen-Chevron und gedreht im „nach unten“-Knopf des Chats).
- ktlint 1.5.0 + detekt 1.23.8 CLI lokal grün, Gradle nicht lokal → CI, APK, Screens.

## 2026-11-04 · 3b-2 + 4b-2 (0.1.4 – Werkzeuge Rest) · review · harness: Hark · model: Hark · branch `hark/day-3b2-4b2`

| task | status | check result | files outside list (reason) | open issues |
|---|---|---|---|---|
| T-068a | review | `TimeParserTest.repeatDays`, `PhoneToolsTest.repeatingAlarm` + JSON | `PhoneAction` (`days`, `calendarDays`), `ConfirmCard` (Tage), `SystemPromptSource` (PHONE_DE/EN) | – |
| T-068b | review | `ActionWordingTest`, `PhoneActionsTest` (Antwort „vorbereitet“) | `ChatEngine.cleaned`, `PhoneTools.TAP_HINT` | Regel ersetzt nur Partizipien/Hilfsverb-Formen, nicht jede Umschreibung |
| T-068c | review | `SettingsRepositoryTest.phoneActionsAskFirstByDefault` | `HainSettings`, `SettingsRepository`, `SettingsActions/Pages`, `ChatExtras`, `ChatScreen` (nur frische Antworten), `MainActivity` | Countdown nicht als UI-Test |
| T-068d | review | `PhoneIntentsTest` (Robolectric, sdk 34) | – | `launch()` (kein App-Fall) nicht getestet |
| T-060a | review | `HttpNetworkGateTest.brainDownloadStreamsThroughTheGate`, `streamKeepsOfflineModeAndLogsFailures` | `NetworkGate` (`stream`, `NetworkGates`), `AppModule`, `ModelDownloadWorker` | Worker nach Prozess-Neustart ohne Activity: Gate noch nicht gesetzt → Download ohne Log-Eintrag |
| T-060b | review | `WebSearchSwitchTest`, `SearchConsentTest` | `ToolRouter` (`schemas(offline, webSearch)`), `ToolContext`, `ChatEngine`, `ToolTimeline`, Settings | „Erst fragen“ erkennt das Ja per Wortliste (DE/EN) |
| T-064a | review | `InfoCardsTest` (Parser, gespeichert in `sources_json`), `WeatherToolTest` (card) | `ToolResult.card`/`InfoCard`, `ChatEngine/ChatStore/ChatViewModel/ToolStepsJson/RoomChatPersistence`, `ChatScreen`, neu `core-cards` res | Karten nur Wetter + Wikipedia; höchstens 3 pro Antwort |
| T-064b | review | `WeatherToolTest.noPlaceUsesTheAllowedLocation`, `noPlaceWithoutLocationAsksForThePlace` | `AndroidManifest` (`ACCESS_COARSE_LOCATION`), neu `WeatherLocation`, `MainActivity` (Permission-Launcher) | nur letzter bekannter Standort, keine neue Ortung |

- Gradle lokal nicht gestartet (qemu, paralleler Agent): ktlint 1.5.0 + detekt 1.23.8 CLI lokal, `tools/check_strings.py` grün; Rest über CI, APK, Screens.
- Version `hain.versionName=0.1.4` (VERSIONING.md: Häppchen 3b-2 + 4b-2).
- README (Funktionstabelle Werkzeuge ✅ + „Neu“), Website EN + /de (Wikipedia & Wetter, Handy-Aktionen, Netzwerk-Log), `TOOLS.md`, `SETTINGS.md` nachgezogen.
- Nicht gemacht: Favicons (T-064c, Bilder müssten durchs Gate), ConfirmCard nach core-cards (T-068e, reiner Umbau ohne Nutzen für 0.2), Host-Test mit echtem LFM3 für `days`/Wetter ohne Ort (kein Host-Lauf in diesem Durchgang).
- Nächstes: Oskar testet auf dem Handy: „Weck mich werktags um 6:30“, Schalter „Handy-Aktionen bestätigen“ aus, „Wie wird das Wetter?“ mit Standort, Websuche „Erst fragen“.

## 2026-11-04 · T-111, T-112, T-113, T-115 (0.1.7 – Gedächtnis) · review · harness: Hark · model: Hark · branch `hark/day-7` (Draft-PR #38)

| task | status | check result | files outside list (reason) | open issues |
|---|---|---|---|---|
| T-111 | review | `MemoryDaoTest` (Einfügen, Suche FTS, Bearbeiten/Löschen hält den Index aktuell, Themen), `HainMigrationTest` (v2-Datei aus `2.json` → v3) | – | `schemas/…/3.json` entsteht beim ersten Gradle-Build und sollte nachgetragen werden; `memory_fact_vec` erst mit T-110 |
| T-112 | review | `ExplicitMemoryTest` (DE/EN merken, „vergiss nicht“, vergessen, 9 Nicht-Befehle, Kürzen), `MemoryStoreTest` (Duplikate, Vorschläge, Bearbeiten/Löschen/Rückgängig, Vergessen, Aus, Export/Import), `RememberToolTest` | `core-tools/.../HainTools.kt` (`extra`-Werkzeuge), `feature-chat/.../ToolTimeline.kt` + Strings („Gemerkt“), `app/.../AppModule.kt`, `MainActivity.kt` | „Gemerkt ✓ Rückgängig“-Chip unter der Antwort fehlt (Chat-UI), dafür Werkzeugzeile + Pims Bestätigung |
| T-113 | review | `RankingTest` (Wortstämme, Stoppwörter, passende Erinnerung gewinnt, Vorschläge nie, alte Treffer zählen, Budget, Hybrid-Formel), `MemoryTurnTest` | – | ohne Embedder nur Stichwörter („Abendessen“ findet „Pilze“ nicht) |
| T-115 | review | `MemoryListTest` (Tabs, Suche, Datum DE/EN); Screenshots `memory_light/dark`, `memory_suggestions_light`, `memory_empty_light` (screens.yml) | `feature-settings` (Kachel „Gedächtnis“ im Hub, `onOpenMemory`), `.github/workflows/screens.yml` (+ `:feature-memory`) | Themen-Tab fehlt (keine Themen ohne T-114) |

- Wie es zusammenhängt: `MemoryTurn.hint()` hängt sich an den bestehenden Zug-Hinweis (`turnHint` in `MainActivity`): ein „merk dir …“ am Anfang der neuesten Nachricht wird sofort gespeichert (bei „Nochmal“ nicht doppelt) und Pim bekommt „Du hast dir gerade gemerkt: …“; sonst die passenden Erinnerungen + eine Zeile zum `remember`-Werkzeug. Der gecachte System-Prompt bleibt gleich. „Über dich“ ist weiter die Einstellung `about_me` (keine eigene Tabelle `memory_profile`). An/aus in SharedPreferences `hain_memory`.
- DB-Version: 3. Andere offene Branches (3b-2/4b-2, 5, 6) können ebenfalls Version 3 nehmen → wer später gemergt wird, nimmt die nächste freie Nummer.
- Gradle nicht lokal (Vorgabe): reine Logik (core-memory, 28 Tests) lokal mit kotlinc 2.1 + JUnit grün, ktlint 1.5.0 + detekt 1.23.8 CLI grün, `check_strings.py` grün; Rest über CI, APK, Screens.
- Version: `hain.versionName=0.1.7`.
- Nächstes: Oskar testet „Merk dir, dass ich keine Pilze mag“ → neuer Chat „Rezept mit Pilzen?“ → „Vergiss, dass ich keine Pilze mag“; dann T-110/T-114.

## 2026-11-04 · T-071 bis T-074 (0.1.5 – Nachrichten) · review · harness: Hark · model: Hark · branch `hark/day-5` (Draft-PR #39)

| task | status | check result | files outside list (reason) | open issues |
|---|---|---|---|---|
| T-071 | review | `NewsRepositoryTest` (14 Feeds aus den echten Fixtures, Zweck NEWS, CORRECTIV-Filter, ETag/If-Modified-Since + 304, Dedup 24 h, 7 Tage, kaputter Feed nach 3×, Modi, eigene Feeds prüfen/umbenennen/löschen, offline = Cache, Suche, JSON-Rundreise) | `NetworkGate.kt` (`GateResponse.headers`, `notModified`), `HttpNetworkGate.kt` (Header lesen), `NetworkLogSection.kt` + Strings (Filter/Zeile „Nachrichten“), `AppModule.kt` (Repository) | Speicher = eine JSON-Datei statt Room-Tabelle `news_item` (keine Migration nötig); Suche per Stichwort statt FTS5; kein WorkManager-Hintergrundabruf, kein Einstellungs-Schlüssel `news_refresh_minutes`/`news_languages` (fest 30 min, beide Sprachen) |
| T-072 | review | `NewsToolTest` (Zeilenformat mit Ortszeit, max. 8, Anriss 200 Zeichen, Sprache, `not_found`, Router nur mit Repository, Fallback Websuche), `NewsTimeTest`, `NewsCardJsonTest` | `Tools.kt` (`NewsHeadline`, `ToolResult.news`), `HainTools.kt`, `ChatEngine`/`ChatStore`/`ChatViewModel`/`ToolStepsJson`/`RoomChatPersistence`/`ChatScreen`/`ToolTimeline` (Karte durchreichen und speichern), `Hain3DIcon` + `icl_newspaper`, `SystemPromptSource` (news im Werkzeug-Satz), `MainActivity` | Karte zeigt höchstens 5 Schlagzeilen; Quellen-Chips zusätzlich wie bei der Websuche |
| T-073 | review | `NewsScreenTest` (Robolectric: genau 3 Modi, kein Schalter, eigene-Feeds-Seite ohne Standard-Feeds), `NewsViewModelTest`, `NewsGroupsTest`; Screenshots `news_light/dark`, `news_summary_light`, `news_feeds_light` (`screens.yml` um `:feature-news` erweitert) | `ChatScreen`/`ChatListDrawer`/`DrawerActions` (Zeile „Nachrichten“), `MainActivity` (Overlay), `feature-news/build.gradle.kts` | Kein Pull-to-refresh (Knopf oben rechts), keine Vorschaubilder (kein Bild-Lader im Projekt), Link öffnet im Browser statt Custom Tab (androidx.browser nicht in UI_LIBRARIES.md) |
| T-074 | review | `SummarizeArticleTest` (5 Punkte, Gate-Zweck NEWS, Paywall 403, Abo-Wand, offline, kein Gehirn, Aufzählungen parsen), VM-Test mit Fakes | `app/.../NewsBrain.kt` (ein Gehirn-Aufruf ohne Denken, lädt das Gehirn bei Bedarf) | Läuft gerade eine Chat-Antwort, wartet die Zusammenfassung nicht darauf (ein Gehirn, eine Generierung) – selten, aber möglich |

- Gradle lokal nicht gestartet (qemu): ktlint 1.5.0 + detekt 1.23.8 CLI lokal grün, `tools/check_strings.py` grün, Rest über CI (CI, APK, Screens).
- Zwischenstand-Commit T-073 baut erst zusammen mit T-074 (ViewModel kennt `SummarizeArticle`).
- `hain.versionName` = 0.1.5 (gradle.properties). README (Funktionstabelle, deutscher Absatz) und Website (EN + /de: Nachrichten = „Schon da“) aktualisiert, `NEWS.md` §7 beschreibt die Abweichungen.
- Nächstes: Oskar testet die APK: Seitenleiste → Nachrichten, Modus wechseln, eigenen Feed hinzufügen, „Zusammenfassen“ (mit Gehirn), im Chat „Was gibt es Neues zum Streik?“.

## 2026-11-04 · T-130, T-131 (0.1.6 – Antwort-Karten) · review · harness: Hark · model: Hark · branch `hark/day-6`

| task | status | check result | files outside list (reason) | open issues |
|---|---|---|---|---|
| T-130 | review | `CardParserTest` (19), `AnswerPartsTest` (10), `CardStateTest` (3) lokal mit kotlinc 2.1 + JUnit grün | `AnswerParts.kt`, `CardText.kt`, `CardState.kt`, `ModelCard.kt` (gleiches Paket) | – |
| T-131 | review | `CardGrammarTest`, `CardActionsTest` (feature-chat), `python3 tools/check_gbnf.py` ok (17 Regeln, 5 gute / 7 schlechte Beispiele); Screenshots `cards_light/dark`, `cards_list_light` (screens.yml) | `ChatScreen.kt` (AnswerBody statt AssistantBlock, CardHandler), `CardHandler.kt`, `ChatStore.kt` (`cardState`), `RoomChatPersistence.kt` (`cards_json`, Spalte gab es schon → keine Migration), `ChatViewModel.kt` (`updateCards`, `ask`), `Messages.kt` (`AnswerActionRow` herausgezogen), `prompts/system_full.*` + Assets, `tools/check_gbnf.py`, README, Website, `HAPTICS.md`, `CARDS.md`, `gradle.properties` | GBNF zur Laufzeit nicht aktiv (JNI: eine Grammatik pro Generierung, kein Umschalten mitten in der Antwort); Chart/Image nicht gerendert; kein C++-Compiler in der Sandbox → `llama-gbnf-validator` ersetzt durch eigenes Python-Prüfskript |

- Ablauf im Chat: `AnswerBody` zerlegt die Antwort mit `AnswerParts.split(text, streaming)`; ohne Karte bleibt es der bisherige `AssistantBlock` (kein Unterschied für normale Antworten). Eine offene Karte wächst beim Streamen mit, Knöpfe sind bis zum Ende gesperrt. Kaputte Karte → bleibt als Codeblock stehen.
- Haken: `CardState` (`{"checked":{"karte.block.punkt":true}}`, nur geänderte Punkte) über `ChatViewModel.updateCards` → `cards_json`. Knopf `ask:"…"` → `ChatViewModel.ask` (setzt Eingabe + sendet; nicht während Pim antwortet). Kopieren/Teilen der ganzen Antwort = Klartext mit Haken (`UiMessage.plainAnswer`).
- Prompt: neuer Abschnitt „# Karten“/„# Cards“ mit einem Beispiel (≈100 Token mehr im gecachten System-Prompt). Mit echtem LFM3 auf dem Host nicht geprüft (kein C++-Build in dieser Sandbox) → Oskar bitte mit „Schreib mir eine Einkaufsliste für Pfannkuchen“ und „Lernplan Mathe bis Freitag“ testen.
- Version `hain.versionName=0.1.6`. Gradle lokal nicht gestartet: ktlint 1.5.0 + detekt 1.23.8 CLI lokal grün, Rest über CI (CI, APK, Screens).
- Parallel laufen `hark/day-3b2-4b2`, `hark/day-5`, `hark/day-7`: Konflikte in README/STATUS/HANDOFF_LOG/Website/gradle.properties beim Mergen → beide Blöcke behalten, Version = höchste.

## 2026-11-04 · Werkzeuge 4d – alle Tools + Prompts · review · harness: Hark · branch `hark/day-4d`

| task | status | check result | files outside list (reason) | open issues |
|---|---|---|---|---|
| T-180 | review | TOOLS.md §0 table (9 tools, params, typical errors) | – | – |
| T-181 | review | `tools/eval/tool_eval.jsonl` (52 questions), `tool_eval.py` host harness, `results/baseline.json` | – | eval uses mock tool results (no network); LFM3 on the host, not the phone |
| T-182 | review | `ToolSpecsTest`, `CompactTest`, `ClockAnswerTest`, `CalculatorTest.spokenSumsAreAnsweredAtOnce` | `HainTools` (clock answer), `PhoneTools` (`say`), all tool classes (schema from `tools.json`) | `wikipedia.lang` and `set_timer.seconds` no longer shown to the brain (still accepted) |
| T-183 | review | `ToolCallFixerTest`, `RepairingToolCallParserTest`, `ToolRouterTest` (retry, fix), `RobustCallsTest`, `StreamSplitterTest` (repair) | `StreamSplitter` (repairing parser by default) | – |
| T-184 | review | `PromptTemplatesTest` (sections, budget, every tool named, no quoted replies), `FollowUpTest` | `SystemPromptSource` (templates from assets), `ChatEngine` (follow-up/link hint, clock answer, dedupe) | example dialogs measured worse → left out (TOOLS.md §8) |
| T-185 | review | before/after table in TOOLS.md §8 and the PR: right tool 88.5 → 96.2 %, args 94.3 → 97.5 %, hallucinations 6 → 1 | README, website (tool text), STATUS, TASKS | host timing not comparable (host was busy) |
| T-186 | review | unit tests above | – | – |

- Next: device test of tools (Oskar), 3b (weather card), 5 (news).

## 2026-11-04 · T-055 (Gehirn-Auswahl in der Kopfzeile) · review · harness: Hark · model: Hark · branch `hark/picker-perf`
- Neu: `BrainChoice` (core-models, reine Regeln: aktives Gehirn = gewähltes wenn installiert → Standard → erstes installiertes; Empfehlung = größtes Gehirn mit DeviceCheck GOOD, sonst kleinstes TIGHT, freier Platz zählt dafür nicht; Menüzeilen installiert zuerst), `BrainChoiceStore` (core-common, eigene DataStore-Datei `datastore/brain.preferences_pb`, Schlüssel `brain_id`, damit `SettingsRepository`/`HainSettings` unberührt bleiben), `ModelPicker.kt` (feature-chat, `HainPopupMenu` = Milchglas, unter dem Titel), `BrainPicking` (app, Glue), `ChatViewModel.switchBrain()` (stoppt laufende Antwort, lädt `modelPath()` neu, wärmt vor).
- `TopBar` (core-ui) hat optional `onTitleClick`/`titleClickLabel`/`titleAccessory` (Titel wird ≥48 dp-Knopf mit kleinem Chevron); ohne die Parameter unverändert.
- Fortschritt: dünne grüne Linie unter der Kopfzeile, solange ein Gehirn mit bekanntem Fortschritt lädt (auch beim ersten Laden), Untertitel zeigt weiter „Gehirn wacht auf … x %“.
- Eigenes Modell aus dem Entwicklermodus hat weiter Vorrang; das Menü sagt das dann in einer Zeile.
- „Schon installiert?“ für Chat/Onboarding = irgendein Gehirn aus dem Katalog (vorher nur das Standard-Gehirn).
- Tests: `BrainChoiceTest` (12), `BrainChoiceStoreTest` (2), `ModelPickerTest` (Robolectric, Fake-Katalog, 5), `ChatViewModelTest` (+3), Screenshots `chat_brain_picker_light/dark` (screens.yml).
- Dateien außerhalb der Liste: `Scaffolding.kt` (TopBar), `ChatScreen.kt` (Einhängepunkt + Ladelinie), `MainActivity.kt`, `AppModule.kt`, `FakeLlm.kt`, Strings EN/DE.

## 2026-11-04 · T-190 Rest (R8 + Baseline-Profil + Release-Smoke-Test) · review · harness: Hark · model: Hark · branch `hark/picker-perf`
- Release: `isMinifyEnabled = true`, `isShrinkResources = true`, `app/proguard-rules.pro` (JNI `NativeLlm` + Callbacks `onToken`/`onProgress`, SQLCipher `net.zetetic.database.**`, Room `*_Impl`/Entities/DAOs, kotlinx.serialization, WorkManager-Worker). Release bleibt unsigniert; nur mit `-Phain.signReleaseWithDebugKey=true` (CI-Test, eigenes Handy) mit dem öffentlichen Debug-Schlüssel signiert.
- Baseline-Profil von Hand: `app/src/main/baselineProfiles/baseline-prof.txt` (Start + Chat-Scroll + Markdown + Milchglas), `androidx.profileinstaller` 1.4.1 (Apache-2.0, LICENSES.md). Kein generiertes Profil (Macrobenchmark braucht API 33+/Root, arm64-APK läuft im x86-Emulator nur übersetzt) → SPEED.md §5.
- CI: neuer Job `release-smoke` in `screens.yml` + `tools/ci/release_smoke.sh`: Release-APK bauen, Inhalt prüfen (`baseline.prof`, `libhain_llm.so`), Android-11-x86_64-Emulator (ARM-Übersetzung), installieren, Start + Prozess lebt + Crash-Log leer, installiertes ART-Profil zählen, Kaltstart (Median von 7) Nightly-Debug vs. Release, jeweils wie installiert und nach `compile -m speed-profile`. Bericht in der Job-Zusammenfassung + Artifact `release-smoke` (APK, mapping.txt, Logs).
- Debug `app.hain.debug` unverändert (kein R8); die Nightly-Debug-APK von main wird im Smoke-Test mit installiert und gestartet.
- CI-Ergebnis (`release-smoke`, 2 Läufe): Release-APK startet und läuft weiter ✅, Nightly-Debug `app.hain.debug` ✅; installiertes Profil ~29.000 Methoden, ~4.200 davon Hain-Code (über `mapping.txt` zurückgerechnet). Kaltstart Median: wie installiert 911→900 ms bzw. 1227→1180 ms, nach `compile -m speed-profile` 978→932 ms bzw. 1145→1065 ms – kleiner Gewinn im Rauschen des CI-Emulators; Messung am echten Handy offen (SPEED.md §5).
- Version: Vorgabe 0.1.3.3 liegt unter main (0.1.4, PR #40) → `hain.versionName` bleibt 0.1.4, Integration setzt die nächste Nummer.
- Screenshots `chat_brain_picker_light/dark` nach `docs/design/screens/`.

## 2026-11-04 · T-056 + T-054 (0.1.8 – Privat & Export) · review · harness: Hark · model: Hark · branch `hark/day-8a`

| task | status | check result | files outside list (reason) | open issues |
|---|---|---|---|---|
| T-056 | review | `PrivateChatTest` (keine DB-Aufrufe, nicht in Suche/Seitenleiste, Löschen beim Verlassen/Umschalten/anderen Chat öffnen, Werkzeug-Schleife wie gewohnt), `AppLockGateTest` (sofort/1/5 min, Uhr rückwärts, System-Prompt sperrt nicht erneut, Dateiauswahl/Teilen sperrt nicht außer > 5 min), `SettingsRepositoryTest` (Sperr-Einstellungen bleiben beim Zurücksetzen) | `MainActivity` (jetzt `FragmentActivity`, Sperre/FLAG_SECURE/ZIP), `HainSettings`/`SettingsRepository`, `SettingsActions/Extras/Pages`, `core-ui` (`ic_eye_off`, `LocalExternalLaunch`), `libs.versions.toml`, `app/build.gradle.kts` | Gedächtnis (Häppchen 7) muss `Chat.isPrivate` / `ChatStore.isPrivate()` beachten; ohne Bildschirmsperre am Handy öffnet sich Hain (sonst Aussperren) |
| T-054 | review | `ChatMarkdownTest` (Format, Weglassen von Denken/Fehlern, sichere Links/Dateinamen, ZIP ohne private/leere Chats, eindeutige Namen), `MarkdownBlocksTest`, `PdfLayoutTest` (Umbruch, Seitenaufteilung, nichts außerhalb des Satzspiegels, Rolle nie allein unten, Code über Seitengrenze mit Hintergrund) | `ChatScreen` (Teilen-Knopf + Privat-Knopf), `ChatListDrawer`/`DrawerActions` (⋯ → Exportieren), `feature-chat/AndroidManifest.xml` (FileProvider) | JSON-Export laut Sprint-Liste durch PDF ersetzt; PDF-Inline-Fett/Kursiv wird als normaler Text gesetzt |

- Privater Chat: `Chat.isPrivate`, `ChatStore.createPrivate/discardPrivate/isPrivate`; alle Schreibwege prüfen `writeFor(chatId)`. Kein Auto-Titel. „Verlassen“ = neuer Chat, anderer Chat, „Beenden“, Augen-Knopf, App geschlossen (ViewModel weg).
- App-Sperre: Logik rein in `core-common/api/lock/AppLockGate.kt` (JVM-Tests), Android-Teil `app/lock/AppLock.kt` (BiometricPrompt im onCreate, `BIOMETRIC_WEAK or DEVICE_CREDENTIAL`, weil STRONG+Credential auf Android 10 nicht erlaubt ist), Uhr = `SystemClock.elapsedRealtime()`. Schalter, Verzögerung und Screenshot-Sperre werden zusätzlich in `hain_start` gespiegelt, damit ein Kaltstart sofort gesperrt ist und FLAG_SECURE vor dem ersten Bild gilt. Einschalten fragt einmal nach Fingerabdruck/PIN.
- Export: Markdown-Format und PDF-Satz in reinen Klassen (`ChatMarkdown`, `MarkdownBlocks`, `PdfLayout`), Zeichnen in `ChatPdf` (Hanken Grotesk/JetBrains Mono aus core-ui). Teilen schreibt nach `cache/exports` (ältere als 10 min werden gelöscht).
- Gradle lokal nicht gestartet (Sprint-Regel): ktlint 1.5.0 + detekt 1.23.8 CLI + `check_strings.py` lokal grün, Rest über CI.
- Version `hain.versionName=0.1.8`. README (Tabelle + „New in the latest test build“), Website (EN + /de) und SETTINGS/MEMORY/DATA/LICENSES aktualisiert.
- Konflikte erwartet mit `hark/day-8b`: `ChatScreen.kt`, `website/src/content.ts` (Zeile „Export & share“ aufgeteilt), README, STATUS/HANDOFF_LOG/TASKS, `gradle.properties`, `libs.versions.toml`.

## 2026-11-04 · T-057 (Rest), T-058, T-144 (0.1.8 – Häppchen 8b) · review · harness: Hark · model: Hark · branch `hark/day-8b`

| task | status | check result | files outside list (reason) | open issues |
|---|---|---|---|---|
| T-057 | review | `BranchesTest` (Pfad, Auswahl, Info, Schleifen, Store, Neustart), `ChatViewModelTest` (Bearbeiten/Nochmal behalten Versionen, Umschalten, Warteschlange: wartet → danach senden, ✕, Stopp, neuer Chat leert), `BranchMigrationTest` (v2-Datei → v4, Eltern nach id, zweimal laufen) | `core-data` (`MessageEntity.selected_at`, `HainDatabase` v4 + `BranchSql`), `ChatStore`, `RoomChatPersistence`, `Composer` (Senden während Antwort, Fokus) | Folgefragen-Vorschläge; `schemas/…/4.json` erzeugt erst der Gradle-Build (nicht eingecheckt) |
| T-058 | review | `ViewportRules` rein; Screenshot `branches_queue` | `ChatScreen.kt` (MessageList nutzt ChatViewport), `feature-chat/build.gradle.kts` (haze für `hainGlass`) | Shared Element Composer→Blase nicht gemacht |
| T-144 | review | `LaunchRequestTest` (Teilen-Text, Titel+Link, leer/falscher Typ, unsichtbare Zeichen, Kürzen, Widget/Kachel, neuester Chat fürs Widget) | `MainActivity` (singleTask, `addOnNewIntentListener`, Widget neu zeichnen), Manifest, `libs.versions.toml` (Glance 1.1.1), `LICENSES.md` | Bilder/PDF teilen (braucht Häppchen 9), Kachel → Sprachmodus (Häppchen 10); privater Chat/App-Sperre (8a): Widget soll private Chats nicht zeigen und bei App-Sperre keinen Titel |

- Verzweigungen: jede Nachricht hat einen Eltern-Eintrag (`parent_id`, erste = 0), Geschwister = Versionen. Angezeigt wird ab der Wurzel jeweils das zuletzt gewählte Kind (`selected_at`, sonst das neueste). Alte Chats: Migration füllt `parent_id` mit der vorigen Nachricht (nach id), im Code fängt `Branches.normalize` dasselbe ab. `Chat.messages` bleibt der sichtbare Pfad (alle anderen Stellen unverändert), `Chat.hidden` hält die anderen Versionen.
- Warteschlange: nur im Speicher; beim Chatwechsel/neuen Chat verworfen; nach einem Fehler kommen die Texte zurück ins Eingabefeld statt automatisch zu senden.
- **Schema-Konflikt mit Häppchen 7 (#38):** #38 nimmt Version 3 (Gedächtnis). Dieser Branch nimmt 4 und hat einen leeren `MIGRATION_2_3`-Platzhalter mit demselben Namen. Beim Mergen: `MIGRATION_2_3` aus #38 behalten, `MIGRATION_3_4` von hier dazu; `addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)`. Wer eine 8b-Test-APK installiert hatte (DB v4 ohne Gedächtnis-Tabellen), muss nach dem Merge die App-Daten löschen.
- Gradle lokal nicht gestartet (Sprint-Regel); ktlint 1.5.0 + detekt 1.23.8 CLI + `check_strings.py` lokal grün, Rest über CI.

## 2026-11-04 · T-140 + T-141 (Häppchen 10 Sprache, 0.1.10) · review · harness: Hark · model: Hark · branch `hark/day-10`

| task | status | check result | files outside list (reason) | open issues |
|---|---|---|---|---|
| T-140 | review | `AudioMathTest`, `DictationControllerTest`, `MoonshineTokenizerTest`, `PackProgressTest`, `VoicePackCatalogTest`, `ChatVoiceUiTest` lokal (kotlinc 2.1 + JUnit) grün; Kotlin-Pipeline `MoonshineStt` auf der Host-JVM mit ORT 1.28.0 und den echten Packs: DE „Hallo, ich bin Pim. Ich wohne in deinem Handy …“ (= Python-Referenz), EN ebenso | `core-models` (Mehrdatei-Packs: `packDir`, Ketten-Download, `PackProgress`, `lang`), `catalog.json`, `core-ui/Composer.kt` (`trailing`-Slot), `ChatScreen.kt` (Hook: Panel, Mikro, LocalChatVoice), `MainActivity.kt` (`chatVoice`), `libs.versions.toml`, `gradle.properties` | Gerätetest (Mikro, Latenz auf dem Handy), Live-Teilergebnisse, Silero-VAD |
| T-141 | review | `SpeechTextTest` (19 Fälle DE/EN), `ReadAloudControllerTest`, `LazySynthTest` | `ChatScreen.kt` (`ReadAloudAction` in der Antwortreihe) | Pocket TTS = T-143; Einstellung Pim/Neutral, Hüllkurve für Pims Sprech-Bewegung |

- Entscheidungen: sherpa-onnx gibt es nicht auf Maven → Moonshine direkt mit ONNX Runtime (MIT). **ORT auf 1.28.0 gepinnt**: ab 1.29 enthält das Android-AAR Microsoft-1DS-Telemetrie (`TelemetryInitializer`, `mobile.events.data.microsoft.com`); zusätzlich entfernt das Manifest von core-voice den Provider. Häppchen 9 (OCR) muss dieselbe Version nehmen.
- Modelle: `Masterx/moonshine-streaming-small-de-ONNX@5d6dadc…` und `Masterx/moonshine-streaming-small-ONNX@dafc41f…` (MIT, Export der offiziellen moonshine-ai-Gewichte mit dem offiziellen Rezept); SHA-256 aus den LFS-OIDs bzw. selbst berechnet und auf dem Host gegengeprüft. Offizielles EN-ONNX von moonshine-ai gibt es nur als `.ort` und fp32-Decoder (größer), offizielles DE nur als safetensors.
- Pocket-TTS-Stand: öffentliche ONNX-Exporte nur Englisch mit fest eingebauter Stimme `alba` (soniqo, Exporter csukuangfj/pocket-tts-onnx-export); Arms DE-Paket braucht deren geschlossene Engine. Für „anna“ + Deutsch braucht es einen eigenen Export (T-143).
- Gradle nicht lokal (Sprint-Regel) → CI. ktlint 1.5.0 + detekt 1.23.8 CLI auf allen geänderten Dateien grün, `check_strings.py` OK.

## 2026-11-04 · T-110, T-114, T-116, T-117 (0.1.7.1 – Gedächtnis Teil 2) · review · harness: Hark · model: Hark · branch `hark/day-7b` (Draft-PR #46, Basis `hark/day-7`)

| task | status | check result | files outside list (reason) | open issues |
|---|---|---|---|---|
| T-110 | review | `VectorsTest` (Kosinus, Kalibrierung, Bytes), `HybridRecallTest` (Gewichte der Spec, Bedeutung ohne gemeinsame Wörter, ohne Paket nur Stichwörter, Vektor neu nach Bearbeiten, nächster Fakt), `HainMigrationTest.version3MigratesToVersion5WithVectors`; nativer Teil nur in CI (apk.yml) gebaut, auf dem Host mit echtem Modell gemessen (1024 Werte, ~45 ms/Fakt, 2 Kerne) | `hain_llm.cpp` (Init mit Mutex, 3 JNI-Funktionen), `NativeLlm.kt`, `MemoryDao`, `HainDatabase` (v5) | RAM neben dem Gehirn auf dem Handy nicht gemessen; kein instrumentierter Test |
| T-114 | review | `FactExtractorTest` (nur Vorschläge, Duplikate, update → Vorschlag mit `replaces` + Annehmen setzt `superseded_by`, Kosinus-Schwellen mit Embedder, Gedächtnis aus/Gehirn scheitert, strenges JSON, Wortähnlichkeit, ≤ 12 Nachrichten), `ExtractionAssetsTest` (Prompts = Assets, Akku-Regel), `MemoryTurnTest` | `LlmEngine`/`LlamaEngine` (`SamplingParams.grammar`), `RememberTool` (`onSaved`) | Läuft auf dem Gehirn des Chats (Cache danach neu); Worker ohne Gerätetest |
| T-116 | review | `MemoryListTest` (Themen, „Ersetzt“, Chips, Kachel-Zustand), `AnswerFooterTest`; Screenshots `memory_topics_light`, `memory_suggestions_replaces_dark`, `memory_smart_search_dark`, `memory_answer_chips_light` | `feature-chat` (`ChatExtras.answerFooter`, ChatScreen: Slot unter der Antwort), `feature-settings` (Beschreibung kind `embed`) | Chips nur im Speicher (nach Neustart weg) |
| T-117 | review | `CatalogParserTest.bundledCatalogHasTheEmbedderPack`, `ChatEngineTest.memoryLineFollowsTheLanguageHintOffTheMainThread`, `ChatViewModelTest.aFinishedAnswerTellsTheMemoryButAStoppedOneDoesNot` | `HainApp` (Hilt-Felder), `AppModule`, `MainActivity` | – |

- Ablauf: der vorige Lauf hatte den Code geschrieben, aber nichts committet (Sicherung `/workspace/work/hain-7b-wip/tree`); dieser Lauf hat ihn übernommen, lint-sauber gemacht, als T-110/T-114 committet und UI + Verdrahtung ergänzt.
- DB: Version 5. Version 4 gehört Häppchen 8b; hier ist `MIGRATION_3_4` ein leerer Platzhalter mit gleichem Namen (beim Mergen von 8b dessen Migration behalten, Platzhalter löschen). `schemas/…/5.json` aus dem CI-Artifact `room-schemas` eingecheckt.
- Abweichung von MEMORY.md §2.3: die Extraktion legt **alles** als Vorschlag ab (nicht nur Heikles) – Oskar entscheidet, ob Unheikles direkt aktiv werden soll. sqlite-vec nicht genutzt (Kosinus in Kotlin).
- Konflikt erwartet mit `hark/rest-tools-cards` (ändert ebenfalls `hain_llm.cpp`/`hain_session.h` und `LlmEngine` für Grammatik während der Antwort): dort nicht eingegriffen.
- Gradle nicht lokal (Vorgabe): ktlint 1.5.0 + detekt 1.23.8 CLI + `check_strings.py` lokal grün; Rest über CI, APK, Screens.
- Nächstes: Oskar lädt unter Einstellungen → Gedächtnis „Suche nach Bedeutung“, merkt sich „Ich mag keine Pilze“ und fragt „Was soll ich heute kochen?“ (Chip „1 Erinnerung genutzt“); nach einem Chat über Familie 2 min warten → Vorschläge.

## 2026-11-04 · T-120 bis T-123 (0.1.9 – Dokumente) · review · harness: Hark · model: Hark · branch `hark/day-9` (Draft-PR #49)

Neu aufgesetzt: Der erste Lauf verlor seinen Worktree (/tmp-Reset); übernommen wurden Modelle/Wörterbuch/Referenzskripte aus `ocr9/` und die teilweise geretteten Dateien (PDF, Text, Chunker, Fixer, Wörterbuch, Test-PDF). OCR-Pipeline, Datenbank, Werkzeug, UI und alle Tests neu geschrieben.

| task | status | check result | files outside list (reason) | open issues |
|---|---|---|---|---|
| T-120 | review | `PdfTextExtractorTest` (Robolectric, Fixture `rechnung.pdf`: 3 Seiten, Seite 2 = Scan), `TextFilesTest` | `gradle/libs.versions.toml` (pdfbox-android), `core-docs/consumer-rules.pro` | Seitenbild nur auf dem Gerät (PdfRenderer) |
| T-121 | review | `CtcDecoderTest`, `RecDictionaryTest` (echte `inference.yml`, 836 Zeichen), `DbPostProcessTest`, `ReadingOrderTest` (Zeilen, Absätze, 2 Spalten, Titel), `OcrImageTest`, `OcrPackTest` (Gate, SHA, Pack entfernen); JVM-Host-Lauf mit ORT 1.28.0: Rechnung + Zwei-Spalten-Seite 100 % | `core-docs/src/main/AndroidManifest.xml` (Telemetrie-Provider entfernt), `libs.versions.toml` (onnxruntime 1.28.0) | instrumentierter Test „≥ 95 % Zeichen“ auf dem Gerät steht aus (`check: device`) |
| T-122 | review | `GermanOcrFixerTest` (alle Spec-Beispiele auf dem echten Wörterbuch) | `docs/specs/LICENSES.md`, `assets/dict/README.txt` | – |
| T-123 | review | `ChunkerTest`, `DocPromptTest`, `DocAttachmentTest`, `SearchDocumentToolTest`, `DocsDatabaseTest` (Robolectric + bundled SQLite), `DocumentToolsTest` (core-tools), `DocMessagesTest`, `ChatViewModelDocsTest` | `core-tools` (`ToolContext.documents`, `ToolRouter.schemas(documents)`, `HainTools.router(extra)`, `Relevance` public), `feature-chat` (`UiMessage.attachments`, `ChatViewModel.attach/detach/documentPrompt`, `ChatEngine.respond(documents)`, `RoomChatPersistence` attachments_json, `ChatScreen` Büroklammer/Leiste/Meine Dateien, neue `ChatDocs.kt`, `DocsUi.kt`, `DocMessages.kt`, `strings_docs.xml` EN+DE, `ic_paperclip`), `MainActivity` | eigene DB `hain_docs.db` statt Tabellen in `hain.db` (Schema-Konflikte mit 7/Gedächtnis vermeiden); keine FTS/vec-Tabellen (BM25 im Speicher); Dokumente stehen an der Nachricht, die sie angehängt hat – fällt diese aus den letzten 24 Nachrichten, fehlt das Dokument im Prompt |

- Gradle lokal nicht gestartet: ktlint 1.5.0 + detekt 1.23.8 CLI grün; reine Kotlin-Tests (OCR, Fixer, Chunker, Prompt, JSON, Text) zusätzlich lokal mit kotlinc 2.1.20 + JUnit 4 gelaufen (48 grün); Rest über CI/APK.
- Strings in eigener Datei `feature-chat/src/main/res/values{,-de}/strings_docs.xml` (gleiche Schlüssel; weniger Merge-Konflikte). `tools/check_strings.py` prüft nur `strings.xml` → Parität von Hand geprüft.
- `hain.versionName=0.1.9`. README (Tabelle, „Neu“, Wissenspakete, Deutsch), Website EN + /de (Dokumente = „Schon da“), DOCS_OCR, DATA, LICENSES, STATUS, TASKS aktualisiert.
- Nächstes: Oskar testet die APK: Büroklammer → PDF mit Text („Wie viel muss ich zahlen?“), dann ein Foto einer Seite (→ „Texterkennung laden?“ → Laden), „Meine Dateien“ → Datei löschen, Pack entfernen.

## 2026-11-04 · T-068e, T-064c, T-132, T-133 (0.1.6.1 – Rest Werkzeuge + Karten) · review · harness: Hark · model: Hark · branch `hark/rest-tools-cards` (Draft-PR #44, gestapelt auf #41)

| task | status | check result | files outside list (reason) | open issues |
|---|---|---|---|---|
| T-068e | review | `ActionCardTest` (core-cards), `PhoneActionsTest` angepasst | `feature-chat` `ConfirmCard.kt` (nur noch Weiterleitung), `ToolSteps.kt`, Strings nach core-cards verschoben | – |
| T-064c | review | `FaviconsTest` (10), `SettingsRepositoryTest` (`source_icons`) | `NetworkGate` (Zweck `IMAGES`), `HainSettings`/`SettingsRepository`, `SettingsPages` (Privatsphäre), `ChatExtras`, `ToolTimeline`, `MainActivity` | – |
| T-132 | review | `CardChartTest` (14), `CardGrammarTest`, `python3 tools/check_gbnf.py` (21 Regeln ok) | Prompt DE+EN (Diagramm-Beispiel) | – |
| T-133 | review | Host: `core-llm/src/test/cpp/card_fence_test.cpp` (12 Prüfungen) grün, Grammatik + Wurzel mit echtem LFM3-Vokabular geladen; Native-Build in CI grün | `hain_session.h`, `hain_llm.cpp`, `NativeLlm.kt`, `LlamaEngine.kt` (liest `card.gbnf` aus den App-Assets) | Fence mitten in einem größeren Token → diese Karte ohne Grammatik (nachsichtiger Parser) |

- T-133 Ablauf: Antwort wird wie bisher gesampelt. Endet der Text genau mit einer Zeile „```card“, schaltet `CardFence` einen Grammatik-Sampler (card.gbnf + Wurzel `hain-card-root ::= root "\n"?`) zu; KV-Cache und Sampler-Kette bleiben. Nach dem schließenden „```“ (auch „```\n“ in einem Token) ist sie wieder aus. Lässt die Grammatik kein Token zu, geht sie für diese Karte aus (Log), der Parser liest dann nachsichtig.
- Host-Test mit echtem LFM3 (`lfm3.gguf`, eigener Harness über `hain::generate`, voller DE-System-Prompt inkl. Werkzeug-Text und Schemas, Temperatur 0.1, Seed 42):
  - Wecker „6:30 an Wochentagen“ → `set_alarm(time=06:30, days=Wochentag)` ✓ (`TimeParser` liest „wochentag“ als Mo–Fr).
  - „Weck mich jeden Samstag und Sonntag um 9 Uhr“ → **kein** Werkzeug-Aufruf, Pim schreibt eine Pseudo-Knopfzeile (auch vor diesem PR so; offen).
  - „Wie wird das Wetter morgen?“ (ohne Ort) → vorher Rückfrage „Wo bist du?“, mit dem neuen Hinweis im Werkzeug-Text `weather()` ohne place ✓. „Brauche ich heute einen Regenschirm?“ ✓, „…nächsten 3 Tage in Hamburg“ → `weather(place=Hamburg, days=3)` ✓. Zwei andere Formulierungen („Hain nimmt dann den Standort“) verwirrten das Modell („Ort Hain“) und wurden verworfen.
  - Karten: Einkaufsliste → gültige Checklisten-Karte ✓ (Grammatik an, 1 Karte). Liniendiagramm Lernstunden und Wetter-Höchstwerte als Balkendiagramm: **ohne** Diagramm-Beispiel im Prompt nie ein Diagramm (Markdown-Tabelle + „Chart type=line“ als Fließtext), **mit** Beispiel 3/3 ✓. Vergleich Zug/Auto → Markdown-Tabelle (keine Karte; wird als Tabelle gezeigt). Einwohner-Diagramm → Pim sucht zuerst im Web (richtig).
  - Gefunden + behoben: mit der Grammatik endete die Karte auf einem einzelnen „```“-Token, danach schrieb LFM3 einen falschen `<tool_call>` (weather/calculate). Seit die Wurzel „```\n“ erlaubt: kein falscher Aufruf mehr (r1, c1, c3 erneut geprüft).
- Prompt: „# Karten“/„# Cards“ hat jetzt ein zweites, kurzes Beispiel (Balkendiagramm, ≈30 Token mehr im gecachten System-Prompt). Werkzeug-Text DE/EN (`SystemPromptSource`): „weather für Wetter (nennt der Nutzer keinen Ort, rufe weather ohne place auf, dann gilt der Standort des Handys)“.
- Favicons standardmäßig an: geladen wird nur das Symbol der Website, die ohnehin gerade Quelle war (kein Dritt-Dienst wie Google S2/DuckDuckGo-Icons), über das NetworkGate (Netzwerk-Log, Online-Punkt, im Offline-Modus gesperrt), einmal pro Website und dann auf dem Handy gespeichert. Aus: Einstellungen → Privatsphäre → „Quellen-Bilder“, dann nur der erste Buchstabe.
- `hark/day-6` (166df06, enthält main mit #40) hineingemergt: nur Reihenfolge-Konflikte (README, STATUS, HANDOFF_LOG, core-cards strings, ChatStore, RoomChatPersistence, gradle.properties), unsere Seite behalten, Version `0.1.6.1`.
- Gradle lokal nicht gestartet: ktlint 1.5.0 + detekt 1.23.8 CLI, `tools/check_strings.py`, `tools/check_gbnf.py` lokal grün, Rest über CI, APK, Screens.
- Achtung Konflikte: `hark/day-7b` ändert ebenfalls `hain_llm.cpp` und `LlmEngine` (nativeGenerate-Signatur hat hier den neuen Parameter `cardGrammar`).

## 2026-11-04 · 0.1.10.1 – main grün + Werkzeuge/Karten (#44) · review · harness: Hark · model: Hark · branch `hark/main-fix`

| task | status | check result | files outside list (reason) | open issues |
|---|---|---|---|---|
| main-CI | done | Job „assemble · test · lint · strings“ auf 65bbc57 scheiterte am NDK-Download (`ZipFile unknown archive`), kein Code-Fehler → Job neu gestartet | – | – |
| Room-Schemas | done | `core-data/schemas/.../4.json` (Export am v4-Stand 90e82ed) neu, `5.json` durch den Export am grünen Head f8835fe ersetzt (mit `message.selected_at`) | Schemas | – |
| #44 | review | gemergt: T-068e, T-064c, T-132, T-133 | `LlamaEngine` (nativeGenerate: `params.grammar` aus 7b **und** `cardGrammar` aus #44; Karten-Grammatik nur ohne Ganz-Antwort-Grammatik, also nie bei der Gedächtnis-Extraktion), Werkzeug-Hinweis „Wetter ohne Ort“ aus #44 in `prompts/tools.<de|en>.txt` (4d hat die Prompt-Konstanten in assets verlegt) | Gerätetest Karten-Grammatik + Favicons |

- Schon in #50 enthalten (f8835fe): Anhänge aus privaten Chats nur flüchtig (`EphemeralDocs`, negative IDs, nie in `hain_docs.db`), Artikel aus NewsCard/Nachrichten über `LocalExternalLaunch` (App-Sperre greift nicht).
- Konflikte: ChatStore/RoomChatPersistence (Karten + Verzweigungen + Anhänge), ChatExtras (answerFooter + favicons), MainActivity (DocLibrary + Favicons), Strings (`chat_action_*` → `cards_action_*` in core-cards), README/Website/TASKS/STATUS.
- `hain.versionName=0.1.10.1`.

## 2026-11-05 · 0.1.10.2 – UI-Feinschliff · review · harness: Hark · model: Hark · branch `hark/ui-polish` (PR #52)

| task | status | check result | files outside list (reason) | open issues |
|---|---|---|---|---|
| T-230–T-233 | review | Screens (CI) grün, `TopBarTitleTest`, `PimStatusTest`, `HainGlassTest`, `MenuBackdropTest` | – | – |
| T-236 | review | `NewsQueryTest`, `ToolRouterTest`, `RobustCallsTest` (Erwartung an die eine Router-Wiederholung nach Netzfehler angepasst) | – | – |
| T-237 | review | CI rot durch `checkDebugAarMetadata`: zstd-jni 1.5.7-25 verlangt `minCompileSdk=37`. Alle AARs geprüft: bis **1.5.7-12** ohne minCompileSdk und 16-KB-ausgerichtet → gepinnt | LICENSES.md (Version) | Gerätetest mit echtem ZIM |
| T-234 | review | `PackCategoryTest`; `ModelPickerTest` folgt dem neuen Text „More brains in Models & packs“ | – | – |
| T-235 | review | `HainWindowSizeTest`, `TabletScreens`, `TabletSettingsScreens` (1280 × 800 dp) | – | Gerätetest Tablet/Foldable, Drehen |
| T-238 | review | Screens (CI) | NewsScreen (`TextAction` 48 dp) | – |

- Gradle lokal nicht gestartet (geteilte Sandbox): ktlint 1.5.0 + detekt 1.23.8 CLI, `tools/check_strings.py` lokal, Rest über CI, APK, Screens.
- `HainWindowSize`: eigene Klasse mit den Grenzen von Androids WindowSizeClass (600/840 dp), keine Material-Bibliothek. Breite aus `LocalConfiguration.screenWidthDp` (folgt Split-Screen und Drehen).
- Konflikte mit `hark/news-reader` erwartet: ChatScreen.kt, ChatEngine.kt, MainActivity.kt, catalog.json, libs.versions.toml, gradle.properties, strings.xml, NewsTool.kt (dazu README/Website/TASKS/STATUS).
