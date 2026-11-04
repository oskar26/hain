# NEWS.md — news feature (ADR-14, ADR-15)

## 1. Modes (the only user choice)

| Mode key | Name DE / EN | Feeds used |
|---|---|---|
| `standard` (default) | Standard / Standard | fixed list §2 |
| `standard_plus_own` | Standard + eigene / Standard + my feeds | §2 + user feeds |
| `own_only` | Nur eigene / My feeds only | user feeds only (empty state: "Füge einen Feed hinzu") |

**Not allowed:** toggling single standard feeds, presets, political labels, categories, "bias" badges, sorting by camp.
UI shows: source name, headline, teaser (max 3 lines), time, optional thumbnail, a small "Paywall" badge if `paywall=true`.
User feeds: add by URL (validate it parses as RSS/Atom/RDF), rename, delete. Max 50.

## 2. Standard feed list (fixed, `core-tools/src/main/assets/news/standard_feeds.json`)

| id | name | lang | url | paywall | note |
|---|---|---|---|---|---|
| tagesschau | tagesschau | de | https://www.tagesschau.de/index~rss2.xml | false | max 60 req/h |
| dlf | Deutschlandfunk | de | https://www.deutschlandfunk.de/nachrichten-100.rss | false | |
| zeit | ZEIT ONLINE | de | https://newsfeed.zeit.de/index | true | |
| taz | taz | de | https://taz.de/!p4608;rss/ | false | |
| jungewelt | junge Welt | de | https://www.jungewelt.de/feeds/newsticker.rss | true | |
| nd | nd | de | https://www.nd-aktuell.de/rss/aktuell.php | true | |
| bbc | BBC News | en | https://feeds.bbci.co.uk/news/rss.xml | false | |
| guardian | The Guardian | en | https://www.theguardian.com/world/rss | false | |
| dw_en | DW | en | https://rss.dw.com/rdf/rss-en-all | false | RDF format |
| aje | Al Jazeera | en | https://www.aljazeera.com/xml/rss/all.xml | false | |
| jacobin | Jacobin | en | https://jacobin.com/feed/ | false | full text in feed → still show teaser only |
| democracynow | Democracy Now! | en | https://www.democracynow.org/democracynow.rss | false | |
| correctiv | CORRECTIV | de | https://correctiv.org/feed/ | false | keep only items with category containing "Faktencheck" |
| fullfact | Full Fact | en | https://fullfact.org/feed/ | false | |

All URLs verified working on 2026-10-27. Language filter: app shows feeds of **both** languages by default,
setting `news_languages` = `both` | `app_language_only`.

## 3. Fetching

- `FeedFetcher` uses `NetworkGate` (purpose = `news`), header `User-Agent: Hain/<version> (+https://github.com/<repo>)`.
- Conditional GET: store `ETag` / `Last-Modified` per feed, send `If-None-Match` / `If-Modified-Since`.
- Interval: setting `news_refresh_minutes` (15/30/60, default 30), only when the news screen or the `news` tool is used, plus optional WorkManager job (Wi-Fi only setting).
- Parser: own small XmlPullParser-based parser supporting RSS 2.0, Atom, RSS 1.0/RDF. Fields: title, link, guid, published, summary/description (HTML stripped, max 500 chars), image (`media:content`, `enclosure`, `media:thumbnail`).
- Health: if response is HTML or has 0 items 3 times in a row → feed `status=broken`; shown in settings for own feeds only; standard feeds are skipped silently and logged.
- Dedup: same normalized title (lowercase, no punctuation) within 24 h → keep first.
- Keep items 7 days (table `news_item`).

## 4. `news` tool (for the LLM)

```json
{"name":"news","description":"Get recent headlines. Use for current events.","parameters":{"type":"object","properties":{"query":{"type":"string","description":"topic, optional"},"lang":{"type":"string","enum":["de","en","any"]}},"required":[]}}
```
Returns to model: up to 8 items `[n] source – title – teaser(200 chars) – date`. Card: `NewsCard` with the items and links.
Query matching: FTS5 over title+summary of cached items; if cache older than refresh interval, refresh first.
If query has no hits in feeds → model may call `web_search`.

## 5. Summary of one article (user action only)

Button "Zusammenfassen / Summarize" on a news item → fetch page via NetworkGate → Readability4K → max 2,500 tokens → LLM summary (5 bullets) → shown, **not stored** (only kept in memory for the open screen). If paywall/blocked → message "Artikel nicht frei lesbar" + open link button. Never bypass paywalls (no archive sites, no cookie tricks).

## 6. Screen

`:feature-news` — list grouped by "Heute / Gestern / Älter", pull to refresh, top bar with mode switch (3 segment control). Tap → in-app Custom Tab (no tracking params added). Long-press → share link.

