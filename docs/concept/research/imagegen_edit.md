# On-device image GENERATION + EDITING for an Android local-chat app (Kotlin, llama.cpp)
Research date: 2026-10-27. Extends `img.md` (same folder). Target: mid-range 6–8 GB phones (Snapdragon 7 Gen x / Dimensity 7000-class), storage-tight, privacy-first.
Legend: **[measured]** = number from a source; **[est.]** = my extrapolation. "Mid-range" numbers are rare; most published phone numbers are flagship.

---

## 0. TL;DR

- **Edit tools are the real win.** Several are tiny (1–30 MB), permissively licensed, run in tens–hundreds of ms on NPU/GPU and ≈0.5–3 s on CPU, and are genuinely useful: **MI-GAN eraser (16 MB, MIT)**, **Real-ESRGAN-General-x4v3 upscaler (1.3–4.7 MB, BSD-3)**, **MODNet portrait cut-out (26 MB, Apache-2.0)**, **MediaPipe MagicTouch tap-to-select (6 MB, Apache-2.0)**. Ship these by default (~50 MB total).
- **Generation is still SD-1.5-class on mid-range.** Prior finding holds: **SD1.5 + DMD2/LCM (4–8 steps), ~1.0–1.15 GB**, is the only open, commercially usable generator that is practical on 6–8 GB mid-range phones. New in 2026: Apache-2.0 **FLUX.2 klein 4B**, its ternary build **Bonsai Image 4B**, **Z-Image-Turbo** and **Qwen-Image-2.1** do run on Android – but at 4–24 GB downloads, 5–8 GB RAM and 2–10 min/image even on flagships. Not for mid-range.
- **The good small 2026 models are still locked:** SnapGen++ (no weights), MobileDiffusion (no weights), BlazeEdit (Google, paper only), DreamLite (gated, CC BY-NC), Mobile-O (CC BY-NC-SA, iOS only). NanoFLUX (Samsung, 2.5 s on S25 Ultra) – paper only.
- **No mainstream small LLM (Gemma 4, Qwen3.x-VL/Omni, LFM2.5, MiniCPM-V 4.7) can output images.** Unified "understand+generate" small models exist (Janus-Pro-1B, Mobile-O) but are low quality (384 px) or non-commercial. Practical path = LLM writes the prompt, separate diffusion module draws.
- **Instruction editing ("make it night", "remove the man") on phone is not practical** in an open, commercial way on mid-range. Closest real thing: **MNN-Sana-Edit-V2** (Apache-2.0, 1.66 GB, but a fixed cartoon filter, 45–62 s on Snapdragon 8-series, 5.5 GB RAM) – a gimmick.

---

## 1. Image GENERATION – 2026 landscape

