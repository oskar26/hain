package app.hain.feature.chat.internal

import app.hain.common.api.net.GateResponse
import app.hain.llm.api.ChatMessage
import app.hain.llm.api.Role
import app.hain.llm.api.ToolSchema
import app.hain.llm.internal.AnswerMode
import app.hain.tools.api.Tool
import app.hain.tools.api.ToolContext
import app.hain.tools.api.ToolResult
import app.hain.tools.api.ToolRouter
import app.hain.tools.api.ToolSource
import app.hain.tools.internal.search.PageReader
import app.hain.tools.internal.search.ReadPageTool
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZonedDateTime

/** Oskar's test of #19: tools in every message, and a search reads several pages (T-062, T-066, T-067). */
class ToolsEveryTurnTest {
    private val ctx = ToolContext("de", ZonedDateTime.now(), offline = false)

    private fun call(
        name: String,
        param: String,
        value: String,
    ) = "<tool_call>\n<function=$name>\n<parameter=$param>\n$value\n</parameter>\n</function>\n</tool_call>"

    private class FakeTool(
        name: String,
        private val answer: (Map<String, Any?>) -> ToolResult,
    ) : Tool {
        var calls = 0
        override val online = true
        override val schema = ToolSchema(name, "$name.", JsonObject(emptyMap()))

        override suspend fun run(
            args: Map<String, Any?>,
            ctx: ToolContext,
        ): ToolResult {
            calls++
            return answer(args)
        }
    }

    private val page =
        "<html><head><title>Seite</title></head><body><p>" + "Pflanzen machen Zucker aus Licht. ".repeat(4) +
            "</p></body></html>"

    private fun searchTool() =
        FakeTool("web_search") { args ->
            ToolResult(
                forModel = """{"query":"${args["query"]}","results":[]}""",
                label = args["query"].toString(),
                sources = listOf(ToolSource("Hit", "https://a.de/1")),
                readUrls = listOf("https://a.de/1", "https://b.de/2", "https://c.de/3"),
            )
        }

    @Test
    fun earlierToolCallsComeBackIntoThePrompt() {
        val long = "{\"extract\":\"" + "x".repeat(900) + "\"}"
        val trace = listOf(ToolTrace("wikipedia", mapOf("topic" to "Photosynthese"), long))
        val weatherTrace = listOf(ToolTrace("weather", mapOf("place" to "Köln"), "{}"))
        val messages =
            listOf(
                UiMessage(1, true, "Was ist Photosynthese?"),
                UiMessage(2, false, "Laut Wikipedia [1] …", trace = trace),
                UiMessage(3, true, "Wetter in Köln?"),
                UiMessage(4, false, "9 Grad [1].", trace = weatherTrace),
                UiMessage(5, true, "Und Berlin?"),
            )
        val history = PromptHistory.of(messages)
        assertEquals("weather", history[5].toolCalls.single().name)
        assertEquals(
            listOf(
                Role.USER,
                Role.ASSISTANT,
                Role.TOOL,
                Role.ASSISTANT,
                Role.USER,
                Role.ASSISTANT,
                Role.TOOL,
                Role.ASSISTANT,
                Role.USER,
            ),
            history.map { it.role },
        )
        assertEquals("wikipedia", history[1].toolCalls.single().name)
        assertEquals(history[1].toolCalls.single().id, history[2].toolCallId)
        // older results are cut hard, so long chats still fit
        assertEquals(PromptHistory.OLD_RESULT_CHARS + 1, history[2].content.length)
        assertEquals("Laut Wikipedia [1] …", history[3].content)
    }

    @Test
    fun traceSurvivesTheDatabase() {
        val trace = listOf(ToolTrace("weather", mapOf("place" to "Köln", "days" to "2"), "{\"t\":9.5}"))
        val entity = UiMessage(7, false, "9,5 Grad", trace = trace).toEntity(chatId = 1, createdAt = 0)
        assertTrue(entity.toolCallsJson!!.contains("\"weather\""))
        assertEquals(trace, entity.toUi().trace)
        assertEquals(emptyList<ToolTrace>(), ToolStepsJson.decodeTrace("kaputt"))
    }

