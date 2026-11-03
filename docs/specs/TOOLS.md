# TOOLS.md — tool router, network gate, all tools

Research: `docs/concept/research/keyless_search.md`.

## 1. NetworkGate (every outbound request)

```kotlin
data class GateRequest(val url: String, val purpose: Purpose, val method: String = "GET",
                       val headers: Map<String,String> = emptyMap(), val body: ByteArray? = null)
enum class Purpose { SEARCH, NEWS, WEATHER, WIKI, GEO, PAGE_READ, MODEL_DOWNLOAD, OTHER }
```
- If setting `offline_mode` = true → throw `OfflineException` (tools turn it into a friendly result) — except `MODEL_DOWNLOAD` started by the user.
- Per-purpose switches (settings): search on/off/ask, news on/off …
- Logs every request **without query bodies by default**: time, host, purpose, bytes. Setting `network_log_detail` shows full URL. Log kept 7 days, table `network_log`.
- Single `OkHttpClient`: timeouts 10 s, `User-Agent: Hain/<ver>` for APIs (Wikipedia, Nominatim, news require it), browser UA only for DuckDuckGo html.
- Cache: OkHttp disk cache 20 MB.

**Stand Werkzeuge I (03.11.2026):** `NetworkGate` (API in `core-common/.../api/net`) is implemented as `HttpNetworkGate` in `core-tools` on plain `HttpURLConnection` (no new library; OkHttp + disk cache come with 3b if needed). Only https. The log is kept **in memory** (7 days / 500 entries, lost on app restart); the Room table `network_log` and routing the model download through the gate are part of 3b. Per-purpose switches: only the global `offline_mode` for now.

## 2. Tool list (v1) — schemas exactly as sent to the model

Descriptions short (small model). English, the model answers in user language anyway.

| name | params | backend | card |
|---|---|---|---|
| `web_search` | `query` (str) | SearchRouter §3, then reads the best pages (§3a) | SourceChips |
| `read_page` | `url` | own HTML-to-text (`HtmlText`, no library) | SourceChips |
| `news` | `query?`, `lang?` | NEWS.md | NewsCard |
| `weather` | `place` (str), `days` (int 1–7, default 1) | Open-Meteo geocoding + forecast | WeatherCard |
| `wikipedia` | `topic`, `lang` | REST `/{lang}.wikipedia.org/api/rest_v1/page/summary/{title}` after `w/api.php?action=opensearch` | WikiCard |
| `calculate` | `expression` | own parser (exp4j-like, own code) + unit conversion table | – (inline) |
| `set_alarm` | `time` (HH:mm), `label?`, `days?` | `AlarmClock.ACTION_SET_ALARM` | ConfirmCard |
| `set_timer` | `seconds`, `label?` | `AlarmClock.ACTION_SET_TIMER` | ConfirmCard |
| `add_calendar_event` | `title`, `start` (ISO), `end?`, `location?` | `Intent.ACTION_INSERT` CalendarContract | ConfirmCard |
| `navigate` | `destination`, `mode?` (walk/drive/bike/transit) | `geo:` / `google.navigation:` fallback to `geo:0,0?q=` | ConfirmCard |
| `remember` | `fact` | MemoryStore | MemoryChip |
| `recall` | `query` | MemoryStore search | – |
| `search_documents` | `query` | DocStore (DOCS_OCR.md) | SourcesCard |

Intent tools **never fire directly**: they return a ConfirmCard; the user taps "Stellen / Set". (Setting `confirm_actions` default true; if false, fire immediately after card shows 3 s undo.)

## 3. SearchRouter (ADR-07)

1. **Classifier** (pure Kotlin, keywords DE+EN, unit-tested): weather words → `weather`; "wer ist/who is/was ist/what is" + proper noun → `wikipedia`; news words → `news`; else general.
   The router only *suggests*; the model chooses tools. Classifier is used when the model calls `web_search` to try cheap sources first.
2. General search order:
   a. user SearXNG URL (`searxng_url` setting) → `GET {url}/search?q=..&format=json&language=de` (if JSON disabled on instance, parse HTML).
   b. DuckDuckGo html: `POST https://html.duckduckgo.com/html/` form `q`, `kl=de-de`|`us-en`. Parse `.result__a` (title, href – decode `uddg` param), `.result__snippet`. Mark source "experimental".
      Back-off: after 2 failures/CAPTCHA (`anomaly` in HTML) disable DDG for 30 min.
   c. Wikipedia search as last fallback.
3. Return top 5 results `[n] title – snippet – url`. The model can call `read_page` for one URL (max 2 per turn).
4. `read_page`: fetch, Readability4K, strip to 2,000 tokens (`tokenCount`), keep title + url.

### 3a. As built (Werkzeuge II, 03.11.2026)

