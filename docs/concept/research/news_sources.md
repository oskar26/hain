# News & Facts – Preset-Quellenliste (Stand 2026-10-27)

Alle URLs unten wurden am 2026-10-27 ca. 23:50 CET per `curl -L` (UA `Mozilla/5.0 (Android) NewsApp/1.0`) abgerufen.
"✅" = HTTP 200 + gültiges RSS/Atom/JSON mit Einträgen. "Inhalt" = was im Feed steckt (gemessen an Median-Länge von `description`/`content:encoded`):
**Teaser** (< ~1 kB, nur Anriss), **Lang** (längerer Auszug), **Voll** (ganzer Artikeltext im Feed).

Leaning-Beschreibungen sind kurze, möglichst neutrale Einordnungen (Selbstverständnis/übliche Medieneinordnung), keine Bewertung.
Lizenz: Wenn nicht anders vermerkt, gilt Urheberrecht des Verlags; RSS ist zum persönlichen Lesen gedacht → App zeigt **nur Headline + Teaser + Link**; Volltext nur on demand zum lokalen Zusammenfassen, nicht speichern/weiterverbreiten.

---

## 1. Deutsch – Mainstream

| ID | Quelle | Feed / API (verifiziert) | Sprache | Einordnung | Inhalt | Paywall / Terms |
|---|---|---|---|---|---|---|
| tagesschau | Tagesschau (ARD) | ✅ `https://www.tagesschau.de/index~rss2.xml` · alle Meldungen: ✅ `https://www.tagesschau.de/infoservices/alle-meldungen-100~rss2.xml` · API ✅ `https://www.tagesschau.de/api2u/homepage/` (+ `/api2u/news/?ressort=inland`, `/api2u/search/?searchText=`) | de | Öffentlich-rechtlich, Mitte | Teaser (content:encoded ~0,7 kB) | Frei. API-Terms (bundesAPI-Doku): **privater, nicht-kommerzieller Gebrauch erlaubt, Veröffentlichung nicht** (außer CC-Inhalte, tagesschau.de/creativecommons); **max. 60 Abrufe/Stunde**. Quelle: github.com/bundesAPI/tagesschau-api |
| zdfheute | ZDFheute | ✅ `https://www.zdfheute.de/rss/zdf/nachrichten` (alte `zdf.de/rss/...` leitet hierhin um) | de | Öffentlich-rechtlich, Mitte | Teaser | Frei |
| dlf | Deutschlandfunk | ✅ `https://www.deutschlandfunk.de/nachrichten-100.rss` | de | Öffentlich-rechtlich, Mitte | Teaser (Kurzmeldungen) | Frei |
| spiegel | DER SPIEGEL | ✅ `https://www.spiegel.de/schlagzeilen/index.rss` | de | Liberal, Mitte-links | Teaser | Teilw. Paywall (SPIEGEL+) |
| zeit | DIE ZEIT / ZEIT ONLINE | ✅ `https://newsfeed.zeit.de/index` | de | Liberal, Mitte-links | Teaser | Teilw. Paywall (Z+) |
| sz | Süddeutsche Zeitung | ✅ `https://rss.sueddeutsche.de/rss/Topthemen` | de | Linksliberal | Teaser | Teilw. Paywall (SZ Plus) |
| faz | FAZ | ✅ `https://www.faz.net/rss/aktuell/` | de | Liberal-konservativ, wirtschaftsliberal | Teaser (~0,5 kB) | Teilw. Paywall (F+) |
| taz | taz | ✅ `https://taz.de/!p4608;rss/` | de | Links-alternativ, grün-nah | Teaser | Frei („taz zahl ich“, freiwillig) |
| standard | Der Standard (AT) | ✅ `https://www.derstandard.at/rss` (DE-Ausgabe: ✅ `https://www.derstandard.de/rss`) | de-AT | Linksliberal | Teaser | Meist frei, teilw. Abo |
| srf | SRF News (CH) | ✅ `https://www.srf.ch/news/bnf/rss/1646` | de-CH | Öffentlich-rechtlich, Mitte | Teaser | Frei |
| dw_de | DW Deutsch | ✅ `https://rss.dw.com/rdf/rss-de-all` | de | Auslandssender (staatl. finanziert), Mitte | Teaser | Frei |
| dpa | dpa | ❌ kein öffentlicher Feed/API – nur kommerzielle Lizenz. dpa-Meldungen erscheinen ohnehin in DLF/ZEIT/SZ-Feeds | – | Nachrichtenagentur | – | kommerziell |

