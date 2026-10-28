# Memory strategy for the on-device chatbot (LFM3-1.7B, Kotlin, Room/SQLite, sqlite-vec)

Research date: 2026-10-27. Scope: what memory design works best for a **small (1.7B) local model** with a practical 8–32K context on a phone, a privacy-focused, non-technical audience, and the existing stack (Room + FTS5 + sqlite-vec + LFM2.5-Embedding-350M). Ties into concept item 13 "Gedächtnis: merkt sich Vorlieben – sichtbar und löschbar" (`konzept.md`, `k2.md`).

---

## TL;DR recommendation

**A layered hybrid. The app's code owns the structure; the model only extracts and classifies, and it never edits files freely.**

| Layer | What | In prompt? | Who writes | Budget |
|---|---|---|---|---|
| **L0 Profile ("Über mich")** | 10–25 short, stable facts (name, language, diet, school/job, standing preferences) | **Always** | User directly, or promotion from L2 after confirmation | ≤ 1,500 chars (~400–500 tokens) |
| **L1 Topic notes** | Short per-topic summaries ("Ernährung", "Schule", "Fitness", "Projekt X") | Index line always (~100 tok); body only when relevant | Background consolidation (app-triggered), user-editable | ≤ 2,000 chars per topic, ≤ 30 topics |
| **L2 Fact store** | Atomic facts with embedding + metadata (time, source chat, category, validity) | Top 5–8 relevant facts per turn (~300–400 tok) | Live "Merk dir…" + background extraction after a chat | ≤ 200 chars/fact; soft cap ~2,000 active |
| **L3 Chat history** | All messages; per-round embeddings + FTS5; per-session summary | Only when triggered (search) | Automatic (no LLM needed except summary) | Unlimited storage; ≤ 600 tok injected |

- **Writes:** explicit ("merk dir", "vergiss") = **live and deterministic**, with an undo chip. Implicit = **background extraction** after the conversation goes idle (WorkManager), with **grammar-constrained JSON** and **code-side reconciliation** (embedding similarity first, then one constrained-choice LLM call). **Never hard-overwrite** a fact: mark it superseded with `valid_to`.
- **Reads:** deterministic retrieval by the app before each turn: hybrid FTS5 + vector, fused with RRF, re-ranked by recency/importance. No model tool call is needed for normal recall. One optional `search_memory` tool for "what did I say last month about…".
- **Transparency:** a "Gedächtnis" screen (profile, topics, all memories, suggestions, settings), a "used memories" indicator per answer, export and full delete.

Why this shape: commercial assistants converged on **"small always-loaded profile + retrieval over past chats + user-visible editable entries"**. Research shows LLM-curated stores (file trees, graphs) **only stay organized with strong models**, and graph pipelines are far too write-expensive for a phone. Details and sources below.

---

## 1. How commercial assistants do memory

### ChatGPT (OpenAI)
- Two independent controls. **Saved memories** are explicit details the user asks it to remember, or that ChatGPT saves as useful. **Reference chat history** draws relevant information from past chats, and "can change as ChatGPT updates what is most useful to remember". Saved memories are stored separately from chat history. **Deleting a chat does not delete a saved memory made from it.** Temporary Chat uses no memory. Projects can isolate memory.
  - https://help.openai.com/en/articles/8590148-memory-faq · https://help.openai.com/en/articles/10169521-projects-in-chatgpt · https://help.openai.com/en/articles/8914046
- Takeaway: a curated **fact list** plus **implicit retrieval over history**, two toggles, a manage UI. In the Mem0 paper's LoCoMo evaluation, "OpenAI memory" scored lowest of the memory systems (52.9 J), so a fact list alone loses detail (https://arxiv.org/abs/2504.19413).

