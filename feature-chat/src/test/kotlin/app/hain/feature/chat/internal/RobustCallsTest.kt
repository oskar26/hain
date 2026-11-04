package app.hain.feature.chat.internal

import app.hain.llm.api.ChatMessage
import app.hain.llm.api.Role
import app.hain.llm.api.ToolSchema
import app.hain.llm.internal.AnswerMode
import app.hain.tools.api.Tool
import app.hain.tools.api.ToolContext
import app.hain.tools.api.ToolResult
import app.hain.tools.api.ToolRouter
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZonedDateTime

/** 4d (T-183): wrong names, duplicates and repeated calls in the chat engine. */
class RobustCallsTest {
    private val ctx = ToolContext("de", ZonedDateTime.now(), offline = false)

    private class Weather : Tool {
        val places = mutableListOf<Any?>()
        override val online = true
        override val schema = ToolSchema("weather", "weather.", JsonObject(emptyMap()))

        override suspend fun run(
            args: Map<String, Any?>,
            ctx: ToolContext,
        ): ToolResult {
            places += args["place"]
            return ToolResult("{\"place\":\"${args["place"]}\",\"t\":9}", args["place"].toString())
        }
    }

    private fun call(
        name: String,
        param: String,
        value: String,
    ) = "<tool_call>\n<function=$name>\n<parameter=$param>\n$value\n</parameter>\n</function>\n</tool_call>"

    @Test
    fun aliasesAndDuplicatesRunOnce() =
        runTest {
            val weather = Weather()
            val llm =
                FakeLlm(
                    listOf(call("weather", "place", "Köln") + "\n\n" + call("get_weather", "city", "köln")),
                    listOf("9 Grad in Köln [1]."),
                )
            val engine = ChatEngine(llm, { "SYS" }, tools = ToolRouter(listOf(weather)), toolContext = { ctx })
            val last =
                engine.respond(listOf(ChatMessage(Role.USER, "Wetter Köln?")), AnswerMode.AUTO).toList().last()
                    as TurnUpdate.Finished
            assertEquals(listOf<Any?>("Köln"), weather.places)
            assertEquals(1, last.steps.size)
            assertEquals("weather", last.trace.single().name)
        }

    @Test
    fun repeatedCallGetsAHintInsteadOfRunningAgain() =
        runTest {
            val weather = Weather()
            val llm =
                FakeLlm(
                    listOf(call("weather", "place", "Bonn")),
                    listOf(call("weather", "place", "Bonn")),
                    listOf("9 Grad in Bonn [1]."),
                )
            val engine = ChatEngine(llm, { "SYS" }, tools = ToolRouter(listOf(weather)), toolContext = { ctx })
            val last =
                engine.respond(listOf(ChatMessage(Role.USER, "Wetter Bonn?")), AnswerMode.AUTO).toList().last()
                    as TurnUpdate.Finished
            assertEquals(1, weather.places.size)
            assertEquals("9 Grad in Bonn [1].", last.output.text)
            assertTrue(llm.prompts[2].contains("\"error\":\"repeat\""))
        }

    @Test
    fun brokenCallFromTheBrainStillRuns() =
        runTest {
            val weather = Weather()
            val llm =
                FakeLlm(
                    listOf("<tool_call>\n<function=weather>\n<parameter=place>\nKöln\n</function>"),
                    listOf("9 Grad [1]."),
                )
            val engine = ChatEngine(llm, { "SYS" }, tools = ToolRouter(listOf(weather)), toolContext = { ctx })
            engine.respond(listOf(ChatMessage(Role.USER, "Wetter Köln?")), AnswerMode.AUTO).toList()
            assertEquals(listOf<Any?>("Köln"), weather.places)
        }
}
