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
