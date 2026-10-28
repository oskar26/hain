# VOICE.md — dictation, read aloud, voice mode (ADR-12)

All via **sherpa-onnx** Android AAR (Apache-2.0). Models are packs (MODELS.md).

## STT (mic button)

- Default: **Moonshine Small Streaming** (MIT) — DE WER 7.5 %, EN 7.8 %. Streaming partial results into the composer.
- Optional pack: Parakeet TDT 0.6B v3 (better, bigger; check licence CC-BY-4.0 → attribution in About).
- VAD: Silero VAD (sherpa-onnx) — stop after 1.2 s silence (setting).
- Language: app language or auto (Moonshine per-language model → use app language; mixed input not supported in v1).
- Permission `RECORD_AUDIO` asked on first mic tap with explanation sheet.

## TTS (read aloud)

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
