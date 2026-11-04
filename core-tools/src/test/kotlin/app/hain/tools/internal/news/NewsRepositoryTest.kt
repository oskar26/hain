package app.hain.tools.internal.news

import app.hain.common.api.net.GateRequest
import app.hain.common.api.net.GateResponse
import app.hain.common.api.net.NetworkGate
import app.hain.common.api.net.OfflineException
import app.hain.common.api.net.Purpose
import app.hain.tools.search.FakeGate
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.kxml2.io.KXmlParser
import java.io.File
import java.time.Instant

/** T-071: modes, conditional GET, dedup, 7 days, CORRECTIV filter, health, own feeds. */
class NewsRepositoryTest {
    private val standard = NewsJson.standardFeeds(File("src/main/assets/news/standard_feeds.json").readText())
    private var now = Instant.parse("2026-11-02T12:00:00Z").toEpochMilli()

    private fun fixture(id: String): String =
        requireNotNull(javaClass.classLoader?.getResourceAsStream("news/fixtures/$id.xml")) { id }
            .use { it.readBytes().toString(Charsets.UTF_8) }

    /** Answers every standard feed with its saved real response. */
    private fun fixtureGate(extra: (GateRequest) -> GateResponse? = { null }) =
        FakeGate { req ->
            extra(req) ?: standard.firstOrNull { it.url == req.url }?.let { GateResponse(200, fixture(it.id)) }
                ?: GateResponse(404, "")
        }

    private fun repo(
        gate: FakeGate,
        store: NewsStore = MemoryNewsStore(),
    ) = NewsRepository(gate, store, standard, FeedParser { KXmlParser() }, clock = { now })

    @Test fun standardListHasNineteenFeedsInBothLanguagesWithJungeWelt() {
        // T-075: public broadcasters, big papers and left media in German and English
        assertEquals(19, standard.size)
        assertTrue(standard.any { it.id == "jungewelt" && it.lang == "de" })
        assertTrue(standard.count { it.lang == "de" } >= 9)
        assertTrue(standard.count { it.lang == "en" } >= 7)
        assertEquals(standard.size, standard.map { it.id }.toSet().size)
        assertTrue(standard.any { it.lang == "en" })
        assertEquals("Faktencheck", standard.first { it.id == "correctiv" }.category)
        assertTrue(standard.all { it.url.startsWith("https://") && !it.own })
    }

    @Test fun refreshFetchesEveryStandardFeedAsNewsPurpose() =
        runTest {
            val gate = fixtureGate()
            val repo = repo(gate)
            val result = repo.refresh()
            assertEquals(19, result.fetched)
            assertEquals(0, result.failed)
            assertEquals(19, gate.requests.size)
            assertTrue(gate.requests.all { it.purpose == Purpose.NEWS })
            val items = repo.items()
            assertTrue(items.size > 100)
            assertTrue(items.any { it.source == "junge Welt" })
            assertTrue(items.zipWithNext().all { (a, b) -> a.time >= b.time })
        }

    @Test fun correctivKeepsOnlyFactChecks() =
        runTest {
            val repo = repo(fixtureGate())
            repo.refresh()
            val correctiv =
                repo.state.value.items
                    .filter { it.feedId == "correctiv" }
            assertTrue(correctiv.isNotEmpty())
            assertTrue(correctiv.size <= 7)
        }

    @Test fun secondRefreshSendsValidatorsAndKeepsItemsOnNotModified() =
        runTest {
            var calls = 0
            val gate =
                fixtureGate { req ->
                    if (req.url != standard.first().url) return@fixtureGate null
                    calls++
                    if (calls == 1) {
                        GateResponse(200, fixture("tagesschau"), mapOf("etag" to "\"abc\"", "last-modified" to "X"))
                    } else {
                        GateResponse(304, "")
                    }
                }
            val repo = repo(gate)
            repo.refresh()
            val before =
                repo.state.value.items
                    .count { it.feedId == "tagesschau" }
            // within the interval nothing is fetched again
            assertEquals(0, repo.refresh().fetched)
            repo.refresh(force = true)
            val second = gate.requests.last { it.url == standard.first().url }
            assertEquals("\"abc\"", second.headers["If-None-Match"])
            assertEquals("X", second.headers["If-Modified-Since"])
            assertEquals(
                before,
                repo.state.value.items
                    .count { it.feedId == "tagesschau" },
            )
        }

