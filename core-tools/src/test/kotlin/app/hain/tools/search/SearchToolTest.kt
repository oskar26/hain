package app.hain.tools.search

import app.hain.common.api.net.GateResponse
import app.hain.common.api.net.Purpose
import app.hain.llm.api.ToolCall
import app.hain.tools.api.ToolContext
import app.hain.tools.api.ToolRouter
import app.hain.tools.internal.net.HttpNetworkGate
import app.hain.tools.internal.search.HtmlText
import app.hain.tools.internal.search.PageReader
import app.hain.tools.internal.search.ReadPageTool
import app.hain.tools.internal.search.SearchParsers
import app.hain.tools.internal.search.SearchTool
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZonedDateTime

class SearchToolTest {
    private val ctx = ToolContext("de", ZonedDateTime.now(), offline = false)

    private val ddg =
        """<div class="result"><h2 class="result__title"><a rel="nofollow" class="result__a" """ +
            """href="//duckduckgo.com/l/?uddg=https%3A%2F%2Fde.wikipedia.org%2Fwiki%2FPhotosynthese&amp;rut=1">""" +
            """<b>Photosynthese</b> – Wikipedia</a></h2>""" +
            """<a class="result__snippet" href="x">Die <b>Photosynthese</b> ist ein Prozess &amp; mehr</a></div>""" +
            """<div class="result"><a rel="nofollow" class="result__a" """ +
            """href="https://duckduckgo.com/y.js?ad=1">Ad</a>""" +
            """<a class="result__snippet" href="y">Werbung</a></div>""" +
            """<div class="result"><a rel="nofollow" class="result__a" """ +
            """href="//duckduckgo.com/l/?uddg=https%3A%2F%2Fwww.planet-wissen.de%2Fphoto.html&amp;rut=2">Planet</a>""" +
            """<a class="result__snippet" href="z">Pflanzen machen Zucker</a></div>"""

    private val page =
        """<html><head><title>Photosynthese einfach erklärt</title><script>var x = 1;</script></head>""" +
            """<body><nav><a href="/">Start</a> Menü Menü Menü Menü Menü Menü Menü Menü Menü Menü</nav>""" +
            """<h1>Photosynthese</h1><p>Bei der Photosynthese machen Pflanzen aus Licht, Wasser und """ +
            """Kohlendioxid Zucker und Sauerstoff.</p><p>Das passiert in den Chloroplasten der Blätter, """ +
            """genauer im grünen Farbstoff Chlorophyll &ndash; ganz ohne Lärm.</p>""" +
            """<footer>Impressum Datenschutz Kontakt Impressum Datenschutz Kontakt</footer></body></html>"""

    @Test
    fun duckDuckGoHitsWithoutAds() {
        val hits = SearchParsers.duckDuckGo(ddg)
        assertEquals(2, hits.size)
        assertEquals("Photosynthese – Wikipedia", hits[0].title)
        assertEquals("https://de.wikipedia.org/wiki/Photosynthese", hits[0].url)
        assertEquals("Die Photosynthese ist ein Prozess & mehr", hits[0].snippet)
        assertEquals("https://www.planet-wissen.de/photo.html", hits[1].url)
    }

    @Test
    fun duckDuckGoLite() {
        val lite =
            """<td><a rel="nofollow" href="//duckduckgo.com/l/?uddg=https%3A%2F%2Fwww.mein%2Deigenheim.de%2Fa.html""" +
                """&amp;rut=7" class='result-link'>Gas mit <b>Wärmepumpe</b></a></td></tr><tr><td>&nbsp;</td>""" +
                """<td class='result-snippet'>Wann lohnt sich eine Hybridheizung?</td>"""
        val hit = SearchParsers.duckDuckGoLite(lite).single()
        assertEquals("https://www.mein-eigenheim.de/a.html", hit.url)
        assertEquals("Gas mit Wärmepumpe", hit.title)
        assertEquals("Wann lohnt sich eine Hybridheizung?", hit.snippet)
    }

