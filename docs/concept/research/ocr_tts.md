# On-device OCR, TTS and ASR quality for a local DE/EN Android chatbot
Research date: 2026-10-27 (Europe/Berlin). Companion to `search_ocr_stack.md`, `stack.md`, `konzept2.md` in this folder.
**(inferred)** = my judgement, not taken from a source. All benchmark numbers are the publisher's own unless a third party is named.

---

## TL;DR verdicts

| Area | Ship this | Why |
|---|---|---|
| OCR, everyday (photos, scans, screenshots) | **PP-OCRv5 mobile det + `latin_PP-OCRv5_mobile_rec`** (Apache-2.0, ~12 MB), render/upscale to ≥300 dpi, plus a small German post-correction step. ML Kit v2 (Latin, bundled) is an equal-quality alternative if Google dependency is OK. | Classic OCR is fast and good on printed German; umlauts mostly fine, known weak spots are capital **Ü** and **ß→B** at low resolution. |
| OCR, hard pages (tables, forms, multi-column, handwriting, low confidence) | **PaddleOCR-VL-1.6 (0.9B)** on demand via llama.cpp (support merged) or LiteRT-LM | Best open document parser (OmniDocBench v1.5 94.5 for v1.5; v1.6 96.33 on OmniDocBench v1.6), 109 languages, already measured on a phone GPU. |
| "Ask about this image/chart" | **LFM3-VL-2B** (Doc/OCR/Chart 83.9) once llama.cpp merges it | General VLM, German-capable; overkill for plain transcription. |
| TTS English | **Kokoro-82M v1.0 fp16** (Apache-2.0, `af_heart`) on fast phones; **Piper** commercially clean voice (`en_GB-cori-medium` public domain, or community "Clean 100") as the fast default/fallback | Kokoro is clearly more natural (UTMOS 4.52) but slow on Android. |
| TTS German | **Piper `de_DE-thorsten-medium`** as default (fast, CC0 data) + optional **Kokoro-German** download (Thorsten-Voice/Kokoro or kikiri Victoria/Martin) for flagships; Android system TTS as zero-size fallback | German Piper voices score far below English ones; Kokoro-German sounds better but has no published MOS and runs at or above real time on phones. |
| ASR | **Moonshine Small Streaming (de + en, MIT, 123M)** for the mic button; **Parakeet TDT 0.6B v3** if you can afford ~0.6B params and want best accuracy | Moonshine is no longer English-only; Whisper base is poor in German (≈26 % WER). |

---

## 1. OCR quality

### 1.1 Classic engines (line detectors + recognisers)

| Engine | German support | Quality evidence | Size / speed | Licence |
|---|---|---|---|---|
| **PaddleOCR PP-OCRv5 mobile, Latin rec** | German is one of ~40 languages in `latin_PP-OCRv5_mobile_rec`; dictionary contains ä ö ü ß Ä Ö Ü | **84.7 %** line accuracy on PaddleX Latin set of 3,111 images (vs 37.9 % for PP-OCRv3 Latin) — http://www.paddleocr.ai/latest/en/version3.x/algorithm/PP-OCRv5/PP-OCRv5_multi_languages.html · a third-party leaderboard lists it at 81.4 % — https://www.sota2.com/research/sota/text-recognition-on-latin-script-evaluation-benchmark. CVPR 2026 paper: PP-OCRv5 (general model) weighted accuracy 80.1 % vs 53.0 % for v4, "rivals billion-parameter VLMs" on OCR — https://arxiv.org/html/2603.24373 | det ~4.7 MB, latin rec ~7.5 MB ONNX (see `search_ocr_stack.md`) | Apache-2.0 |
| — German user reports | | Lowercase umlauts reliable; **capital Ü missed** in some fonts and at 200 dpi ("Uberprüfung"); **ß read as B** ("GröBe", "StraBe") in some fonts; at 300 dpi nearly all correct (server model, PaddleOCR 3.0) — https://github.com/PaddlePaddle/PaddleOCR/issues/15457 · Another user confirms the Latin mobile rec "detects German diacritics fine, even at 150 dpi" — https://github.com/PaddlePaddle/PaddleOCR/issues/14861 · The default (non-Latin) model lacks some German letters, so use the Latin rec explicitly (same thread) | | |
| **RapidOCR** | Ships the same PP-OCR models (incl. German/Latin) | Same accuracy as PP-OCR; it is packaging (ONNX/MNN runtimes), not a different model | same | Apache-2.0 |
| **Google ML Kit Text Recognition v2 (Latin)** | German listed as "Supported" — https://developers.google.com/ml-kit/vision/text-recognition/v2/languages | Field-level F1 on mobile card/badge/screenshot set: ML Kit **75.5 %**, PP-OCRv4 74.7 %, tesseract.js 59.0 %; ML Kit degraded best on "hostile" images (75.9) — https://prasad.tech/blog/benchmarking-on-device-ocr.md (English-dominant set; PP-OCRv5 not tested). Google: characters should be ≥16×16 px — https://developers.google.com/ml-kit/vision/text-recognition/v2/android | Bundled or Play-Services model, very fast | Proprietary, free; Google dependency/telemetry |
| **Tesseract 5 (`deu`)** | Good German dictionaries | Excellent on clean synthetic scans (CER 0.33 % vs PaddleOCR 2.1 %) but collapses after degradation/preprocessing (CER 15 %) — https://github.com/kyupark/ocr-pipeline-demo · On noisy forms/receipts (FUNSD/SROIE/CORD) mean CER 0.43–0.80 vs PaddleOCR 0.31–0.48 — https://arxiv.org/html/2609.17538v2 · Handwriting CER 18.6 % vs TrOCR 4.2 % (industrial comparison) — https://www.ijsdr.org/papers/IJSDR2610024.pdf | ~15–50 MB, slower | Apache-2.0 |