    @Test fun sameHeadlineWithin24HoursOnlyOnce() {
        val a = item("a", "Bundestag beschließt Haushalt!", now - HOUR)
        val b = item("b", "bundestag beschließt   Haushalt", now)
        val c = item("c", "Bundestag beschließt Haushalt", now + 2 * DAY)
        val kept = NewsRepository.dedup(listOf(b, a, c))
        assertEquals(listOf("a", "c"), kept.map { it.feedId })
    }

    @Test fun itemsOlderThanSevenDaysAreDropped() =
        runTest {
            val old = item("tagesschau", "Alt", now - 8 * DAY)
            val fresh = item("tagesschau", "Neu", now - DAY)
            val repo = repo(fixtureGate(), MemoryNewsStore(NewsSnapshot(items = listOf(old, fresh))))
            repo.load()
            assertEquals(listOf("Neu"), repo.items().map { it.title })
        }

    @Test fun ownFeedTurnsBrokenAfterThreeAnswersWithoutFeed() =
        runTest {
            var html = false
            val ownUrl = "https://blog.example.org/feed"
            val gate =
                fixtureGate { req ->
                    when {
                        req.url != ownUrl -> null
                        html -> GateResponse(200, "<html><body>Not here</body></html>")
                        else -> GateResponse(200, fixture("jacobin"))
                    }
                }
            val repo = repo(gate)
            val added = repo.addFeed(ownUrl) as AddFeedResult.Added
            repo.setMode(NewsMode.OWN_ONLY)
            html = true
            repeat(3) { repo.refresh(force = true) }
            assertEquals(setOf(added.feed.id), repo.brokenOwnFeeds())
        }

    @Test fun modesPickTheirFeeds() =
        runTest {
            val ownUrl = "https://blog.example.org/feed"
            val gate = fixtureGate { req -> if (req.url == ownUrl) GateResponse(200, fixture("fullfact")) else null }
            val repo = repo(gate)
            assertTrue(repo.items().isEmpty())
            repo.setMode(NewsMode.OWN_ONLY)
            assertTrue(repo.feeds(NewsMode.OWN_ONLY).isEmpty())
            val added = repo.addFeed("blog.example.org/feed", "Mein Blog") as AddFeedResult.Added
            assertEquals("Mein Blog", added.feed.name)
            assertTrue(repo.items().isNotEmpty())
            assertTrue(repo.items().all { it.feedId == added.feed.id })
            repo.setMode(NewsMode.STANDARD_PLUS_OWN)
            assertEquals(20, repo.feeds(NewsMode.STANDARD_PLUS_OWN).size)
            repo.setMode(NewsMode.STANDARD)
            assertTrue(repo.items().none { it.feedId == added.feed.id })
        }

    @Test fun addFeedChecksTheAddress() =
        runTest {
            val gate = fixtureGate { req -> if (req.url.contains("nofeed")) GateResponse(200, "<html/>") else null }
            val repo = repo(gate)
            assertEquals(AddFeedResult.NotAFeed, repo.addFeed("not a url"))
            assertEquals(AddFeedResult.NotAFeed, repo.addFeed("https://nofeed.example.org"))
            assertEquals(AddFeedResult.Duplicate, repo.addFeed("https://www.tagesschau.de/index~rss2.xml"))
        }

