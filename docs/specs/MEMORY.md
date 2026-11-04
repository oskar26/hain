# MEMORY.md — 4-layer memory (ADR-16)

Research: `docs/concept/research/memory.md`. Product text: KONZEPT.md §9.

## 1. Layers

| Layer | Table | In prompt | Limits |
|---|---|---|---|
| L0 About me | `memory_profile` (single row, text) | always | 1,500 chars |
| L1 Topics | `memory_topic` (title, note) | titles always; note if relevant (score ≥ 0.45) | 30 topics, 2,000 chars each |
| L2 Facts | `memory_fact` + `memory_fact_vec` (sqlite-vec) + FTS5 | top 5–8 relevant | 200 chars each |
| L3 Chats | `message` FTS5 + `chat_summary` | only when user refers to the past ("letztes Mal", "last time", "hatten wir") | 600 tokens |

Total memory budget ≤ 25 % of context (default 600 tokens at 8K ctx).

## 2. Write paths

1. **Explicit (code, no LLM):** regex DE/EN on the user message: `merk dir|merke dir|remember( that)?|vergiss|forget`. → insert fact / mark deleted. Show chip "Gemerkt ✓ Rückgängig".
2. **`remember` tool** called by the model → same insert, chip shown.
3. **Background extraction** (`MemoryExtractionWorker`, after a chat is idle 2 min, only on charger or battery > 30 %):
   - Prompt `prompts/extract_facts.txt` (T-112 writes it) + last ≤ 12 messages, grammar forces JSON:
     `{"facts":[{"text":"...","category":"preference|person|place|work|health|other","sensitive":false}]}`
   - For each fact: embed → nearest existing fact cosine:
     ≥ 0.92 skip (duplicate) · ≤ 0.75 add · between → ask model with grammar `{"label":"same|update|new|contradiction"}`.
     `update`/`contradiction` → add new fact, set old `superseded_by` (never overwrite, never hard-delete automatically).
   - `sensitive=true` or category `health` → store with `status=suggested` (shown under "Vorschläge", not used until accepted).
4. Weekly `TopicRefreshWorker`: group active facts by category → rewrite topic notes (LLM, 2,000 chars max) → suggest profile additions.

## 3. Read path (`MemoryStore.context(query, budget)`)

1. Embed query (LFM2.5-Embedding-350M via llama.cpp embedding mode, or fallback: none → FTS only).
2. Hybrid score = 0.6·cosine + 0.25·BM25(normalized) + 0.1·recency (half-life 90 days) + 0.05·importance.
3. Drop < 0.35. Drop superseded and suggested. Take top 8, trim to budget.
4. Output block (goes into `{memory_snippets}`):
```
Profile: <L0 text>
Topics: Schule, Musik, Kochen
Relevant: - mag keine Pilze (2026-10-07) - spielt Gitarre (2026-10-20)
```
5. UI chip under answer: "3 Erinnerungen genutzt" → sheet listing them.

## 4. Privacy

- Memory off → nothing read or written. Private chat → nothing written, nothing read except L0 if setting allows.
- Delete fact → removed from fact table, vec table, FTS and any topic note containing it (LLM rewrite of that note, or remove the line).
- Export: JSON (all layers) via SAF. Import validates schema.

## 5. Screen (`:feature-memory`)

Tabs: Über mich (editable text) · Themen · Alle (search, each fact shows origin chat link + date, swipe delete) · Vorschläge (accept/reject) · settings row (on/off, use chat history on/off, export, import, delete all with confirm).

## 6. State (0.1.7, T-111–T-113, T-115)