**Handwriting:** none of the classic engines is good at it (Tesseract 18.6 % CER above; PaddleOCR mobile has no dedicated handwriting model). ML Kit "Digital Ink" only works on pen strokes drawn on screen, not on photos — https://developers.google.com/ml-kit/vision/digital-ink-recognition. Use a VLM for handwritten notes.

**Watch item:** a sota2 leaderboard row lists "PP-OCRv6 small/medium" at 84/88 % on the Latin benchmark (June 2026), and PaddleOCR docs mention a newer 50-language model with +5.1 % recognition over PP-OCRv5. I only saw search excerpts, not the release page — verify before switching.

### 1.2 VLM-OCR (document parsing)

OmniDocBench v1.5 overall (higher = better), from the official README / papers:

| Model | Params | OmniDocBench v1.5 | Notes | On-device status |
|---|---|---|---|---|
| GLM-OCR | 0.9B | 94.62 | https://arxiv.org/html/2603.10910v1 | not checked |
| **PaddleOCR-VL-1.5** | 0.9B | **94.50** (text edit 0.035) | https://raw.githubusercontent.com/opendatalab/OmniDocBench/v1_5/README.md | — |
| PaddleOCR-VL (1.0) | 0.9B | 92.86 | same | — |
| **PaddleOCR-VL-1.6** | 0.9B | **96.33 on OmniDocBench v1.6** (new SOTA claim) | https://github.com/PaddlePaddle/PaddleOCR/releases | **llama.cpp support merged** (PR #18825, build b8110+), official GGUF https://huggingface.co/PaddlePaddle/PaddleOCR-VL-1.6-GGUF ; LiteRT-LM build measured on **Galaxy S26 GPU: decode 35.6 tok/s, prefill 1,239 tok/s, TTFT 190 ms; CPU run failed (degenerate output)** — https://huggingface.co/litert-community/PaddleOCR-VL-1.6 |
| MinerU2.5 | 1.2B | 90.67 | https://preview.aclanthology.org/acl-fellows/2026.acl-industry.3.pdf | server-oriented |
| dots.ocr | ~3B | 88.41 | benchmarklist / OmniDocBench | too big for most phones (inferred) |
| Granite-Docling-258M | 0.26B | not on the board; own card: full-page OCR F1 0.84, edit distance 0.45, OCRBench 500; English only + experimental JA/AR/ZH | https://huggingface.co/ibm-granite/granite-docling-258M · Galaxy S26 CPU **35.4 s/page** (LiteRT-LM) https://huggingface.co/litert-community/granite-docling-258M · near-zero on handwritten French text in a French benchmark https://arxiv.org/html/2602.11960v1 | runs, but not for German |
| LFM3-VL-2B | 1.9B | not reported; Liquid's "Document, OCR & Chart" category **83.9** (Qwen3.5-2B 79.1, Gemma 4 12B 85.7), Multilingual 84.4 | https://www.liquid.ai/blog/lfm3 | llama.cpp support pending (see `search_ocr_stack.md`) |

