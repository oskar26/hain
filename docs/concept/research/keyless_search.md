# Keyless web search for a local, privacy-first Android chatbot (Kotlin, on-device LFM3-1.7B)

Research date: 2026-10-27. Constraint: no API key, no paid API, no developer-run server. All traffic goes from the phone directly to public endpoints.
Not legal advice — ToS notes summarise what the providers publish.

---

## TL;DR recommendation

Build a small **Kotlin multi-source "search router"** in the app (OkHttp + jsoup), not a scraper of one engine:

1. **Intent routing first** (cheap, sanctioned, reliable): weather → Open-Meteo; places → Open-Meteo Geocoding / Nominatim; German news → Tagesschau API/RSS; facts/definitions → German Wikipedia API (+ Wikidata); papers → arXiv.
2. **General web search** only when routing doesn't cover it: **user-configured SearXNG URL (preferred, JSON)** → **DuckDuckGo HTML/lite (best-effort, low volume, kl=de-de)** → optional **WebView-rendered DDG** fallback → **Wikipedia search** as last resort. Wiby as a niche extra.
3. **Optional offline mode**: Kiwix ZIM (German Wikipedia mini 3.9 GB / nopic ~13 GB) via java-libkiwix — mind GPL-3.0.
4. **Page reading**: OkHttp fetch → Readability4J (or Readability4K) → plain text/markdown, truncated to the small model's context; WebView + Mozilla Readability.js fallback for JS-heavy pages.
5. **Guardrails**: identifying User-Agent for APIs, per-host rate limiters, on-disk cache, exponential backoff + circuit breaker per source, search only on explicit user toggle, clear disclosure of which host receives the query.

DuckDuckGo scraping works today for light personal use but is **not sanctioned** and will break periodically. Treat it as a fallback, not the foundation.

---

## (a) Scraping DuckDuckGo HTML / lite directly from the phone

