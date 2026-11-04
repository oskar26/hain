# DATA.md — database & storage

Room database `hain.db`, encrypted with **SQLCipher** (key in Android Keystore-wrapped random 32 bytes, DataStore holds the wrapped key).
SQLite build: SQLCipher for Android (`net.zetetic:sqlcipher-android`), decided in T-020 (STATUS.md → Pinned versions). Full-text tables are **FTS4** (Room `@Fts4`, tokenizer unicode61); sqlite-vec decided in T-110.

## Tables

| table | columns |
|---|---|
| `chat` | id PK, title, created_at, updated_at, model_id, pinned bool, private bool, archived bool (T-056: private chats are never written, so `private` stays false in the DB; no schema change) |
| `message` | id PK, chat_id FK, role (system/user/assistant/tool), content TEXT, reasoning TEXT?, tool_calls_json TEXT?, tool_call_id TEXT?, cards_json TEXT?, sources_json TEXT?, attachments_json TEXT?, created_at, tokens INT?, parent_id (for edit/regenerate branches; 0 = first message), thinking_ms INT?, status (ok/stopped/failed), selected_at INT? (T-057: when this version was picked among its siblings; newest pick is shown) |
| `message_fts` | FTS4(content, content='message'), unicode61 — Room keeps it in sync |
| `chat_summary` | chat_id PK, summary, updated_at |
| `memory_profile` | not a table (T-111): L0 "Über dich" is the setting `about_me` (DataStore), already in the system prompt; the memory page edits it |
| `memory_topic` | id, title UNIQUE, note, updated_at |
| `memory_fact` | id, text, category, status (active/suggested/deleted), sensitive bool, importance REAL, origin_chat_id?, created_at, superseded_by? |
| `memory_fact_fts` | FTS4(text, content='memory_fact'), unicode61 — Room keeps it in sync (DB version 3, T-111) |
| `memory_fact_vec` | vec0(embedding float[1024]) rowid = fact id (dimension from model, check T-110) — not yet, comes with T-110 |
| `document` | id, name, uri, mime, pages, added_at, status |
| `doc_chunk` | id, document_id, page, text, ocr bool, confidence REAL? |
| `doc_chunk_fts`, `doc_chunk_vec` | as above |
| `news_feed` | id, name, url, lang, is_standard bool, paywall bool, etag, last_modified, status, fail_count |
| `news_item` | id, feed_id, guid, title, link, summary, image_url, published_at, fetched_at |
| `news_item_fts` | FTS5(title, summary) |
| `network_log` | id, time, host, purpose, url_detail?, bytes, ok |
| `installed_model` | id, file_path, sha256, size, installed_at, version |

Migrations: every schema change = new Room version + migration test (`MigrationTestHelper`).

Versions: 1 chats/messages (T-020) · 2 `network_log` (3b) · **3 memory: `memory_fact` (+ index on status), `memory_fact_fts`, `memory_topic` (T-111, `MIGRATION_2_3`, PR #38)** · **4 branches (T-057, `MIGRATION_3_4`, PR #47)**: adds `message.selected_at` and fills empty `parent_id` with the message before it by id (per chat), so old chats stay one line; safe to run twice. Path on the phone: 2 → 3 → 4 (the 8b placeholder `MIGRATION_2_3` was dropped in the integration PR #50, the real one from #38 stays). Tests: `HainMigrationTest` (v2 file from `schemas/…/2.json` → v4, and a v3 file with memories → v4), `BranchMigrationTest` (v2 → v4, branches). `schemas/…/3.json` and `4.json` are written by KSP on the first Gradle build and are committed from the CI artifact when available.

## Settings

Jetpack DataStore (Proto or Preferences) — all keys in SETTINGS.md. Not in Room.

## Files

- Models: `context.getExternalFilesDir("models")` (user can see/delete via app), fallback internal.
- Attachments: copied into `filesDir/attachments/<chatId>/` (encrypted with `EncryptedFile` if app lock on).
- Exports via Storage Access Framework only.