    @Test fun renameAndDeleteOwnFeed() =
        runTest {
            val ownUrl = "https://blog.example.org/feed"
            val gate = fixtureGate { req -> if (req.url == ownUrl) GateResponse(200, fixture("fullfact")) else null }
            val repo = repo(gate)
            val feed = (repo.addFeed(ownUrl) as AddFeedResult.Added).feed
            repo.renameFeed(feed.id, "  Fakten  ")
            assertEquals(
                "Fakten",
                repo.state.value.ownFeeds
                    .single()
                    .name,
            )
            repo.deleteFeed(feed.id)
            assertTrue(
                repo.state.value.ownFeeds
                    .isEmpty(),
            )
            assertTrue(
                repo.state.value.items
                    .none { it.feedId == feed.id },
            )
        }

    @Test fun offlineKeepsTheCache() =
        runTest {
            val cached = item("tagesschau", "Gespeichert", now - HOUR)
            val gate = FakeGate { throw OfflineException() }
            val repo = repo(gate, MemoryNewsStore(NewsSnapshot(items = listOf(cached))))
            val result = repo.refresh()
            assertTrue(result.offline)
            assertEquals(listOf("Gespeichert"), repo.items().map { it.title })
        }

    @Test fun searchFindsMatchingHeadlinesAndFiltersLanguage() =
        runTest {
            val items =
                listOf(
                    item("tagesschau", "Streik bei der Bahn", now - HOUR, lang = "de"),
                    item("bbc", "Rail strike in Britain", now - HOUR, lang = "en"),
                    item("dlf", "Wetter am Wochenende", now - HOUR, lang = "de"),
                )
            val repo = repo(fixtureGate(), MemoryNewsStore(NewsSnapshot(items = items, lastRefresh = now)))
            assertEquals(listOf("Streik bei der Bahn"), repo.search("Bahn Streik").map { it.title })
            assertEquals(listOf("Rail strike in Britain"), repo.search("strike", lang = "en").map { it.title })
            assertEquals(3, repo.search(null).size)
            assertFalse(repo.stale())
        }

    @Test fun slowFeedDoesNotHoldBackTheOthers() =
        runTest {
            val slowUrl = standard.first { it.id == "bbc" }.url
            val inner = fixtureGate()
            val gate =
                object : NetworkGate by inner {
                    override suspend fun fetch(request: GateRequest): GateResponse {
                        if (request.url == slowUrl) delay(10 * NewsRepository.FEED_TIMEOUT_MS)
                        return inner.fetch(request)
                    }
                }
            val repo = NewsRepository(gate, MemoryNewsStore(), standard, FeedParser { KXmlParser() }, clock = { now })
            val result = repo.refresh()
            assertEquals(1, result.failed)
            val sources = repo.items().map { it.feedId }.toSet()
            assertTrue("bbc" !in sources)
            assertTrue(sources.size >= 15)
        }

    @Test fun brokenFeedsDoNotStopTheFirstScreen() =
        runTest {
            // T-075: even with four feeds failing, the first open has plenty of headlines of many sources
            now = Instant.parse("2026-11-05T06:00:00Z").toEpochMilli() // after the newest saved feed
            val broken = standard.map { it.url }.take(4).toSet()
            val repo = repo(fixtureGate { req -> if (req.url in broken) GateResponse(500, "") else null })
            val result = repo.refresh()
            assertEquals(4, result.failed)
            val items = repo.items()
            assertTrue(items.size >= 20)
            assertTrue(items.map { it.source }.toSet().size >= 12)
            assertTrue(items.any { it.source == "junge Welt" })
        }

    @Test fun mixedListNeverShowsTheSameSourceThreeTimesInARowWhileOthersWait() {
        val items =
            (1..10).map { item("jungewelt", "jW $it", now - it * 1000L) } +
                (1..5).map { item("tagesschau", "ts $it", now - HOUR - it * 1000L) }
        val mixed = NewsRepository.mixed(items.sortedByDescending { it.time })
        assertEquals(items.size, mixed.size)
        assertEquals(items.toSet(), mixed.toSet())
        val firstTen = mixed.take(10).map { it.feedId }
        assertTrue(firstTen.windowed(3).none { w -> w.toSet().size == 1 })
        assertEquals("jungewelt", mixed.first().feedId)
    }