## 2. Deutsch – links / antikapitalistisch / marxistisch

| ID | Quelle | Feed (verifiziert) | Einordnung | Inhalt | Paywall / Hinweis |
|---|---|---|---|---|---|
| jungewelt | junge Welt | ✅ `https://www.jungewelt.de/feeds/newsticker.rss` (33 Items; `/feeds/aktuell.rss` = 404) | Marxistische Tageszeitung; wird im Verfassungsschutzbericht des Bundes als linksextremistisch geführt (sachlicher Hinweis, ggf. im Info-Dialog) | Teaser | Teilw. Paywall (Online-Abo) |
| nd | nd (nd-aktuell) | ✅ `https://www.nd-aktuell.de/rss/aktuell.php` | Sozialistisch, links (Genossenschaft, früher PDS/Linke-nah) | Teaser | Teilw. Paywall |
| freitag | der Freitag | ✅ `https://www.freitag.de/@@RSS` | Linksliberal bis links, Debattenmedium | Lang (~1,8 kB) | Teilw. Paywall |
| jacobin_de | Jacobin (DE) | ✅ `https://jacobin.de/rss/` (`/rss` & `/feed` leiten um) | Demokratisch-sozialistisch | Teaser/kurzer Auszug | Online frei, Print-Abo |
| ak | analyse & kritik | ✅ `https://www.akweb.de/feed/` | Undogmatische radikale Linke | Lang (Auszug) | Teilw. Paywall |
| kgk | Klasse gegen Klasse | ✅ `https://www.klassegegenklasse.org/feed/` | Trotzkistisch (RIO / FT-CI) | **Voll** | Frei |
| uz | Unsere Zeit (DKP) | ✅ `https://www.unsere-zeit.de/feed/` | Kommunistisch, Parteizeitung der DKP | Teaser | Teilw. Abo |
| marx21 | marx21 | ⚠️ `https://marx21.de/feed/` antwortet 200, aber **0 Items** → nicht als Default; ggf. später erneut prüfen | Trotzkistisch (IS-Tradition), Linke-nah | – | – |
| telepolis | Telepolis | ✅ `https://www.telepolis.de/news-atom.xml` (→ `/feed.xml`, Atom, 100 Einträge) | Heterodox/kritisch, Debattenportal (früher heise) | Teaser | Teilw. Paywall (heise+/Telepolis+) |
| lcm | Lower Class Magazine | ✅ `https://lowerclassmag.com/feed/` | Autonom-revolutionäre Linke | **Voll** | Frei |

## 3. English – Mainstream

| ID | Quelle | Feed / API (verifiziert) | Einordnung | Inhalt | Paywall / Terms |
|---|---|---|---|---|---|
| bbc | BBC News | ✅ `https://feeds.bbci.co.uk/news/rss.xml` · World ✅ `https://feeds.bbci.co.uk/news/world/rss.xml` | Public broadcaster (UK), centre | Teaser | Free; RSS for personal, non-commercial use |
| guardian | The Guardian | ✅ `https://www.theguardian.com/world/rss` · ✅ `https://www.theguardian.com/international/rss` · API `https://content.guardianapis.com/search?api-key=KEY` | Centre-left / liberal | Teaser (RSS); API liefert Volltext | Free (Spendenmodell). **API braucht Key**: kostenloser Developer-Key (nicht-kommerziell, ~1 Call/s, 500/Tag; open-platform.theguardian.com/access). Shared Key `test` gab heute **401** → nicht einplanen; App sollte optional User-Key annehmen |
| reuters | Reuters | ❌ Öffentliche RSS-Feeds seit ~2020 eingestellt; `reutersagency.com/feed/` = 404, `reuters.com/world/rss` = 401 | Agency, centre | – | Nicht verfügbar |
| ap | AP News | ❌ Kein offizieller RSS; `apnews.com/index.rss` = 403 (Cloudflare), rsshub.app = 403 | Agency, centre | – | Nicht verfügbar |
| npr | NPR | ✅ `https://feeds.npr.org/1001/rss.xml` | Public radio (US), centre-left | Teaser (~0,7 kB) | Free, personal use |
| aje | Al Jazeera English | ✅ `https://www.aljazeera.com/xml/rss/all.xml` | Qatari state-funded, Global-South focus | Teaser | Free |
| dw_en | DW English | ✅ `https://rss.dw.com/rdf/rss-en-all` | German public intl. broadcaster, centre | Teaser | Free |

