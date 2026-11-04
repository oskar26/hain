# VOICE.md — dictation, read aloud, voice mode (ADR-12)

All via **sherpa-onnx** Android AAR (Apache-2.0). Models are packs (MODELS.md).

## STT (mic button)

> **Built in Häppchen 10 (0.1.10, T-140):** Moonshine **Streaming Small** (MIT) as ONNX int8, run directly with
> ONNX Runtime 1.28.0 (no sherpa-onnx AAR on Maven; ORT ≥ 1.29 ships telemetry, see LICENSES.md). Packs
> `moonshine-small-de` (166 MB) / `moonshine-small-en` (209 MB): `frontend.onnx`, `encoder_int8.onnx`,
> `adapter_int8.onnx`, `cross_kv_int8.onnx`, `decoder_kv_int8.onnx`, `tokenizer.json`, `streaming_config.json`
> (exports `Masterx/moonshine-streaming-small(-de)-ONNX`, pinned commit). Pipeline: 16 kHz floats → frontend (5 zero
> states) → encoder → adapter (`pos_offset` 0) → cross K/V → greedy decoder with self K/V cache from length 0, max
> 6.5 tokens/s. The whole utterance runs after you stop (≈0.6 s for 6 s of speech on 2 desktop cores); live partials
> and Silero VAD are not in v1 — leading/trailing silence is cut by energy (−42 dBFS, 30 ms windows, 250 ms margin).
> Download: the normal pack downloader (resume, SHA-256) through the NetworkGate (MODEL_DOWNLOAD). Mic: AudioRecord VOICE_RECOGNITION at 16 kHz, else 48/44.1 kHz + resample. Max 60 s per dictation.

- Default: **Moonshine Small Streaming** (MIT) — DE WER 7.5 %, EN 7.8 %. Streaming partial results into the composer.
- Optional pack: Parakeet TDT 0.6B v3 (better, bigger; check licence CC-BY-4.0 → attribution in About).
- VAD: Silero VAD (sherpa-onnx) — stop after 1.2 s silence (setting).
- Language: app language or auto (Moonshine per-language model → use app language; mixed input not supported in v1).
- Permission `RECORD_AUDIO` asked on first mic tap with explanation sheet.

## TTS (read aloud)

> **Built in Häppchen 10 (0.1.10, T-141):** read aloud under every answer through `SpeechSynth`; the engine today is
> the Android system voice with Pim's pitch (×1.1225 = +2 st) and speed 1.15 (`SystemSpeech`). Text prep below is
> implemented in `SpeechText` (tables are read row by row, formulas → "Formel übersprungen"). The language is the
> language of the answer (offline guess), else the answer language. Pocket TTS = T-145 (see TASKS.md for its state).

### Pim voice — one voice for all languages (owner decision 2026-10-28)

- **Engine: Kyutai Pocket TTS** (≈100M params, code MIT, weights `kyutai/pocket-tts-without-voice-cloning` **CC-BY-4.0** → attribution "Voice: Kyutai Pocket TTS (CC-BY 4.0)" in About/Licences). 24 kHz.
- **Voice: built-in `anna`** in every language, pitched **+2 semitones** (natural ≈ 203 Hz → ≈ 228 Hz) at **speed 1.15** (owner: 1.0 too slow). Owner wants Pim to sound the same everywhere ("lass uns einheitlich bleiben").
  - Chosen from 12 built-in voices by UTMOS in EN + DE (anna +2 st: EN 4.46, DE 3.47; rough, English-trained metric) and stays stable when pitched.
  - Pitch and speed are applied at playback (`setPitch(1.1225f)`, `setSpeed(1.15f)`) with resample-style shift (Android `AudioTrack PlaybackParams.setPitch(1.1225f)` + speed compensation / Sonic). Do **not** use formant-preserving shifters (rubberband formant mode tested: clearly worse).
- **Languages = per-language model packs**: English, Deutsch, Français, Español, Italiano, Português, Nederlands. One pack ≈ 220 MB as published (bf16 safetensors); ONNX/int8 size for Android still to measure (T-143).
- **Setup (onboarding)**: language picker, **English preselected and always installed** as default/fallback; the chosen language pack is downloaded additionally via `NetworkGate` + ModelManager (resume, SHA-256). Changing language later in Settings downloads that pack. App UI strings exist for DE/EN first; other UI translations follow, voice packs are ready earlier.
- **Samples**: `assets/voice/samples/pim_<language>.mp3` (same sentence, 7 languages, +2 st). Reproduce: `tools/voice/pim_voice_multi.py anna english,german,...`, then `ffmpeg -af "asetrate=24000*1.122462,aresample=24000,atempo=1.024533"` (= +2 st at speed 1.15).
- Speed: ≈ 3× faster than real time on 2 desktop CPU cores; phone RTF to be measured on T-000 devices. Slow devices: Android system TTS as fallback (DeviceCheck RTF > 0.8).
- Android port reference: PocketTTS-Android-Engine (ONNX packs) — technical reference only, verify licence of any reused code.
- Rejected: Piper (all voices, quality), Kokoro German Kerstin (owner: unusable), separate engines per language (inconsistent voice).
- Lip-sync: TTS amplitude envelope (RMS per 30 ms) drives Pim's `speaking` motion — DESIGN_SYSTEM §4.

### Other voices
| lang | optional |
|---|---|
| de | Kokoro Victoria/Martin (kikiri), Thorsten medium (neutral male) |
| en | Kokoro `bf_emma` (British) |

- Do **not** ship lessac, ryan, amy voices (licence).
- Text prep: strip markdown, code blocks → "Codeblock übersprungen", URLs → domain only, numbers/dates read naturally (sherpa handles), split into sentences, synthesize sentence n+1 while playing n.
- Speed 0.8–1.5, setting.
- espeak-ng data needed by Piper is GPL-3.0 → fine (ADR-13).

## Voice mode (full screen, design 5)

v1 = STT → LLM (fast mode, short prompt) → TTS loop with barge-in (tap Pim to interrupt).
States: listening (green ring follows mic RMS), thinking (leaf wiggle), speaking (ring pulses with audio). Transcript toggle.
v2 = Omni native speech (LLM.md §10).