    @Test fun articleDatedTomorrowIsShownAsNow() =
        runTest {
            val future = "Thu, 05 Nov 2099 00:00:00 +0100"
            val rss =
                "<rss version=\"2.0\"><channel><title>jW</title><item><title>Morgen in der Zeitung</title>" +
                    "<link>https://www.jungewelt.de/artikel/1.html</link><pubDate>$future</pubDate>" +
                    "</item></channel></rss>"
            val jw = standard.first { it.id == "jungewelt" }.url
            val repo = repo(fixtureGate { req -> if (req.url == jw) GateResponse(200, rss) else null })
            repo.refresh()
            val item = repo.items().first { it.title == "Morgen in der Zeitung" }
            assertTrue(item.time <= now)
        }

    @Test fun onlyArticlesFoundCutOffCarryThePaywallMark() =
        runTest {
            val a = item("jungewelt", "Frei", now - HOUR)
            val b = item("jungewelt", "Gesperrt", now - HOUR)
            val repo = repo(fixtureGate(), MemoryNewsStore(NewsSnapshot(items = listOf(a, b), lastRefresh = now)))
            repo.load()
            assertTrue(repo.items().none { it.paywall })
            repo.markRead(b.link!!, locked = true, image = "https://www.jungewelt.de/img/1.jpg")
            val shown = repo.items().associateBy { it.title }
            assertTrue(shown.getValue("Gesperrt").paywall)
            assertFalse(shown.getValue("Frei").paywall)
            assertEquals("https://www.jungewelt.de/img/1.jpg", shown.getValue("Gesperrt").imageUrl)
            repo.markRead(b.link!!, locked = false, image = null)
            assertTrue(repo.items().none { it.paywall })
        }

    @Test fun pagePictureIsLookedUpOnceThroughTheGate() =
        runTest {
            val noPicture = item("dw_en", "Ohne Bild", now - HOUR)
            val page =
                "<html><head><meta property=\"og:image\" content=\"https://static.dw.com/image/1_302.jpg\">" +
                    "</head><body></body></html>"
            val gate = fixtureGate { req -> if (req.url == noPicture.link) GateResponse(200, page) else null }
            val repo = repo(gate, MemoryNewsStore(NewsSnapshot(items = listOf(noPicture), lastRefresh = now)))
            repo.load()
            repo.lookUpPageImage(noPicture.link!!)
            repo.lookUpPageImage(noPicture.link!!)
            assertEquals("https://static.dw.com/image/1_302.jpg", repo.items().single().imageUrl)
            val asked = gate.requests.filter { it.url == noPicture.link }
            assertEquals(1, asked.size)
            assertEquals(Purpose.IMAGES, asked.single().purpose)
        }

    @Test fun snapshotSurvivesJson() {
        val s =
            NewsSnapshot(
                mode = NewsMode.STANDARD_PLUS_OWN,
                ownFeeds = listOf(NewsFeed("own-1", "Blog", "https://b.example.org/rss", own = true)),
                feedStates = mapOf("own-1" to FeedState("e", "m", 2, 5)),
                items = listOf(item("own-1", "Titel", now)),
                lastRefresh = 9,
                pageImages = mapOf("https://example.org/Titel" to "https://example.org/b.jpg"),
                locked = setOf("https://example.org/Titel"),
            )
        assertEquals(s, NewsJson.decode(NewsJson.encode(s)))
    }

    private fun item(
        feedId: String,
        title: String,
        time: Long,
        lang: String? = null,
    ) = NewsItem(feedId, feedId, title, "https://example.org/$title", "$feedId/$title", time, time, "", lang = lang)

    private companion object {
        const val HOUR = 60 * 60 * 1000L
        const val DAY = 24 * HOUR
    }
}