PaddleOCR-VL's own multilingual line-level test: Latin-script edit distance **0.013** (in-house set, 109 languages) — https://arxiv.org/html/2510.14528v2. No German-specific or German-handwriting number was found for any VLM; treat handwriting quality as unknown until tested.

Note: the 83.9 for LFM3-VL is an average of Liquid's chosen benchmarks (DocVQA, OCRBench, ChartQA …), not a transcription CER, so it is not directly comparable to OmniDocBench.

### 1.3 OCR verdict and pipeline

**Verdict:** For printed German (letters, school sheets, receipts, screenshots) PP-OCRv5 mobile Latin is good enough and tiny; quality issues are predictable (capital Ü, ß/B at low DPI) and fixable. For complex layouts and handwriting, a 0.9B document VLM is a step change, and PaddleOCR-VL-1.6 is the best open choice that already runs on a phone.

Recommended pipeline:
1. **PDF with text layer** → extract text directly (PdfRenderer / PDFium), no OCR.
2. **Photo / scan** → deskew + crop (ML Kit Document Scanner or OpenCV) → render/upscale so text lines are ~32–48 px high (≈300 dpi) → **PP-OCRv5 mobile det + latin rec** (ONNX Runtime / ncnn). Use the Latin rec, never the default Chinese/English rec.
3. **German post-correction (inferred, cheap):** dictionary/regex pass for `Uber→Über`, `Ubung→Übung`, `Anderung→Änderung`, `B` between lowercase letters → `ß` when the corrected word is in a German word list; or let LFM3-1.7B fix obvious OCR errors when summarising.
4. **Escalate to PaddleOCR-VL-1.6** (Q8/Q4 GGUF + mmproj, load on demand, unload the chat model first) when: average line confidence < ~0.8, tables/forms detected, many short boxes (handwriting), or user asks for "as Markdown/table".
5. **LFM3-VL-2B** only for questions about the image ("what does this chart show?"), not for full-page transcription.
6. Alternative if Google dependency is acceptable: ML Kit v2 bundled Latin instead of step 2 (similar quality, easier integration).

---

## 2. TTS voice quality (German and English)

### 2.1 Comparison

