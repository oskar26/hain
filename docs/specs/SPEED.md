# SPEED.md — why Hain felt slow again, and what changed (T-088, 03.11.2026)

Oskar (03.11. 23:09): *"es ist wieder so langsam"* — "Pim denkt nach…" for a long time before the
first word. Measured on the host (aarch64, 2 cores, the real LFM3-1.7B-QAD-Q4_0, the app's own
`hain_session.h`, temp 0.1 / top-k 50 / repeat 1.1). Phone figures are estimated with Oskar's
benchmark (prompt 177 t/s, writing 29 t/s); the host reads prompts at ~65 t/s.

## 1. Context 8192 vs 2048: not the cause

`LlmEngine.load()` used 8192 (the 01.11. benchmark ran 2048). Same 978-token first prompt:

| n_ctx | load | first token, cold | first token, warm | writing | peak RSS |
|---|---|---|---|---|---|
| 2048 | 1.8 s | 14.5 s | 0.83 s | 15.9 t/s | 2245 MB |
| 4096 | 1.7 s | 14.6 s | 0.88 s | 15.5 t/s | 2294 MB |
| 8192 | 2.1 s | 14.2 s | 0.88 s | 17.5 t/s | 2390 MB |

Speed is the same within noise; only memory grows (+145 MB). 8192 stays (`CONTEXT_SIZE`): chats are
not shortened yet and a "Gründlich" answer (pages + thinking) needs up to ~3000 tokens, so 2048
would end long chats with "context full".

## 2. The real cause: the clock in the system prompt

The system prompt carried the time (`Jetzt: Dienstag, 3. November 2026, 21:00`). The chat engine moves
it on every 30 minutes, and everything after it — the user's instructions **and the whole tool list**
(~630 of ~980 tokens) — had to be read again. Measured "next message half an hour later":

| | tokens read again | first token (host) | ≈ phone |
|---|---|---|---|
| before (time in the system prompt) | 634 | 9.6 s | 3.6 s |
| after (time in the hint before the answer) | 69 | 1.2 s | 0.4 s |

Now the system prompt only holds the date; `Uhrzeit: 21:07.` goes into the short hint right before
the answer (`SystemPromptSource.turnHint`). The system prompt and the tools stay in memory all day.

## 3. Checked, already fine

- The brain stays loaded between messages (`LlamaEngine` is a singleton; `load()` returns at once
  for the same file) and the system prompt is read ahead after loading (`ChatEngine.warmUp`).
- Prompt: ~980 tokens (system ~550, tools ~380, 5 tools; phone actions add 4 more tools).
- Streaming: the answer reaches the UI at most every **50 ms** (`STREAM_THROTTLE_MS`); Pim and the
  thinking glow animate in the draw phase only (no recomposition, nothing on the main thread blocks).
- "Gründlich" is slow by design: thinking (up to 4096 tokens) plus up to 60 s research.

## 4. Logcat

`adb logcat -s HainLlm` now shows `first token after … ms` for every generation, next to
`brain loaded in … ms, context 8192` and the prompt/reuse line of the native side.

## 5. App start and scrolling: R8 + baseline profile (T-190, 04.11.2026)

- **Release build = R8.** `app/build.gradle.kts` release: `isMinifyEnabled = true`, `isShrinkResources = true`,
  `proguard-android-optimize.txt` + `app/proguard-rules.pro`. Keep rules only for what is reached by name:
  JNI (`NativeLlm` natives, `TokenCallback.onToken` / `LoadProgress.onProgress` called with `GetMethodID`),
  SQLCipher (`net.zetetic.database.**`), Room `*_Impl`, kotlinx.serialization serializers, WorkManager
  workers (class name is stored, must survive updates). Everything else comes from the libraries' consumer rules.
- **Baseline profile, written by hand:** `app/src/main/baselineProfiles/baseline-prof.txt` (start: app, DI,
  settings, theme, onboarding/chat; chat scroll: message list, components, Markdown renderer; frosted glass).
  AGP merges it with the libraries' profiles into `assets/dexopt/baseline.prof`. **ProfileInstaller**
  (`androidx.profileinstaller`, Apache-2.0) compiles it on phones without Play – GitHub and F-Droid APKs
  otherwise run interpreted/JIT until the phone's idle dexopt.
- **Not (yet) a generated profile:** a Macrobenchmark `:baselineprofile` module (androidx.baselineprofile
  plugin) needs an API 33+ device or a rooted emulator to record, and our arm64-only APK runs on the CI's
  x86_64 emulator only through ARM translation. The hand-written rules cover the same paths; a recorded
  profile can replace them later without app changes.
- **Debug builds unchanged:** `app.hain.debug` (CI `-Phain.fastDebug=true`, not debuggable) keeps no R8, so
  its build stays fast; it gets the ProfileInstaller too.
- **Check in CI:** job `release-smoke` in `.github/workflows/screens.yml` builds the release APK (signed with
  the public debug key, `-Phain.signReleaseWithDebugKey=true`, only for the test), checks that
  `baseline.prof` and `libhain_llm.so` are inside, installs it on an Android 11 x86_64 emulator, checks that it
  starts and keeps running, reads the installed ART profile and measures cold start
  (`am start -W`, median of 7) against the nightly debug APK of main – as installed and after
  `cmd package compile -m speed-profile`. Script: `tools/ci/release_smoke.sh`, report in the job summary.

**Measured (CI emulator, Android 11 x86_64, two runs on 04.11.2026, cold start without brain, median of 7):**

| Build | Run 1 | Run 2 |
|---|---|---|
| before · nightly debug APK of main, as installed | 911 ms | 1227 ms |
| after · release APK (R8 + profile), as installed | 900 ms | 1180 ms |
| before · after `compile -m speed-profile` | 978 ms | 1145 ms |
| after · after `compile -m speed-profile` | 932 ms | 1065 ms |

The installed ART profile holds ~29,000 methods, ~4,200 of them Hain's own code (counted back through
`mapping.txt`), so the hand-written rules work. The start-time gain on the shared CI emulator is small
(about 1–7 %) and inside its noise; the real gain is expected on phones (interpreted → compiled code
from the first start, smaller dex). A measurement on a real phone is still open.
