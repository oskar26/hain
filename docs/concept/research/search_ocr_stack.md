# Local ChatGPT-style Android app — Web search, OCR/PDF, tech stack
Research date: 2026-10-27 (Europe/Berlin). Target model: LFM3-1.7B (on-device, llama.cpp).
Prices from vendor pages fetched today unless marked otherwise. "Inferred" = my judgement, not a vendor statement.

---

## 0. Two facts from today that change the plan

1. **Liquid's LEAP SDK is deprecated.** Liquid now says new projects should use llama.cpp directly (LEAP was "a Kotlin Multiplatform wrapper around llama.cpp"). Artifacts stay on Maven (last v0.10.x, minSdk 31) but get no new releases.
   - https://docs.liquid.ai/deployment/on-device/sdk/overview
   - https://docs.liquid.ai/deployment/on-device/llama-cpp/mobile (official Android guide: llama.cpp `examples/llama.android` module or your own JNI via CMake `add_subdirectory`, model downloads via WorkManager, Q4_0 GGUF recommended on phones)
2. **LFM3 shipped today (2026-10-27).** LFM3-1.7B: 1.73B params, 256K context, LFM Open License v1.0. LFM3-VL-2B: 1.92B params (1.73B LM + 164M vision encoder), 128K context, German among 16 languages. **LFM3-VL support in llama.cpp is still under review (PR ggml-org/llama.cpp#31252), no video yet.** The tool-call format changed to a superset of `qwen3_coder` (XML `<tool_call><function=…>`), so the web-search tool parser must target that, not the LFM2 format.
   - https://www.liquid.ai/blog/lfm3 · https://huggingface.co/LiquidAI/LFM3-1.7B · https://huggingface.co/LiquidAI/LFM3-VL-2B
   - LFM Open License has a commercial revenue cap (secondary source says $10M; check the license text yourself): https://huggingface.co/LiquidAI/LFM3-1.7B/blob/main/LICENSE

---

## 1. Web search APIs as an LLM tool

| Provider | Free tier | Paid price | LLM-ready content? | Privacy | DE/EU results | ToS / app notes |
|---|---|---|---|---|---|---|
| **Brave Search API** | $5 credits/month (about **1,000 requests**), card needed against abuse. The landing page also says "up to $20 free/month"; the pricing doc says $5 | **$5 / 1k** requests (Search plan, 50 qps); Answers $4/1k + $5/M tokens | **Yes**: `/res/v1/llm/context` returns ranked, extracted passages (text, tables, code), token budget 1k–~33k. Also web/news/images/places | Own index, no Google/Bing fallback. "Architectural ZDR", queries not used for training; full ZDR is Enterprise only | `country=de`, `search_lang=de`; Goggles for reranking | Storing results needs a plan with storage rights. Google CSE migration guide. https://brave.com/search/api/ · https://api-dashboard.search.brave.com/documentation/pricing |
| **Tavily** (now "by Nebius") | **1,000 credits/month, no card** | PAYG $0.008/credit; Project $30 for 4k; basic search = 1 credit, advanced = 2 | **Yes**: snippets plus optional extracted content; Extract endpoint (5 URLs = 1 credit) | SOC 2/ISO; "Tavily EU" offering exists | Has a country parameter; German quality not verified | Free for students. https://www.tavily.com/pricing · https://docs.tavily.com/documentation/api-credits |
| **Exa** | $10 credits/month + $10 onboarding bonus, no card (about 2,500 Lite searches/month) | Lite $4/1k, Auto $7/1k, Deep $12/1k; Contents $1/1k pages | **Yes**: page text and highlights | ZDR only on Enterprise | Neural index, English-leaning (inferred) | https://exa.ai/pricing |
| **SearXNG** (self-host) | Free and unlimited (your server) | Your VPS (a few €/month) | No, only snippets (`format=json`); pair it with a page fetcher | **Best**: you control logs; upstream engines see your server IP, not the user | Aggregates Google/Bing/Brave/Qwant etc.; `language=de` | **Public instances usually disable JSON** and rate-limit bots, so don't ship an app against them. https://docs.searxng.org/dev/search_api.html |
| **DuckDuckGo** (unofficial) | Free | n/a | No | n/a | OK | **No official web-results API.** Libraries like `ddgs` scrape html/lite endpoints, hit "202 Ratelimit" CAPTCHAs, and break DDG's ToS. Don't use it in a shipped app. https://pypi.org/project/ddgs · https://github.com/deedy5/ddgs/issues/304 |
| **Serper** (Google SERP) | 2,500 queries one-time, no card | $50 = 50k ($1/1k) down to $0.30/1k; credits valid 6 months | No, SERP JSON snippets only | Proxies Google; scraping-based (Brave says this violates Google ToS) | **Best DE relevance** (it's Google), `gl=de&hl=de` | Legal/ToS grey area for production. https://serper.dev |
| **Jina Reader / Search** | Reader `r.jina.ai` keyless at 20 RPM; new key gets **10M free tokens**; Search `s.jina.ai` needs a key and costs ≥10k tokens/request (about 1,000 searches) | Token top-ups | **Yes**: URL → clean Markdown, PDFs too; search returns top-5 with full content | `DNT` header = no cache/log; EU-residency flag (experimental). Now part of Elastic | Locale header | Reader models are CC-BY-NC, but API use is fine. Site says "professional use only". https://jina.ai/reader/ |
| **Kagi Search API** | none listed | **$12 / 1k**; Extract $4/1k pages | Yes, "extra snippets: full, clean page content" | Strong privacy brand | Good (lenses, country) | Invoiced monthly/at $100. https://kagi.com/api/pricing |
| **Mojeek** | Free credit on Startup signup (card needed, no auto-charge) | **£2 / 1k** (Startup, 100k/day), £3/1k Business | No, only URL/title/snippet | **EU-only infrastructure, no sub-processors**, 90-day retention (ZDR on request) | Independent index; country/language boosting; smaller index (inferred) | Startup plan may only cache 1h; storage rights from Business up. https://www.mojeek.com/services/search/web-search-api/ |
| **Perplexity Search API** | none stated on pricing page | **$5 / 1k**; Fast Search $1/1k; up to 5 queries per request = 1 unit | Yes, ranked results with extracted page text in `snippet`, no LLM | Not assessed | Country bias, language filter | (Sonar is now under the "Agent API"; tool `web_search` $2.50/1k.) https://docs.perplexity.ai/getting-started/pricing |
| **Google Custom Search JSON** | (100/day, existing customers only) | $5/1k | No | Google | Best | **Closed to new customers; shuts down 1 Jan 2027.** Not an option. https://developers.google.com/custom-search/v1/overview |

### Recommendation
- **Default: Brave Search API, `llm/context` endpoint.** It returns pre-extracted, token-budgeted passages, which matters a lot for a 1.7B model with limited prefill speed on a phone. It runs on its own index, has a ZDR claim, supports `country=de`, and the free $5/month covers about 1,000 searches, plenty for a hobby app. Use `maximum_number_of_tokens` around 1,500–3,000 so mobile prefill stays fast (inferred).
- **Fallback: Tavily** (1,000 free credits, no card, content included). **Privacy option: self-hosted SearXNG** plus a page fetcher (Jina Reader keyless, or on-device jsoup + Readability) for users who want zero third-party API. Avoid DDG scraping and Serper for anything you publish.
- Abstract providers behind one `SearchProvider` interface (query → list of {title, url, text}). Then the BYO-key setting can switch Brave / Tavily / SearXNG-URL.

### Keeping API keys safe in an Android app
- **Never ship a key in the APK.** BuildConfig fields, `strings.xml` and NDK obfuscation can all be extracted in minutes with apktool/jadx.
- **Option A (simplest for a hobby app): user-supplied key ("bring your own key").** The user pastes their own Brave/Tavily key in settings. Store it encrypted with an Android Keystore-backed AES key (e.g. via Google Tink, or DataStore plus a Keystore-wrapped key). Note: `androidx.security:security-crypto` was deprecated in 2025 as far as I know; I didn't re-verify that today. Exclude it from backups (`android:dataExtractionRules`).
- **Option B (if you ship with your own key): thin proxy.** Use a Cloudflare Worker / small VPS endpoint that holds the key and forwards only `q`. Add: Play Integrity API token check, per-install rate limit (e.g. 50/day), a provider-side spend cap (Brave prepaid credits are a hard cap by design), and no query logging (state it in your privacy policy, which matters for GDPR). A proxy also lets you swap providers without an app update.
- Either way, send only the search query, never the chat history.

---

## 2. On-device OCR / PDF text extraction

### PDF text layer (try this first; it's exact and nearly free)
| Option | Notes | Licence |
|---|---|---|
| **Android `PdfRenderer.Page.getTextContents()` / `searchText()`** | New in API 35 (Android 15). PdfRenderer moved to a Play-updatable module, and a pre-V surface `PdfRendererPreV` reaches back to Android 11 (R) via SDK extensions. Rendering (`render()` to Bitmap) works since API 21 (PDFium inside). Also Jetpack `androidx.pdf` | AOSP (Apache 2.0). https://developer.android.com/reference/android/graphics/pdf/PdfRenderer.Page · https://android-developers.googleblog.com/2024/03/the-second-developer-preview-of-android-15.html |
| **PdfBox-Android** (`com.tom-roush:pdfbox-android:2.0.27.0`) | `PDFTextStripper` works on all API levels; heavier, pure Java, slower on big PDFs (inferred) | Apache 2.0. https://github.com/TomRoush/PdfBox-Android |
| **pdfium bindings** (e.g. `io.legere:pdfiumandroid`, API 24+) | Fast native text extraction and rendering | PDFium BSD-3; binding Apache 2.0. https://pdfium.googlesource.com/pdfium/+/HEAD/LICENSE |
| **MuPDF** | Excellent quality and speed | **AGPL** (your whole app must be open-source) or commercial. https://mupdf.readthedocs.io/en/latest/license.html |

Scanned-page heuristic (inferred): if a page's extracted text has fewer than about 30–50 characters, or mostly non-letter characters, render it at ~300 dpi (2–3× scale) and send it to OCR.

### OCR engines
| Engine | Speed / size | Accuracy (German, umlauts) | Licence / notes |
|---|---|---|---|
| **Google ML Kit Text Recognition v2 (Latin)** | Bundled about **+4 MB per script per ABI**, unbundled about +260 KB via Play Services. "Real-time on most devices" for Latin. API 21+ | German is a "Supported" language. Latin script covers ä ö ü ß. Gives block/line/element boxes. Weak on complex tables and handwriting (inferred) | Free, closed-source, Google ML Kit terms. Unbundled needs Play Services (bundled works without). https://developers.google.com/ml-kit/vision/text-recognition/v2/android · https://developers.google.com/ml-kit/vision/text-recognition/v2/languages |
| **PaddleOCR PP-OCRv5 mobile** (det + `latin_PP-OCRv5_mobile_rec`) | det ~4.7 MB; latin rec ~7.5 MB ONNX (~14 MB Paddle). Rec ~5.4 ms GPU / 21 ms CPU per crop (server benchmark). Runs via ONNX Runtime Mobile, ncnn (Android demo, API 21+), LiteRT or Paddle-Lite | Latin rec avg accuracy 84.7% on PaddleX's set; German among 32 Latin languages. Good on photos, rotated text | Apache 2.0, fully FOSS. https://huggingface.co/monkt/paddleocr-onnx · https://github.com/nihui/ncnn-android-ppocrv5 · https://paddlepaddle.github.io/PaddleX/3.3/en/pipeline_usage/tutorials/ocr_pipelines/OCR.html |
| **RapidOCR** | Packaged PP-OCR models (ONNX/MNN/etc.), includes `german` lang | Same as PP-OCR | Apache 2.0; mostly Python/C++, so Android needs your own JNI/ORT glue. https://rapidai.github.io/RapidOCRDocs/main/en/model_list |
| **Tesseract (Tesseract4Android 4.9.0 = Tesseract 5.5.1)** | `deu.traineddata` (fast/best variants). LSTM is slow on ARM (old report: 2.4 s legacy vs 15.6 s LSTM per page on a 2016 tablet). 16 KB page-size ready since 4.8.0 | Good on clean, binarised scans; poor on photos without preprocessing | Apache 2.0. tess-two is archived. https://github.com/adaptech-cz/Tesseract4Android |
| **docTR** | Python (PyTorch/TF); no ready Android path | Strong on documents | Apache 2.0; would need an ONNX export, so too much effort |
| **LFM3-VL-2B** (VLM) | 1.92B params; Q4_0 GGUF + F16 mmproj in llama.cpp. Seconds-to-tens-of-seconds per page on phone (inferred) | Document/OCR/Chart category **83.9** (vs 71.6 no-think, 71.2 LFM2.5-VL-3B); DocVQA 96.7, OCRBench v1 86.3, ChartQA 92.7, OCRBench v2 (En) 63.5. Liquid's numbers, run in thinking mode | LFM Open License v1.0. **llama.cpp support pending (PR #31252)**. "Not recommended for multi-turn conversation." https://huggingface.co/LiquidAI/LFM3-VL-2B |
| **Granite-Docling-258M** | GGUF Q8_0 ~170–178 MB + mmproj ~182 MB; already supported in llama.cpp (PR 16206) | Page → DocTags/Markdown with tables, formulas, lists. **English-focused** (German not officially supported) | Apache 2.0. https://huggingface.co/ibm-granite/granite-docling-258M · https://huggingface.co/ggml-org/granite-docling-258M-GGUF |
| SmolDocling-256M | Predecessor of Granite-Docling | — | CDLA-Permissive-2.0; superseded |
| GOT-OCR2.0 (580M) | Needs GPU in official code; community llama.cpp/ONNX ports | Strong formatted OCR | Repo says weights/data are research-only (conflicting HF Apache tag), so avoid for distribution. https://arxiv.org/abs/2409.01704 |
| olmOCR-2 (7–8B, Qwen2.5-VL-7B) | GPU/server only | SOTA English PDFs | Apache 2.0; **not on-device**. https://huggingface.co/allenai/olmOCR-2-7B-1025 |

### Recommended pipeline
1. **Text layer first:** `PdfRenderer.getTextContents()` on API 35+ (or PreV on API 30+); fall back to pdfium/PdfBox-Android on older devices or for odd PDFs.
2. **Scanned pages / photos → ML Kit Text Recognition v2 (bundled Latin).** Fast, tiny, good German. Rebuild reading order from line boxes (sort by y, then x; detect columns). If you want a fully open-source build, offer **PP-OCRv5 mobile + latin rec via ONNX Runtime** as the alternative engine.
3. **Tables, charts, forms, handwriting, or low OCR confidence → VLM pass**, on demand, per page or per crop:
   - Now: **Granite-Docling-258M** (works in llama.cpp today, Apache 2.0) for table/structure → Markdown. Fine for numbers and layout, weaker for German prose.
   - When llama.cpp merges LFM3-VL: **LFM3-VL-2B** as the main "smart OCR / chart Q&A" model (German-capable, 83.9 doc score). Prompt it with the page image plus "Transcribe as Markdown; tables as GFM tables". Use thinking mode only for chart reasoning, not plain transcription (latency, inferred).
4. Load models one at a time: unload LFM3-1.7B → run the VLM → reload. Two ~1–1.5 GB Q4 models resident at once is risky on 6–8 GB phones (inferred). Cache OCR results per file hash in the DB.

---

## 3. Recommended tech stack

**Kotlin + Jetpack Compose (native Android)** is the right call:
- llama.cpp's own Android example (`examples/llama.android`) is Kotlin + NDK/CMake with a ready `InferenceEngine` (Flow<String> streaming), and Liquid's docs use exactly that. ML Kit, WorkManager, Room and PdfRenderer are all first-class Kotlin APIs. No bridge layer between UI and the JNI hot path.
- **Flutter:** works via `dart:ffi` to llama.cpp, but you write and maintain C glue, plus platform channels for ML Kit/WorkManager. Only worth it if you know Dart and want iOS. **React Native:** JSI/TurboModules plus community llama bindings, the most moving parts. **KMP:** only if iOS is planned. Room 2.7+/3.0 and `BundledSQLiteDriver` are KMP-ready, so keep business logic in pure Kotlin modules and you can move to KMP later.

**Native inference layer**
- Add llama.cpp as a git submodule and `add_subdirectory` it in `app/src/main/cpp/CMakeLists.txt` (turn off tests, examples, server). Write your own thin JNI (load, tokenize, decode loop, sampler, cancel) or reuse the `llama.android` `lib` module. Enable `LLAMA_BUILD_MTMD` for vision.
- `abiFilters = arm64-v8a` only. Use NDK r27+ for **16 KB page-size** compliance (Android 15+ requirement). CPU backend by default; Vulkan/OpenCL (Adreno) need per-device testing (Liquid's advice).
- Threads = number of big cores (≈ cores − 2). Keep `n_ctx` small (4–8K) even though LFM3 supports 256K, because the KV cache grows linearly. mmap the GGUF from `filesDir`.
- LFM3-1.7B recommended sampling (from card family): temp 0.1, top_k 50, rep. penalty ~1.05–1.1. Parse `qwen3_coder`-style XML tool calls for `web_search(query)`.

**Local DB / RAG / memory**
- **Room** (2.8.x stable, minSdk 23; Room 3.0 announced Mar 2026: KSP-only, coroutine-first, drops SupportSQLite) with **`BundledSQLiteDriver`**, which gives you a modern SQLite with **FTS5** for keyword search.
- Vectors: **sqlite-vec** loaded via `BundledSQLiteDriver.addExtension("sqlitevec","sqlite3_vec_init")` (working example: https://github.com/zeeshanali-k/GenAI_PG). Pre-v1, brute-force KNN, which is fine for a personal corpus (tens of thousands of chunks). Brute-force cosine in plain Kotlin is also viable: about 8 ms for 3,000×384 vectors in one report.
- **ObjectBox 4.x** gives a real HNSW index in Kotlin and scales to millions, but adds about 10–15 MB to the APK and is its own non-SQL DB. Pick it only if the corpus gets large.
- Hybrid retrieval = FTS5 BM25 + vector KNN, merged with reciprocal-rank fusion (inferred best practice). You need a small embedding model (e.g. a GGUF embedding model via llama.cpp `--embedding`; not researched here).
- https://developer.android.com/kotlin/multiplatform/sqlite · https://github.com/asg017/sqlite-vec · https://docs.objectbox.io/on-device-vector-search

**Background model downloads**
- **WorkManager** `CoroutineWorker` with `setForeground()` (foreground service type `dataSync`) for 1–2 GB GGUFs. Resume with HTTP `Range`, verify with SHA-256, constrain to unmetered network + storage-not-low. Write to `filesDir/models/` (a real file is needed for mmap). Liquid's docs recommend exactly this.

**Android version**
- **targetSdk 36** (Play requires API 36 for new apps/updates since 31 Aug 2026). https://developer.android.com/google/play/requirements/target-sdk
- **minSdk recommendation: 29 (Android 10)**, about 91% cumulative reach (apilevels.com, April 2026 StatCounter data). Room needs ≥23, ML Kit ≥21, LEAP needed 31. The real limit is RAM (6 GB+ for a 1.7B Q4 model plus OCR), and those phones mostly run Android 12+. **minSdk 31** (about 69–79% reach depending on source) is acceptable if you want simpler APIs. Use `getTextContents()` only behind `Build.VERSION.SDK_INT >= 35` / an SDK-extension check.
- https://apilevels.com/ · https://composables.com/android-distribution-chart

---

## 4. Gaps / not verified
- No hands-on German result quality test of Brave/Tavily/Mojeek; DE notes are qualitative.
- Brave free tier: landing page "up to $20/month" vs pricing doc "$5/month" (I used $5).
- Perplexity and Kagi free tiers: none stated on the pages I read.
- No phone benchmarks for LFM3-VL-2B or Granite-Docling OCR latency; numbers are vendor benchmarks only.
- LFM Open License commercial threshold comes from a secondary source; read the license.
- Status of `androidx.security:security-crypto` deprecation not re-checked today.