| Engine / voice | Languages | Quality numbers | Size | Speed (RTF, lower = faster) | Licence |
|---|---|---|---|---|---|
| **Piper** (VITS, ONNX) | ~50 languages | Loudness.ai ranking of 1,683 commercially usable voices (UTMOS22 naturalness + Whisper CER clarity, 0–100): **best English 96.4** (community Clean 100), en_GB Cori 92.4; **best German only 81.8** (Karlsson), MLS spk 162 81.2, **Kerstin 80.2**, German median 70.0 — https://loudness.ai/assets/research/piper-voices-v1/LA-RB-2026-003-en.pdf (thorsten not scored there, see licence column). Kokoro-paper comparison: `en_US-lessac-medium` UTMOS **4.36** — https://arxiv.org/html/2610.06817v1 | low/medium ~60–65 MB, high ~110–130 MB (thorsten-high) | Laptop M4, 2 threads: medium ≈0.08–0.09, LibriTTS-high 0.57 (Loudness.ai). **Android field data: median 0.247 (p10–p90 0.115–0.782), 297 installs** — https://eist.app/research/on-device-tts-benchmarks/piper | **Engine:** current `OHF-Voice/piper1-gpl` is **GPL-3.0** (licence changed in v1.3.0, July 2025; old MIT `rhasspy/piper` archived) — https://github.com/OHF-Voice/piper1-gpl/releases/tag/v1.3.0 . **Voices:** thorsten (all), kerstin = **CC0** dataset; eva_k / karlsson / ramona = M-AILABS "BSD-style", original licence page offline — https://huggingface.co/cstr/piper-voices-GGUF , https://huggingface.co/nrl-ai/edgevox-models/blob/main/LICENSE.md ; pavoque = CC BY-NC-SA. **en_US-lessac = non-commercial (Blizzard 2013); ryan = CC BY-NC-SA; amy = Lessac-lineage.** Loudness.ai also excludes **thorsten-medium/high as "Lessac lineage"** (fine-tuned from lessac) as a precaution. |
| **Kokoro-82M v1.0** (StyleTTS2) | EN (US/UK, 28 voices), ES, FR, IT, HI, PT-BR, JA, ZH — **no official German** | `af_heart` UTMOS **4.52**, WER 5.7 % (vs Piper lessac 4.36 / 8.8 %, KittenTTS nano 4.01) — https://arxiv.org/html/2610.06817v1 ; TTS Arena V2: rank 33/42, Elo 1471, still among the top open small models (secondary source https://hivebook.wiki/wiki/kokoro-tts-82m-open-weight-multilingual-text-to-speech) | fp32 326 MB, **fp16 164 MB (spectral corr 0.999)**, int8 114 MB (corr 0.916) — https://github.com/thewh1teagle/kokoro-onnx/releases . Users report int8 sounds "low-bitrate MP3"/sibilant on a Galaxy S23 — https://github.com/NickMonrad/kernel-ai-assistant/issues/854 | **Android field median 3.48 (slower than real time!)**, 63 installs, sherpa-onnx fp32 — https://eist.app/research/on-device-tts-benchmarks/kokoro ; MediaTek MT6991 flagship ≈1.15 — https://www.retar.app/blog/on-device-neural-tts-android ; Snapdragon 8 Gen 3 with QNN NPU: 306 ms to first audio — https://github.com/cedgeremek/kokoro-offline-tts-android | Apache-2.0 (needs espeak-ng = GPL-3 for G2P, see 2.3) |
| **Kokoro German (community)** | DE only, one voice per weight set | kikiri-tts recipe (51-speaker German base, CC0) → voices **Martin** (m), **Victoria** (f), Bernd (m) — https://github.com/semidark/kikiri-tts ; **Thorsten-Voice/Kokoro** (official Thorsten fine-tune, Apache-2.0, CC0 data, epoch 5 default "more natural" by ear; known bug: short ü `ʏ` must be mapped to `y`) — https://huggingface.co/Thorsten-Voice/Kokoro ; **Kerstin** ONNX with own German G2P, no espeak-ng — https://huggingface.co/crane-local-ai/Kokoro-82M-v1.0-German-ONNX . **No MOS/UTMOS published for any German Kokoro voice.** | 82M each; ONNX exports: https://huggingface.co/Godelaune/Kokoro-82M-ONNX-German-Martin , https://huggingface.co/soniqo/Kokoro-82M-German-Victoria-ONNX | same as Kokoro (≈1–3.5 on phones) | Apache-2.0 weights; data CC0 (Thorsten) / see cards |
| **Android system TTS (Google Speech Services)** | very many | Offline voice packs rated "fair"; network voices excellent but cloud — https://lecternreader.app/blog/best-offline-text-to-speech-apps , https://speechcentral.net/2026/04/18/speech-central-on-android-vs-ios-in-2026-a-detailed-platform-comparison/ ; fastest engine in VoicePing's Android benchmark (33 tok/s) — https://voiceping.net/en/blog/research-offline-tts-eval/ | 0 MB in your APK | fastest | proprietary, free; may stream to Google unless offline pack installed |
| **LFM3-Audio-2B** | 18 languages / 19 locales incl. German; one female + one male voice per language | **English TTS UTMOSv2 3.83, WER 1.4** (LFM2.5-Audio 3.22, Qwen2.5-Omni-3B 3.42). **No German TTS number.** — https://huggingface.co/LiquidAI/LFM3-Audio-2B , https://www.liquid.ai/blog/lfm3 (UTMOSv2 ≠ UTMOS22, so not comparable with the Kokoro 4.52) | 2B params (~1.3 GB Q4, see `konzept2.md`) | LiquidCodec 87× real time on 1 M4 Max thread; streaming; **no phone numbers yet** | LFM Open License v1.0 |
| **Kitten TTS** | legacy nano/mini (15–80M): English only; **KittenTTS 2** (1.7B, 1-bit, 506 MiB): 20 languages incl. German, voice cloning — https://github.com/KittenML/KittenTTS/blob/main/README.md | nano 0.8 UTMOS 4.01 (Kokoro paper) | 25–80 MB legacy | fast | Apache-2.0 (legacy, per tts-bench); KittenTTS 2 licence not checked |
| **NeuTTS** | Air: **English only**, 748M, Apache-2.0; Nano (~120M): EN/ES/DE/FR, NeuTTS Open License — https://github.com/neuphonic/neutts , https://alphasignal.ai/news/neuphonic-s-neutts-brings-voice-cloning-to-your-device-without-cloud-apis | no German MOS | GGUF | Picovoice: Neu-TTS-Nano Q4 first-audio slower than Kokoro — https://picovoice.ai/blog/on-device-tts/ | see left |
| **Chatterbox Multilingual V3** | 23 languages incl. German, cloning, watermark | "preferred vs ElevenLabs" (vendor) | 0.5B | Picovoice measured Chatterbox-Turbo at ~48 s latency on device → not phone-real-time (inferred) | MIT — https://github.com/resemble-ai/chatterbox |
| **MeloTTS** | EN/ES/FR/ZH/JA/KO — no German (from project README; not re-verified this session) | TTS.ai community 3.8/5 naturalness vs Piper 3.5 — https://tts.ai/tts-arena | ~160 MB | fast | MIT |
| **Matcha-TTS (sherpa-onnx)** | only `en_US-ljspeech` and `zh-baker` models in sherpa-onnx — https://k2-fsa.github.io/sherpa/onnx/tts/index.html | — | small | best speed/efficiency balance on iOS (VoicePing) | Apache-2.0 / dataset |