**Endpoints** (per SearXNG's DDG engine source, https://docs.searxng.org/_modules/searx/engines/duckduckgo.html):
- `https://html.duckduckgo.com/html` — no-JS HTML, HTTP POST form data (`q`, `kl`, paging via `s`/`dc`/`vqd` from the "next" form).
- `https://lite.duckduckgo.com/lite` — even lighter HTML, POST form data.
- `https://links.duckduckgo.com/d.js?q=..` — JS API, needs `vqd` token.
- `https://noai.duckduckgo.com/` — "no-AI" web.
- `api.duckduckgo.com` Instant Answer API — free, keyless, but returns topic summaries only, **not** web results.
- German results: `kl=de-de` region parameter. Result links are wrapped (`//duckduckgo.com/l/?uddg=<urlencoded>`) — decode `uddg`.

**ToS / legality**
- DDG Terms: https://duckduckgo.com/terms — access "as authorized", may suspend access. Acceptable Use Policy https://duckduckgo.com/acceptable-use forbids, among others, to "Frame, inline link, or similarly display any portion of the services within another service" and to "Interfere with or disrupt the integrity or performance of the services". Displaying DDG results inside your app arguably touches the "display within another service" clause.
- There is **no official web-results API** and no paid tier to upgrade to. Third-party guides (e.g. https://iproyal.com/blog/duckduckgo-api/, https://link.sc/blog/duckduckgo-search-api-guide) describe scraping the HTML endpoints as against DDG's terms. For a free, non-commercial, user-initiated, low-volume app the practical risk is blocking, not lawsuits — but it is a grey zone; avoid it in a commercial product.

**Reliability / rate limits (2025–2026)**
- No published limit. Blocking shows up as `202 Ratelimit` from html/lite, 403s, or a CAPTCHA/anomaly page (ddgs issues #290, #304: https://github.com/deedy5/ddgs/issues/304; maintainer note https://github.com/deedy5/ddgs/discussions/348).
- In Sept 2025 ddgs **disabled its DDG backend "until ratelimit is fixed"** and later re-enabled it with a patched HTTP client (commit https://github.com/deedy5/ddgs/commit/6fd63d96120df79701cf8141cd7e68fbc0db1cbc) — i.e. DDG fingerprints clients (TLS/headers), not just counts requests.
- Advantage on a phone: each user has their **own residential/mobile IP**, so per-user volume is tiny — far better than a shared server. Carrier-grade NAT can occasionally put many users behind one IP.
- Expect breakage when markup changes; keep selectors in one place, add a parser self-test, and support a remote-free update path (app update).

**Libraries**
- Python: `ddgs` (renamed from `duckduckgo-search` in mid-2025) v9.16.1 (2026-10-23), https://pypi.org/project/ddgs — now a **metasearch** library: text backends bing, brave, duckduckgo, google, grokipedia, mojeek, startpage, yandex, yahoo, wikipedia. Good reference for current request shapes/selectors (MIT). Not usable on Android directly (Python), except via Chaquopy.
- Kotlin/Java ports: `senkuboy0-cyber/ddgs-android` (Kotlin port using Ktor+jsoup, 0 stars — unproven) https://github.com/senkuboy0-cyber/ddgs-android ; `OverclockedSenku/DuckDuckJVM` https://github.com/OverclockedSenku/DuckDuckJVM ; old `nstrydom2/duckduckgo` (Java). None is a maintained, trusted dependency → **write your own ~150-line jsoup parser**, using ddgs as the reference.
- Minimal pattern (used e.g. in a Quarkus demo): `Jsoup.connect("https://html.duckduckgo.com/html/?q=...")` then select `.result__a`, `.result__snippet`.

**What other apps do**
- **Open WebUI**: ships `ddgs` built in; engine value `duckduckgo`, no key, docs warn about rate limiting. https://docs.openwebui.com/features/chat-conversations/web-search/providers/ddgs/ . SearXNG supported via base URL (no key).
- **PocketPal AI** (v1.17.0, Aug 2026): web_search + read_url "talents", **BYOK** only (Brave, Tavily, Exa, later Firecrawl). https://github.com/a-ghorbani/pocketpal-ai/pull/808
- **Off Grid** (off-grid-ai, React Native): optional web_search tool, off by default; docs say it **scrapes Brave Search** for the top 5 results (no key). https://github.com/off-grid-ai/OGAM ; https://mintlify.wiki/alichherawalla/off-grid-mobile-ai/development/faq
- **RikkaHub** (Kotlin Android LLM client): has a `SearXNGService.kt` for user-supplied SearXNG instances. https://github.com/rikkahub/rikkahub
- **AnythingLLM Mobile**: advertises "web search built in, no API key" (implementation not public in sources checked). https://anythingllm.com/alternatives/pocketpal
- Keyless MCP servers (desktop) converge on the same design: DuckDuckGo HTML → Bing/Brave/Mojeek/Startpage fallback → SearXNG, plus caching (e.g. https://github.com/sweetcornna/free-search-mcp, https://github.com/tickernelz/mcp-web-search, https://github.com/mrkrsl/web-search-mcp).
- Maid / Layla / Jan / Perplexica: not verified in this pass. Perplexica is built on **SearXNG** as its search backend (from prior knowledge, not re-verified today).

## (b) SearXNG / metasearch ON the phone

- **SearXNG** (AGPL-3.0, Python, ~260 engines) **can run on Android via Termux** without root: guides https://github.com/LukasSR04/SearXNG-Termux-Android-noRoot (tested 2026, needs ≥4 GB RAM, ~2 GB storage) and https://github.com/Z-Hussein/SearxngOnAndroid ("not all engines work on Android"). Your app could then call `http://127.0.0.1:8888/search?q=..&format=json` (enable `json` in settings.yml).
  → Good **power-user option**, not something to bundle: requires Termux, Python, background process.
- Embedding Python in the APK (Chaquopy, now open source) to run SearXNG or ddgs in-process is technically possible but heavy (APK size, startup, battery) and AGPL obligations apply to SearXNG.
- **No mature Kotlin/Java metasearch library exists.** A NewPipe-Extractor-style approach (pure-Java extractors per engine, updated with app releases) is the realistic path: implement 2–4 engines yourself. Don't copy SearXNG engine code into a non-AGPL app; re-implement from observed behaviour.
- Search engine wrapper apps exist (e.g. searxist, https://codeberg.org/searxist/android) but only open instances in a UI.

## (c) Public SearXNG instances (searx.space)

- List + machine-readable JSON: https://searx.space/ , https://searx.space/data/instances.json (updated every 24 h; response times every 3 h).
- **JSON output is disabled by default** in SearXNG; requesting `format=json` on such instances returns **403** (docs: https://docs.searxng.org/dev/search_api.html). Disabling non-HTML formats was a deliberate hardening because "bots are the only users of this feature on a public instance" (https://github.com/searxng/searxng/pull/226).
- Real-world tests in 2026: top instances return 429 or 403 for programmatic JSON, others return a bot-challenge HTML page with 200 OK (https://github.com/ChHsiching/agent-web-search/issues/10 ; https://dev.to/wrencastellan/your-searxng-json-integration-works-on-your-own-instance-and-silently-breaks-on-everyone-elses-d7o).
- Scraping the HTML UI of public instances circumvents the operators' limiter → poor etiquette; public instances also get blocked upstream more often (searx.space says so itself), and the operator sees the queries (trust boundary, https://github.com/ihor-sokoliuk/mcp-searxng/blob/main/docs/public-searxng-instances.md).
- **Verdict:** don't hardcode or auto-rotate public instances. Offer a **"Your own SearXNG URL"** setting (self-hosted, a friend's, or Termux localhost), validate it returns JSON, and use it as the top-priority general search source when present.

## (d) Keyless sources

| Source | Endpoint (examples) | Key? | Limits / terms | German? | Verdict |
|---|---|---|---|---|---|
| **Wikipedia / Wikimedia** | `https://de.wikipedia.org/w/rest.php/v1/search/page?q=..&limit=5`; `/w/api.php?action=query&list=search&srsearch=..&format=json`; summaries `/api/rest_v1/page/summary/{title}` | No | 2026 global rate limits: **10 req/min for unidentified clients, 200 req/min with a compliant User-Agent** (name/version + contact URL/email); ≤3 concurrent; honour 429 Retry-After. https://www.mediawiki.org/wiki/Wikimedia_APIs/Rate_limits ; UA policy https://foundation.wikimedia.org/wiki/Policy:Wikimedia_Foundation_User-Agent_Policy . Content CC BY-SA (attribute). | Excellent (de.wikipedia) | **Core source.** Set a proper UA or you get 10/min. |
| **Wikidata** | `https://www.wikidata.org/w/api.php?action=wbsearchentities&language=de&search=..`; SPARQL `query.wikidata.org` | No | Same Wikimedia limits/UA policy. CC0 data. | Yes (labels in de) | Good for facts (population, birthdates). |
| **Open-Meteo** | `https://api.open-meteo.com/v1/forecast?latitude=..&longitude=..`; geocoding `https://geocoding-api.open-meteo.com/v1/search?name=Köln&language=de` | No | Free **non-commercial**: <10,000 calls/day, 5,000/h, 600/min; CC BY 4.0 attribution. https://open-meteo.com/en/terms | Yes | **Core for weather**; geocoding avoids Nominatim. Per-device IP → limits irrelevant. Commercial app needs paid plan. |
| **Nominatim (OSM)** | `https://nominatim.openstreetmap.org/search?q=..&format=jsonv2&accept-language=de` | No | **Max 1 req/s**, identifying UA/Referer required (stock library UAs not OK), attribution, ODbL, no heavy use / no autocomplete. https://operations.osmfoundation.org/policies/nominatim/ | Yes | Use sparingly (address lookups), cache results. |
| **Tagesschau** | `https://www.tagesschau.de/api2u/homepage/`, `/api2u/news/?regions=..`, `/api2u/search/?searchText=..`; RSS `https://www.tagesschau.de/index~rss2.xml` | No | **Private, non-commercial use only, no republication; max 60 requests/hour** (community docs https://tagesschau.api.bund.dev/ ; RSS terms https://www.tagesschau.de/infoservices/rssfeeds). API is unofficial. | Native | Good for "Nachrichten heute" — user's own device, private use fits. Cache 10–15 min. |
| **Other RSS** (DLF, ZDF heute, Spiegel, heise…) | publisher feeds | No | Per-publisher terms; usually personal use OK | Yes | Add a few curated feeds for news intent. |
| **arXiv** | `http://export.arxiv.org/api/query?search_query=..` | No | arXiv asks for ~1 request / 3 s (from arXiv API docs, not re-verified today) | English | Niche, cheap to add. |
| **Marginalia** | `https://api2.marginalia-search.com/search?query=..` header `API-Key: public` | "public" demo key | Shared global limit across all users, "often hits a rate limit" (HTTP 503/429); free-key results CC-BY-NC-SA; own free non-commercial key by email. https://about.marginalia-search.com/article/api | Poor (small, mostly English non-commercial web) | Skip, or optional extra. Shipping a personal key = an API key. |
| **Wiby** | `https://wiby.me/json/?q=..&p=2` | No | Terms: use at own risk, **link back to wiby.me**. https://wiby.me/json/ | Poor (tiny "classic web" index) | Optional novelty source only. |
| **Mojeek** | scraping www.mojeek.com | — | ToS **prohibits automated access** except authorised API users; API is paid (£2/1,000, free credit needs card). https://www.mojeek.com/about/terms.html , https://www.mojeek.com/services/search/web-search-api | OK-ish | Don't scrape. |
| **Brave Search** | scraping search.brave.com | — | Brave sells an API ($5 free credit/month, needs card + key); Off Grid scrapes the HTML anyway. Consumer-site ToS on scraping not verified in this pass; assume not permitted. | Good | Grey zone; possible fallback like DDG, low priority. |
| **Startpage / Qwant / Ecosia / Bing / Google / Yahoo** | HTML scraping | — | ddgs implements several of these; consumer ToS generally disallow automated queries (not individually verified today). Google/Bing aggressively CAPTCHA. Bing Search API was retired Aug 2025. | Good | Avoid as defaults; at most user-enabled "experimental" fallbacks. |

## (e) Offline knowledge: Kiwix / ZIM

- German Wikipedia ZIM sizes (mirror listing https://www.mirrorservice.org/sites/download.kiwix.org/zim/wikipedia/): **`wikipedia_de_all_nopic_2025-09` = 13 GB**, `wikipedia_de_all_maxi_2026-01` = 49 GB, **`wikipedia_de_all_mini_2026-07` = 3.9 GB** (intro + infobox only). Also topic subsets (`wikipedia_de_top_*`, physics etc.). Flavour definitions: https://get.kiwix.org/en/faq-items/what-do-mini-nopic-and-maxi-mean-in-the-wikipedia-zim-files
- Library: **java-libkiwix** — Kotlin/Java bindings to libkiwix + libzim for Android (`Archive("path")`, search, main page). https://github.com/kiwix/java-libkiwix — **GPL-3.0**, so linking it makes your app GPL-3.0 (fine for an open-source app, a problem for proprietary).
- Android storage: Play-Store builds can only read files in app-specific/SAF-granted locations (Kiwix itself moved to this model). Let the user download the ZIM in-app or pick it via SAF.
- Fit for a 1.7B model: mini ZIM + full-text search → top 3 article intros is an excellent offline RAG source for facts; nopic gives full articles if the user has 13 GB to spare.

## Fetching & cleaning result pages on-device

- **HTTP**: OkHttp (timeouts 8–10 s, max body ~2 MB, follow redirects, gzip/brotli). Respect robots? Not legally required for user-initiated single fetches, but skip obvious paywalls/logins.
- **Parsing**: jsoup (https://github.com/jhy/jsoup) or ksoup (KMP port).
- **Main-content extraction**:
  - **Readability4J** — Kotlin port of Mozilla Readability, Apache-2.0, same output as Firefox Reader View; `net.dankito.readability4j:readability4j:1.0.8` (last release Oct 2021; Feeder devs call it under-maintained with some over/under-stripping bugs, https://github.com/spacecowboy/Feeder/issues/930). Use `Readability4JExtended`. https://github.com/dankito/Readability4J
  - **Readability4K** — newer Kotlin Multiplatform port (ksoup, Android minSdk 26, JitPack, v0.2.0). https://github.com/Sermilion/readability4k
  - **Crux** (Chimbori) — metadata/URL cleanup, recommends Readability4J for article text. https://github.com/chimbori/crux
  - **WebView + Mozilla Readability.js** (https://github.com/mozilla/readability): load the URL in an off-screen WebView, inject Readability.js, `evaluateJavascript("new Readability(document.cloneNode(true)).parse()")`. Handles JS-rendered pages and uses a real Chromium fingerprint; slower (1–3 s) and heavier — use as fallback.
- **Post-processing for a 1.7B model**: convert to plain text/markdown, strip boilerplate, chunk by heading, rank chunks against the query (BM25 or a small on-device embedding model), feed **≤1,500–2,500 tokens** total with source numbers `[1] [2]` for citations. Small models degrade fast with long, noisy context.

---

## Recommended architecture (Kotlin)

```
SearchTool (called by LLM tool-call or a pre-classifier)
 ├─ IntentRouter (rules + tiny prompt): weather | place | news | fact | paper | web
 ├─ Providers (interface SearchProvider { suspend fun search(q, lang="de"): List<Hit> })
 │   ├─ OpenMeteoProvider (+ OpenMeteoGeocoding)      // weather
 │   ├─ NominatimProvider (1 req/s limiter)          // addresses
 │   ├─ TagesschauProvider (API + RSS, 60/h budget)  // news
 │   ├─ WikipediaProvider (de, REST search + summary) // facts
 │   ├─ WikidataProvider                              // structured facts
 │   ├─ ArxivProvider                                 // papers
 │   ├─ UserSearxngProvider (JSON, if URL configured) // general web, preferred
 │   ├─ DuckDuckGoHtmlProvider (POST html/lite, kl=de-de) // general web, best effort
 │   ├─ DuckDuckGoWebViewProvider (fallback when 202/403/CAPTCHA)
 │   ├─ ZimProvider (java-libkiwix, offline)          // optional
 │   └─ WibyProvider (optional)
 ├─ FallbackChain: per intent, ordered list; stop at first provider with ≥3 good hits
 ├─ ResilienceLayer: per-host token bucket, exp. backoff + jitter, circuit breaker
 │                    (e.g. DDG disabled 30 min after 2 rate-limit responses), timeouts
 ├─ Cache: Room/SQLite, key = normalized query+provider; TTL: news 15 min,
 │         weather 30 min, web 24 h, wiki 7 d; page-extract cache by URL
 ├─ PageReader: OkHttp → Readability4J/4K → (fallback) WebView+Readability.js
 └─ ContextBuilder: dedupe hosts, chunk, rank, cap tokens, attach citations
```

Default general-web chain: **User SearXNG → DDG html → DDG lite → DDG via WebView → Wikipedia search** (and show "Websuche gerade nicht verfügbar" rather than looping).

### Implementation details that matter
- **User-Agent**: for Wikimedia/Nominatim/Open-Meteo/Tagesschau use an honest app UA with contact, e.g. `MeinChatbot/1.2 (https://github.com/<you>/<app>; contact@...) okhttp/4.12`. For DDG, use a normal current mobile Chrome UA plus consistent `Accept-Language: de-DE,de;q=0.9` — header sets must be self-consistent (mismatches trigger blocks). Don't rotate UAs aggressively.
- **Volume discipline**: one search per user turn at most, max 5 results, fetch ≤3 pages; search only when the user enables the web toggle or the model emits a tool call the user can see.
- **German results**: DDG `kl=de-de`; SearXNG `language=de`; Wikipedia `de.`; Open-Meteo `language=de`; Nominatim `accept-language=de`.
- **Privacy**: the query necessarily leaves the device to whichever host is used — show the host in the UI ("Gesucht über DuckDuckGo"), no telemetry, no cookies persisted for search hosts, optional Tor/Orbot proxy setting.
- **Attribution**: Wikipedia (CC BY-SA), Open-Meteo (CC BY 4.0), OSM (ODbL), Wiby (link back), Tagesschau ("Quelle: tagesschau.de", private use).

## Risks and mitigations

| Risk | Mitigation |
|---|---|
| DDG markup change / 202 ratelimit / CAPTCHA | Fallback chain, circuit breaker, WebView path, selector tests in CI against saved fixtures, quick app updates; never make DDG the only path |
| ToS exposure (DDG, Brave, Mojeek, Startpage…) | Low per-user volume, user-initiated only, non-commercial; prefer sanctioned APIs via intent routing; label scraping sources "experimental"; for a commercial release, budget for a paid API or require user-provided SearXNG/BYOK |
| Wikimedia 10 req/min cap | Compliant UA → 200/min; ≤3 concurrent; honour Retry-After; cache |
| Public SearXNG 403/429/bot pages | Don't use them automatically; only user-supplied URL; validate JSON on save |
| Tagesschau 60 req/h, unofficial API | Cache 15 min, fall back to RSS |
| Open-Meteo / Tagesschau non-commercial clauses | Fine for a free hobby/open-source app; revisit if monetised |
| GPL (java-libkiwix), AGPL (SearXNG) | Keep app open source (GPL-3.0 compatible) if you embed libkiwix; don't copy SearXNG code |
| Small-model context overload / hallucinated citations | Strict token budget, chunk ranking, numbered sources, instruct "answer only from sources" |
| Carrier-grade NAT shared IPs | Backoff + cache; WebView fallback; SearXNG option |

## Gaps / not verified in this pass
- Maid, Layla, Jan web-search internals; AnythingLLM Mobile's keyless implementation.
- Current consumer ToS wording of Brave, Startpage, Qwant, Ecosia, Bing regarding automated queries (assumed restrictive).
- arXiv's current rate guidance (from prior knowledge: ~1 req/3 s).
- Marginalia public-key exact quota today (HN 2023 comment said ~15 queries/min shared).
