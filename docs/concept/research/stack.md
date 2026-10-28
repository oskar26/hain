# Local "Gemini-like" Android assistant around Liquid LFM3: stack research
Research date: 2026-10-27 (LFM3 launch day). Audience: hobby developer. Things marked **(inferred)** are my judgment and not taken from a source.

---
## 0. TL;DR recommended stack
| Layer | Pick | Why |
|---|---|---|
| Runtime | **llama.cpp built through the NDK** (start from `examples/llama.android`, or your own JNI wrapper) | Only runtime with merged day-0 support for all four LFM3 generative models. Liquid's own docs now say to use it directly. |
| Main model | **LFM3-1.7B Q4_0 GGUF** (~1 GB) | Tool calling, 256K context, small KV cache |
| Vision | LFM3-VL-2B GGUF + mmproj | Screenshots, camera, GUI understanding |
| Voice | LFM3-Audio-2B GGUF (ASR, TTS and voice chat with tool calls in one model), or sherpa-onnx (Whisper/Parakeet + Kokoro/Piper) as a lighter fallback | |
| RAG | LFM2.5-Embedding-350M GGUF (llama.cpp) or EmbeddingGemma 2 (LiteRT-LM) | |
| Alternative runtime | LiteRT-LM (Gemma 4 E2B/E4B, Qwen3.5) | Best GPU/NPU path for Gemma. LFM3 support **not confirmed** |
| Assistant surface | VoiceInteractionService + ROLE_ASSISTANT, Assist API for screen context, Intents for actions | Usable on Play. AppFunctions *calling* is closed to 3rd-party apps |
| Fork base | SmolChat (Apache-2.0, Kotlin + llama.cpp JNI) or AI Edge Gallery (Apache-2.0, LiteRT-LM, MCP, skills) | |

---
## 1. Inference runtimes on Android