**sherpa-onnx** (Apache-2.0) runs Piper/VITS, Kokoro, Matcha, Kitten on Android with a ready TTS-engine APK; German models in its zoo: thorsten (low/medium/high), thorsten_emotional, kerstin, eva_k, karlsson, ramona, pavoque, mls, plus community "miro" and "dii" — list and audio samples: https://k2-fsa.github.io/sherpa/onnx/tts/all/German/index.html . Kokoro in sherpa-onnx ships only EN/ZH multi-lang packs; German Kokoro needs a custom export (inferred from the kokoro docs page).

### 2.2 Audio sample links
- Piper all voices: https://rhasspy.github.io/piper-samples/
- Piper ranked explorer with samples and licences: https://loudness.ai/research/piper-tts-voices-ranked/
- sherpa-onnx German samples (thorsten high/medium/low, kerstin, eva_k, ramona …): https://k2-fsa.github.io/sherpa/onnx/tts/all/German/index.html (e.g. https://k2-fsa.github.io/sherpa/onnx/tts/all/German/vits-piper-de_DE-thorsten-medium.html)
- Kokoro-German Thorsten (14 samples, umlauts, ich/ach-Laut, ß): https://huggingface.co/Thorsten-Voice/Kokoro/tree/main/test_audio_epoch5
- Kokoro-German Victoria samples: https://huggingface.co/kikiri-tts/kikiri-german-base-51speakers-synthetic ; Bernd: https://huggingface.co/kikiri-tts/kikiri-german-bernd
- Kokoro English voices: https://huggingface.co/onnx-community/Kokoro-82M-v1.0-ONNX
- LFM3-Audio demo Space: https://huggingface.co/spaces/hugging-apps/lfm3-audio-2b
- Many open TTS models side by side (UTMOS/WER + listen): https://5uck1ess.github.io/tts-bench/scores.html

