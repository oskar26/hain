package app.hain.tools.internal.news

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.kxml2.io.KXmlParser
import java.time.Instant

/** One real response per standard feed (NEWS.md §2), saved 2026-11-01 under resources/news/fixtures. */
class FeedParserTest {
    private val parser = FeedParser { KXmlParser() }

    private data class Expect(
        val format: FeedFormat,
        val items: Int,
        val withImages: Int,
    )

    // T-075: pictures also come from the item's own HTML (tagesschau, Deutschlandfunk, Democracy Now!)
    private val expected =
        mapOf(
            "tagesschau" to Expect(FeedFormat.RSS2, 63, 58),
            "zdf" to Expect(FeedFormat.RSS2, 180, 179),
            "dlf" to Expect(FeedFormat.RSS2, 38, 38),
            "dw_de" to Expect(FeedFormat.RSS2, 83, 83),
            "spiegel" to Expect(FeedFormat.RSS2, 30, 30),
            "sz" to Expect(FeedFormat.RSS2, 15, 15),
            "zeit" to Expect(FeedFormat.RSS2, 15, 15),
            "taz" to Expect(FeedFormat.RSS2, 49, 49),
            "jungewelt" to Expect(FeedFormat.RSS2, 36, 34),
            "nd" to Expect(FeedFormat.RSS2, 20, 20),
            "bbc" to Expect(FeedFormat.RSS2, 30, 30),
            "guardian" to Expect(FeedFormat.RSS2, 45, 45),
            "dw_en" to Expect(FeedFormat.RDF, 122, 0),
            "npr" to Expect(FeedFormat.RSS2, 10, 10),
            "aje" to Expect(FeedFormat.RSS2, 25, 0),
            "jacobin" to Expect(FeedFormat.ATOM, 20, 20),
            "democracynow" to Expect(FeedFormat.RSS2, 33, 23),
            "correctiv" to Expect(FeedFormat.RSS2, 100, 0),
            "fullfact" to Expect(FeedFormat.RSS2, 10, 0),
        )

    private fun parse(id: String): ParsedFeed {
        val stream = requireNotNull(javaClass.classLoader?.getResourceAsStream("news/fixtures/$id.xml")) { id }
        val result = stream.use { parser.parse(it) }
        assertTrue("$id: $result", result is FeedParseResult.Success)
        return (result as FeedParseResult.Success).feed
    }

    @Test fun tagesschau() {
        check("tagesschau")
    }

    @Test fun zdfHeute() {
        check("zdf")
    }

    @Test fun dwGerman() {
        check("dw_de")
    }

    @Test fun spiegel() {
        check("spiegel")
    }

    @Test fun sueddeutsche() {
        check("sz")
    }

    @Test fun npr() {
        check("npr")
    }

    @Test fun everyPictureIsAnHttpsAddress() {
        expected.keys.forEach { id ->
            parse(id).items.mapNotNull { it.imageUrl }.forEach { url ->
                assertTrue("$id $url", url.startsWith("https://"))
            }
        }
    }

    @Test
    fun pictureInsideTheItemHtmlIsFoundButNotATrackingPixel() {
        val rss = """<?xml version="1.0"?><rss version="2.0"><channel><title>T</title>
            <item><title>Mit Bild</title><link>https://a.example/news/1</link>
            <description>&lt;img src="https://a.example/px.gif" width="1" height="1"/&gt;&lt;p&gt;Text&lt;/p&gt;
            &lt;img src="/bilder/gross.jpg?w=640&amp;amp;h=360" alt=""/&gt;</description></item>
            <item><title>Ohne Bild</title><link>https://a.example/news/2</link><description>Nur Text</description></item>
            </channel></rss>"""
        val items = (parser.parse(rss.byteInputStream()) as FeedParseResult.Success).feed.items
        assertEquals("https://a.example/bilder/gross.jpg?w=640&h=360", items[0].imageUrl)
        assertEquals(null, items[1].imageUrl)
    }

    @Test fun deutschlandfunk() {
        check("dlf")
    }

    @Test fun zeit() {
        check("zeit")
    }

    @Test fun taz() {
        check("taz")
    }

    @Test fun jungeWelt() {
        check("jungewelt")
    }

    @Test fun nd() {
        check("nd")
    }

    @Test fun bbc() {
        check("bbc")
    }

    @Test fun guardian() {
        check("guardian")
    }

    @Test fun dwRdf() {
        check("dw_en")
    }

    @Test fun alJazeera() {
        check("aje")
    }

    @Test fun jacobinAtom() {
        check("jacobin")
    }

    @Test fun democracyNow() {
        check("democracynow")
    }

