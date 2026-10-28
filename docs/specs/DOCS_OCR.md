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
- "Eigene Dateien" library screen (v1: list + delete).

## Optional packs

- PaddleOCR-VL-1.6 (0.9B GGUF + mmproj) for tables/forms: needs mtmd native support (T-150). Output markdown tables.
- Omni/VL image questions: "Was ist auf dem Foto?" → T-150.