- Done: tables + DAO (DB v3), explicit "merk dir / remember / vergiss nicht / don't forget" and "vergiss / forget" (`ExplicitMemory`, start of the message only; "merk dir das", questions and "vergiss alles" are left out), `remember` tool (sensitive/health → suggestion), keyword retrieval (`Ranking`: BM25 with loose endings, 0.75·keyword + 0.15·recency + 0.1·importance, ≥ 0.35, ≤ 8 facts, ≤ 1,800 chars; with a cosine the formula of §3), memory page (Erinnerungen with search/edit/delete/undo, Vorschläge, Über dich, on/off, export/import JSON via the system file picker, delete all).
- The memory line goes into the per-turn hint after the history (`MemoryTurn`), never into the cached system prompt, so the brain's cache stays valid. A handled "merk dir" is not saved twice when the answer is regenerated.
- On/off lives in SharedPreferences `hain_memory` (not DataStore), so the settings repository did not change.
- Open after 0.1.7: see §7.

## 7. State (0.1.7.1, 7b: T-110, T-114, T-116, T-117)

- **Embedder (T-110):** optional pack "Suche nach Bedeutung" = `LFM2.5-Embedding-350M-Q4_0.gguf` (219,308,800 bytes, sha256 `08bb9bfd…a3cea`, catalogue kind `embed`, `min_ram_gb` 3). llama.cpp embedding mode in its own small context (512 tokens, 2 threads, `hain_embed.h`), next to the brain, loaded on first use and freed after 60 s idle (`LlamaEmbedder`). Asymmetric prefixes `query: ` / `document: ` (`EmbedKind`). 1024 values, L2-normalised; host: ~45 ms per short fact on 2 cores.
- **Vectors:** `memory_fact_vec` (DB v5), filled in the background for new/edited facts (`VectorIndex.sync`), dropped with the fact. Cosine in Kotlin, no sqlite-vec.
- **Hybrid score:** §3 formula. LFM2.5-Embedding gives low question↔fact cosines (unrelated −0.15…0.08, fitting 0.15…0.35), so the cosine is calibrated before it enters the score: `relevance = clamp((cos − 0.05) / 0.25, 0, 1)` (`Vectors.FLOOR`/`SPAN`). The question is embedded with a 2.5 s timeout; without the pack (or on timeout) keywords only.
- **Background extraction (T-114):** `MemoryExtractionWorker` (WorkManager, unique per chat, REPLACE) 2 min after the last finished answer; battery > 30 % or charging (else re-queued with "requires charging"); never for private chats or with the memory off; only when there are new user messages since the last run. Uses the brain that is already loaded (loads the default brain only if none is). Steps aside as soon as the user writes (`yieldToChat` cancels the generation). Prompts `extract_facts.txt` / `compare_facts.txt` (copied to `core-memory/src/main/assets/prompts/`, a test keeps them equal), JSON forced by GBNF (`SamplingParams.grammar`). Dedupe: cosine ≥ 0.92 skip, ≤ 0.75 new, between → brain label same/update/new/contradiction; without the pack the same thresholds on a word-overlap score. **Everything found becomes a suggestion** (stricter than §2.3, which only sends sensitive facts there): nothing is used before the user's yes. update/contradiction → suggestion with `replaces`; accepting it sets `superseded_by` on the old fact (never overwritten, never deleted automatically).
- **Topics:** the "Themen" tab groups kept facts by category (topic title = category key) with a note of the user's own (≤ 2,000 chars, `memory_topic`); the note of a matching category goes into the memory line (cut to 300 chars). The weekly `TopicRefreshWorker` (LLM rewrite) is not built.
- **Chips (T-116):** `MemoryTurn.uses` records per turn (key = last 3 user messages) what was used/saved/forgotten; `ChatExtras.answerFooter` shows "Gemerkt ✓ · Rückgängig", "Vergessen ✓ · Rückgängig" and "N Erinnerungen genutzt" (tap opens the list) under finished answers. Kept in memory only: after an app restart old answers show no chips.
- **Still open:** L3 chat search, `TopicRefreshWorker`, private-chat rules (Häppchen 8), device measurement of the embedder (RAM next to the brain, speed on the phone).

