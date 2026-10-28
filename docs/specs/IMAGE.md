# IMAGE.md — image editing & optional generation (ADR-17)

Research: `docs/concept/research/imagegen_edit.md`.

## Edit kit (base app, ~50 MB, LiteRT/ONNX)

| Tool | Model | UI |
|---|---|---|
| Select object | MediaPipe MagicTouch (Interactive Segmenter) | tap on object |
| Erase | MI-GAN (512 px inpainting) | brush or selected object → "Entfernen" |
| Cut out person | MODNet | one button "Freistellen" → transparent PNG |
| Upscale ×4 | Real-ESRGAN x4v3 (general) | tile 128 px with overlap 16 |

Pipeline: work on a copy, max 2,048 px side, tiles for upscale, undo stack (5 steps), save as new file in Pictures/Hain via MediaStore.
Licences: check every model file in LICENSES.md before adding (MI-GAN MIT, MODNet Apache-2.0, Real-ESRGAN BSD-3, MagicTouch Apache-2.0 — verify).

## Image generation (optional pack, ≥ 8 GB RAM)

- SD 1.5 + DMD2/LCM 4–8 steps, 512×512, MNN runtime. Chat model unloaded during generation (ModelSlot).
- Fixed safety: prompt blocklist (DE/EN) + NSFW classifier on output (e.g. a small NSFW detector, licence check) → blur + message. Cannot be disabled.
- Entry: `/bild …` or chip "Bild erstellen", or model tool `generate_image` only if pack installed.
- Later task group (T-17x), not needed for first beta.
