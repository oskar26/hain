package app.hain.tools.router

import app.hain.common.api.WebSearch
import app.hain.llm.api.ToolCall
import app.hain.llm.api.ToolSchema
import app.hain.tools.api.Tool
import app.hain.tools.api.ToolContext
import app.hain.tools.api.ToolResult
import app.hain.tools.api.ToolRouter
import app.hain.tools.internal.calc.CalculatorTool
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.ZonedDateTime

/** 3b-2: the setting `web_search` (on / ask first / off) decides whether Pim may search the web. */
class WebSearchSwitchTest {
    private class FakeSearch : Tool {
        var runs = 0
        override val online = true
        override val schema = ToolSchema("web_search", "Search.", JsonObject(emptyMap()))

        override suspend fun run(
            args: Map<String, Any?>,
            ctx: ToolContext,
        ): ToolResult {
            runs++
            return ToolResult("""{"hits":[]}""")
        }
    }

    private val search = FakeSearch()
    private val router = ToolRouter(listOf(CalculatorTool(), search))
    private val call = ToolCall("1", "web_search", mapOf("query" to "Köln"))

    private fun ctx(
        mode: WebSearch,
        approved: Boolean = false,
    ) = ToolContext("de", ZonedDateTime.now(), offline = false, webSearch = mode, searchApproved = approved)

    @Test
    fun offHidesTheSearchAndRefusesIt() =
        runTest {
            val off = router.schemas(offline = false, webSearch = WebSearch.OFF)
            assertEquals(listOf("calculate"), off.map { it.name })
            assertEquals(2, router.schemas(offline = false, webSearch = WebSearch.ASK).size)
            assertEquals("search_off", router.run(call, ctx(WebSearch.OFF)).error)
            assertEquals(0, search.runs)
        }

    @Test
    fun askRunsOnlyAfterYes() =
        runTest {
            assertEquals("ask_first", router.run(call, ctx(WebSearch.ASK)).error)
            assertEquals(0, search.runs)
            assertNull(router.run(call, ctx(WebSearch.ASK, approved = true)).error)
            assertNull(router.run(call, ctx(WebSearch.ON)).error)
            assertEquals(2, search.runs)
        }
}