    @Test fun correctiv() {
        val feed = check("correctiv")
        assertTrue(feed.items.any { item -> item.categories.any { "Faktencheck" in it } })
    }

    @Test fun fullFact() {
        check("fullfact")
    }

    private fun check(id: String): ParsedFeed {
        val want = expected.getValue(id)
        val feed = parse(id)
        assertEquals("$id format", want.format, feed.format)
        assertEquals("$id items", want.items, feed.items.size)
        assertEquals("$id images", want.withImages, feed.items.count { it.imageUrl != null })
        // ZDFheute keeps a few evergreen pieces from 2019 in its feed (the 7-day window drops them)
        val earliest = Instant.parse("2015-01-01T00:00:00Z")
        feed.items.forEach { item ->
            assertTrue("$id title", item.title.isNotBlank() && '<' !in item.title)
            assertTrue("$id link ${item.link}", item.link?.startsWith("http") == true)
            assertTrue("$id guid", item.guid.isNotBlank())
            assertNotNull("$id date for ${item.title}", item.published)
            assertTrue("$id date", item.published!!.isAfter(earliest))
            assertTrue("$id summary length", item.summary.length <= FeedParser.MAX_SUMMARY_CHARS)
            assertTrue("$id summary html: ${item.summary}", !HTML_TAG.containsMatchIn(item.summary))
            assertTrue("$id soft hyphen", '\u00AD' !in item.title)
        }
        return feed
    }

    @Test
    fun htmlPageIsNotAFeed() {
        val html = "<!DOCTYPE html><html><head><title>Fehler</title></head><body><p>404</p></body></html>"
        val result = parser.parse(html.byteInputStream())
        assertTrue(result.toString(), result is FeedParseResult.NotAFeed)
    }

    @Test
    fun brokenXmlIsNotAFeed() {
        val result = parser.parse("<rss><channel><item><title>x</title></chan".byteInputStream())
        assertTrue(result is FeedParseResult.NotAFeed)
    }

    @Test
    fun minimalFormatsWithEnclosureAndAtomLinks() {
        val rss = """<?xml version="1.0"?><rss version="2.0"><channel><title>T</title>
            <item><title><![CDATA[Ein <b>Titel</b> &amp; mehr]]></title><link>https://a.example/1</link>
            <pubDate>Sun, 1 Nov 2026 18:30:00 +0100</pubDate>
            <description>&lt;p&gt;Hallo&amp;nbsp;Welt&lt;/p&gt;</description>
            <enclosure url="https://a.example/i.jpg" type="image/jpeg" length="1"/></item>
            <item><title>ohne link und guid</title></item></channel></rss>"""
        val feed = (parser.parse(rss.byteInputStream()) as FeedParseResult.Success).feed
        val item = feed.items.single()
        assertEquals("Ein Titel & mehr", item.title)
        assertEquals("Hallo Welt", item.summary)
        assertEquals("https://a.example/i.jpg", item.imageUrl)
        assertEquals(Instant.parse("2026-11-01T17:30:00Z"), item.published)

        val atom = """<feed xmlns="http://www.w3.org/2005/Atom"><title>A</title><entry><title>E</title>
            <link rel="self" href="https://a.example/self"/><link rel="alternate" href="https://a.example/e"/>
            <id>urn:1</id><updated>2026-11-01T10:00:00+01:00</updated><category term="Klima"/></entry></feed>"""
        val entry = (parser.parse(atom.byteInputStream()) as FeedParseResult.Success).feed.items.single()
        assertEquals("https://a.example/e", entry.link)
        assertEquals("urn:1", entry.guid)
        assertEquals(listOf("Klima"), entry.categories)
        assertEquals(Instant.parse("2026-11-01T09:00:00Z"), entry.published)
    }

    @Test
    fun summaryIsCutAtAWord() {
        val long = "Wort ".repeat(200)
        val summary = TextCleaner.summary(long)
        assertTrue(summary.length <= FeedParser.MAX_SUMMARY_CHARS)
        assertTrue(summary.endsWith("Wort…"))
    }

    @Test
    fun datesInCommonFeedFormats() {
        val expected = Instant.parse("2026-11-01T17:30:00Z")
        listOf(
            "Sun, 01 Nov 2026 17:30:00 GMT",
            "Sun, 01 Nov 2026 18:30:00 +0100",
            "Sun, 1 Nov 2026 17:30 GMT",
            "2026-11-01T18:30:00+01:00",
            "2026-11-01T17:30:00Z",
        ).forEach { assertEquals(it, expected, Dates.parse(it)) }
        assertEquals(null, Dates.parse("gestern"))
    }

    private companion object {
        val HTML_TAG = Regex("""</?(p|a|img|div|br)\b""")
    }
}
