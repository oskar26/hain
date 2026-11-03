package app.hain.tools.search

import app.hain.common.api.net.GateResponse
import app.hain.llm.api.ToolCall
import app.hain.tools.api.ToolContext
import app.hain.tools.api.ToolRouter
import app.hain.tools.internal.search.SearchTool
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZonedDateTime

/** 4b: DuckDuckGo rests 10 minutes after its bot check; the user's own SearXNG is asked first. */
class SearchBackoffTest {
    private val ctx = ToolContext("de", ZonedDateTime.now(), offline = false)
    private val wiki = """{"query":{"search":[{"title":"Photosynthese","snippet":"Pflanzen machen Zucker"}]}}"""
    private val bot = """<html><form id="challenge-form" action="//duckduckgo.com/anomaly.js"></form></html>"""

    @Test
    fun botCheckPausesDuckDuckGoForTenMinutes() =
        runTest {
            var time = 1_000_000L
            val gate =
                FakeGate { r ->
                    when {
                        "duckduckgo" in r.url -> GateResponse(202, bot)
                        "wikipedia" in r.url -> GateResponse(200, wiki)
                        else -> GateResponse(500, "")
                    }
                }
            val tool = SearchTool(gate, searxInstances = emptyList(), clock = { time })
            val router = ToolRouter(listOf(tool))
            val call = ToolCall("1", "web_search", mapOf("query" to "Photosynthese"))

            assertEquals(null, router.run(call, ctx).error)
            assertEquals(2, gate.requests.count { "duckduckgo" in it.url })
            assertTrue(tool.isPaused(SearchTool.DDG))
            assertTrue(tool.isPaused(SearchTool.DDG_LITE))

            // within 10 minutes: straight to Wikipedia, DuckDuckGo is not asked
            gate.requests.clear()
            time += 9 * 60_000
            assertEquals(null, router.run(call, ctx).error)
            assertEquals(0, gate.requests.count { "duckduckgo" in it.url })

            // after 10 minutes DuckDuckGo gets another chance
            gate.requests.clear()
            time += 2 * 60_000
            assertFalse(tool.isPaused(SearchTool.DDG))
            router.run(call, ctx)
            assertEquals(2, gate.requests.count { "duckduckgo" in it.url })
        }

    @Test
    fun ownSearxIsAskedFirst() =
        runTest {
            val searx = """{"results":[{"title":"Photosynthese","url":"https://example.org/p","content":"Licht"}]}"""
            val gate =
                FakeGate { r ->
                    if (r.url.startsWith("https://searx.mine.de/")) GateResponse(200, searx) else GateResponse(500, "")
                }
            val tool = SearchTool(gate, searxInstances = listOf("https://searx.be"))
            val call = ToolCall("1", "web_search", mapOf("query" to "Photosynthese"))
            val r = ToolRouter(listOf(tool)).run(call, ctx.copy(searxUrl = "https://searx.mine.de"))
            assertEquals(null, r.error)
            assertTrue(
                gate.requests
                    .first()
                    .url
                    .startsWith("https://searx.mine.de/search?q=Photosynthese"),
            )
            assertEquals(1, gate.requests.size)
        }
}
