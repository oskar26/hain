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

## 2. Tool list (v1) — schemas exactly as sent to the model

Descriptions short (small model). English, the model answers in user language anyway.

| name | params | backend | card |
|---|---|---|---|
| `web_search` | `query` (str), `lang` (de/en) | SearchRouter §3 | SourcesCard |
| `read_page` | `url` | Readability4K | – |
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
data class ToolContext(val appLang: AppLang, val now: ZonedDateTime, val chatId: Long, val offline: Boolean)
```

## 7. Errors

`ToolResult.error` short English code + forModel `{"error":"<code>","hint":"<one sentence>"}`. Model is told by system prompt to say so plainly.