### LFM3 artifacts on Hugging Face (all present on day 1)
- Collection: https://huggingface.co/collections/LiquidAI/lfm3
- LFM3-1.7B: https://huggingface.co/LiquidAI/LFM3-1.7B · GGUF: https://huggingface.co/LiquidAI/LFM3-1.7B-GGUF (also ONNX, MLX-4bit, FP8, NVFP4). 1.73B params; 24 layers (13 short conv, 5 SWA, 6 global attention); 256K context; 16 languages incl. German; ChatML template; reasoning on by default (`enable_thinking=False` exists but "can be inconsistent"); tool calls come out as XML `<tool_call><function=..><parameter=..>`; supports deferred tools / tool search (`defer_loading=True`). Sampling: temp 0.1, top_k 50, repetition penalty 1.1.
- LFM3-VL-2B: https://huggingface.co/LiquidAI/LFM3-VL-2B (GGUF: https://huggingface.co/LiquidAI/LFM3-VL-2B-GGUF), 1.92B params, 128K context
- LFM3-Audio-2B: https://huggingface.co/LiquidAI/LFM3-Audio-2B (GGUF: https://huggingface.co/LiquidAI/LFM3-Audio-2B-GGUF). Input is 16 kHz audio or text. Output is text, 24 kHz speech, or both interleaved. 32K context. Does ASR, LiveASR (English only), TTS (female/male voice), speech translation and tool calls from speech. Covers 18 languages/19 locales incl. German.
- LFM3-Omni-2B: https://huggingface.co/LiquidAI/LFM3-Omni-2B (GGUF: https://huggingface.co/LiquidAI/LFM3-Omni-2B-GGUF)
- d1-2B / d1-Omni-2B decision models: https://huggingface.co/LiquidAI/d1-2B. Typed answers (point/box, read spans, estimate, pick) with calibrated probabilities, all in one forward pass. Useful as a fast **intent router / screen-element grounder** **(inferred use)**. llama.cpp support for d1: **not verified**.
- License: LFM Open License v1.0 (https://huggingface.co/LiquidAI/LFM3-1.7B/blob/main/LICENSE). Free for commercial use below $10M annual revenue according to secondary coverage (https://www.orcarouter.ai/blog/lfm3-audio-vs-ming-3-0-audio-flash). Fine for a hobby app.
- Launch blog: https://www.liquid.ai/blog/lfm3 · HF blog: https://huggingface.co/blog/LiquidAI/lfm3

### Runtime-by-runtime
| Runtime | LFM3 day-1? | Notes / URLs |
|---|---|---|
| **llama.cpp** | **YES.** Text + VL: PR #31252, merged 2026-10-27 (https://github.com/ggml-org/llama.cpp/pull/31252). Audio + Omni: PR #31257, merged 2026-10-27 (https://github.com/ggml-org/llama.cpp/pull/31257) | Video input for VL/Omni and LiveASR are "in follow-up PRs". Use a build from after these merges; older builds will fail. Android: `examples/llama.android` gives a Kotlin `AiChat`/`InferenceEngine` API, CPU kernels up to Arm SME2/KleidiAI with runtime detection (https://github.com/ggml-org/llama.cpp/blob/master/docs/android.md). Enable `LLAMA_BUILD_MTMD` for vision/audio. Liquid's mobile guide: https://docs.liquid.ai/deployment/on-device/llama-cpp/mobile (Q4_0 is the best phone default because it is repacked into Arm kernels; download the GGUF via WorkManager into app-private storage; mmap). |
| llama.cpp GPU backends | partial | OpenCL backend is Adreno-tuned (verified on 8 Gen 3, 7s Gen 3, 8 Elite, 8 Elite Gen 5; Q4_0 optimized): https://github.com/ggml-org/llama.cpp/blob/master/docs/backend/OPENCL.md. Vulkan on Android is often slower than CPU. A 2026 user test on SD 8 Gen 2 found OpenCL/HTP *slower* than CPU (https://github.com/ggml-org/llama.cpp/discussions/22791). Liquid: "On Android, CPU is the safe default." There is also a Hexagon (HTP) backend, used by Qualcomm GenieX. |
| **Liquid LEAP Android SDK** | **NO, deprecated.** | "The LEAP SDK is deprecated and no longer receives new releases… new projects should use llama.cpp directly" (https://docs.liquid.ai/deployment/on-device/sdk/overview). Last version `ai.liquid.leap:leap-sdk:0.10.7` (Kotlin Multiplatform; `LeapModelDownloader.loadModel(modelName, quantizationType)`; minSdk 31). Migration guide: https://docs.liquid.ai/deployment/on-device/llama-cpp/migrating-from-leap-sdk. Its docs list LFM2/LFM2.5 only, not LFM3. |
| Google LiteRT-LM | Not confirmed for LFM3 | Kotlin `com.google.ai.edge.litertlm:litertlm-android`, GPU/NPU, tool use, multimodal (https://developers.google.com/edge/litert-lm/android). Model types include `Lfm2` but not LFM3 yet (https://github.com/google-ai-edge/LiteRT-LM/blob/main/runtime/proto/llm_model_type.proto). Best path for Gemma 4 / Qwen3.5 / EmbeddingGemma 2. **MediaPipe LLM Inference is maintenance-only; migrate to LiteRT-LM** (https://developers.google.com/edge/mediapipe/solutions/genai/llm_inference). |
| ExecuTorch | LFM2/2.5 yes; LFM3 not confirmed | https://github.com/pytorch/executorch/blob/main/examples/models/lfm2/README.md · Android AAR: https://docs.pytorch.org/executorch/stable/llm/run-on-android.html |
| MLC-LLM | No evidence of LFM support | Android OpenCL on Adreno/Mali: https://github.com/mlc-ai/mlc-llm |
| Qualcomm GenieX / AI Hub | Named as a day-0 launch partner ("Qualcomm via Qualcomm AI Hub and GenieX", per the Liquid blog). No LFM3 model page verified. | GenieX = llama.cpp (incl. Hexagon NPU) + QAIRT runtimes, Android supported: https://geniex.aihub.qualcomm.com/en/get-started/what-is-geniex. Snapdragon only. |
| Nexa SDK | LFM2/2.5 on Hexagon NPU; LFM3 not verified | https://www.qualcomm.com/developer/blog/2025/11/nexa-ai-for-android-simple-way-to-bring-on-device-ai-to-smartphones-with-snapdragon |

### Expected speed
- Liquid has published no Android number for LFM3 yet. Its iPhone 17 Pro figures (llama.cpp Q4_0, 32K context, GPU): ~1,061 tok/s prefill, **43.6 tok/s decode**, 1.90 GB (via VentureBeat https://venturebeat.com/technology/liquid-ais-new-lfm3-models-put-browser-agents-voice-commands-and-image-recognition-onto-enterprise-controlled-devices). A full 128K context needs a 1.6 GB KV cache (Qwen3.5-4B needs 4.3 GB).
- Reference point: LFM2.5-1.2B Q4_0 on a Galaxy S25 Ultra (CPU, llama.cpp) runs at 335 prefill / **70 tok/s decode**; Qwen3-1.7B runs at 40 tok/s (https://huggingface.co/LiquidAI/LFM2.5-1.2B-Thinking). On NPU via NexaML (ROG Phone 9 Pro) it reaches 82 tok/s.
- Mid-range reference: Gemma 4 E2B on a Galaxy A35 (Exynos 1380, 8 GB) runs at 5–7 tok/s, and E4B at 3.4–3.6 tok/s. The limit is memory bandwidth (https://dev.to/baiju_rajyaguru_a70384dfd/i-ran-gemma-4-on-a-mid-range-android-phone-heres-what-actually-happened-40ge).
- **(Inferred) estimate:** LFM3-1.7B Q4_0 on CPU should decode at roughly 12–25 tok/s on a Snapdragon 7-series or Dimensity 7000-class phone, and 40–70 tok/s on 2025–26 flagships. Benchmark with `llama-bench -m model.gguf -p 512 -n 128` from the prebuilt Android release before building the app.

---
## 2. Other models worth integrating
| Need | Model | Runtime | Notes |
|---|---|---|---|
| Text, stronger knowledge | Qwen3.5-4B (Q4_K_M 2.74 GB) / Qwen3.5-2B (1.28 GB) | llama.cpp, LiteRT-LM | Beats LFM3 on GPQA/HLE and RULER 128K; LFM3 wins on agent/tool/IF benchmarks (benchmark table on the LFM3 card). GGUF: unsloth/Qwen3.5-*-GGUF (listed in https://huggingface.co/DKTechin/local-llm-gguf) |
| Multimodal + audio, Google stack | Gemma 4 E2B (2.58 GB litertlm) / E4B (3.65 GB) | LiteRT-LM (GPU), llama.cpp (QAT Q4_0 GGUF) | S26 Ultra: E2B decodes at 47 (CPU) / 52 (GPU) tok/s (https://developers.google.com/edge/litert-lm/models/gemma-4). Apache-2.0 |
| Small / fast fallback | LFM2.5-1.2B-Instruct, LFM2.5-350M, LFM2.5-230M | llama.cpp | https://huggingface.co/LiquidAI/LFM2.5-350M-GGUF |
| Embeddings (RAG) | **LFM2.5-Embedding-350M** (1024-d, 11 languages; GGUF Q4_K_M/Q8_0; llama.cpp PR #24913) | llama.cpp `--embeddings`; also as LiteRT .tflite (371 MB int8) | https://huggingface.co/LiquidAI/LFM2.5-Embedding-350M-GGUF · https://huggingface.co/litert-community/LFM2.5-Embedding-350M · ColBERT variant: LFM2.5-ColBERT-350M |
| Embeddings, alternative | EmbeddingGemma 2 (270M/440M/740M) | LiteRT-LM | https://github.com/google-ai-edge/litert-samples |
| ASR | LFM3-Audio-2B (EN WER 6.2, multilingual 6.0) or Whisper/Parakeet/Moonshine via **sherpa-onnx** | llama.cpp / sherpa-onnx | sherpa-onnx has Android APKs, a Kotlin API, Silero VAD, KWS and a QNN NPU path: https://github.com/k2-fsa/sherpa-onnx · https://k2-fsa.github.io/sherpa/onnx/android/index.html |
| TTS | LFM3-Audio (UTMOSv2 3.8) or **Kokoro-82M / Piper via sherpa-onnx** | | Example apps: SherpaTTS (F-Droid, Piper/Coqui) https://f-droid.org/packages/org.woheller69.ttsengine · VoxSherpa (Kokoro+Piper) https://github.com/CodeBySonu95/VoxSherpa-TTS · HayaiTTS (GPL-3) https://github.com/HayaiApp/HayaiTTS. A small TTS model is cheaper than running a 2B model for speech output **(inferred)**. |

---
## 3. Interfaces that make it act like Gemini

| Interface | Status for a 3rd-party Play app | URLs / notes |
|---|---|---|
| **AppFunctions** (Android 16+, `androidx.appfunctions:1.0.0-beta01`, 2026-10-21) | **Exposing** your own functions: yes. **Calling other apps' functions: effectively NO.** Callers need `EXECUTE_APP_FUNCTIONS`, which AOSP says requires being on a device allowlist plus user approval (earlier it was privileged-only). Gemini integration is in private preview. | https://developer.android.com/ai/appfunctions · https://developer.android.com/jetpack/androidx/releases/appfunctions · AppFunctionManager: https://developer.android.com/reference/android/app/appfunctions/AppFunctionManager · permission-level change: https://gitlab.e.foundation/e/os/android_frameworks_base/-/commit/d04774f3559e6309d5578ae396add5136ad676c2 · samples: https://github.com/android/appfunctions. Plan for it, but don't depend on it. |
| **Default digital assistant** | YES. Implement `VoiceInteractionService` + `VoiceInteractionSessionService` + a `RecognitionService` (required), or handle `ACTION_ASSIST`. Request `RoleManager.ROLE_ASSISTANT`. The user picks it under Settings → Default apps → Digital assistant. That gives you long-press power/home and the corner swipe. | https://developer.android.com/reference/android/app/role/RoleManager · https://developer.android.com/reference/kotlin/android/service/voice/VoiceInteractionService · reference implementations: https://github.com/equerry-dev/equerry (reads the screen via the Assist API, no AccessibilityService) · https://github.com/IgnacioLD/edgemind (Gemma 4 + tools) · https://github.com/DarkJoker360/OpenAssistant-app (LiteRT-LM + Vosk + MCP) · https://github.com/OpenAppCapabilityProtocol/hark |
| **Screen context** | YES, while invoked. `VoiceInteractionSession.onHandleAssist` / `onHandleScreenshot` gives you AssistStructure (view tree text) + a screenshot, if the user enables "use text/screenshot from screen". Pass it to LFM3-VL / d1-2B for grounding. | VoiceInteractionService docs above; Equerry is the reference |
| **Intents** (deterministic tools) | YES, and this is the safest action layer. `AlarmClock.ACTION_SET_ALARM`/`SET_TIMER`, `CalendarContract` insert intent or provider (READ/WRITE_CALENDAR), Contacts provider, `ACTION_SENDTO smsto:` / `ACTION_DIAL` (user confirms), `geo:`/Maps intents, `ACTION_SEND`, Settings panels. | Standard Android docs. Map each one to an LFM3 tool schema. droid-mcp packages 151 such capabilities as LLM tools (https://github.com/stixez/droid-mcp) |
| **AccessibilityService** | **Risky on Play.** Since 2026-01-28: "Any use of the Accessibility API that enables an app to autonomously initiate, plan, and execute actions or decisions is strictly prohibited." Only deterministic, user-scripted automation is allowed, plus real accessibility tools (`isAccessibilityTool`). It needs a declaration form. Android 17's Advanced Protection also blocks non-accessibility apps. | https://support.google.com/googleplay/android-developer/answer/10964491 · https://www.heise.de/en/news/Android-17-Google-cracks-down-on-Accessibility-API-abuse-11210703.html. Workaround used by others: a separate sideloaded "agent" companion app (e.g. zerotap https://zerotap.app/agent). Fine for personal sideload/F-Droid builds. |
| NotificationListenerService | YES (user grants access in Settings). Lets you summarize notifications and reply via `RemoteInput` actions. | Play's user-data policy applies; the 2026-07-15 policy clarifies that it covers third-party AI integrations (https://support.google.com/googleplay/android-developer/answer/17134731) |
| Quick Settings tile / widget / share target | YES. `TileService` → open the voice overlay. Glance/AppWidget for a home-screen chat entry. `ACTION_SEND`/`PROCESS_TEXT` intent filters to summarize/translate shared text or images. | Standard APIs |
| On-device MCP | Possible in-app (embedded MCP client/server, Ktor). | AI Edge Gallery added MCP (https://developers.googleblog.com/a-smarter-google-ai-edge-gallery-mcp-integration-notifications-and-session-continuity/) · https://github.com/danielealbano/android-remote-control-mcp · https://github.com/satyajiit/aster-mcp · ADB-based (dev only): https://github.com/CursorTouch/Android-MCP, https://github.com/mobile-next/mobile-mcp |
| Wake word | openWakeWord: code Apache-2.0, but the **bundled pretrained models are CC BY-NC-SA** (fine for a hobby app; commercial use needs a license from openwakeword.com, $185 per word). Porcupine: its free tier reportedly shut down in 2026 (HA forum). sherpa-onnx KWS is a free alternative. Always-on listening needs a foreground service with microphone type and drains the battery. | https://github.com/dscripka/openWakeWord · https://openwakeword.com/license · https://picovoice.ai/docs/quick-start/porcupine-android/ · https://community.home-assistant.io/t/porcupine-free-tier-shutdown-alternatives-for-home-assistant-voice-users/1012382 |

**Play constraints summary:** the assistant role, the Assist API and Intents are fine. Autonomous Accessibility control is banned unless the app is a real accessibility tool. AppFunctions calling isn't open to 3rd parties. Notification, SMS, call-log and contacts access is sensitive and needs prominent disclosure (SMS/Call Log permission is restricted to default-handler-type apps; the assistant role is one of the listed exceptions per RoleManagerCompat docs, **verify** before relying on it). Sending SMS or calling should go through user-confirmed intents.

---
## 4. Open-source reference apps
| App | License | Stack | Use |
|---|---|---|---|
| **SmolChat-Android** https://github.com/shubham0204/SmolChat-Android | Apache-2.0 | Kotlin/Compose, llama.cpp JNI (`smollm` module) | Cleanest Kotlin base to fork. Bump llama.cpp to a build after #31252/#31257. |
| **Google AI Edge Gallery** https://github.com/google-ai-edge/gallery | Apache-2.0 | Kotlin, LiteRT-LM; agent skills (text/JS/native intents), MCP, calendar skills, notifications | Best reference for tool and skill UX. Weak on GGUF/LFM3. |
| PocketPal AI https://github.com/a-ghorbani/pocketpal-ai | MIT | React Native + llama.rn | Mature model manager, HF download, benchmarking |
| ChatterUI https://github.com/Vali-98/ChatterUI | AGPL-3.0 (copyleft) | React Native + llama.cpp | Power-user chat |
| Maid https://github.com/Mobile-Artificial-Intelligence/maid | MIT | Flutter + llama.cpp | F-Droid |
| llama.android example https://github.com/ggml-org/llama.cpp/tree/master/examples/llama.android | MIT | Kotlin | Minimal binding |
| Liquid cookbook (LeapChat, Koog agent, VLM examples) https://github.com/Liquid4All/cookbook | see repo | LEAP SDK (deprecated) | Still useful for UX ideas |
| PocketLFM https://github.com/Jeevav62/pocketlfm | MIT | llama.cpp + LFM2.5 | Small LFM-specific example |
| LLM-Hub https://github.com/timmyy123/LLM-Hub | see repo | agent + MCP + device tools | Agent ideas |
| MLC Chat | Apache-2.0 (MLC-LLM) | TVM/OpenCL | Not recommended for LFM3 |
| Layla | closed source / paid | | Product inspiration only |
| Liquid Apollo https://play.google.com/store/apps/details?id=ai.liquid.chatapp | closed source | LEAP | Benchmark how LFM models feel. Not forkable. |

---
## 5. Suggested build order (inferred)
1. Fork SmolChat → update llama.cpp → LFM3-1.7B Q4_0 chat with streaming, `enable_thinking` toggle, and Liquid sampling params.
2. Tool layer: parse LFM3 XML tool calls → Kotlin dispatcher → Intents (alarm, timer, calendar, maps, dial/SMS draft, web search) with a confirmation UI for anything outbound.
3. VoiceInteractionService + ROLE_ASSISTANT overlay; Assist API screenshot → LFM3-VL-2B.
4. Voice: sherpa-onnx VAD + LFM3-Audio-2B (ASR + TTS + spoken tool calls) or Whisper + Kokoro.
5. RAG over notes/files with LFM2.5-Embedding-350M + sqlite-vec/ObjectBox.
6. Optional: expose your own AppFunctions; QS tile, widget, share target; wake word via sherpa KWS.
7. Ship via GitHub/F-Droid first; for Play, keep Accessibility out or limit it to deterministic macros.

## Gaps / not verified
- No official Android tok/s for LFM3; LFM3 support in LiteRT-LM, ExecuTorch, MLC and Nexa is unconfirmed.
- llama.cpp support for d1-2B is unverified. Video input (VL/Omni) and LiveASR in llama.cpp are pending follow-up PRs.
- Exact EXECUTE_APP_FUNCTIONS allowlist process for non-Google agents is not public.
- Reddit coverage was thin on launch day (only r/LocalLLaMA announcement thread: https://www.reddit.com/r/LocalLLaMA/comments/1xgy8s3/liquidailfm317b_hugging_face/); there are no community Android benchmarks yet.
