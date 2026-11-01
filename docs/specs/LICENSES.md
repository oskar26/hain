# LICENSES.md — dependencies and model licences

App licence: GPL-3.0 (ADR-13). Every new dependency/model gets a row **before** it is added. "verify" = must be checked against the official page by the task that adds it.

| Component | Licence | Status |
|---|---|---|
| llama.cpp / ggml | MIT | ok |
| LFM3 models (1.7B, VL, Omni, Audio) | LFM Open License v1.0 (free < $10M revenue) | ok for non-commercial/small; show in About |
| LFM2.5-Embedding-350M | LFM Open License | verify |
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
| ONNX Runtime | MIT | ok |
| kotlinx.serialization (json, 1.8.1) | Apache-2.0 | ok (T-040, ToolSchema JSON) |
| kxml2 2.3.0 (test only, XmlPullParser on the JVM) | BSD-style (MIT-like) | ok (T-070) |
| androidx.work (work-runtime-ktx 2.12.0) | Apache-2.0 | ok (T-082, model downloads) |
| LFM3-1.7B GGUF (downloaded at runtime, not bundled) | LFM Open License v1.0 | model weights, not linked code; licence shown in onboarding (T-083) |
| pdfbox-android | Apache-2.0 | ok |
| MuPDF | AGPL | **do not use** |
| Readability4K | Apache-2.0 | verify |
| jsoup | MIT | ok |
| OkHttp | Apache-2.0 | ok |
| SQLCipher Community | BSD-style | ok |
| sqlite-vec | MIT/Apache-2.0 | ok |
| Room, Compose, Hilt, WorkManager | Apache-2.0 | ok |
| multiplatform-markdown-renderer | Apache-2.0 | ok |
| Inter, Atkinson Hyperlegible, JetBrains Mono | OFL-1.1 | ok |
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
| Lucide (icon SVG sources) | ISC | ok |

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
