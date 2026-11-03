package app.hain.tools.research

import app.hain.common.api.net.GateResponse
import app.hain.llm.api.ToolCall
import app.hain.tools.api.HainTools
import app.hain.tools.api.ToolContext
import app.hain.tools.api.ToolRouter
import app.hain.tools.internal.search.SearchHit
import app.hain.tools.internal.search.SearchTool
import app.hain.tools.internal.wiki.WikipediaTool
import app.hain.tools.search.FakeGate
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZonedDateTime

/** 4c: Wikipedia ↔ web search fall back on each other in the tool layer, not in the brain. */
class FallbackTest {
    private val ctx = ToolContext("de", ZonedDateTime.now(), offline = false)

    private val ddg =
        """<div class="result"><a rel="nofollow" class="result__a" """ +
            """href="//duckduckgo.com/l/?uddg=https%3A%2F%2Fwww.a.de%2F1&amp;rut=1">Seite A</a>""" +
            """<a class="result__snippet" href="x">Text A</a></div>""" +
            """<div class="result"><a rel="nofollow" class="result__a" """ +
            """href="//duckduckgo.com/l/?uddg=https%3A%2F%2Fblog.a.de%2F2&amp;rut=2">Seite A2</a>""" +
            """<a class="result__snippet" href="x">Text A2</a></div>""" +
            """<div class="result"><a rel="nofollow" class="result__a" """ +
            """href="//duckduckgo.com/l/?uddg=https%3A%2F%2Fb.org%2F3&amp;rut=3">Seite B</a>""" +
            """<a class="result__snippet" href="x">Text B</a></div>"""

    private fun router(answer: (String) -> GateResponse): Pair<ToolRouter, FakeGate> {
        val gate = FakeGate { answer(it.url) }
        val tools = listOf(WikipediaTool(gate), SearchTool(gate, emptyList()))
        return ToolRouter(tools, fallbacks = HainTools.FALLBACKS) to gate
    }

    @Test
    fun emptyWikipediaBecomesAWebSearch() =
        runTest {
            val (router, gate) =
                router { url ->
                    when {
                        url.contains("opensearch") -> GateResponse(200, """["x",[],[],[]]""")
                        url.contains("duckduckgo") -> GateResponse(200, ddg)
                        else -> GateResponse(404, "")
                    }
                }
            val result = router.run(ToolCall("1", "wikipedia", mapOf("topic" to "Hainer Kiezfest")), ctx)
            assertNull(result.error)
            assertEquals(SearchTool.NAME, result.via)
            assertTrue(result.forModel.startsWith("{\"note\":\"Wikipedia had nothing"))
            assertTrue(result.forModel.contains("Seite B"))
            // one hit per site: www.a.de and blog.a.de are the same site
            assertEquals(listOf("https://www.a.de/1", "https://b.org/3"), result.hits.map { it.url })
            // de and en Wikipedia were both asked before the web
            assertEquals(2, gate.requests.count { it.url.contains("opensearch") })
        }

    @Test
    fun disambiguationPageAlsoFallsBack() =
        runTest {
            val (router, _) =
                router { url ->
                    when {
                        url.contains("opensearch") -> GateResponse(200, """["Merkur",["Merkur"],[""],[""]]""")
                        url.contains("summary") ->
                            GateResponse(
                                200,
                                """{"type":"disambiguation","title":"Merkur","extract":"Merkur steht für:",""" +
                                    """"content_urls":{"desktop":{"page":"https://de.wikipedia.org/wiki/Merkur"}}}""",
                            )
                        url.contains("duckduckgo") -> GateResponse(200, ddg)
                        else -> GateResponse(404, "")
                    }
                }
            val result = router.run(ToolCall("1", "wikipedia", mapOf("topic" to "Merkur")), ctx)
            assertEquals(SearchTool.NAME, result.via)
        }

    @Test
    fun whenTheFallbackHasNothingTooTheFirstErrorStays() =
        runTest {
            val (router, _) = router { GateResponse(503, "") }
            val result = router.run(ToolCall("1", "wikipedia", mapOf("topic" to "nichts")), ctx)
            assertEquals("network", result.error)
            assertNull(result.via)
        }

    @Test
    fun emptyWebSearchAsksWikipediaInGermanAndEnglish() =
        runTest {
            val wiki = """{"query":{"search":[{"title":"Ada Lovelace","snippet":"Mathematikerin"}]}}"""
            val (router, gate) =
                router { url ->
                    when {
                        url.startsWith("https://de.wikipedia.org/w/api.php?action=query") ->
                            GateResponse(200, """{"query":{"search":[]}}""")
                        url.startsWith("https://en.wikipedia.org/w/api.php?action=query") -> GateResponse(200, wiki)
                        else -> GateResponse(503, "")
                    }
                }
            val result = router.run(ToolCall("1", "web_search", mapOf("query" to "Ada Lovelace")), ctx)
            assertNull(result.error)
            assertEquals("wikipedia", result.via)
            assertEquals("https://en.wikipedia.org/wiki/Ada_Lovelace", result.readUrls.single())
            assertTrue(result.forModel.contains("The web search had nothing"))
            assertTrue(gate.requests.any { it.url.startsWith("https://de.wikipedia.org") })
        }

    @Test
    fun sitesAreComparedWithoutSubdomains() {
        assertEquals("wikipedia.org", SearchTool.siteOf("https://de.m.wikipedia.org/wiki/X"))
        assertEquals("bbc.co.uk", SearchTool.siteOf("https://www.bbc.co.uk/news"))
        val hits =
            listOf(
                SearchHit("a", "https://www.a.de/1", ""),
                SearchHit("a2", "https://a.de/2", ""),
                SearchHit("b", "https://b.de/1", ""),
            )
        assertEquals(listOf("a", "b"), SearchTool.onePerSite(hits).map { it.title })
    }
}