    @Test
    fun searxngAndWikipediaParsers() {
        val searx = """{"results":[{"title":"A","url":"https://a.de/","content":"<b>x</b>"},{"title":"B"}]}"""
        assertEquals(listOf("https://a.de/"), SearchParsers.searxng(searx).map { it.url })
        assertEquals("x", SearchParsers.searxng(searx).single().snippet)
        val wiki =
            """{"query":{"search":[{"title":"Ada Lovelace",""" +
                """"snippet":"britische <span>Mathematikerin</span>"}]}}"""
        val hit = SearchParsers.wikipedia(wiki, "de").single()
        assertEquals("https://de.wikipedia.org/wiki/Ada_Lovelace", hit.url)
        assertEquals("britische Mathematikerin", hit.snippet)
        assertTrue(SearchParsers.searxng("kaputt").isEmpty())
    }

    @Test
    fun htmlTextKeepsContentAndCuts() {
        val text = HtmlText.extract(page, 2000)
        assertEquals("Photosynthese einfach erklärt", text.title)
        assertTrue(text.text.startsWith("Bei der Photosynthese machen Pflanzen"))
        assertTrue(text.text.contains("Chlorophyll – ganz"))
        assertFalse(text.text.contains("var x"))
        assertFalse(text.text.contains("Impressum"))
        assertFalse(text.text.contains("Menü"))
        assertEquals("Satz eins ist da.", HtmlText.cut("Satz eins ist da. Satz zwei ist viel länger", 22))
        assertEquals("ä & ü", HtmlText.decode("&#228; &amp; &uuml;"))
    }

    @Test
    fun searchFallsBackFromSearxToDuckDuckGoAndPicksPagesToRead() =
        runTest {
            val asked = mutableListOf<String>()
            val gate =
                HttpNetworkGate(
                    transport = { req ->
                        asked += req.url
                        when {
                            req.url.startsWith("https://searx.test") -> GateResponse(429, "")
                            req.url.contains("duckduckgo") -> GateResponse(200, ddg)
                            else -> GateResponse(404, "")
                        }
                    },
                )
            val tool = SearchTool(gate, listOf("https://searx.test"))
            val result = tool.run(mapOf("query" to "Photosynthese"), ctx.copy(pagesToRead = 1))
            assertNull(result.error)
            assertEquals("Photosynthese", result.label)
            assertEquals(listOf("https://de.wikipedia.org/wiki/Photosynthese"), result.readUrls)
            assertTrue(result.forModel.contains("\"n\":1"))
            assertEquals(2, asked.size)
            assertTrue(gate.log.value.all { it.purpose == Purpose.SEARCH })
        }

    @Test
    fun searchEndsInWikipediaAndSaysWhenNothingFound() =
        runTest {
            val gate = HttpNetworkGate(transport = { GateResponse(503, "") })
            val result = SearchTool(gate, emptyList()).run(mapOf("query" to "xyz"), ctx)
            assertEquals("not_found", result.error)
            assertEquals(3, gate.log.value.size) // DuckDuckGo, DuckDuckGo Lite, Wikipedia
        }

    @Test
    fun routerReadsPagesInParallelAndSkipsBrokenOnes() =
        runTest {
            val gate =
                FakeGate { req ->
                    if (req.url.contains("good")) GateResponse(200, page) else GateResponse(500, "")
                }
            val reader = PageReader(gate)
            val router = ToolRouter(listOf(ReadPageTool(reader)), pages = reader)
            val read = router.readPages(listOf("https://good.de/a", "https://bad.de/b", "https://good.de/c"), ctx)!!
            assertEquals("2", read.label)
            assertEquals(listOf("https://good.de/a", "https://good.de/c"), read.sources.map { it.url })
            assertTrue(read.forModel.startsWith("{\"pages\":[{\"n\":1"))
            assertTrue(gate.log.value.all { it.purpose == Purpose.PAGE_READ })
            assertNull(router.readPages(listOf("https://bad.de/x"), ctx))
            assertNull(router.readPages(listOf("https://good.de/a"), ctx.copy(offline = true)))
        }

    @Test
    fun readPageToolChecksTheAddress() =
        runTest {
            val gate = FakeGate { GateResponse(200, page) }
            val router = ToolRouter(listOf(ReadPageTool(PageReader(gate))))
            val ok = ReadPageTool(PageReader(gate)).run(mapOf("url" to "https://www.good.de/a"), ctx)
            assertNull(ok.error)
            assertEquals("good.de", ok.label)
            assertEquals("Photosynthese einfach erklärt", ok.sources.single().title)
            val bad = router.run(ToolCall("c", "read_page", mapOf("url" to "file:///etc/passwd")), ctx)
            assertEquals("bad_arguments", bad.error)
        }
}
