# KNOWN_ISSUES.md — build & environment gotchas

> Read before touching the Gradle / JDK / lint setup. Append-only; remove or
> edit an entry only when it is fixed, and say so in the HANDOFF_LOG.md entry.

## KI-01 · detekt 1.23.8 cannot run on a JDK 25 Gradle daemon — FIXED, do not regress

**Important finding for future sessions:** detekt 1.23.8's embedded Kotlin CLI
crashes on a JDK 25 Gradle daemon (`IllegalArgumentException: 25.0.3` in
`JavaVersion.parse`) as soon as any module has a Kotlin plugin — i.e. exactly
what adding Hilt in T-004 caused. Fixed portably with
`gradle/gradle-daemon-jvm.properties` (`toolchainVersion=21`, provisioned via
the foojay resolver already configured in `settings.gradle.kts`).
**Do not remove this until detekt 2.0.0-alpha+**, the first line tested
against JDK 25 (per the detekt releases page and compatibility table).

Failure modes if the pin is removed (seen on Fedora, JDK 25.0.3, 2026-10-28):

- `Invalid value (25) passed to --jvm-target, must be one of [1.6 … 22]` —
  the Hilt/Kotlin plugin resets detekt's jvmTarget inference to the daemon JVM.
- `Execution failed for task ':app:detekt' > 25.0.3` — Kotlin CLI
  `JavaVersion.parse` cannot read the JDK 25 runtime version.

Dead ends already tried (do not retry):

- Setting `jvmTarget` on the `Detekt` task does **not** help — the crash is in
  the CLI's JDK detection, not in target selection.
- `DetektExtension` in 1.23.x has no `jvmTarget` property at all (script
  compile error).

Consequences:

- CI (T-006) must keep the daemon-JVM criteria (JDK 21); do not force a newer
  daemon JVM in the workflow.
- After any Kotlin/detekt/AGP upgrade, re-run `./gradlew detekt` on the
  machine's default JDK before considering to drop the pin.

## KI-02 · T-004 on-device acceptance — OPEN (device attached but unauthorized, 2026-11-01)

"App starts on device/emulator" could not be checked: no device attached. Build-level
checks are green and the Hilt-generated components were verified inside the APK dex.

Attempt 2026-10-30 (agent sandbox): the local `Pixel_7` AVD was an unusable stub
(missing `~/.android/avd/Pixel_7.ini`, config.ini without `image.sysdir.1`/`tag.id`/
`abi.type`); it was repaired and the emulator boots. But the agent sandbox reaps
background processes: the adb server dies between tool calls and the emulator launcher
(which owns the adb bridge) gets killed, leaving an orphaned qemu that `adb devices`
never shows. Human decided to stop the attempt. Verify instead on a real device:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n app.hain/.MainActivity   # expect no crash, app label "Hain"
```

Then close this entry and note the result in HANDOFF_LOG.md. Note for the emulator
path: the AVD works when started from Android Studio outside the sandbox
(`emulator -avd Pixel_7`, API 36, x86_64, ARM translation for the arm64-only APK).

## KI-03 · recorded review deviations (no action needed)

- T-002 committed `docs/workflow/AGENT_PROMPTS.md` and
  `docs/workflow/LOCAL_MODELS.md` beyond its file list (generated planner
  docs; harmless, kept).
- T-001's repo-init commit includes the planner docs/assets skeleton.

## KI-04 · pinned toolchain caps UI-library versions (watch when upgrading)

The project pins AGP 9.0.1 / compileSdk 36 (T-002), and AGP 9.0.1 embeds a
Kotlin compiler 2.2.0 that reads Kotlin metadata up to 2.3.0. The newest
"latest stable" of several UI libraries (T-018, 2026-10-30 snapshots) require
compileSdk 37, AGP ≥ 9.1 or Kotlin ≥ 2.3 metadata and therefore cannot be used
yet. Newest compatible versions actually pinned (see LICENSES.md):
Compose Unstyled 2.6.0 (2.9.2+ needs SDK 37, 2.9.x/2.8.x need Kotlin 2.4
stdlib), Haze 1.7.0 (2.x needs SDK 37), markdown-renderer 0.41.0 (0.42+ needs
SDK 37), Coil 3.4.0 (3.5+ needs SDK 37/Kotlin 2.4), Konfetti 2.0.5 (2.0.6+
built with Kotlin 2.4), Backdrop 2.0.0-alpha03 (2.0.0 stable needs SDK 37).
`androidx.compose.material3` is excluded from every :core-ui configuration.
Upgrading to AGP 9.1 + compileSdk 37 would unlock the newer versions; that is
a T-002-scale toolchain change and needs a human decision.