    @Test
    fun secondQuestionStillOffersToolsAndShowsTheFirstCall() =
        runTest {
            val wiki = FakeTool("wikipedia") { ToolResult("{\"extract\":\"Licht\"}", "Photosynthese") }
            val weather = FakeTool("weather") { ToolResult("{\"t\":4}", "Berlin") }
            val llm =
                FakeLlm(
                    listOf(call("wikipedia", "topic", "Photosynthese")),
                    listOf("Pflanzen nutzen Licht [1]."),
                    listOf(call("weather", "place", "Berlin")),
                    listOf("In Berlin sind es 4 Grad [1]."),
                )
            val engine = ChatEngine(llm, { "SYS" }, tools = ToolRouter(listOf(wiki, weather)), toolContext = { ctx })
            val first =
                engine.respond(listOf(ChatMessage(Role.USER, "Photosynthese?")), AnswerMode.FAST).toList().last()
                    as TurnUpdate.Finished
            val saved =
                listOf(
                    UiMessage(1, true, "Photosynthese?"),
                    UiMessage(2, false, first.output.text, trace = first.trace),
                    UiMessage(3, true, "Wetter in Berlin?"),
                )
            val second =
                engine.respond(PromptHistory.of(saved), AnswerMode.FAST).toList().last() as TurnUpdate.Finished
            assertEquals("In Berlin sind es 4 Grad [1].", second.output.text)
            assertEquals(1, weather.calls)
            val prompt = llm.prompts[2]
            assertTrue(prompt.contains("<tools>"))
            assertTrue(prompt.contains("<function=wikipedia>"))
            assertTrue(prompt.contains("<tool_response>\n{\"extract\":\"Licht\"}"))
        }

    @Test
    fun searchReadsThePagesAndShowsEachStep() =
        runTest {
            val gate =
                FakeGate { req ->
                    if (req.url.contains("b.de")) GateResponse(500, "") else GateResponse(200, page)
                }
            val reader = PageReader(gate)
            val search = searchTool()
            val router = ToolRouter(listOf(search, ReadPageTool(reader)), pages = reader)
            val llm = FakeLlm(listOf(call("web_search", "query", "Photosynthese")), listOf("Antwort [1][2]."))
            val engine = ChatEngine(llm, { "SYS" }, tools = router, toolContext = { ctx })
            val ask = listOf(ChatMessage(Role.USER, "Recherchier Photosynthese"))
            val updates = engine.respond(ask, AnswerMode.AUTO).toList()
            val last = updates.last() as TurnUpdate.Finished
            assertEquals(listOf("web_search", ChatEngine.READ_STEP), last.steps.map { it.name })
            assertEquals("2", last.steps[1].detail)
            assertEquals(listOf("https://a.de/1", "https://c.de/3"), last.sources.map { it.url })
            val reading = updates.filterIsInstance<TurnUpdate.Streaming>().flatMap { it.steps }
            assertTrue(reading.any { it.name == ChatEngine.READ_STEP && it.state == StepState.RUNNING })
            assertTrue(llm.prompts[1].contains("{\"search\":{\"query\":\"Photosynthese\""))
            assertTrue(llm.prompts[1].contains("Pflanzen machen Zucker"))
            assertEquals(3, gate.log.value.size)
            assertEquals("web_search", last.trace.single().name)
        }

    @Test
    fun fastModeSearchesOnceAndReadsOnePage() =
        runTest {
            val gate = FakeGate { GateResponse(200, page) }
            val reader = PageReader(gate)
            val search =
                FakeTool("web_search") {
                    ToolResult("{}", "q", readUrls = listOf("https://a.de/1"))
                }
            val again = call("web_search", "query", "noch mal")
            val llm = FakeLlm(listOf(call("web_search", "query", "q")), listOf(again), listOf("Fertig."))
            val engine =
                ChatEngine(
                    llm,
                    { "SYS" },
                    tools = ToolRouter(listOf(search), pages = reader),
                    toolContext = { ctx },
                )
            val last =
                engine.respond(listOf(ChatMessage(Role.USER, "Suche q")), AnswerMode.FAST).toList().last()
                    as TurnUpdate.Finished
            assertEquals("Fertig.", last.output.text)
            assertEquals(1, search.calls)
            assertEquals(1, gate.log.value.size)
            assertEquals(StepState.FAILED, last.steps.last().state)
            assertEquals("limit", last.steps.last().error)
        }
}
