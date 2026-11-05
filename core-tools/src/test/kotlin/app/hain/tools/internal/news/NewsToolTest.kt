package app.hain.tools.internal.news

import app.hain.common.api.net.GateResponse
import app.hain.tools.api.HainTools
import app.hain.tools.api.ToolContext
import app.hain.tools.search.FakeGate
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.kxml2.io.KXmlParser
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

/** T-072: the `news` tool — matching, at most 8 lines, the line format, card data and no-hit answer. */
class NewsToolTest {
    private val now = Instant.parse("2026-11-04T16:30:00Z").toEpochMilli()
    private val ctx = ToolContext("de", ZonedDateTime.ofInstant(Instant.ofEpochMilli(now), BERLIN), offline = false)
    private val feeds =
        listOf(
            NewsFeed("tagesschau", "tagesschau", "https://www.tagesschau.de/index~rss2.xml", "de"),
            NewsFeed("jungewelt", "junge Welt", "https://www.jungewelt.de/feeds/newsticker.rss", "de"),
            NewsFeed("bbc", "BBC News", "https://feeds.bbci.co.uk/news/rss.xml", "en"),
        )

    private fun tool(
        items: List<NewsItem>,
        locked: Set<String> = emptySet(),
    ): NewsTool {
        val store = MemoryNewsStore(NewsSnapshot(items = items, lastRefresh = now, locked = locked))
        val gate = FakeGate { GateResponse(500, "") }
        return NewsTool(NewsRepository(gate, store, feeds, FeedParser { KXmlParser() }, clock = { now }))
    }

    private fun item(
        feed: String,
        title: String,
        minutesAgo: Long,
        summary: String = "",
        lang: String = "de",
    ) = NewsItem(
        feedId = feed,
        source = feeds.first { it.id == feed }.name,
        title = title,
        link = "https://example.org/${title.hashCode()}",
        guid = "$feed/$title",
        published = now - minutesAgo * MINUTE,
        fetchedAt = now,
        summary = summary,
        lang = lang,
    )

    @Test fun linesHaveSourceTitleTeaserAndLocalDate() =
        runTest {
            val ford = item("jungewelt", "Streik bei Ford", 30, "Beschäftigte legen die Arbeit nieder.")
            // T-077: only an article the reading view found cut off carries the mark
            val result = tool(listOf(ford), locked = setOf(ford.link!!)).run(mapOf("query" to "Streik"), ctx)
            assertNull(result.error)
            assertEquals(
                "[1] junge Welt – Streik bei Ford – Beschäftigte legen die Arbeit nieder. – 2026-11-04 17:00",
                result.forModel,
            )
            assertEquals("Streik", result.label)
            assertEquals("junge Welt: Streik bei Ford", result.sources.single().title)
            assertTrue(result.news.single().paywall)
            val free = tool(listOf(ford)).run(mapOf("query" to "Streik"), ctx)
            assertFalse(free.news.single().paywall)
        }

    @Test fun atMostEightHeadlinesNewestFirstWithoutQuery() =
        runTest {
            val items = (1..12).map { item("tagesschau", "Meldung $it", it.toLong()) }
            val result = tool(items).run(emptyMap(), ctx)
            val lines = result.forModel.lines()
            assertEquals(8, lines.size)
            assertTrue(lines.first().startsWith("[1] tagesschau – Meldung 1 –"))
            assertEquals(8, result.news.size)
        }

    @Test fun teaserIsCutTo200Chars() =
        runTest {
            val long = "a".repeat(400)
            val result = tool(listOf(item("tagesschau", "Lang", 5, long))).run(emptyMap(), ctx)
            assertTrue(result.forModel.contains("a".repeat(NewsTool.TEASER_CHARS) + " – "))
            assertFalse(result.forModel.contains("a".repeat(NewsTool.TEASER_CHARS + 1)))
        }

    @Test fun languageFilter() =
        runTest {
            val bayern = item("tagesschau", "Wahl in Bayern", 5)
            val bavaria = item("bbc", "Election in Bavaria", 5, lang = "en")
            val result = tool(listOf(bayern, bavaria)).run(mapOf("lang" to "en"), ctx)
            assertEquals(listOf("Election in Bavaria"), result.news.map { it.title })
        }

    @Test fun noHitIsNotFound() =
        runTest {
            val result = tool(listOf(item("tagesschau", "Wetter", 5))).run(mapOf("query" to "Raumfahrt"), ctx)
            assertEquals("not_found", result.error)
        }

    @Test fun routerOffersTheNewsToolOnlyWithARepository() {
        val gate = FakeGate { GateResponse(500, "") }
        assertTrue(HainTools.router(gate).schemas(offline = false).none { it.name == NewsTool.NAME })
        val repo = NewsRepository(gate, MemoryNewsStore(), feeds)
        assertTrue(HainTools.router(gate, repo).schemas(offline = false).any { it.name == NewsTool.NAME })
        assertTrue(HainTools.router(gate, repo).schemas(offline = true).none { it.name == NewsTool.NAME })
        assertTrue(NewsTool.NAME in HainTools.FALLBACKS)
    }

    private companion object {
        const val MINUTE = 60_000L
        val BERLIN: ZoneId = ZoneId.of("Europe/Berlin")
    }
}