### 2.3 Licence notes (not legal advice)
- **Piper engine is GPL-3.0** now (piper1-gpl). **espeak-ng is GPL-3.0** too, and Piper voices and Kokoro (misaki/espeak) normally phonemise with it. For an open-source (GPL-compatible) app this is fine; for a closed-source app you need either the GPL obligations, a separate engine process, or a G2P without espeak-ng (e.g. Crane's German Kokoro Kerstin with its own lexicon/rules) (inferred).
- **Voice files:** thorsten + kerstin = CC0 dataset (thorsten medium/high flagged as Lessac-lineage by Loudness.ai, legally unsettled); M-AILABS voices (eva_k, karlsson, ramona) = BSD-style but source page offline; avoid pavoque (NC), en_US-lessac (NC), en_US-ryan (NC). Commercially clean English Piper: en_GB-cori (public domain), community Clean 100 (CC BY 4.0, credit).
- Kokoro weights and German fine-tunes: Apache-2.0. LFM3-Audio: LFM Open License v1.0.

### 2.4 TTS verdict
- **English:** **Kokoro-82M fp16, `af_heart`** is the quality pick (UTMOS 4.52 vs Piper's 4.36, Apache-2.0). But field data says Kokoro is **slower than real time on the median Android phone (RTF 3.48)**, so: Kokoro on flagships / NPU, or pre-synthesise sentence-by-sentence while the LLM streams; **Piper `en_GB-cori-medium`** (public domain, 92.4) as the default/fallback on mid-range phones. Avoid lessac/ryan for licence reasons.
- **German:** default **Piper `de_DE-thorsten-medium`** (22 kHz, best-known German voice, CC0 data, RTF ~0.25 on Android). If you want a female voice: **kerstin-low** (CC0, 80.2, but 16 kHz). Offer **Kokoro-German** (Thorsten-Voice/Kokoro for male, kikiri Victoria for female) as an optional "natural voice" download (~82–164 MB) for fast phones; listen first, no MOS exists. German Piper voices top out at ~82/100 vs ~96 for English, so expect German to sound noticeably more robotic.
- **Voice mode:** if LFM3-Audio-2B is loaded anyway for spoken chat, use its own speech output (German supported) instead of a second TTS model; quality in German is unmeasured.
- **Fallback:** Android system TTS (zero size), with the user's offline German pack.

---

## 3. On-device speech recognition (brief)

| Model | DE WER | EN WER | Size | Licence |
|---|---|---|---|---|
| Whisper tiny / base / small / medium (zero-shot, FLEURS German) | **37.5 / 25.6 / 14.7 / 10.5 %** — https://aclanthology.org/2023.swisstext-1.8.pdf ; MLS-German (8 clips): small 18.1 %, large-v3-turbo 12.0 % — https://greffier.app/benchmarks/ | LibriSpeech clean ≈ base 5.0 %, small 3.4 % (Whisper paper, via https://openwhispr.com/blog/whisper-model-sizes-explained) | 39M / 74M / 244M / 769M | MIT |
| **Moonshine** (no longer English-only) | **German Small Streaming 123M: 7.5 %; Tiny Streaming 34M: 12.0 %** (400-clip macro, not FLEURS-comparable) | Medium Streaming 245M 6.65 %, Small 7.84 %, Tiny 12.0 % (Open ASR avg) | 34–245M | **MIT** (streaming models) — https://moonshine-voice.readthedocs.io/en/stable/models/available-models/ |
| **Parakeet TDT 0.6B v3** | FLEURS de **5.04 %** (NVIDIA), 4.84 % on-device Mac test (Whisper large-v3-turbo 5.08 %) — https://yapapp.cc/questions/speech-recognition-accuracy-german ; MLS-de 13.7 % int8 vs Whisper small 18.1 % (Greffier) | ~4.5 % FLEURS en (yapapp) | 600M | CC-BY-4.0 per NVIDIA card (not re-checked this session) — https://huggingface.co/nvidia/parakeet-tdt-0.6b-v3 |
| **Vosk small de 0.15** | 13.75 % (Tuda-de), 30.67 % (podcast); umlaut words fail with grammar mode — https://alphacephei.com/vosk/models , https://github.com/alphacep/vosk-api/issues/2040 | small en-us 9.85 % (LibriSpeech clean) | 45 MB | Apache-2.0 |
| LFM3-Audio-2B | multilingual FLEURS avg (9 langs) 6.0 % | Open ASR avg 6.2 % | 2B | LFM Open License |

**ASR verdict:** Mic button → **Moonshine Small Streaming** (de and en models, MIT, streaming, 123M) with Silero VAD via sherpa-onnx or Moonshine's own library; offer **Parakeet v3** (25 European languages, auto language detection, ~5 % German WER) as "high accuracy" if storage/RAM allow. Don't ship Whisper base for German (≈26 % WER); Whisper small is the minimum, and Moonshine/Parakeet beat it. Vosk only for tiny command grammars.

---

## Gaps / not established
- No German MOS/UTMOS for Kokoro-German voices, thorsten (Piper) in an independent ranking, or LFM3-Audio German TTS.
- No German-handwriting CER for any engine; PaddleOCR-VL handwriting in German is untested.
- No phone latency for LFM3-VL/LFM3-Audio; PaddleOCR-VL-1.6 phone numbers are from one Galaxy S26 GPU run (CPU failed).
- PP-OCRv6 / newer PaddleOCR multilingual model seen only in search excerpts.
- MeloTTS language list and Parakeet licence taken from memory/cards without re-fetching this session.