| Model | Params / download | Weights & licence | Android runtime | Phone numbers | Verdict for mid-range |
|---|---|---|---|---|---|
| **SD 1.5 + DMD2/LCM** (e.g. Absolute Reality, DreamShaper 8) | ~1.29 B; **MNN OpenCL pkg 1.15 GB** (unet 863 MB + TE 238 MB + VAE 50 MB); **QNN zips 0.99–1.16 GB** | Open, CreativeML OpenRAIL-M (fine-tunes: own terms) | MNN (CPU/OpenCL, Apache-2.0), QNN (Snapdragon NPU), stable-diffusion.cpp (slow) | SD 7 Gen 4 NPU: UNet 162 ms/pass, VAE 383 ms **[measured, model-only]** → 8-step DMD2 ≈2–4 s end-to-end **[est.]**; SD 7-series CPU ~30 s at 20 steps **[measured, Off Grid]** → 8–15 s with DMD2 **[est.]**; Local Dream: DMD2 "≈4× faster, much less heat" | **Only realistic choice.** Optional download. |
| SD1.5 **inpainting** (9-ch UNet, e.g. DreamShaper 8 Inpainting) | ~1 GB | OpenRAIL-M | QNN | 512², 20 steps **3.9 s on Galaxy S25 Ultra** (8 Elite) **[measured]** (hf: AbrahamPJ/dreamshaper-8-inpainting-qnn) | Good "generative fill" but a second 1 GB model. Local Dream's inpaint is latent-blending with the normal model (cannot really remove objects – per that model card). |
| **SANA-Sprint 0.6B** | 0.6 B DiT + Gemma-2-2B TE (~2–3 GB total) | Apache-2.0 (Gemma terms for TE) | community QNN build | 1024², 2 steps ≈3.5 s on 8 Gen 3 (community claim, from img.md) | Flagship-only; TE RAM too high. |
| **FLUX.2 klein 4B** | 4 B + Qwen3-4B TE; **~7.4 GB** LiteRT bundle (Box app) | **Apache-2.0** (9B variant is non-commercial) | LiteRT (Box), stable-diffusion.cpp GGUF | Box: "photorealistic in 4 steps"; no mid-range timing | Not for mid-range. Notable: also supports **image editing / multi-reference** – the first permissive editor-capable model that runs on Android at all. |
| **Bonsai Image 4B** (PrismML ternary/1-bit FLUX.2 klein) | **~4.3 GB** LiteRT bundle (Box); 10.7 GB NPU bundle (Duration port) | Apache-2.0 | LiteRT CPU (Box), QNN (Duration), sd.cpp fork | Box: "a couple of minutes per image, ~8 GB RAM" (CPU). S25+ (8 Elite) NPU: **~140 s/512²**, peak ~5 GB; sd.cpp CPU **8–9 min** **[measured]** | Too slow/big. Quality clearly better than SD1.5 (Box: "noticeably better"). |
| **Z-Image-Turbo** (6B, Tongyi) | GGUF Q4 ≈3.7–3.9 GB DiT + Qwen3-4B Q4 ≈2.5 GB + VAE; Box LiteRT bundle **~10.6 GB** | Apache-2.0 | stable-diffusion.cpp (Termux; Local-Diffusion app outdated), LiteRT (Box) | sd.cpp has an Android bug report (Galaxy Tab S10 FE+, Q4_K + Qwen3-4B Q3) – runs on CPU, no usable timing; AMD iGPU/CPU desktop: 12–14 min | Not for phones in practice. |
| **Qwen-Image-2.1** | DiT int4 4.5 GB (2-bit 3.6 GB) + Qwen3-VL-8B TE 4.7 GB + VAE; repo 23.7 GB total | **Qwen Research licence** (commercial restrictions) | MNN OpenCL (libQwenImage21, Sep 2026) | 8 Gen 2/16 GB: **200–220 s** (6-step turbo), 400–450 s (20 steps); POCO F6 12 GB: OOM on turbo, 255 s @320 px **[measured]** | Tech demo only. |
| SnapGen++ (Snap, Jan 2026, rev. Jul 2026) | 0.3 B tiny ("budget Android") / 0.4 B small / 1.6 B full | **No weights or code** (project page = paper only) | – | 1024², 4 steps: tiny 1.2 s, small 1.8 s on iPhone 16 Pro Max | Watch. |
| NanoFLUX (Samsung, ICML 2026) | 2.4 B (from FLUX.1-schnell) | **Paper only** | – | 512² in ~2.5 s on S25 Ultra | Watch. |
| DreamLite (ByteDance) | 0.39 B + 4-bit Qwen-VL | Gated, CC BY-NC (merged into diffusers Jun 2026) | – | <1 s @1024² Xiaomi 14 (paper) | Not shippable. |
| Mobile-O-0.5B (MBZUAI, Feb 2026) | 1.6 B total (FastVLM-0.5B + SANA-600M-512) | **CC BY-NC-SA / BY-NC**, iOS (MLX/CoreML) only | – | ~3 s/512² iPhone 17 Pro, <2 GB | Not shippable. |
| BitDance (ByteDance, Feb 2026) | **14 B** AR (34 GB); 260M/1B only ImageNet class-conditional | Apache-2.0 | none | – | Server-class; irrelevant for phones. |
| Janus-Pro-1B (DeepSeek) | ~1.5 B, 4.2 GB | MIT code, DeepSeek model licence | ONNX (browser/WebGPU demos) | – | 384 px, weak aesthetics → toy. |
| JuZhou 1.0, Chitra Tiny (from img.md) | 0.39 B / 0.21 B | unclear / open | – | – | Chitra 256 px = toy. |

