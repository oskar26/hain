# DOCS_OCR.md — PDFs, photos, OCR (ADR-11)

Research: `docs/concept/research/ocr_tts.md`.

## Pipeline

```
input (PDF | image | camera)
 → PDF? PdfTextExtractor (PdfRenderer can't read text → use pdfbox-android (Apache-2.0) for text layer)
     page has ≥ 40 chars text → use it
     else render page at 300 dpi (PdfRenderer) → OCR
 → image: normalize (EXIF rotate, max side 2,480 px ≈ 300 dpi A4, grayscale copy for OCR)
 → OcrEngine = PP-OCRv5 mobile (det + cls + **Latin rec**) via ONNX Runtime Android (or RapidOCR-Android wrapper, decide T-121)
 → GermanOcrFixer (pure Kotlin)
 → confidence < 0.80 or table detected (many aligned boxes) → offer "Genauer lesen" with PaddleOCR-VL-1.6 pack (later task)
 → chunks (≈ 400 tokens, per page) → doc_chunk + FTS + vec
```

## GermanOcrFixer rules (unit tests with the examples)

1. `Uber`/`UBER` at word start → `Über` if the corrected word is in the dictionary (`assets/dict/de_words.txt`, ~200k words from a free list, licence in LICENSES.md).
2. `B` between lowercase letters (`GröBe`, `StraBe`) → `ß` if dictionary hit.
3. Also `A/O/U` + lowercase rest → `Ä/Ö/Ü` only with dictionary hit and original not a word.
4. Join hyphenated line breaks: `Ver-\nsicherung` → `Versicherung` if dictionary hit.
5. Never change words that are already in the dictionary. English text (detected by stopword ratio) → skip rules 1–3.

Tests: `Uberprüfung→Überprüfung`, `GröBe→Größe`, `StraBe→Straße`, `BUS` unchanged, `Bus` unchanged, `Uber` (company) unchanged when in English sentence.

## Using documents in chat

- Attach → reading progress bar → "📄 Rechnung.pdf · 3 Seiten" chip in composer.
- Small doc (< 3,000 tokens) → put text directly into the user message as `<document name="…">…</document>`.
- Larger → `search_documents` tool active for this chat, top 4 chunks with page numbers; answer cites `[S. 2]`.
- "Eigene Dateien" library screen (v1: list + delete). Built as **"Meine Dateien"** (EN "My files"): list with preview, tap = attach again, delete, text recognition pack (load / remove).
- Tool name as built: `search_document` (args `query`, optional `document`); only in the prompt while a long document is attached in the chat.

## As built (Häppchen 9, 0.1.9)

- Module `core-docs`: `internal/pdf` (PdfTextExtractor, PdfPages), `internal/text` (TextFiles), `ocr` (OcrPack, OrtOcrEngine, DbPostProcess, CtcDecoder, ReadingOrder, RecDictionary, OcrImage), `fix` (GermanOcrFixer, WordList), `internal/chunk` (Chunker), `internal/db` (hain_docs.db), `api` (DocLibrary, DocPrompt, DocAttachment, SearchDocumentTool).
- **T-121 decision: own ONNX Runtime pipeline** (not RapidOCR-Android). ONNX Runtime Android **1.28.0, pinned** (1.29+ ships a Microsoft telemetry initializer); `ai.onnxruntime.TelemetryInitializer` is removed in the manifest anyway. Same version as Häppchen 10.
- Models (Apache-2.0) from PaddlePaddle's own Hugging Face repos, pinned commits, SHA-256 checked, together 13.9 MB, stored in `filesDir/ocr/`: `PP-OCRv5_mobile_det_onnx@e6f4fa8`, `latin_PP-OCRv5_mobile_rec_onnx@89d3a50` (+ its `inference.yml` with the 836-character dictionary), `PP-LCNet_x0_25_textline_ori_onnx@aea1f18`. Downloaded only after "Texterkennung laden?" via `NetworkGate.stream` (purpose MODEL_DOWNLOAD, user-started). "Pack entfernen" in "Meine Dateien".
- Pipeline: detection on the long side ≤ 1,280 px (multiples of 32, BGR, ImageNet mean/std) → DB post-process (thresh 0.3, box 0.6, unclip 1.5, axis-aligned boxes) → crop per line, vertical lines turned, orientation model (180° when ≥ 0.9) → recognition at height 48 → greedy CTC → reading order (lines by vertical overlap, paragraphs by gap, two columns by a gutter no box crosses, full-width titles first) → GermanOcrFixer. Lines below 0.3 certainty are dropped.
- Not built yet: "Genauer lesen" with PaddleOCR-VL (confidence < 0.80 / tables), `doc_chunk_fts`/`doc_chunk_vec` (search is BM25 in memory over the attached documents, enough for a handful of files), EXIF-less HEIC on old phones.

## Optional packs

- PaddleOCR-VL-1.6 (0.9B GGUF + mmproj) for tables/forms: needs mtmd native support (T-150). Output markdown tables.
- Omni/VL image questions: "Was ist auf dem Foto?" → T-150.
