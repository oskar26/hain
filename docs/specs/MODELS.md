# MODELS.md — catalogue, downloads, device check

## Catalogue (`core-models/src/main/assets/catalog.json`)

Each entry: `id, name, kind (brain|embed|stt|tts|ocr|ocr_vl|omni|vl|imagegen|imageedit), lang, files[{url, sha256, size}], min_ram_gb, licence, licence_url, default bool`.
Brains may also carry `tier` (`standard` = for every phone, default; `strong` = optional "Großes Gehirn", T-240/ADR-24; `legacy` = former default, kept only while installed, T-260) and `recommend_ram_gb` (memory from which the pack is marked "Empfohlen für dein Handy"; default = `min_ram_gb`). A `strong` brain is listed only where DeviceCheck is not `TOO_SMALL` or when it is already on the phone, and is never picked without the user.
**URLs and sha256 must be filled from Hugging Face by a human or strong model (task T-080) — never invented.**

| id | kind | approx size | default |
|---|---|---|---|
| lfm3-omni-2b-q4_0 (LFM3-Omni-2B, text brain, rev d508b88, LFM Open License v1.0, T-260) | brain | 1.05 GB | ✓ |
| lfm3-1.7b-qad-q4_0 (`tier: legacy` since 0.1.10.7 – listed only while installed, never recommended) | brain | 1.0 GB | – |
| jan-v3.5-4b-q4_k_m (`tier: strong`, min 8 / recommended 12 GB RAM, Apache-2.0, ADR-24) | brain | 2.7 GB | – |
| lfm2.5-embed-350m-q8 | embed | 0.2 GB | ✓ |
| ppocrv5-mobile-latin | ocr | 12 MB | ✓ (bundled in APK or first-start download) |
| moonshine-small-de / -en | stt | 0.12–0.25 GB | ✓ for app language |
| pim-voice-en (Pocket TTS English, voice anna) | tts | ≈ 236 MB published, ONNX size tbd | ✓ always (default) |
| pim-voice-<lang> (Pocket TTS de/fr/es/it/pt/nl, voice anna) | tts | ≈ 220 MB each, ONNX size tbd | ✓ language chosen at setup |
| piper-de-thorsten-medium (neutral, optional) | tts | 60 MB | optional |
| kokoro-82m + af_heart (Pim voice EN, ONNX) | tts | ≈ 90–165 MB | ✓ if EN and device fast enough |
| imageedit-kit | imageedit | 50 MB | ✓ |
| lfm3-omni-2b-q4km (+mmproj) | omni | 1.65 GB | – |
| lfm3-vl-2b | vl | ~1.3 GB | – |
| paddleocr-vl-1.6 | ocr_vl | ~0.6 GB | – |
| kokoro-82m-fp16 | tts | 164 MB | – |
| parakeet-tdt-0.6b-v3 | stt | ~0.6 GB | – |
| sd15-dmd2-mnn | imagegen | 1.1 GB | – |
| alt brains: lfm2.5-1.2b, qwen3.5-2b, gemma-4-e2b | brain | 0.7–1.6 GB | – |

User can also import any `.gguf` (SAF picker → copy → mark `kind=brain, custom=true`, chat template from GGUF metadata → if not LFM3 ChatML, use llama.cpp's built-in `llama_chat_apply_template` path instead of our PromptBuilder; tool calling disabled for custom models).

## Downloader

WorkManager `CoroutineWorker` as foreground service (notification with progress), HTTP Range resume, temp file `.part`, SHA-256 verify, then atomic rename. Wi-Fi only setting. Storage check before start (size × 1.1).

## DeviceCheck

RAM (`ActivityManager.MemoryInfo.totalMem`), SoC (`Build.SOC_MODEL` API 31+), free storage, Vulkan available.
Quick bench (onboarding, optional): 64 tokens with brain → tokens/s stored. Rules:
- RAM < 6 GB → warn, suggest LFM2.5-1.2B, ctx 4096.
- RAM < 8 GB → hide image generation.
- tokens/s < 5 → suggest short prompt + fast mode.
