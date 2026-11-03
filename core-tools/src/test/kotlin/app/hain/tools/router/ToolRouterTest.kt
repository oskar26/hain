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

    @Test
    fun errorsBecomeShortCodes() =
        runTest {
            val router = ToolRouter(listOf(CalculatorTool()))
            assertEquals("unknown_tool", router.run(call("nope"), ctx).error)
            assertEquals("bad_arguments", router.run(call("calculate"), ctx).error)
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
    fun offlineHidesOnlineTools() {
        val router = ToolRouter(listOf(CalculatorTool(), Fake(true) { ToolResult("{}") }))
        assertEquals(listOf("calculate", "fake"), router.schemas(offline = false).map { it.name })
        assertEquals(listOf("calculate"), router.schemas(offline = true).map { it.name })
    }
}