### Can a chat LLM itself generate/edit images?
- **Gemma 4 (E2B/E4B/12B), Qwen3-VL / Qwen3-Omni, LFM2.5-VL, MiniCPM-V 4.7: input-only for images; output text (Omni: +speech).** Qwen-Omni explicitly "cannot generate images or video".
- Unified generators that are small: Janus-Pro-1B (weak), Mobile-O (NC), SenseNova-U1.5 (8B, Apache – too big). None fits a llama.cpp phone stack.
- **Design implication:** keep the chat LLM as the "prompt writer / tool caller" (it can expand "a cat in space" into a good SD prompt and choose an edit tool), and call separate image modules. Unload the LLM while a diffusion model runs (RAM).

---

## 2. Image EDITING tools

Mid-range reference point: **Snapdragon 7 Gen 4 is the only mid-range chip Qualcomm AI Hub profiles**; Dimensity 7000-class has no public per-model numbers → run on GPU via LiteRT/MNN/ncnn or CPU.

### 2a. Object eraser / inpainting
| Model | Size | Licence | Speed | Quality | Runtime |
|---|---|---|---|---|---|
| **MI-GAN** (Picsart, ICCV'23) – 512² Places2 | **16.3 MB** fp16 LiteRT | **MIT** | Pixel 8a GPU (LiteRT CompiledModel) ~6 ms; Galaxy S26 GPU 26.6 ms / NPU 30.9 ms; Pi 5 CPU 1.8 s **[measured]**; paper: 256² on Galaxy Tab S7+ CPU (ONNX) ~0.69 s. Mid-range GPU: tens–hundreds of ms **[est.]** | Good for small/medium objects, "magic eraser" level; smears on large regions | LiteRT (GPU/NPU), ONNX Runtime. Already shipped in Box app (Erase tile). |
| **LaMa-Dilated** | **174 MB** fp32 (≈45–90 MB if quantized **[est.]**) | Apache-2.0 | 512²: 8 Gen 3 NPU 54 ms, 8 Gen 1 NPU 109–134 ms, 8 Elite Gen 5 34 ms **[measured]**; no 7-series row | Better than MI-GAN on large holes/repeating textures | QNN / ONNX / LiteRT |
| AOT-GAN | 58 MB | MIT | AI Hub profiled | Older; LaMa/MI-GAN preferred | QNN |
| SD1.5-inpaint (9-ch) | ~1 GB | OpenRAIL-M | 3.9 s on 8 Elite NPU (above) | Generative fill (add things) | QNN; only in the optional generator pack |
| BlazeEdit (Google, May 2026) | 195 M params (remove, outpaint, relight, tone, sticker) | **Not released** | 290 ms Pixel 10 TPU | Strong (paper) | – |

### 2b. Background removal / segmentation
| Model | Size | Licence | Speed | Notes |
|---|---|---|---|---|
| **MODNet** | **26 MB** (LiteRT) | **Apache-2.0** | Pixel 8a: GPU 60–79 ms, CPU 419 ms **[measured]** | People/animals only; fails on objects |
| **ML Kit Subject Segmentation** | ~200 KB app + model via Google Play services (one repo says ~10 MB, unverified) | Google ML Kit terms (free) | ~200 ms Pixel 7 Pro **[measured, Google]** | Multi-subject, objects+pets+people, very easy. **But:** needs Play services (not on de-Googled ROMs), beta, and ML Kit ships Google "Clearcut" usage logging (Box app had to disable it) → clash with a privacy-first pitch. |
| **BiRefNet-lite** (512 export) | 98 MB fp16 / 192 MB fp32 (int8 ≈48 MB on OpenVINO) | **MIT** | Desktop browser: 1.1 s WebGPU, 1.6 s WASM; no phone number → mid-range CPU likely 2–6 s **[est.]** | Best general quality among open-licence models |
| ISNet-general int8 | 44 MB | MIT | 0.32 s WebGPU / 2.2 s WASM (desktop) | Leaves haze, keeps props |
| U²-Netp | 4.7 MB | Apache-2.0 | fast | Low quality fallback |
| RMBG-1.4 / RMBG-2.0 (BRIA) | 44 MB q8 / ~1 GB | **Non-commercial** (bria licence / CC BY-NC 4.0) | – | Do not ship (unless free app & you accept NC). |
| **MediaPipe Interactive Segmenter (MagicTouch)** | **~6 MB** (float32 v1); v2 int8 768² `.task` | **Apache-2.0** | Pixel 10: CPU 208 ms, GPU 581 ms; ~30 FPS Pixel 7 GPU (v1 card) **[measured]** | Tap-a-point → object mask. Perfect input for the eraser and for "cut out this object". MediaPipe Tasks Kotlin API. |

### 2c. Upscaling / restoration
| Model | Size | Licence | SD 7 Gen 4 NPU (per 128² tile) | Flagship | Verdict |
|---|---|---|---|---|---|
| **Real-ESRGAN-General-x4v3** | **4.65 MB** fp32 / **1.25 MB** w8a8 | BSD-3 | **2.4 ms/tile** **[measured]** → 1024² input (64 tiles) ≈0.2 s NPU; CPU/GPU ~1–5 s **[est.]** | 8 Elite Gen 5 2.7 ms (fp) | **Ship.** Best size/quality. |
| Real-ESRGAN-x4plus | 63.9 MB / **16.7 MB** w8a8 | BSD-3 | **30–37 ms/tile** **[measured]** → ≈2–2.5 s for 1024² input NPU; CPU tens of s **[est.]** | 8 Gen 3 50 ms | Optional "HQ" (Box bundles it). |
| XLSR (3×) | 115 KB / 46 KB | Qualcomm AIMET zoo licence | 0.4 ms/tile | – | Real-time but mild; nice "quick" option. |
| EDSR ×4 | (Box bundles; size n/a) | – | – | – | Alternative. |
| NAFNet deblur/denoise | bundled in Box (size n/a) | MIT (upstream) | GPU, 256² tiles | – | Useful "fix blurry/noisy photo" tool; worth a look. |
| **GFPGAN v1.4** (face restore) | **431 MB** fp16 LiteRT (+0.3 MB YuNet) | Apache-2.0 | – | S26 NPU 32 ms, GPU 89 ms; Pixel 8a GPU 265 ms–1.2 s; CPU 6 s **[measured]** | Works, but 431 MB → optional download only. |
| CodeFormer | ~360 MB | **S-Lab NC** | – | – | Don't ship. |
| GPEN-BFR-256 | 72 MB | check (non-commercial per common listings – verify) | – | – | Smaller alternative, verify licence. |
| CFRNet (Jun 2026 paper) | 2 M params | not released? | – | – | Watch. |

### 2d. Style transfer / "AI filters"
| Model | Size | Licence | Speed | Verdict |
|---|---|---|---|---|
| Fast-Neural-Style (Johnson; litert-community) | **3.5 MB per style** (4 styles) | BSD (PyTorch examples) | ~9 ms @256² Pixel 8a GPU **[measured]** | Cheap toy filter; fine as a bonus. |
| Magenta arbitrary style transfer (TFLite) | int8 2.8 MB + 0.2 MB | Apache-2.0 | ~100s of ms | Any style image; toy-ish. |
| AnimeGANv3 | 1.1–4 MB | **Non-commercial** (needs author letter) | fast | Popular but licence blocks commercial apps. |
| MNN-Sana-Edit-V2 (cartoon/Ghibli edit) | **1.66 GB** (Qwen3-0.6B 373 MB + DiT 884 MB + VAE enc/dec 318 MB + connector 76 MB) | Apache-2.0 | 512², 10 steps: OnePlus 13 (8 Elite) **45 s**, Xiaomi 12 Pro (8 Gen 1) **62 s**, iPhone 17 Pro 1.4–4.7 s; **RAM ≈5.5 GB; needs Snapdragon 8+** **[measured, vendor]** | Gimmick for mid-range: one fixed style, huge, slow. |

### 2e. Instruction-based editing ("change the sky to sunset")
- **InstructPix2Pix** (SD1.5-based, ~1 GB, MIT-ish/OpenRAIL) would cost about the same as SD1.5 img2img with CFG on two conditionings (≈2× UNet passes) → ≈1.5–2× SD1.5 time; no maintained Android port found. Quality is dated (frequent over-editing). **Not recommended.**
- **Qwen-Image-Edit / FLUX.1 Kontext**: 12–20 B → impossible on phone. **FLUX.2 klein 4B** (Apache-2.0) is the only permissive modern editor that has been run on Android (Box, via LiteRT, generation); editing would need 7+ GB download and minutes/image – flagship-only experiment.
- DreamLite (edit+gen, 0.39 B) and BlazeEdit would be ideal but are NC/unreleased.
- **Practical substitute:** let the chat LLM map instructions to deterministic tools ("remove X" → MagicTouch + MI-GAN; "upscale" → ESRGAN; "cut out" → MODNet/MagicTouch; "make it brighter/warmer" → classic color ops; "make it a painting" → style net). This covers most real requests at ~50 MB.

---

## 3. Android runtimes (edit + gen)
| Runtime | Use for | Licence | Notes |
|---|---|---|---|
| **LiteRT (CompiledModel)** | MI-GAN, MODNet, ESRGAN, GFPGAN, style – litert-community has ready `.tflite` + Kotlin snippets; GPU (ML Drift) + Qualcomm/MediaTek NPU JIT | Apache-2.0 | Best fit for a Kotlin app; XNNPACK CPU declines some fp16 graphs (MI-GAN, MODNet) → provide int8/fp32 CPU fallback or ONNX. |
| **MediaPipe Tasks** | MagicTouch interactive segmenter | Apache-2.0 | Kotlin API, 6 MB model. (MediaPipe *Image Generator* is deprecated.) |
| **ONNX Runtime (+QNN EP / NNAPI)** | any ONNX model (BiRefNet, LaMa, MI-GAN ONNX) | MIT | NNAPI deprecated in Android 15; QNN EP for Snapdragon. |
| **Qualcomm AI Hub / QNN** | precompiled per-chip binaries incl. **SD 7 Gen 4** | model licences + QNN redistribution terms | Fastest on Snapdragon; nothing for MediaTek. |
| **MNN** | SD1.5 (OpenCL/CPU, new Hexagon backend 3.6.1), Sana-Edit, Qwen-Image | Apache-2.0 | Best cross-vendor generator engine. |
| **ncnn** | Real-ESRGAN-ncnn-vulkan, many GAN ports | BSD-3 | Vulkan GPU on any vendor; good for ESRGAN on Dimensity. |
| **stable-diffusion.cpp** | SD/FLUX/Z-Image/Qwen GGUF | MIT | Pairs naturally with llama.cpp (same ggml), Vulkan/OpenCL; but phone CPU speed is poor (minutes). |

---

## 4. Recommendation

### Ship by default (≈45–55 MB total, all permissive, all useful)
1. **Magic eraser:** MediaPipe **MagicTouch** (6 MB, tap-to-select) + **MI-GAN** (16 MB, MIT). LiteRT GPU; CPU fallback. Mid-range: well under 1 s on GPU **[est.]**.
2. **Upscale ×4:** **Real-ESRGAN-General-x4v3** (1.3–4.7 MB, BSD-3), tiled. (+XLSR 0.1 MB as "fast".)
3. **Portrait cut-out / background blur:** **MODNet** (26 MB, Apache-2.0). For objects use MagicTouch mask.
4. Optional tiny bonus: Fast-Neural-Style (3.5 MB/style) filters.
→ Let the chat LLM route natural-language edit requests to these tools (tool calling), with an "edited on-device" badge.

### Optional downloads ("Image Lab")
- **Generator:** **SD1.5 fine-tune + DMD2/LCM, 512², 4–8 steps** (~1.0–1.15 GB). Engine: **MNN** (works on all vendors; Apache-2.0) and, on Snapdragon V68+/7 Gen 1+, **QNN** `_min` binaries (Local Dream community zips – check NC licence of Local Dream *code*; model zips are OpenRAIL-M). Gate on ≥8 GB RAM; unload the LLM during generation; add an NSFW filter (age-appropriate users / Play policy). Expected mid-range: ~2–5 s on SD 7-Gen NPU, ~10–30 s CPU/GPU on Dimensity **[est.]**.
- **HQ add-ons:** Real-ESRGAN-x4plus (16.7 MB int8), LaMa (large-hole eraser, ~174 MB / smaller quantized), BiRefNet-lite (better object cut-out, 48–98 MB), GFPGAN (431 MB face fix), SD1.5-inpaint (~1 GB "generative fill", Snapdragon NPU).
- **Flagship-only experiment (≥12 GB RAM):** Bonsai Image 4B / FLUX.2 klein 4B (Apache-2.0, 4.3–7.4 GB, minutes/image) – much better quality and (klein) editing, but not for the target device.

### Gimmicks / skip
- MNN-Sana-Edit-V2 cartoonizer (1.66 GB, 45–62 s, flagship only), Qwen-Image-2.1 on Android (24 GB repo, 3–7 min), Z-Image-Turbo on phone, Janus-Pro-1B, Bonsai on CPU (8–9 min), InstructPix2Pix, AnimeGANv3 (NC), RMBG-1.4/2.0 (NC), CodeFormer (NC), DreamLite/Mobile-O (NC).
- **ML Kit Subject Segmentation:** technically great and tiny, but Play-services-dependent + Google telemetry → conflicts with the privacy promise; use only if you accept that (or offer as opt-in).

### Watch list (could flip the generator verdict)
SnapGen++ tiny (0.3 B, "for budget Android") weights; NanoFLUX; BlazeEdit (would be the ideal default edit model at 195 M); DreamLite licence change; LiteRT GPU support for 2-GB+ graphs (would make Bonsai faster).

---

## 5. Sources (opened)
- SnapGen++ paper/project: https://arxiv.org/abs/2601.08303 , https://snap-research.github.io/snapgenplusplus/ (no code/weights links)
- BlazeEdit: https://arxiv.org/html/2605.28067
- NanoFLUX: https://arxiv.org/abs/2602.06879 ; S25U breakdown via alphaXiv summary
- Mobile-O: https://github.com/Amshaker/Mobile-O , https://huggingface.co/Amshaker/Mobile-O-0.5B (CC BY-NC)
- DreamLite: https://arxiv.org/abs/2603.28713 , GitHub (gated, NC)
- BitDance: https://github.com/shallowdream204/BitDance , https://arxiv.org/abs/2602.14041
- Box app (Bonsai/FLUX.2 klein/Z-Image LiteRT, MI-GAN erase, ESRGAN, NAFNet, ML Kit Clearcut note): https://github.com/jegly/Box
- Bonsai on S25+ NPU: https://www.duration.ai/blog/generating-images-with-a-2025-android
- FLUX.2 klein licence: https://huggingface.co/black-forest-labs/FLUX.2-klein-4B , https://bfl.ai/blog/flux2-klein-towards-interactive-visual-intelligence
- Qwen-Image-2.1 on Android (MNN): https://huggingface.co/Qwen/Qwen-Image-2.1/discussions/51 ; file sizes via HF API evankuo/Qwen-Image-2.1-MNN
- Z-Image on sd.cpp: https://github.com/leejet/stable-diffusion.cpp/blob/master/docs/z_image.md , Android issue https://github.com/leejet/stable-diffusion.cpp/issues/1264
- MNN-Sana-Edit-V2: https://huggingface.co/taobao-mnn/MNN-Sana-Edit-V2 (sizes via HF API), benchmarks https://www.besthub.dev/articles/how-mnn-sana-edit-v2-brings-comic-style-image-editing-to-your-phone-in-15-seconds-2e0715fa3611
- SD1.5 MNN sizes: HF API taobao-mnn/stable-diffusion-v1-5-mnn-opencl; QNN zips: xororz/sd-qnn
- SD1.5 Qualcomm (7 Gen 4): https://huggingface.co/qualcomm/Stable-Diffusion-v1.5
- SD1.5 inpaint QNN: https://huggingface.co/AbrahamPJ/dreamshaper-8-inpainting-qnn
- Local Dream features (inpaint, DMD2): https://ld-guide.chino.icu/features , https://github.com/xororz/local-dream
- MI-GAN LiteRT: https://huggingface.co/litert-community/MI-GAN-512-Places2-LiteRT ; paper https://openaccess.thecvf.com/content/ICCV2023/papers/Sargsyan_MI-GAN_A_Simple_Baseline_for_Image_Inpainting_on_Mobile_Devices_ICCV_2023_paper.pdf
- LaMa: https://huggingface.co/qualcomm/LaMa-Dilated
- Real-ESRGAN: https://huggingface.co/qualcomm/Real-ESRGAN-x4plus , https://huggingface.co/qualcomm/Real-ESRGAN-General-x4v3 ; XLSR https://huggingface.co/qualcomm/XLSR
- MODNet LiteRT: https://huggingface.co/litert-community/MODNet-LiteRT
- Browser BG-removal comparison (BiRefNet-lite/ISNet/BEN2/MODNet): https://swordfishsoft.org/blog/background-removal-models-in-the-browser/
- RMBG licences: https://huggingface.co/briaai/RMBG-1.4 , https://github.com/Bria-AI/RMBG-2.0
- ML Kit Subject Segmentation: https://developers.google.com/ml-kit/vision/subject-segmentation , /android
- MediaPipe Interactive Segmenter: https://ai.google.dev/edge/mediapipe/solutions/vision/interactive_segmenter ; model card https://storage.googleapis.com/mediapipe-assets/Model%20Card%20MagicTouch.pdf
- GFPGAN LiteRT: https://huggingface.co/litert-community/GFPGAN-v1.4-LiteRT ; CodeFormer licence https://github.com/sczhou/CodeFormer/blob/master/LICENSE
- Style: https://huggingface.co/litert-community/Fast-Neural-Style-LiteRT ; AnimeGANv3 licence https://github.com/TachibanaYoshino/AnimeGANv3
- Qwen-Omni output modalities: https://blog.gopenai.com/qwen-omni-a-deep-technical-dive-into-alibabas-unified-multimodal-model-family-b4133525c0e5

## 6. Gaps
- No public per-model benchmarks on Dimensity 7000/7300-class or Snapdragon 7 Gen 1–3 for MI-GAN, MODNet, BiRefNet, LaMa; mid-range CPU/GPU figures above are extrapolated.
- ML Kit subject-segmentation model download size not officially stated (~10 MB claim unverified).
- GPEN licence and EDSR/NAFNet bundle sizes not verified.
- Qualcomm AI Hub "7 Gen 4" timings are model-only (no pre/post-processing, no tiling overhead).
- Bonsai/FLUX.2 klein LiteRT editing capability on Android not verified (Box exposes T2I only).
