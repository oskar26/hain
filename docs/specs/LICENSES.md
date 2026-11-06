# LICENSES.md — dependencies and model licences

App licence: GPL-3.0 (ADR-13). Every new dependency/model gets a row **before** it is added, and an entry in `tools/licenses/oss.toml` (shown in the app under Settings → Über Hain → Open-source-Lizenzen; `python3 tools/licenses/gen_licenses.py --check` in CI fails when a runtime dependency, model or font is missing there, T-285). "verify" = must be checked against the official page by the task that adds it.

| Component | Licence | Status |
|---|---|---|
| llama.cpp / ggml | MIT | ok |
| AndroidX DataStore (preferences) | Apache-2.0 | ok |
| LFM3 models (1.7B, VL, Omni, Audio) | LFM Open License v1.0 (free < $10M revenue) | ok for non-commercial/small; show in About |
| LFM2.5-Embedding-350M | LFM Open License v1.0 (`lfm1.0`, checked 04.11.2026 on the Hugging Face model card) | ok, same terms as the brain; optional pack |
| Jan-v3.5-4B (optional „Großes Gehirn“, ADR-24) | Apache-2.0 (checked 05.11.2026 on the Hugging Face model cards of janhq/Jan-v3.5-4B and janhq/Jan-v3.5-4B-gguf; base Jan-v3-4B-base-instruct, Qwen3-4B architecture) | ok with GPL-3.0; optional pack |
| zstd-jni (com.github.luben:zstd-jni 1.5.7-12, Android AAR, T-237) | BSD-2-Clause (zstd itself: BSD-3-Clause) | ok with GPL-3.0; reads the offline Wikipedia packs |
| Wikipedia texts in the knowledge packs (Kiwix ZIM "mini", T-237) | CC BY-SA 4.0 (Wikipedia authors), packaged by Kiwix / openZIM | ok; downloaded only on request, not in the APK; source and licence shown in Settings → Modelle & Pakete → Wissen |
| sherpa-onnx | Apache-2.0 | ok |
| Piper (engine, current) | GPL-3.0 | ok with GPL app |
| espeak-ng | GPL-3.0 | ok |
| Piper voice thorsten-medium | dataset CC0; voice trained on lessac base (precaution) | verify, decide Q-02 |
| Piper voice cori | public domain | verify |
| Kyutai Pocket TTS (code MIT, weights kyutai/pocket-tts-without-voice-cloning CC-BY-4.0) | attribution required | Pim voice, all languages |
| kikiri-tts / misaki (G2P, conversion) | Apache-2.0 | ok |
| Piper voice kerstin-low (Pim DE fallback) | dataset CC0 (Kerstin, Thorsten-Voice project) | verify model card on HF before T-080 |
| Kokoro-82M | Apache-2.0 | ok |
| Moonshine | MIT | ok |
| Parakeet TDT 0.6B v3 | CC-BY-4.0 | verify, attribution |
| PP-OCRv5 / PaddleOCR | Apache-2.0 | ok |
| PaddleOCR-VL-1.6 | Apache-2.0 | verify |
| ONNX Runtime (`onnxruntime-android` **1.28.0**, Häppchen 10 dictation + Häppchen 9 OCR, one library, one `.so`) | MIT | ok – pinned: from 1.29 the Android AAR adds Microsoft 1DS telemetry (`ai.onnxruntime.TelemetryInitializer`, `mobile.events.data.microsoft.com`); 1.28.0 has none, and core-voice's manifest removes the provider in case a newer version is pulled in. Every ORT user (also OCR, T-121) must stay on ≤ 1.28.0 |
| Moonshine Streaming Small DE/EN (ONNX int8 exports `Masterx/moonshine-streaming-small(-de)-ONNX`, of `moonshine-ai/moonshine-streaming-small(-de)`) | MIT | ok – downloaded at runtime (catalog.json, pinned commit + SHA-256), not in the APK; third-party export of the official weights, checked on the host (same text as the reference pipeline) |
| kotlinx.serialization (json, 1.8.1) | Apache-2.0 | ok (T-040, ToolSchema JSON) |
| kxml2 2.3.0 (test only, XmlPullParser on the JVM) | BSD-style (MIT-like) | ok (T-070) |
| androidx.work (work-runtime-ktx 2.12.0) | Apache-2.0 | ok (T-082, model downloads) |
| androidx.profileinstaller (1.4.1) | Apache-2.0 | ok (T-190, installs the baseline profile on phones without Play) |
| reactivecircus/android-emulator-runner (GitHub Action, CI only, not in the app) | Apache-2.0 | ok (T-190, release smoke test) |
| LFM3-Omni-2B Q4_0 GGUF (optional brain since 0.1.10.8, `tier: optional`; default in 0.1.10.7; downloaded at runtime, not bundled; T-260/T-270) | LFM Open License v1.0 (https://huggingface.co/LiquidAI/LFM3-Omni-2B-GGUF/blob/d508b88f08a5b4286f2d2805a6a8d69a0ff44c72/LICENSE, pinned revision d508b88, SHA-256 checked) | model weights, not linked code; catalogue entry carries `licence` + `licence_url`, licence shown in onboarding and Modelle & Pakete |
| LFM3-1.7B GGUF (default brain again since 0.1.10.8, T-270; downloaded at runtime, not bundled) | LFM Open License v1.0 | model weights, not linked code; licence shown in onboarding (T-083) |
| LFM2.5-Embedding-350M-Q4_0 GGUF (optional pack "Suche nach Bedeutung", downloaded at runtime, not bundled; T-110) | LFM Open License v1.0 (https://huggingface.co/LiquidAI/LFM2.5-Embedding-350M-GGUF/blob/main/LICENSE) | model weights, not linked code; catalogue entry carries `licence` + `licence_url` |
| Jan-v3.5-4B Q4_K_M GGUF (optional „Großes Gehirn“, downloaded at runtime, not bundled; T-240, ADR-24) | Apache-2.0 (https://huggingface.co/janhq/Jan-v3.5-4B-gguf, pinned revision 2b0fcab0066198e78f7c004a0815179fbce13985, README `license: apache-2.0`) | model weights, not linked code; catalogue entry carries `licence` + `licence_url`, shown in Modelle & Pakete. Underdog Poodle 4B was dropped before merge: its base repo switched to CC BY-NC 4.0 on 05.11.2026 |
| pdfbox-android (`com.tom-roush:pdfbox-android` 2.0.27.0, T-120) | Apache-2.0 | ok |
| PP-OCRv5 mobile det + latin rec + PP-LCNet textline orientation, ONNX exports from huggingface.co/PaddlePaddle (downloaded at runtime, pinned commit + SHA-256) | Apache-2.0 | ok (T-121; shown in "Meine Dateien") |
| MuPDF | AGPL | **do not use** |
| German word list `core-docs/src/main/assets/dict/de_words.txt` (208,341 words, T-122): igerman98 (Björn Jacke, GPL-2.0/GPL-3.0) + FrequencyWords DE (Hermit Dave, CC-BY-SA-4.0) | GPL / CC-BY-SA-4.0 (one-way compatible with GPL-3.0) | ok; sources in `assets/dict/README.txt` |
| Readability4K | Apache-2.0 | verify |
| jsoup | MIT | ok |
| OkHttp | Apache-2.0 | ok |
| SQLCipher Community | BSD-style | ok |
| sqlite-vec | MIT/Apache-2.0 | ok |
| Room, Compose, Hilt, WorkManager | Apache-2.0 | ok |
| AndroidX Core, AndroidX Activity, AndroidX Lifecycle (core-ktx, activity-compose, lifecycle-runtime/-viewmodel) | Apache-2.0 | ok |
| Kotlin standard library, kotlinx.coroutines | Apache-2.0 | ok |
| SQLite (inside SQLCipher) | public domain | ok |
| Room 2.7.2 (runtime, ktx, compiler via KSP) + androidx.sqlite 2.5.2 | Apache-2.0 | ok (T-020) |
| net.zetetic:sqlcipher-android 4.10.0 | BSD-style (zetetic.net/sqlcipher/license) | ok (T-020, encrypted `hain.db`) |
| androidx.biometric 1.1.0 (+ androidx.fragment-ktx 1.8.9; pulls androidx.appcompat 1.2.0 at runtime) | Apache-2.0 | ok (T-056, app lock with BiometricPrompt + device PIN; no Play Services) |
| Robolectric 4.16 + androidx.test:core 1.7.0 (test only) | MIT / Apache-2.0 | ok (T-020 DAO tests) |
| multiplatform-markdown-renderer | Apache-2.0 | ok |
| Hanken Grotesk 3.014 (Light, Regular, Medium, SemiBold; app font since 03.11.2026, text in `docs/specs/licenses/HankenGrotesk-OFL.txt`), Atkinson Hyperlegible, JetBrains Mono | OFL-1.1 | ok |
| Lucide icons | ISC | ok |
| Compose Unstyled | MIT | ok |
| Haze | Apache-2.0 | ok |
| Backdrop (Kyant0) | Apache-2.0 | ok |
| Capsule (Kyant0) | Apache-2.0 | ok |
| compose-shimmer | Apache-2.0 | ok |
| Rive Android runtime | MIT | ok; .riv export needs Rive paid plan or shows splash (Q-04) |
| Lottie Compose | Apache-2.0 | ok; each animation file: check its own licence |
| Coil 3 | Apache-2.0 | ok |
| Highlights (via markdown `-code`) | Apache-2.0 | verify |
| assistant-ui | MIT | **not a dependency** – UX reference only |
| Composables UI (copied source) | MIT | ok, keep headers, NOTICE |
| SmolChat-Android (copied/adapted JNI code) | Apache-2.0 | ok, keep headers, NOTICE |
| Google AI Edge Gallery (reference) | Apache-2.0 | ok if copied: headers + NOTICE |
| GetStream stream-chat-android-ai | Stream License | **forbidden** (not GPL-compatible) |
| Konfetti (konfetti-compose) | ISC | ok |
| Compose Unstyled (composablehorizons) | MIT (verify at add time) | ok, backup component base |
| Showkase (debug only) | Apache-2.0 | ok |
| Roborazzi (screenshot testing) | Apache-2.0 | ok |
| Roborazzi compose + junit-rule, Robolectric, compose ui-test-junit4/-manifest (test only, T-011 screenshots in feature modules) | Apache-2.0 / MIT | ok |
| Lucide (icon SVG sources) | ISC | ok |
| AndroidX Glance (glance-appwidget 1.1.1; home screen widget "Frag Pim", T-144; no glance-material3) | Apache-2.0 | ok |

**Added in T-018 (2026-10-30).** All licences verified against the artifacts' POMs on Maven Central.
Newest stable versions that still work with the pinned toolchain (AGP 9.0.1 / compileSdk 36 /
AGP-embedded Kotlin 2.2.0, which reads Kotlin metadata ≤ 2.3.0): Compose Unstyled 2.6.0,
Haze 1.7.0 (1.x `hazeEffect` API), Capsule 2.1.3, Backdrop 2.0.0-alpha03, compose-shimmer 1.3.2,
multiplatform-markdown-renderer 0.41.0 (+ `-code`), Coil 3.4.0, Lottie Compose 6.7.1,
Konfetti 2.0.5, Showkase 1.0.5 (debug + KSP only), Roborazzi 1.77.0 (test only).
`androidx.compose.material3` is excluded from every :core-ui configuration (hard rule).

| MediaPipe | Apache-2.0 | ok |
| MI-GAN | MIT | verify |
| MODNet | Apache-2.0 | verify |
| Real-ESRGAN | BSD-3 | verify |
| SD 1.5 | CreativeML OpenRAIL-M | verify use restrictions |
| MNN | Apache-2.0 | ok |
| Kiwix/libkiwix (optional, later) | GPL-3.0 | ok with GPL app |
| Google ML Kit / Play Services | proprietary | **do not use** |

## Website (`website/`, not part of the app)

Full list and notes: [website/LICENSES.md](../../website/LICENSES.md).

| Component | Licence | Status |
|---|---|---|
| motion (DOM helpers, shipped to visitors) | MIT | ok |
| React / React DOM (build-time prerendering only) | MIT | ok |
| Tailwind CSS 4, Vite 8, @vitejs/plugin-react | MIT | ok (build tools) |
| TypeScript | Apache-2.0 | ok (build tool) |
| lucide-react | ISC | ok |
| Inter Variable (@fontsource-variable/inter, self-hosted) | OFL-1.1 | ok |
| Aceternity UI / 21st.dev | own licences | **inspiration only, no code copied** (Aceternity licence forbids redistributing source) |