- `SearchTool` (core-tools/internal/search): SearXNG instances (`SEARX_INSTANCES`, JSON) → DuckDuckGo HTML (GET `html.duckduckgo.com/html/?q=&kl=`) → DuckDuckGo Lite (`lite.duckduckgo.com/lite/`, used when the HTML page shows its bot check) → Wikipedia `list=search`. First service with hits wins; ads, video and social hosts are dropped; at most 6 hits. Every request goes through the NetworkGate (`Purpose.SEARCH`).
- **Research in several steps (Oskar's test of #19):** the search result names the best pages in `ToolResult.readUrls`; `ChatEngine` then calls `ToolRouter.readPages` – 3 pages in parallel (8 s each, 15 s together, `Purpose.PAGE_READ`), each cut to 1,200 characters at a sentence end. The brain gets one answer `{"search":{…hits…},"read":{"pages":[{n,title,url,text}]}}` and may search again. Timeline: "Suche · <query> ✓", then "Lese 3 Seiten…" → "3 Seiten gelesen ✓". Sources = the pages that were really read.
- Limits: 4 tool rounds per answer (was 3), 2 web searches per answer. Mode **Schnell**: 1 search, 1 page.
- `read_page` (link from the user): one page, 3,000 characters; only `http(s)://`.
- No Readability4K: own `HtmlText` (drops script/style/nav/header/footer/aside/form, keeps lines ≥ 40 characters, decodes entities). Not yet: DDG back-off timer, user SearXNG URL setting → 4b.

### 3b. Tools in every message (T-062 fix)

Bug (Oskar, 03.11.): after the first tool answer the brain said "laut Wikipedia …" without calling anything. Cause: the chat history only kept the answer text, so the brain saw earlier answers "from Wikipedia" without a call and copied them.
Fix:
1. Each answer keeps its tool calls with a short copy of the result (`ToolTrace`: name, args, result ≤ 1,200 chars) in `message.tool_calls_json` (column existed since v1 → no migration).
2. `PromptHistory.of` puts them back for older turns as call → `<tool_response>` → answer; the last answer's results keep 600 characters, older ones 200.
3. System prompt rule (DE+EN, `SystemPromptSource.TOOLS_*`): "Für jede neue Frage, die Fakten, Wetter oder Aktuelles braucht, rufe das Werkzeug neu auf … Sag nie, dass du nachgeschaut hast, wenn du in dieser Antwort kein Werkzeug benutzt hast."
4. Tool schemas are in every round's prompt (rounds 1–4); the language hint is a short system line after the history and does not replace them.
5. The language hint right before the answer now ends with a tool nudge (`SystemPromptSource.TOOL_NUDGE_*`, not in offline mode): "Für Fakten über Personen, Orte, Dinge, Wetter oder Aktuelles rufe zuerst ein Werkzeug auf." Small talk still gets no call (host check).

**Host test 03.11.2026** (llama.cpp `39febd2`, LFM3-1.7B-QAD-Q4_0, temp 0.1, German system prompt, one chat with the questions in a row, real Open-Meteo/Wikipedia/DuckDuckGo results; `/workspace/work/hain-4-host/multi*.py`):

| # | Frage | vorher (#19: nur Text im Verlauf) | nur Verlauf + Regel | jetzt (+ Werkzeug-Hinweis) |
|---|---|---|---|---|
| 1 | Wie wird das Wetter morgen in Köln? | ✅ `weather(Köln)` | ✅ `weather(Köln)` | ✅ `weather(Köln, 2)` |
| 2 | Erklär mir kurz Photosynthese, schau bei Wikipedia nach. | ❌ kein Aufruf, Antwort aus dem Gedächtnis | ✅ `wikipedia(Photosynthese)` | ✅ `wikipedia(Photosynthese)` |
| 3 | Und wie ist das Wetter in Berlin? | ❌ kein Aufruf („brauche ein aktuelles Tool“) | ✅ `weather(Berlin)` | ✅ `weather(Berlin)` |
| 4 | Wer war Ada Lovelace? | – (Lauf abgebrochen) | ❌ kein Aufruf (richtig aus dem Gedächtnis, ohne „nachgeschaut“) | ✅ `wikipedia(Ada Lovelace)` |
| 5 | Recherchier mal: Wie viel Strom spart eine Wärmepumpe gegenüber einer Gasheizung? | – | ✅ `web_search` → 6 Treffer, 3 Seiten gelesen | ✅ `web_search` → 6 Treffer, 3 Seiten gelesen |

Small talk with the nudge ("Danke dir! Wie geht es dir eigentlich?" at the end of the same chat) → no tool call, normal answer.

## 4. Weather (Open-Meteo, no key)

- Geocode: `https://geocoding-api.open-meteo.com/v1/search?name={place}&count=1&language={de|en}`.
- If `place` empty → last known location (only if permission granted) else ask.
- Forecast: `https://api.open-meteo.com/v1/forecast?latitude=..&longitude=..&current=temperature_2m,weather_code,wind_speed_10m,precipitation&daily=weather_code,temperature_2m_max,temperature_2m_min,precipitation_probability_max&timezone=auto&forecast_days={days}`.
- WMO weather codes → DE/EN text + icon (table in `WeatherCodes.kt`, unit tested).
- forModel: compact JSON, e.g. `{"place":"Köln","now":{"t":9.5,"code":"Regen"},"days":[{"date":"2026-10-29","max":12,"min":6,"rain":70}]}`.

## 5. Calculator

Own recursive-descent parser: `+ - * / ^ % ( )`, `sqrt sin cos tan log ln`, constants `pi e`, decimal comma accepted (`3,5`).
Units: length, mass, temperature, volume, speed, time, data (table). Currency: **not in v1** (needs online rates).
Also used automatically: if user message is a pure expression, answer without LLM.

## 6. ToolContext

```kotlin
data class ToolContext(val lang: String /* de|en */, val now: ZonedDateTime, val offline: Boolean, val pagesToRead: Int = 3 /* 1 in Schnell */)
```
(Werkzeuge I: `chatId` left out until a tool needs it.) While offline, the router leaves online tools out of the prompt; a call that still arrives gets `{"error":"offline"}`.
Tool results also carry a short `label` ("Köln") and `sources` for the ToolTimeline/SourceChips (T-059); both are stored with the answer in `message.sources_json` (`{"steps":[…],"sources":[…]}`, no schema change).

## 7. Errors

`ToolResult.error` short English code + forModel `{"error":"<code>","hint":"<one sentence>"}`. Model is told by system prompt to say so plainly.
