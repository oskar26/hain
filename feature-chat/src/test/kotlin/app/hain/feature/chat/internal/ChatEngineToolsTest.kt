package app.hain.feature.chat.internal

import app.hain.llm.api.ChatMessage
import app.hain.llm.api.Role
import app.hain.llm.api.StopReason
import app.hain.llm.api.ToolSchema
import app.hain.llm.internal.AnswerMode
import app.hain.tools.api.ResearchBudget
import app.hain.tools.api.Tool
import app.hain.tools.api.ToolContext
import app.hain.tools.api.ToolResult
import app.hain.tools.api.ToolRouter
import app.hain.tools.api.ToolSource
import app.hain.tools.internal.calc.CalculatorTool
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZonedDateTime

class ChatEngineToolsTest {
    private val ctx = ToolContext("de", ZonedDateTime.now(), offline = false)

    private class FakeWeather : Tool {
        var calls = 0
        override val online = true
        override val schema = ToolSchema("weather", "Weather.", JsonObject(emptyMap()))

        override suspend fun run(
            args: Map<String, Any?>,
            ctx: ToolContext,
        ): ToolResult {
            calls++
            val source = ToolSource("Open-Meteo", "https://open-meteo.com/")
            return ToolResult("""{"place":"Köln","now":{"t":9.5}}""", "Köln", listOf(source))
        }
    }

    private fun call(
        name: String,
        param: String,
        value: String,
    ) = "<tool_call>\n<function=$name>\n<parameter=$param>\n$value\n</parameter>\n</function>\n</tool_call>"

    private fun user(text: String) = listOf(ChatMessage(Role.USER, text))

    @Test
    fun toolLoopRunsToolAndAnswersFromResult() =
        runTest {
            val weather = FakeWeather()
            val llm = FakeLlm(listOf(call("weather", "place", "Köln")), listOf("In Köln sind es 9,5 °C [1]."))
            val engine = ChatEngine(llm, { "SYS" }, tools = ToolRouter(listOf(weather)), toolContext = { ctx })
            val updates = engine.respond(user("Wetter in Köln?"), AnswerMode.FAST).toList()
            val last = updates.last() as TurnUpdate.Finished
            assertEquals("In Köln sind es 9,5 °C [1].", last.output.text)
            assertEquals(1, weather.calls)
            assertEquals(listOf(ToolStep("weather", "Köln", StepState.DONE, online = true)), last.steps)
            assertEquals("Open-Meteo", last.sources.single().title)
            assertEquals(2, llm.prompts.size)
            assertTrue(llm.prompts[0].contains("<tools>\n{\"name\": \"weather\""))
            assertTrue(llm.prompts[1].contains("<tool_response>\n{\"place\":\"Köln\""))
            // the running chip was shown while the tool worked
            assertTrue(updates.any { it is TurnUpdate.Streaming && it.steps.any { s -> s.state == StepState.RUNNING } })
        }

    @Test
    fun offTopicWeatherIsNotRunAndTheBrainAnswersWithoutIt() =
        runTest {
            // T-263: "ich bin neu nach Köln gezogen was machen" is no weather question
            val weather = FakeWeather()
            val llm = FakeLlm(listOf(call("weather", "place", "Köln")), listOf("Geh an den Rhein und in die Altstadt."))
            val engine = ChatEngine(llm, { "SYS" }, tools = ToolRouter(listOf(weather)), toolContext = { ctx })
            val last =
                engine.respond(user("ich bin neu nach Köln gezogen was machen"), AnswerMode.AUTO).toList().last()
                    as TurnUpdate.Finished
            assertEquals(0, weather.calls)
            assertEquals("Geh an den Rhein und in die Altstadt.", last.output.text)
            assertTrue(last.steps.isEmpty())
            assertTrue(llm.prompts[1].contains("Die Frage braucht weather nicht."))
        }

    @Test
    fun afterTheRoundBudgetTheBrainMustAnswerWithoutTools() =
        runTest {
            val weather = FakeWeather()
            // 4d: a repeated identical call no longer runs, so each round asks for another place
            val first = listOf(call("weather", "place", "Köln"))
            val second = listOf(call("weather", "place", "Bonn"))
            val llm = FakeLlm(first, second, listOf("Fertig."))
            val engine = ChatEngine(llm, { "SYS" }, tools = ToolRouter(listOf(weather)), toolContext = { ctx })
            val last = engine.respond(user("Wetter?"), AnswerMode.FAST).toList().last() as TurnUpdate.Finished
            assertEquals("Fertig.", last.output.text)
            assertEquals(ResearchBudget.FAST.maxRounds, weather.calls)
            assertFalse(llm.lastPrompt.contains("<tools>"))
        }

    @Test
    fun pureSumIsAnsweredWithoutTheBrain() =
        runTest {
            val llm = FakeLlm()
            val engine = ChatEngine(llm, { "SYS" }, tools = ToolRouter(listOf(CalculatorTool())), toolContext = { ctx })
            val last = engine.respond(user("12*7"), AnswerMode.FAST).toList().single() as TurnUpdate.Finished
            assertEquals("12*7 = 84", last.output.text)
            assertEquals(StopReason.END, last.reason)
            assertEquals("calculate", last.steps.single().name)
            assertTrue(llm.prompts.isEmpty())
        }

    @Test
    fun offlineLeavesOnlineToolsOutOfThePrompt() =
        runTest {
            val llm = FakeLlm(listOf("Ok."))
            val engine =
                ChatEngine(
                    llm,
                    { "SYS" },
                    tools = ToolRouter(listOf(CalculatorTool(), FakeWeather())),
                    toolContext = { ctx.copy(offline = true) },
                )
            engine.respond(user("Wetter?"), AnswerMode.FAST).toList()
            assertTrue(llm.lastPrompt.contains("\"calculate\""))
            assertFalse(llm.lastPrompt.contains("\"weather\""))
        }

    @Test
    fun stepsSurviveTheDatabase() {
        val steps =
            listOf(
                ToolStep("weather", "Köln", StepState.DONE, true),
                ToolStep("calculate", null, StepState.FAILED, false, "bad_arguments"),
            )
        val sources = listOf(SourceRef("Open-Meteo", "https://open-meteo.com/"))
        assertEquals(steps to sources, ToolStepsJson.decode(ToolStepsJson.encode(steps, sources)))
        assertEquals(null, ToolStepsJson.encode(emptyList(), emptyList()))
        assertEquals(emptyList<ToolStep>() to emptyList<SourceRef>(), ToolStepsJson.decode("kaputt"))
    }
}