## 4. English – left / anti-capitalist / Marxist

| ID | Quelle | Feed (verifiziert) | Einordnung | Inhalt | Paywall / Lizenz |
|---|---|---|---|---|---|
| jacobin | Jacobin | ✅ `https://jacobin.com/feed/` (Atom-artig, `<content>` ~13 kB) | Democratic socialist | **Voll** | Free online |
| democracynow | Democracy Now! | ✅ `https://www.democracynow.org/democracynow.rss` | Progressive independent news (US) | Lang (Zusammenfassungen) | Free; Feed deklariert **CC BY-NC-ND 3.0 US** |
| intercept | The Intercept | ✅ `https://theintercept.com/feed/?rss` | Adversarial/investigative, progressive | **Voll** (~18 kB) | Free |
| monthlyreview | Monthly Review | ✅ `https://monthlyreview.org/feed/` | Marxist (independent socialist magazine) | **Voll** | Teilw. Abo (Magazin) |
| mronline | MR Online | ⚠️ `https://mronline.org/feed/` → Bot-Challenge-Seite (HTML) vom Server aus; auf Gerät evtl. ok, nicht als Default | Marxist, aggregiert/republiziert | – | – |
| morningstar | Morning Star (UK) | ✅ `https://morningstaronline.co.uk/rss.xml` (nur 10 Items) | Socialist daily, CPB-nah | Teaser | Teilw. Abo |
| peoplesdispatch | Peoples Dispatch | ✅ `https://peoplesdispatch.org/feed/` | Left / Global-South movements | **Voll** | Free (Lizenz nicht separat geprüft) |
| commondreams | Common Dreams | ✅ `https://www.commondreams.org/feeds/feed.rss` | Progressive | Lang (~4 kB) | Free (Lizenz nicht separat geprüft) |
| truthout | Truthout | ✅ `https://truthout.org/feed/` | Progressive / left | Teaser | Free (Spendenmodell) |
| wsws | World Socialist Web Site | ✅ `https://www.wsws.org/en/rss.xml` | Trotzkistisch; Organ des ICFI / der Socialist Equality Parties – **Parteimedium**, im Label kennzeichnen | Teaser | Free |
| novara | Novara Media | ✅ `https://novaramedia.com/feed/` | Left (UK), Labour-left/Corbyn-nah | **Voll** | Free |

## 5. Faktenchecker

| ID | Quelle | Feed (verifiziert) | Sprache | Hinweis |
|---|---|---|---|---|
| correctiv | CORRECTIV (gesamt) | ✅ `https://correctiv.org/feed/` (100 Items) | de | Der Unterfeed `correctiv.org/faktencheck/feed/` liefert aktuell nur **1 Item** → Gesamtfeed nehmen und clientseitig nach Kategorie „Faktencheck“ filtern |
| dpa_fc | dpa-Faktencheck | ❌ kein Feed gefunden (`dpa-factchecking.com/germany/feed/`, `/index.xml`, `/rss.xml` = 404; Seite ist JS-App) | de | Nur als Link-Ziel `https://dpa-factchecking.com/germany/` |
| mimikama | Mimikama (AT) | ✅ `https://www.mimikama.org/feed/` | de | Teaser |
| snopes | Snopes | ✅ `https://www.snopes.com/feed/` | en | Teaser |
| fullfact | Full Fact (UK) | ✅ `https://fullfact.org/feed/` | en | Teaser |

## 6. Fakten / Nachschlagen (APIs, alle ohne Key getestet)

