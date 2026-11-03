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
