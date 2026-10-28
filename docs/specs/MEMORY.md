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