| ID | Dienst | Verifizierter Test-Call | Key? | Terms / Limits |
|---|---|---|---|---|
| wiki_de / wiki_en | Wikipedia | ✅ `https://de.wikipedia.org/api/rest_v1/page/summary/Köln` · ✅ `https://en.wikipedia.org/w/api.php?action=query&list=search&srsearch=cologne&format=json` | nein | CC BY-SA 4.0; eigenen User-Agent mit Kontakt setzen |
| wikidata | Wikidata | ✅ `https://www.wikidata.org/w/api.php?action=wbsearchentities&search=Cologne&language=en&format=json` · SPARQL ✅ `https://query.wikidata.org/sparql?query=…&format=json` | nein | CC0 |
| wikivoyage | Wikivoyage | ✅ `https://de.wikivoyage.org/api/rest_v1/page/summary/Köln` | nein | CC BY-SA |
| wiktionary | Wiktionary | ✅ `https://de.wiktionary.org/w/api.php?action=query&titles=Haus&prop=extracts&format=json` | nein | CC BY-SA |
| genesis | Destatis GENESIS-Online | REST nur noch **POST** (seit 15.07.2025, GET abgeschaltet), Zugangsdaten im Header. Docs: genesis.destatis.de/genesisWS/swagger-ui/ | **Ja, für die meisten Methoden** (kostenloser Account → persönlicher API-Token); Gast/`GAST` laut Swagger-Default nur eingeschränkt (Test hier nicht erfolgreich) | Datenlizenz Deutschland – Namensnennung 2.0. → als optionale Quelle mit User-Token; für DE-Basisdaten Eurostat nutzen |
| eurostat | Eurostat | ✅ `https://ec.europa.eu/eurostat/api/dissemination/statistics/1.0/data/tps00001?geo=DE&format=JSON` | nein | Wiederverwendung mit Quellenangabe |
| worldbank | World Bank | ✅ `https://api.worldbank.org/v2/country/DE/indicator/SP.POP.TOTL?format=json&mrv=1` | nein | CC BY 4.0 |
| oecd | OECD Data (SDMX) | ✅ `https://sdmx.oecd.org/public/rest/dataflow/all` (Achtung: 8,9 MB – nur einzelne Dataflows abfragen) | nein | Rate-Limits beachten |
| owid | Our World in Data | ✅ `https://ourworldindata.org/grapher/life-expectancy.csv` (CSV pro Grapher; `?country=` filtert nicht serverseitig) | nein | CC BY 4.0 |
| bpb | Bundeszentrale für politische Bildung | ✅ „Hintergrund aktuell“ `https://www.bpb.de/rss-feed/230153.rss` · ✅ Online-Angebote `https://www.bpb.de/rss-feed/133223.rss` · ✅ APuZ `https://www.bpb.de/rss-feed/230868.rss` · ✅ Publikationen `https://www.bpb.de/rss-feed/62409.rss` (Übersicht: bpb.de/die-bpb/ueber-uns/service/rss/) | nein | Viele Texte CC BY-NC-ND (je Artikel prüfen) |
| openmeteo | Open-Meteo | ✅ `https://api.open-meteo.com/v1/forecast?latitude=50.94&longitude=6.96&current=temperature_2m` | nein | Frei nicht-kommerziell (<10k Calls/Tag), CC BY 4.0 |
| arxiv | arXiv | ✅ `https://export.arxiv.org/api/query?search_query=all:llm&max_results=1` (Atom) | nein | ≤1 Request / 3 s |
| pubmed | PubMed E-utilities | ✅ `https://eutils.ncbi.nlm.nih.gov/entrez/eutils/esearch.fcgi?db=pubmed&term=covid&retmode=json&retmax=1` | nein (optional, erhöht Limit) | 3 req/s ohne Key |
| openalex | OpenAlex | ✅ `https://api.openalex.org/works?search=climate&per-page=1` | **Hinweis**: Antwort meldet „No API key → shares one free daily budget ($0.10) per IP“ – ohne Key nutzbar, aber knapp; kostenloser Key empfohlen | CC0-Daten |
| crossref | Crossref | ✅ `https://api.crossref.org/works?query=climate&rows=1` | nein | `mailto=` Parameter für „polite pool“ |
| nominatim | OSM Nominatim | ✅ `https://nominatim.openstreetmap.org/search?q=Köln&format=json&limit=1` | nein | ODbL; **max. 1 req/s, eigener User-Agent, kein Autocomplete, kein Bulk** |

