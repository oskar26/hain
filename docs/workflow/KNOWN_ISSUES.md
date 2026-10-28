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

## KI-02 · T-004 on-device acceptance still unverified — OPEN

"App starts on device/emulator" could not be checked: `adb devices` shows no
device. Build-level checks are green and the Hilt-generated components were
verified inside the APK dex. Verify on the first device connection and record
the result in HANDOFF_LOG.md, then close this entry.

## KI-03 · recorded review deviations (no action needed)

- T-002 committed `docs/workflow/AGENT_PROMPTS.md` and
  `docs/workflow/LOCAL_MODELS.md` beyond its file list (generated planner
  docs; harmless, kept).
- T-001's repo-init commit includes the planner docs/assets skeleton.
