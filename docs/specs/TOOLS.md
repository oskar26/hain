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

Intent tools **never fire directly**: they return a ConfirmCard; the user taps "Stellen / Set". (Setting `confirm_actions` default true; if false, fire immediately after card shows 3 s undo.) As built in 4b: see §3c (`days` for repeating alarms and `confirm_actions` → 4c).

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
- No Readability4K: own `HtmlText` (drops script/style/nav/header/footer/aside/form, keeps lines ≥ 40 characters, decodes entities). DDG back-off and the user's SearXNG address: done in 4b, see §3c.

### 3c. As built (Werkzeuge II Rest, 4b, 03.11.2026)

**Phone actions (T-068).** Four offline tools in `core-tools/internal/phone` (`set_alarm`, `set_timer`, `add_calendar_event`, `navigate`). They never act: the result is `{"prepared":…,"status":"waiting_for_tap","hint":…}` plus `ToolResult.action` (`PhoneAction` in `core-tools/api`). `ChatEngine` turns it into an `ActionCard` (state `PENDING`) on the answer; it is stored in `message.sources_json` under `"actions"` (no schema change) and shown as **ConfirmCard** under the answer (`feature-chat/api/ConfirmCard.kt`, `hainSurface` RAISED, Lucide icon, green button + "Abbrechen"). Only the tap builds the intent (`PhoneIntents`):

| tool | args (schema) | intent after the tap | button |
|---|---|---|---|
| `set_alarm` | `time` ("06:30", "um 7", "halb 8", "7 am"), `label?` | `AlarmClock.ACTION_SET_ALARM` (hour, minutes, message, skip UI = false) | Stellen / Set |
| `set_timer` | `seconds` (number or "10 Minuten", "1h 30m"; also `minutes`), `label?` | `AlarmClock.ACTION_SET_TIMER` (length, message) | Starten / Start |
| `add_calendar_event` | `title`, `start` (ISO or "morgen 15 Uhr", "Freitag 19 Uhr", "4.11. 9 Uhr", "in 10 Minuten"), `end?` (default +1 h), `location?`; only a day → whole day | `Intent.ACTION_INSERT` on `CalendarContract.Events` – the calendar app opens filled in, **no calendar permission** | Eintragen / Add |
| `navigate` | `destination`, `mode?` | `geo:0,0?q=<destination>` – the phone lets you pick the map app | Starten / Start |

- Card states: `PENDING` → `DONE` (intent started) / `CANCELLED` / `NO_APP` (`ActivityNotFoundException` → friendly hint, e.g. "Organic Maps oder OsmAnd sind kostenlos"). One tap, one action: a finished card never changes again (`ActionCard.after`, unit-tested). The state is kept with the message (also after a restart).
- Times are read by `TimeParser` (own code, DE+EN) relative to `ToolContext.now`: only a time → the next such time (today or tomorrow); a day without a year → the next such day.
- Permission: only `com.android.alarm.permission.SET_ALARM` (normal, no prompt). Works in offline mode (tools are offline; system prompt says so).
- System prompt (`SystemPromptSource.PHONE_DE/EN`): which tool for alarm/timer/appointment/route, `start` as `2026-11-04T15:00`, and "never say it is already set".

**DuckDuckGo back-off (T-066).** `SearchParsers.isBotCheck` (HTTP 202/403/429 or the anomaly/challenge page). The page that showed it (`ddg` = HTML, `ddg_lite`) is skipped for 10 minutes (`SearchTool.BACKOFF_MS`, in memory), the next source answers instead (Lite → Wikipedia). Unit-tested with a fake clock.

**Own SearXNG address.** Settings → Privacy → "Eigene Suche (SearXNG)" (`searxng_url` in DataStore). `SearxAddress.normalize` makes `searx.example.org/` into `https://searx.example.org`, cuts `/search?…`, refuses `http://` (the gate only talks https); a quiet line under the field says whether it can be used. `ToolContext.searxUrl` puts it first, before the public instances.

**Host test 03.11.2026** (llama.cpp `39febd2`, LFM3-1.7B-QAD-Q4_0, temp 0.1, German system prompt of the app incl. PHONE_DE, all 9 tool schemas, fresh chat per request, turn hint + tool nudge as in the app; `/workspace/work/hain-4b-host/phone.py`):

| # | Anfrage | Aufruf des Gehirns | Karte | richtig? |
|---|---|---|---|---|
| 1 | Stell mir einen Wecker um 6:30 | `set_alarm(time=06:30)` | Wecker um 06:30 · Stellen | ✅ |
| 2 | Timer 10 Minuten | `set_timer(seconds=600)` | Timer für 10 Min. · Starten | ✅ |
| 3 | Trag Zahnarzt morgen um 15 Uhr ein | `add_calendar_event(title=Zahnarzt, start=2026-11-04T15:00)` | Zahnarzt · 04.11.2026 · 15:00–16:00 · Eintragen | ✅ |
| 4 | Navigier mich zum Kölner Dom | `navigate(destination=Kölner Dom)` | Kölner Dom · Starten | ✅ |
| 5 | Set an alarm for 7 am | `set_alarm(time=07:00)` | Alarm at 07:00 · Set | ✅ |
| 6 | Remind me with a timer in 25 minutes | `set_timer(seconds=1500, label=Reminder)` | Timer for 25 min · Start | ✅ |
| 7 | Put dinner with Lea on Friday at 7 pm in my calendar | `add_calendar_event(title=Dinner with Lea, start=2026-11-06T19:00)` | Dinner with Lea · Nov 6, 2026 · 7:00 PM–8:00 PM · Add | ✅ |

All 7 requests (the 4 from the plan + 3 English ones) → the right tool with the right arguments, answers in the language of the message (the 8th, "Take me to Cologne Central Station", stopped because the sandbox ran out of memory). Weak spot: the answer text still often says "eingestellt"/"I've set" although nothing is set yet, even with the hint in the tool result and the rule in the system prompt (2 of 7 say "vorbereitet"/"prepared"). The card makes it clear (green button, nothing happens before the tap); sharpening the wording → 4c.

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