---

## 7. Vorgeschlagener Default-Mix (12 Feeds, ~60/40)

**Deutsch (6):** Tagesschau · Deutschlandfunk · ZEIT · taz (Mainstream, 4) · **junge Welt** · nd (links, 2)
**English (6):** BBC · Guardian · DW English · Al Jazeera English (mainstream, 4) · Jacobin · Democracy Now! (left, 2)
**Plus immer an (separate Kategorie „Faktencheck“):** CORRECTIV, Full Fact.
**Optional-Pool (Toggle aus):** ZDFheute, Spiegel, SZ, FAZ, Standard, SRF, DW de, Freitag, Jacobin DE, ak, KGK, UZ, Telepolis, LCM · NPR · Intercept, Monthly Review, Morning Star, Peoples Dispatch, Common Dreams, Truthout, WSWS, Novara · Mimikama, Snopes · bpb.
Begründung: Mainstream-Kern mit öffentlich-rechtlichen Quellen + je ein liberales/linksalternatives Blatt; links/marxistisch mit den zwei reichweitenstärksten Tageszeitungen (jW, nd) bzw. den bekanntesten EN-Titeln. FAZ als konservativ-liberaler Gegenpol im Optional-Pool empfehlen (oder statt ZEIT, falls mehr Spektrum gewünscht).

## 8. Transparente Kennzeichnung im UI

- Jede Quelle trägt ein **Chip-Label** (DE/EN lokalisiert): `Öffentlich-rechtlich` · `Mainstream liberal` · `Mainstream konservativ` · `Links-alternativ` · `Sozialistisch/Marxistisch` · `Parteimedium` · `Staatlich finanziert (Ausland)` · `Faktencheck`.
- Zusätzlich kleine Badges: `Paywall teilweise`, `Volltext im Feed`.
- Tippen auf Chip → Info-Sheet: Eigentümer/Träger, Selbstverständnis, Finanzierung, Link „Über uns“, ggf. sachlicher Hinweis (z. B. jW: Erwähnung im Verfassungsschutzbericht; WSWS/UZ: Parteiorgan).
- Einstellungen: Liste nach Kategorie gruppiert, **Toggle pro Quelle**, „Eigene RSS-URL hinzufügen“, Regler/Preset „Ausgewogen / Nur Mainstream / Mehr links / Alle“.
- Chat-Antworten mit News nennen immer die Quelle(n) + Label; bei Themen-Zusammenfassungen möglichst ≥2 Quellen unterschiedlicher Kategorien zitieren.
- Keine automatische Rangfolge nach Leaning; chronologisch oder nach Relevanz.

## 9. Urheberrecht / Technik

- Anzeige: **Headline + Teaser (aus Feed) + Link + Quelle/Label**. Keine Volltexte speichern oder anzeigen.
- Volltext nur **on demand** abrufen (User tippt „zusammenfassen“), lokal zusammenfassen, Zusammenfassung kurz halten, Original-Link prominent; Paywall-Inhalte nicht umgehen (bei Paywall nur Teaser zusammenfassen).
- Auch „Voll“-Feeds (KGK, LCM, Jacobin, Intercept, MR, Peoples Dispatch, Novara) nur als Teaser darstellen – Lizenz ist i. d. R. nicht geklärt; Ausnahme ggf. explizite CC-Lizenz (Democracy Now!: BY-NC-ND → nur unverändert + Namensnennung, Zusammenfassung = Bearbeitung → trotzdem nur Link + eigene kurze Inhaltsangabe).
- Abrufrate: Feeds max. alle 15–30 min, Tagesschau-API ≤60/h, `If-Modified-Since`/ETag nutzen; eigenen User-Agent setzen (`AppName/1.0 (+kontakt)`).
- Feed-Health-Check in der App: bei 0 Items / HTML statt XML Quelle als „gestört“ markieren (betrifft heute: marx21, MR Online).
- Nicht verfügbar ohne Lizenz: Reuters, AP, dpa, dpa-Faktencheck (kein Feed).
