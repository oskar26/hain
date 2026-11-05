package app.hain.tools.router

import app.hain.common.api.net.OfflineException
import app.hain.llm.api.ToolCall
import app.hain.llm.api.ToolSchema
import app.hain.tools.api.Tool
import app.hain.tools.api.ToolContext
import app.hain.tools.api.ToolResult
import app.hain.tools.api.ToolRouter
import app.hain.tools.internal.calc.CalculatorTool
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.time.ZonedDateTime

class ToolRouterTest {
    private val ctx = ToolContext("de", ZonedDateTime.now(), offline = false)

    private class Fake(
        override val online: Boolean,
        val block: suspend () -> ToolResult,
    ) : Tool {
        override val schema = ToolSchema("fake", "Fake.", JsonObject(emptyMap()))

        override suspend fun run(
            args: Map<String, Any?>,
            ctx: ToolContext,
        ) = block()
    }

    private fun call(
        name: String,
        vararg args: Pair<String, Any?>,
    ) = ToolCall("call_0", name, mapOf(*args))

    @Test
    fun runsCalculator() =
        runTest {
            val r = ToolRouter(listOf(CalculatorTool())).run(call("calculate", "expression" to "12*7"), ctx)
            assertNull(r.error)
            assertEquals("""{"expression":"12*7","result":"84"}""", r.forModel)
            assertEquals("12*7 = 84", r.label)
        }

    /** A tool registered under a name the fixer only knows as an alias of another tool. */
    private class News : Tool {
        var ran = false
        override val online = true
        override val schema = ToolSchema("news", "Headlines.", JsonObject(emptyMap()))

        override suspend fun run(
            args: Map<String, Any?>,
            ctx: ToolContext,
        ): ToolResult {
            ran = true
            return ToolResult("[1] tagesschau – Titel", null)
        }
    }

    @Test
    fun newsWithoutTopicRunsInsteadOfAskingForASearchTerm() =
        runTest {
            // T-236: "news" was read as web_search, which needs a query → Pim asked for a search term
            val news = News()
            val r = ToolRouter(listOf(news)).run(call("news"), ctx)
            assertNull(r.error)
            assertTrue(news.ran)
        }

    @Test
    fun errorsBecomeShortCodes() =
        runTest {
            val router = ToolRouter(listOf(CalculatorTool()))
            assertEquals("unknown_tool", router.run(call("nope"), ctx).error)
            assertEquals("missing_argument", router.run(call("calculate"), ctx).error)
            assertEquals("bad_arguments", router.run(call("calculate", "expression" to "1/0"), ctx).error)
            assertTrue(router.run(call("nope"), ctx).forModel.startsWith("""{"error":"unknown_tool","hint":"""))
        }

    @Test
    fun networkOfflineAndTimeout() =
        runTest {
            val net = ToolRouter(listOf(Fake(true) { throw IOException("x") }))
            assertEquals("network", net.run(call("fake"), ctx).error)
            assertEquals("offline", net.run(call("fake"), ctx.copy(offline = true)).error)
            val off = ToolRouter(listOf(Fake(true) { throw OfflineException() }))
            assertEquals("offline", off.run(call("fake"), ctx).error)
            val slow =
                ToolRouter(
                    listOf(
                        Fake(false) {
                            delay(60_000)
                            ToolResult("{}")
                        },
                    ),
                    timeoutMs = 100,
                )
            assertEquals("timeout", slow.run(call("fake"), ctx).error)
        }

    @Test
    fun networkErrorIsTriedOnceMore() =
        runTest {
            var tries = 0
            val flaky =
                Fake(true) {
                    tries++
                    if (tries == 1) throw IOException("reset") else ToolResult("{\"ok\":1}")
                }
            val r = ToolRouter(listOf(flaky), retryDelayMs = 10).run(call("fake"), ctx)
            assertNull(r.error)
            assertEquals(2, tries)
            var offlineTries = 0
            val off =
                Fake(true) {
                    offlineTries++
                    throw OfflineException()
                }
            assertEquals("offline", ToolRouter(listOf(off)).run(call("fake"), ctx).error)
            assertEquals(1, offlineTries)
        }

    @Test
    fun callsAreFixedBeforeTheyRun() =
        runTest {
            val router = ToolRouter(listOf(CalculatorTool()))
            val r = router.run(call("calculator", "expr" to "12*7"), ctx)
            assertNull(r.error)
            assertEquals("12*7 = 84", r.label)
            val missing = router.run(call("calculate", "foo" to "x"), ctx)
            assertTrue(missing.forModel.contains("needs 'expression'"))
        }

    @Test
    fun offlineHidesOnlineTools() {
        val router = ToolRouter(listOf(CalculatorTool(), Fake(true) { ToolResult("{}") }))
        assertEquals(listOf("calculate", "fake"), router.schemas(offline = false).map { it.name })
        assertEquals(listOf("calculate"), router.schemas(offline = true).map { it.name })
    }
}
