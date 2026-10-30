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