## 7. As built (0.1.5, T-071–T-074)

- Code: `core-tools/.../internal/news/` (`NewsRepository`, `NewsStore`, `NewsTool`, `FeedParser`), `core-cards/.../NewsCard.kt`, `feature-news/.../api/` (`NewsScreen`, `NewsViewModel`, `OwnFeedsPage`, `SummarizeArticle`), `app/.../NewsBrain.kt`.
- Storage: one JSON file `files/news/news.json` (mode, own feeds, ETag/Last-Modified + failure count per feed, items of the last 7 days, at most 150 per feed) instead of a Room table; no schema migration.
- `news` tool query matching: keyword score over title (×3) and teaser instead of FTS5; no hit → `not_found` → router falls back to `web_search`.
- Refresh: fixed 30 min, only when the news screen opens (or the refresh button) or the `news` tool runs; no WorkManager job and no `news_refresh_minutes` / `news_languages` settings yet (both languages always).
- Network log: feeds and summarised articles use `Purpose.NEWS` and have their own filter "Nachrichten / News".
- Screen: refresh button instead of pull-to-refresh, no thumbnails (no image loader in the project), links open in the browser (no Custom Tabs dependency).
- Summary: page text cut to 9,000 characters (~2,500 tokens), brain call without thinking, max 400 tokens, 5 bullets; answers 4xx, short pages and subscription walls ("Jetzt abonnieren", "Subscribe to continue" …) → "Artikel nicht frei lesbar".

## 8. As built (0.1.10.3, T-075–T-079, news reader)

- **Feeds (T-075):** 19 standard feeds (added ZDFheute, DW Deutsch, DER SPIEGEL, Süddeutsche Zeitung, NPR); no paywall flag per feed any more (see T-077). Each feed loads on its own with a 15 s limit and shows as soon as it is there; dates in the future (junge Welt dates articles on tomorrow's paper) are clamped to now. `NewsRepository.mixed()` lets sources take turns when one source has many new items.
- **Pictures (T-075):** from the feed (`media:*`, `enclosure`, `<img>` in the HTML of the description) or, for cards without one, the page's `og:image` (`PageImage`, once per link, remembered in `news.json`). Loaded by `NewsImages` through `NetworkGate.stream` (purpose `IMAGES`, offline mode blocks), checked by their first bytes (an HTML error page is never a picture), disk cache `cache/news-images` (48 MB, oldest go first) + decoded memory cache. Setting "Bilder in der Liste" / "Bilder im Artikel laden".
- **Screen:** frosted top bar (Haze through `hainGlass`), mode chips in one line (scroll sideways instead of wrapping), "Dein Briefing heute", a big top story (newest item with a picture, ≤ 36 h), cards with picture right, Heute/Gestern/Älter, own pull to refresh (nested scroll, no Material), settings page (gear). From 840 dp list (42 %, 340–460 dp) left and article right.
- **Reading view (T-076), replaces §5:** page through the gate (purpose `NEWS`, only on tap), `Readability` (own code: container scoring, siblings, link density, JSON-LD fallback) → headings, paragraphs, quotes, lists, pictures with captions. Less than 280 characters and no wall → "kann Hain nicht direkt anzeigen" + website. Settings: text size (5 steps), serif/sans, line spacing (3), colours (app/light/dark/sepia), article pictures; max 680 dp wide. The last 12 articles stay in memory while the screen is open.
- **Paywall (T-077):** `Paywall.kt` decides after reading whether the article is cut off (JSON-LD `isAccessibleForFree=false`, typical wall texts, short text with a wall); only then the card gets the "Paywall" mark (`markRead`, `news.json` v2). The reader shows what the page shows, with a hint and "Auf Website öffnen". No archive sites, no cookie tricks.
- **Ask Pim (T-078):** Pim bubble in the reading view → sheet with "Zusammenfassen", "Einfach erklärt", "Hintergrund" and a text field. The article text is cut to the brain's context: (context − 1,100 tokens) × 3 characters, 1,500–24,000, at a paragraph or sentence end; thinking off, temperature 0.1, max 500 tokens, answer only from the article. Nothing stored; "Im Chat weiterführen" opens a new chat with article, link, question and answer in the box.
- **Briefing (T-079):** `BriefingPicker` clusters the last 24 h (≥ 3 shared words or Jaccard ≥ 0.28), score = sources × 3 + freshness, max 2 leads per source, 5–7 points. Sentences by the brain only when it is already loaded on open (no 1 GB load just for that), "Neu erzeugen" loads it; otherwise the story's first sentence. Kept once a day in `files/news/briefing.json`. Optional morning note around 07:00 (`BriefingDaily` + `BriefingWorker`, WorkManager, network required, without the brain), off by default; switching it on asks for POST_NOTIFICATIONS on Android 13+.