### Claude (Anthropic): three different mechanisms
1. **Claude.ai consumer memory:** started (2025) as a **memory summary** synthesized from chats, project-scoped, viewable and editable (https://claude.com/blog/memory). As of Aug 2026 it is a **set of individual, categorized entries ("Topics") that Claude reads and updates during conversations**, replacing the daily summary. Every topic is listed in Settings → Memory and can be edited or deleted. **Sensitive subjects (health, beliefs) are not stored unless the user opts in.** Chat search sits alongside it.
   - https://claude.com/blog/claudes-memory-works-everywhere-and-you-decide-whats-in-it · https://support.claude.com/en/articles/11817273 · https://support.claude.com/en/articles/12138966-release-notes
2. **Claude Code:** `CLAUDE.md` files (user/project scope) plus **auto memory**: plain markdown the user can edit. Only "the first 200 lines of MEMORY.md, or the first 25KB" load at start. The docs warn that "longer files consume more context and reduce adherence".
   - https://code.claude.com/docs/en/memory
3. **API memory tool (`memory_20250818`):** a client-side `/memories` directory. The model issues `view / create / str_replace / insert / delete / rename` and checks the directory before starting tasks. The developer implements storage and must guard against path traversal and memory poisoning.
   - https://platform.claude.com/docs/en/agents-and-tools/tool-use/memory-tool · cookbook: https://platform.claude.com/cookbook/tool-use-memory-cookbook

### Gemini (Google)
- **Personal context / "Memories":** learns key details and preferences from past chats (default on in many regions), plus **Saved info** (explicit), user **instructions**, and Temporary Chats. Not available under 18 or on supervised accounts. A "Remembered Items" dashboard for explicitly saved memories is in testing (Oct 2026). Gemini also imports memory from other assistants.
  - https://blog.google/products-and-platforms/products/gemini/temporary-chats-privacy-controls · https://support.google.com/gemini/answer/16598469 · https://support.google.com/gemini/answer/16413516 · https://www.androidheadlines.com/2026/10/google-gemini-memory-remembered-items-bulk-delete-tools.html · https://gemini.google/us/release-notes/

### File-based memory (Claude Code, Letta MemFS, and similar assistants)
- The pattern: an always-loaded profile file, per-topic markdown files, and dated logs. The model reads and edits them with file tools, and users can open and edit the same files. Letta now projects memory blocks into a **git-backed markdown filesystem (MemFS)** and runs periodic "defragmentation" subagents to split and merge files (https://docs.letta.com/letta-agent/memory).
- Strengths: transparent, human-editable, cheap reads. Weakness: quality depends on the model **curating** the tree (see §2, filesystem study).

**Convergent pattern across vendors:** (a) a small, always-present profile or instructions; (b) implicit retrieval from past chats; (c) **individually viewable, editable, deletable entries**; (d) sensitive-category guardrails; (e) incognito mode; (f) scopes (projects). All of these vendors use frontier models for curation. A 1.7B model cannot carry the same curation load, so the app must.

---

## 2. Research and open source

| System | Core idea | Relevance for a 1.7B phone app |
|---|---|---|
| **MemGPT / Letta** (https://docs.letta.com/v1-sdk/memory/memory-blocks, https://docs.letta.com/v1-sdk/memory/archival-memory, https://www.letta.com/blog/agent-memory/) | OS analogy. **Core memory blocks** (labelled, size-limited, always in context, agent-editable) vs **archival memory** (vector DB, tool-queried) vs **recall memory** (searchable message history). Recommended limits: <20 blocks, <50k chars. | Adopt the *layering* (L0 ≈ core, L2 ≈ archival, L3 ≈ recall). Don't adopt "agent self-edits core memory via tools on every turn": too much tool-use burden for 1.7B. |
| **Generative Agents** (Park et al. 2023, https://arxiv.org/abs/2304.03442) | Memory stream of timestamped observations. Retrieval score = recency (exponential decay) + importance (LLM-rated 1–10) + relevance (embedding sim), all weights 1. **Reflection** synthesizes higher-level insights when accumulated importance passes a threshold. | Use the **scoring formula** (cheap, code-side). Do reflection as **offline consolidation** into L1 topic notes, not live. |
| **Mem0** (paper Apr 2025, https://arxiv.org/abs/2504.19413) | Two phases. **Extraction** of candidate facts from the new message pair plus a running summary and recent messages; then an **update** phase: retrieve the top-10 similar memories, and the LLM picks **ADD / UPDATE / DELETE / NOOP** via tool call. LoCoMo: 26% relative LLM-judge gain over OpenAI memory, ~90% fewer tokens than full context; the graph variant (Mem0ᵍ) adds ~2%. Uses GPT-4o-mini. | The blueprint for L2. **2026 update:** Mem0 moved to **single-pass ADD-only extraction**: "no overwrite or delete step, so history survives intact", with conflicts handled at read time by temporal re-ranking and soft **memory decay** (nothing deleted). Claimed LoCoMo 92.5 / LongMemEval 94.4 at ~7K tokens per query (vendor numbers, managed platform) — https://mem0.ai/research, https://github.com/mem0ai/mem0. **That is good news for small models:** ADD-only removes the hardest decision (UPDATE/DELETE) from the LLM. |
| **A-MEM** (NeurIPS 2025, https://arxiv.org/abs/2502.12110) | Zettelkasten notes: each memory gets LLM-generated context, keywords, tags and links to related notes; new notes can trigger "memory evolution" (rewriting old notes). | Keep the cheap parts (keywords/tags, links by embedding sim). Skip LLM-driven rewriting of old notes: error-prone with 1.7B, and rewrites corrupt user-visible entries. |
| **Zep / Graphiti** (https://arxiv.org/abs/2501.13956, https://github.com/getzep/graphiti) | **Bi-temporal knowledge graph**: facts as edges with valid_from/valid_to, **invalidated, not deleted** on contradiction; hybrid search (cosine + BM25 + graph). LongMemEval 71.2% vs 60.2% full context with gpt-4o, ~90% lower latency. | Adopt **bi-temporal validity columns** on facts. Reject the full graph: an independent evaluation measured **>600k tokens to ingest a single LoCoMo conversation** because of cascaded LLM calls for entity extraction, resolution and invalidation (cited in Agent Zero Memory, https://arxiv.org/pdf/2608.29606v1). At phone speeds that is hours of compute and battery. |
| **LangMem** (https://langchain-ai.github.io/langmem/concepts/conceptual_guide, https://github.com/langchain-ai/langmem) | Semantic memory as **profiles** (one schema doc) or **collections** (unbounded, searched); episodic and procedural memory; "hot-path" tools vs a **background memory manager**. | Maps directly: profile = L0, collection = L2, background manager = the extraction worker. |
| **MemoryOS** (EMNLP 2025 oral, https://arxiv.org/abs/2506.06326) | Short-term (FIFO dialogue pages) → mid-term (topic segments with "heat") → long-term persona (user profile + knowledge). LoCoMo +49% F1 over baselines on GPT-4o-mini. | Validates **STM/MTM/LTM tiers with heat-based promotion**. L1 topic notes are the MTM, L0 the persona. |
| **LightMem (SLM-driven)** (ACL 2026, https://arxiv.org/abs/2604.07798) | Memory system **driven by small language models**. STM/MTM/LTM; **online** fixed retrieval budget (vector coarse retrieval → semantic re-rank); **offline** consolidation into LTM. ~+2.5 F1 over A-MEM on LoCoMo; 83 ms retrieval, 581 ms end to end. | The closest published analogue to this app. It confirms: **separate online (cheap, deterministic retrieval) from offline (LLM consolidation)**. |
| **HippoRAG / HippoRAG 2** (https://arxiv.org/abs/2405.14831, https://github.com/OSU-NLP-Group/HippoRAG) | KG + Personalized PageRank for multi-hop associativity; HippoRAG 2 keeps factual recall at standard-RAG level. | Overkill for personal chat memory; relevant only if file/document RAG later needs multi-hop. |
| **Cognee** (https://docs.cognee.ai/getting-started/introduction, https://github.com/topoteretes/cognee) | Graph + vector + relational memory; v1.0 API `remember / recall / improve / forget`. | Good API vocabulary; the Python, LLM-heavy graph build doesn't fit on-device Kotlin. |
| **Filesystem-based memory study** (Jul 2026, https://arxiv.org/abs/2607.26637) | First systematic test of "directory of markdown files the agent maintains". Organized stores **roughly halve retrieval cost** on large material. But **"organization erodes for all but the strongest management agent"**, no agent turned organization into better answers, and **the tool set alone reshapes the store as strongly as swapping the model.** | **The key evidence against letting a 1.7B model manage a file tree.** Keep files and topics as a *presentation and consolidation* layer that code controls. |
| **AgeMem** (Jan 2026, https://arxiv.org/abs/2601.01885) | Memory operations (store/retrieve/update/summarize/discard) as tools, **learned with RL**. | Shows tool-based memory needs *training* to work well. An option later: fine-tune LFM3 on the app's memory ops. |
| **On-device specific (2026):** ScrapMem (https://arxiv.org/abs/2605.03804v2), HGP hybrid graph memory (https://arxiv.science/abs/2610.10071), SuperLocalMemory (zero-LLM mode 70.4% LoCoMo, https://arxiv.org/abs/2604.04514) | Edge memory via compression/forgetting, lightweight classifiers for routing, and **zero-LLM retrieval** (BM25 + dense + temporal fusion). | Supports doing most of the work **without LLM calls**: classifiers, hybrid search, decay. |
| **Survey** (Mar 2026, https://arxiv.org/abs/2603.07670) | Memory as a write → manage → read loop; stresses write-path filtering. | Framing. |
| **PersistBench** ("When should long-term memories be forgotten?", https://arxiv.org/html/2602.01146v2) | Benchmarks memory risks: **cross-domain leakage** (memory injected where it doesn't belong) and **memory-induced sycophancy**. | Inject only *relevant* facts; instruct the model not to bring memories up unprompted. |

### Benchmarks
- **LoCoMo** (ACL 2024, https://arxiv.org/abs/2402.17753): 10 very long dialogues (~300–600 turns, 9–16K tokens, up to 35 sessions). QA in single-hop, multi-hop, temporal, open-domain and adversarial categories, plus event summarization. Small and saturating (vendors now report >90).
- **LongMemEval** (ICLR 2025, https://arxiv.org/abs/2410.10813, https://github.com/xiaowu0162/LongMemEval): 500 questions on **information extraction, multi-session reasoning, temporal reasoning, knowledge updates, abstention**. Commercial assistants and long-context LLMs drop ~30% accuracy over sustained interactions. Design findings: **"round" is the better storage granularity, and extracting user facts as index keys improves recall and QA** (https://iclr.cc/media/iclr-2025/Slides/28290_OvN8vCf.pdf). LongMemEval-V2 (2026) targets agent trajectories.
- For this app: build a **small German test set (~100 questions) in LongMemEval's 5 categories** from synthetic multi-session chats, and run it on-device to compare variants. Public scores are all from GPT-4o-class models and don't transfer to 1.7B.

---

## 3. Single file vs file directory vs vector DB vs knowledge graph, for a SMALL model

Constraints that matter here:
- **LFM3-1.7B** (https://www.liquid.ai/blog/lfm3): strong instruction following (IFEval 96.7, IFStruct 93.7) and tool use (BFCL v4 71.4) for its size. Low world knowledge, but very low hallucination (AA-Omniscience non-hallucination 92.9). 256K context; at 128K the KV cache is 1.6 GB, so 8–32K is realistic on a phone. Fine-tunable on a single GPU.
  - Implication: very good at *following a fixed format* (JSON extraction, single-choice classification). Weaker at *open-ended multi-step curation* (choosing which file, writing exact `str_replace` anchors, keeping a taxonomy tidy). A 71% BFCL score means roughly 1 in 3–4 complex calls goes wrong. That is fine for reads, which fail softly, and risky for writes, which corrupt user-visible memory.
- **LFM2.5-Embedding-350M** (https://huggingface.co/LiquidAI/LFM2.5-Embedding-350M, https://docs.liquid.ai/lfm/models/lfm25-embedding-350m): 1024-d, cosine, **512-token input**, 11 languages **including German**. Good for short facts and chat rounds; long topic notes must be chunked.

| Option | Reliability with 1.7B writes | Prompt cost | Transparency / editability | Conflicts | Forgetting | Verdict |
|---|---|---|---|---|---|---|
| **Single profile file always in prompt** (MEMORY.md / core block) | High if the *app* edits it (key-value rows); medium if the model rewrites it | Fixed and grows linearly. Must cap (Claude Code caps at 200 lines; adherence drops with length) | Excellent | Manual or by overwrite | Easy (delete line) | ✅ **Use as L0, small and capped** |
| **Directory of markdown files edited via memory-tool calls** (Claude memory tool style) | **Low**: needs path choice, exact-match `str_replace`, taxonomy upkeep. The filesystem study shows organization erodes except with the strongest models | Low reads if indexed, but needs extra tool turns (latency on phone) | Excellent (human-readable) | Model-dependent, inconsistent | Model-dependent | ❌ as the *write* mechanism. ✅ as a **presentation** layer (topics rendered from DB rows, user-editable) |
| **Vector DB of atomic facts** (Mem0-style) | **High** when the LLM only emits JSON facts and code reconciles (ADD-only plus a similarity gate) | Bounded: top-k facts (~300–400 tok) | Good, if each fact is a short readable sentence with its source | Code-side: `valid_to` / supersede, plus temporal re-rank | Decay in ranking; hard delete on request | ✅ **Core of the system (L2)** |
| **Knowledge graph** (Zep/Graphiti, Cognee, HippoRAG) | **Low**: entity resolution and relation typing are hard for small models; write amplification is huge (>600k tokens per LoCoMo conversation) | Compact at read time | Poor for non-technical users (nodes and edges) | Best-in-class (bi-temporal) | Good | ❌ Borrow only **bi-temporal columns + an entity-tag column** |
| **Raw chat-history search** | No LLM needed | Only when triggered | Users already understand "chats" | None (raw) | Delete chat | ✅ **L3 fallback**: catches what extraction missed |

**Conclusion:** for a 1.7B model, move intelligence from the model into the app. The model does **narrow, schema-constrained** jobs (extract facts, choose 1 of 4 labels, summarize a session in ≤400 chars). Code does storage, dedup, versioning, retrieval, budgeting and UI.

---

## 4. Concrete design for the app

### 4.1 Data model (Room; sqlite-vec virtual tables; FTS5)

```kotlin
@Entity data class MemoryFact(
  @PrimaryKey val id: Long,
  val text: String,               // ≤ 200 chars, 3rd person, self-contained: "Isst vegetarisch."
  val category: String,           // identity|preference|constraint|relationship|plan|event|skill|other
  val topicId: Long?,             // → MemoryTopic
  val entities: String,           // comma tags: "Rosa, Schule" (cheap A-MEM/graph substitute)
  val importance: Int,            // 1..5 (LLM-rated at extraction, user can pin)
  val sensitivity: Int,           // 0 normal, 1 sensitive (health, religion, sexuality, politics, finances)
  val origin: String,             // explicit|extracted|user_edit
  val status: String,             // active|suggested|superseded|deleted
  val pinnedToProfile: Boolean,
  val validFrom: Long, val validTo: Long?,      // bi-temporal (Zep idea)
  val supersededBy: Long?,
  val sourceChatId: Long?, val sourceMsgId: Long?,  // provenance → "aus Chat vom 12.10."
  val createdAt: Long, val lastUsedAt: Long?, val useCount: Int
)
@Entity data class MemoryTopic(val id: Long, val title: String, val summary: String /*≤2000 chars*/, val updatedAt: Long, val userEdited: Boolean)
@Entity data class ChatRound(val id: Long, val chatId: Long, val userText: String, val assistantText: String, val ts: Long) // LongMemEval: round granularity
@Entity data class ChatSummary(val chatId: Long, val summary: String /*≤400 chars*/, val ts: Long)
// sqlite-vec: vec_facts(fact_id, embedding float[1024]), vec_rounds(round_id, embedding float[1024]), vec_topics(...)
// FTS5: fts_facts(text, entities), fts_rounds(userText, assistantText)
```
- Index chat rounds with **fact-augmented keys**: embed `userText + extracted facts of that round` (a LongMemEval finding).
- Scale: 2,000 facts × 1024 float32 ≈ 8 MB, and brute-force KNN is fine (see `search_ocr_stack.md`). Chat rounds can reach tens of thousands; consider int8 quantization of vectors if it gets slow.

### 4.2 Read path (every turn; deterministic, no tool call)
1. Embed the user message (~10 ms class on device, per Liquid's latency numbers; verify on target phones).
2. **Hybrid search** over `fts_facts` + `vec_facts` → **RRF fusion** → re-rank with the Generative-Agents-style score: `0.5·relevance + 0.2·recency(decay, half-life ~30 days on lastUsedAt) + 0.2·importance + 0.1·explicit_bonus`. Drop `superseded` and `deleted` facts; drop `sensitivity=1` unless enabled or the query is clearly on that topic.
3. **Relevance threshold** (no injection below the cutoff; this guards against PersistBench cross-domain leakage). Take the top 5–8 facts.
4. If one topic has ≥3 hits or a high topic-embedding similarity → inject that **topic note** (≤500 tokens).
5. **History trigger:** regex/keyword cues ("letztes Mal", "erinnerst du", "wie hieß", "vorhin", "last time") or the model calls `search_memory(query)` → search `fts_rounds + vec_rounds` and inject the 2–3 best rounds plus their date (≤600 tokens).
6. Assemble the prompt by the budget below; update `lastUsedAt`/`useCount` for the injected facts; show the **"🧠 2 Erinnerungen verwendet"** chip on the answer (tap to see which ones, with an edit/delete shortcut).

**Prompt budget (8K mode → 32K mode):**

| Block | 8K | 32K |
|---|---|---|
| System prompt + tool schemas | ~1,200 | ~1,500 |
| L0 profile | ≤500 | ≤500 |
| L1 topic index (titles only) | ~100 | ~150 |
| L2 retrieved facts | ≤400 | ≤800 |
| L1 topic note (conditional) | ≤500 | ≤1,500 |
| L3 history excerpts (conditional) | ≤600 | ≤3,000 |
| Rolling conversation (recent turns + running summary of older turns) | rest (~3,500) | rest |
| Reserved for answer (+ reasoning, if on) | ≥1,200 | ≥4,000 |

Memory never takes more than ~25% of the context. The conversation itself wins any conflict.

System prompt rule (short): *"Nutze 'Erinnerungen' nur, wenn sie zur Frage passen. Erwähne sie nicht ungefragt. Bei Widerspruch gilt, was der Nutzer jetzt sagt."* ("Use 'memories' only when they fit the question. Don't mention them unprompted. If they contradict, what the user says now wins.")

### 4.3 Write path

**A) Explicit, live (highest reliability)**
- Detect intent with **code first**: patterns like `merk dir|merke dir|vergiss nicht|ab jetzt|ich bin|ich heiße|remember|vergiss|lösch das` → then one **simple tool** `remember(text, category)` / `forget(query)` that the model can also call. Keep exactly these 2–3 memory tools so the tool surface stays small (the filesystem study showed the tool set shapes behaviour).
- Write immediately (`origin=explicit`, `importance≥4`), run the reconciliation gate (below), and show the inline chip **"Gemerkt: Isst vegetarisch · Rückgängig · Bearbeiten"**.
- `forget(query)`: retrieve candidates, show them for confirmation ("Diese 2 Erinnerungen löschen?"), then hard delete.

**B) Implicit, background extraction (Mem0 / LangMem background manager)**
- Trigger: chat idle ≥5 min, app backgrounded, or chat closed → **WorkManager** job (constraint: battery not low; prefer charging for big backlogs). Process each **finished session once**, in chunks of ~10 rounds plus the running summary (Mem0 used summary + the last 10 messages).
- **Extraction call:** LFM3 with a **GBNF/JSON-schema grammar** in llama.cpp, so the output is always parseable:
  ```json
  {"facts":[{"text":"Hat zwei Geschwister, Rosa und Artur.","category":"relationship","importance":3,"sensitive":false,"entities":["Rosa","Artur"],"time_ref":null}]}
  ```
  Prompt rules: only facts **about the user** that stay useful for weeks; one fact per item; third person; no assistant opinions; no secrets (passwords, card or ID numbers → code-side regex filter as well); resolve relative dates to absolute dates from the session timestamp; an empty list is fine.
- **Reconciliation, mostly in code (ADD-first, à la Mem0 v3):**
  1. Embed the candidate; get the top-5 active facts (vector + FTS).
  2. cos ≥ 0.92 (tune) → **NOOP** (bump `useCount`, `lastUsedAt`).
  3. cos < 0.75 → **ADD**.
  4. Between the two → **one constrained-choice LLM call**: `SAME | MORE_DETAIL | CONTRADICTS | DIFFERENT` (grammar restricts output to one token/label). SAME → NOOP; MORE_DETAIL → ADD the new fact and supersede the old one (keep the link); CONTRADICTS → new fact active, old `validTo=now`, `status=superseded` (Zep invalidation, never destructive); DIFFERENT → ADD.
  - Nothing is ever deleted by the model. Deletes come only from the user or from retention rules.
- **Status:** non-sensitive extracted facts → `active` directly (with a weekly "Neu gelernt" digest in the UI). **Sensitive** facts (health, religion, sexuality, politics, finances, third parties' private details) → `suggested`, never used until the user accepts. This mirrors Claude's sensitive-topics default and matters for minors (the user base may include teens; Gemini blocks memory under 18).

**C) Offline consolidation (Generative Agents reflection / MemoryOS MTM / LightMem offline)**
- Weekly, or when a topic gains ≥5 new facts, while charging: regenerate that **topic note** from its active facts (≤2,000 chars, a bullet list, grammar-limited). If the user edited a note (`userEdited=true`), don't rewrite it: append "Neue Punkte:" suggestions instead.
- **Topic assignment:** nearest topic embedding; if none is above the threshold and ≥3 unassigned facts cluster together → propose a new topic (the LLM names it in ≤3 words).
- **Profile promotion:** facts with `category ∈ {identity, constraint, preference}` and (explicit, or useCount ≥3 across ≥2 chats) → suggest "Zu 'Über mich' hinzufügen?". The profile is never auto-edited without the user (except explicit "merk dir" identity facts like name or diet, which go in directly with an undo).
- **Session summary** (≤400 chars) per chat for L3 browsing and as context for future extraction.

### 4.4 Conflict resolution
- **Recency wins** in retrieval: of two facts in the same entity/category cluster, the newer `validFrom` ranks higher, and superseded facts are excluded (Mem0 v3 temporal re-rank; Zep validity).
- **User statement in the current chat > memory** (system prompt rule).
- **User edits are authoritative:** `origin=user_edit` facts are never superseded automatically. A conflicting candidate becomes a `suggested` item ("Du hattest gesagt X, jetzt klingt es nach Y. Aktualisieren?").
- Keep a superseded fact's history visible in the detail view ("früher: …"), so "what did I say before" questions still work (LongMemEval knowledge-update and temporal categories).

### 4.5 Forgetting and retention
- **Soft forgetting = ranking decay only** (recency half-life). Explicit memories are never deleted for being old (Mem0: "nothing is ever deleted or hidden" by decay).
- `superseded` facts: kept 90 days for history, then purged (setting).
- **Hard delete on user request** must cascade: fact row + `vec_facts` + `fts_facts` + mentions in topic notes (regenerate the note) + the profile line. Run `VACUUM` periodically so deleted text doesn't linger in the SQLite file.
- **Deleting a chat:** unlike ChatGPT (where saved memories survive chat deletion), **ask**: "Auch die 3 Erinnerungen aus diesem Chat löschen?" Provenance (`sourceChatId`) makes this possible.
- **Incognito chat:** no extraction, no L3 indexing, no memory reads (optional).
- **Memory off:** stops reads and writes; offers "Alles löschen".
- Encrypt the DB at rest (e.g. SQLCipher or Android Keystore-wrapped key) if you want to promise strong privacy; exclude it from cloud backup unless the user opts in.

### 4.6 User UI ("Gedächtnis" screen, non-technical wording)
1. **Über mich:** an editable card list (name, how to address me, language, diet, school/job, interests, "Das soll die KI immer beachten"). A live counter "312 / 1.500 Zeichen".
2. **Themen:** topic cards (title, 2-line preview, number of memories). Tap → the editable note plus its memories.
3. **Alle Erinnerungen:** a searchable list; filter by topic, category, "neu", "sensibel". Each item shows its text, date, source ("aus Chat 'Matheklausur' · 12.10."), and edit / delete / pin to profile.
4. **Vorschläge:** pending sensitive or conflicting facts with **Übernehmen / Ändern / Verwerfen**.
5. **Einstellungen:** toggles: *Gedächtnis verwenden* · *Aus Chats lernen* · *Frühere Chats durchsuchen* · *Sensible Themen merken* (off by default) · *Wochenrückblick "Neu gelernt"* · Export (JSON + Markdown) · Import (paste text from another assistant, as Gemini offers, run through extraction) · **Alles löschen**.
- In chat: the "Gemerkt ✓ / Rückgängig" chip on writes; the "🧠 n Erinnerungen verwendet" chip on reads; a long-press on any message → "Merken" / "Das stimmt nicht mehr".
- Optional power-user view: "Als Markdown anzeigen". This renders the whole memory as one file (profile + topics + facts) for transparency and export. It is a *view* of DB rows, not the source of truth.

### 4.7 Size limits (summary)
- Profile: ≤25 entries, ≤1,500 chars total (~450 tokens). Hard stop with a prompt to merge.
- Fact: ≤200 chars; ≤2,000 active (above that, prompt to clean up the lowest-score facts; retrieval stays fast regardless).
- Topic note: ≤2,000 chars; ≤30 topics; index line ≤150 tokens.
- Session summary: ≤400 chars.
- Injected memory: ≤~25% of the context (≈2K tokens in 8K mode).
- Extraction batch: ≤10 rounds + summary per call (fits the 8K context comfortably).

### 4.8 Making the small model good at this
- **Grammar-constrained decoding** (llama.cpp GBNF / JSON schema) for every memory call, so malformed output is impossible.
- Turn **reasoning off** for extraction and classification (faster; `konzept.md` notes that disabling LFM3's thinking may be unreliable, so strip or ignore the thinking block in the parser).
- **Tool-call format:** `konzept.md` says LFM3 emits XML (qwen3_coder-style) tool calls, while Liquid's LFM2.5 docs describe Pythonic calls. Follow the LFM3 model card / chat template; not re-verified here.
- **Fine-tune (later, high leverage):** LFM3 is designed for single-GPU fine-tuning. Generate ~5–10K synthetic German chats with a frontier model, label extraction output and the reconciliation labels, and LoRA-train a memory adapter. AgeMem shows that trained memory ops beat prompted ones.
- **Evaluate** on the in-house German LongMemEval-style set: extraction precision/recall, knowledge-update accuracy, abstention (does it say "weiß ich nicht" instead of inventing), leakage (memory used where irrelevant).

### 4.9 Rollout order
1. L0 profile (manual editing + explicit "merk dir") + UI screen + chips. Already delivers concept item 13.
2. L3 chat-history hybrid search (no LLM needed) + session summaries.
3. L2 background extraction + reconciliation + Vorschläge.
4. L1 topic consolidation + profile promotion.
5. Fine-tuned memory adapter; optional encrypted export/sync.

---

## 5. Open points / not verified
- On-device latency of LFM2.5-Embedding-350M and of extraction runs on mid-range Android phones: Liquid's numbers are from their own benchmarks; measure on target devices.
- Similarity thresholds (0.75 / 0.92) are starting values, not sourced. Tune them on the test set.
- Mem0's 2026 benchmark numbers are vendor-reported on their managed platform; the open-source version is "directionally similar".
- The ">600k tokens per LoCoMo conversation" for Zep comes from a third-party evaluation cited in the Agent Zero Memory paper, not from Zep.
- sqlite-vec int8/bit quantization support: check the current sqlite-vec release before relying on it.
