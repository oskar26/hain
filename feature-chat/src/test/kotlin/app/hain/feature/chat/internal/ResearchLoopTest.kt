package app.hain.feature.chat.internal

import app.hain.common.api.net.GateResponse
import app.hain.llm.api.ChatMessage
import app.hain.llm.api.Role
import app.hain.llm.api.ToolSchema
import app.hain.llm.internal.AnswerMode
import app.hain.tools.api.HainTools
import app.hain.tools.api.Tool
import app.hain.tools.api.ToolContext
import app.hain.tools.api.ToolHit
import app.hain.tools.api.ToolResult
import app.hain.tools.api.ToolRouter
import app.hain.tools.internal.search.PageReader
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZonedDateTime

/** T-069 „Gründliche Recherche“: budgets, several sources at once, notes, fallback, Stop and time. */
class ResearchLoopTest {
    private val ctx = ToolContext("de", ZonedDateTime.now(), offline = false)

    private class FakeTool(
        name: String,
        private val answer: suspend (Map<String, Any?>) -> ToolResult,
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

    private fun call(
        name: String,
        param: String,
        value: String,
    ) = "<tool_call>\n<function=$name>\n<parameter=$param>\n$value\n</parameter>\n</function>\n</tool_call>"

    private fun user(text: String) = listOf(ChatMessage(Role.USER, text))

    /** A page whose one sentence names its own address, so each page gives its own note. */
    private val gate =
        FakeGate { req ->
            val site = req.url.substringAfter("://").substringBefore('/')
            GateResponse(
                200,
                "<html><head><title>$site</title></head><body><p>Auf $site steht: Die Zugspitze ist " +
                    "2962 Meter hoch und liegt in Bayern an der Grenze zu Österreich.</p></body></html>",
            )
        }

    private fun search(vararg urls: Pair<String, List<String>>) =
        FakeTool("web_search") { args ->
            val q = args["query"].toString()
            val list = urls.toMap()[q].orEmpty()
            ToolResult(
                """{"query":"$q","results":[]}""",
                q,
                readUrls = list,
                hits = list.map { ToolHit(it, it, "snippet $it") },
            )
        }

    @Test
    fun thoroughRunsSearchesAtOnceAndReadsManyDifferentSites() =
        runTest {
            val tool =
                search(
                    "Zugspitze Höhe" to listOf("https://a.de/1", "https://b.de/1", "https://c.de/1"),
                    "Zugspitze height" to
                        listOf("https://www.a.de/2", "https://d.de/1", "https://e.de/1") +
                        listOf("https://f.de/1", "https://g.de/1"),
                )
            val reader = PageReader(gate)
            val router = ToolRouter(listOf(tool), pages = reader)
            val first = call("web_search", "query", "Zugspitze Höhe")
            val both = first + "\n" + call("web_search", "query", "Zugspitze height")
            val llm = FakeLlm(listOf("plane", "</think>", both), listOf("kurz", "</think>", "2962 m [1][4]."))
            val engine = ChatEngine(llm, { "SYS" }, tools = router, toolContext = { ctx })
            val updates = engine.respond(user("Wie hoch ist die Zugspitze?"), AnswerMode.THOROUGH).toList()
            val last = updates.last() as TurnUpdate.Finished
            assertEquals("2962 m [1][4].", last.output.text)
            assertEquals(2, tool.calls)
            // a.de was read for the first search, so www.a.de is skipped: 3 + 4 = 7 different sites
            assertEquals(7, last.sources.size)
            assertEquals(
                7,
                last.sources
                    .map {
                        it.url
                            .substringAfter("://")
                            .removePrefix("www.")
                            .substringBefore('/')
                    }.toSet()
                    .size,
            )
            val names = listOf("web_search", "web_search", ChatEngine.READ_STEP, ChatEngine.NOTES_STEP)
            assertEquals(names, last.steps.map { it.name })
            assertEquals("7", last.steps[2].detail)
            // all seven sites say the same: one fact, confirmed by seven sources
            assertEquals("1/7", last.steps[3].detail)
            // the second search's pages carry the next source numbers, so [4] means the 4th chip
            assertTrue(llm.prompts[1].contains("\"n\":4"))
            assertTrue(llm.prompts[1].contains("Gründliche Recherche"))
            // the time display runs while researching
            assertTrue(updates.any { it is TurnUpdate.Streaming && it.research?.budgetMs == 60_000L })
        }

    @Test
    fun whenTheTimeIsUpPimAnswersWithWhatItHas() =
        runTest {
            var now = 1_000L
            val tool =
                FakeTool("web_search") {
                    now += 61_000L
                    ToolResult("""{"query":"x","results":[]}""", "x", readUrls = listOf("https://a.de/1"))
                }
            val again = call("web_search", "query", "noch mehr")
            val start = listOf("p", "</think>", call("web_search", "query", "x"))
            val llm = FakeLlm(start, listOf("Antwort."), listOf(again))
            val engine =
                ChatEngine(
                    llm,
                    { "SYS" },
                    clock = { now },
                    tools = ToolRouter(listOf(tool), pages = PageReader(gate)),
                    toolContext = { ctx },
                )
            val last = engine.respond(user("Recherchier x"), AnswerMode.THOROUGH).toList().last() as TurnUpdate.Finished
            assertEquals("Antwort.", last.output.text)
            assertEquals(2, llm.prompts.size)
            // T-294: the tool block stays in the closing round, so the prompt is read from memory
            assertTrue(llm.lastPrompt.contains("<tools>"))
            assertTrue(llm.lastPrompt.contains("Die Recherche ist beendet"))
            assertEquals(1, tool.calls)
        }

    @Test
    fun stopEndsTheResearchAndPimStillAnswers() =
        runTest {
            lateinit var engine: ChatEngine
            var stopped: Boolean? = null
            val tool =
                FakeTool("web_search") {
                    stopped = engine.stopResearch()
                    ToolResult("""{"query":"x","results":[]}""", "x")
                }
            val llm =
                FakeLlm(
                    listOf(call("web_search", "query", "x")),
                    listOf("Mit dem, was ich habe: …"),
                    listOf(call("web_search", "query", "y")),
                )
            engine = ChatEngine(llm, { "SYS" }, tools = ToolRouter(listOf(tool)), toolContext = { ctx })
            val updates = engine.respond(user("Recherchier x"), AnswerMode.AUTO).toList()
            val last = updates.last() as TurnUpdate.Finished
            assertEquals(true, stopped)
            assertEquals("Mit dem, was ich habe: …", last.output.text)
            assertEquals(2, llm.prompts.size)
            assertTrue(llm.lastPrompt.contains("<tools>"))
            assertFalse(engine.stopResearch())
        }

    @Test
    fun stopBeforeAnyToolIsAnOrdinaryStop() {
        val engine = ChatEngine(FakeLlm(), { "SYS" })
        assertFalse(engine.stopResearch())
    }

    @Test
    fun olderResultsShrinkToNotes() =
        runTest {
            val tool = search("Zugspitze" to listOf("https://a.de/1"))
            val weather =
                FakeTool("weather") { ToolResult("""{"place":"Garmisch","now":{"t":2}}""", "Garmisch") }
            val router = ToolRouter(listOf(tool, weather), pages = PageReader(gate))
            val llm =
                FakeLlm(
                    listOf(call("web_search", "query", "Zugspitze")),
                    listOf(call("weather", "place", "Garmisch")),
                    listOf("Fertig [1]."),
                )
            val engine = ChatEngine(llm, { "SYS" }, tools = router, toolContext = { ctx })
            val updates = engine.respond(user("Zugspitze Höhe und Wetter?"), AnswerMode.AUTO).toList()
            val last = updates.last() as TurnUpdate.Finished
            assertEquals("Fertig [1].", last.output.text)
            // round 2: the page is in full
            assertTrue(llm.prompts[1].contains("\"pages\":[{\"n\":1"))
            // round 3: the page left, its note stayed
            assertFalse(llm.prompts[2].contains("\"pages\":"))
            assertTrue(llm.prompts[2].contains("\"notes\":[\"[1] Auf a.de steht"))
            assertTrue(llm.prompts[2].contains("Garmisch"))
            assertTrue(
                last.trace
                    .first()
                    .result
                    .contains("notes"),
            )
        }

    @Test
    fun emptyWikipediaFallsBackToTheWebAndTheTimelineSaysSo() =
        runTest {
            val wiki = FakeTool("wikipedia") { ToolRouter.failure("not_found", "nothing") }
            val tool = search("Hainer Kiezfest" to listOf("https://a.de/1", "https://b.de/1"))
            val router = ToolRouter(listOf(wiki, tool), pages = PageReader(gate), fallbacks = HainTools.FALLBACKS)
            val llm = FakeLlm(listOf(call("wikipedia", "topic", "Hainer Kiezfest")), listOf("Laut [1] und [2] …"))
            val engine = ChatEngine(llm, { "SYS" }, tools = router, toolContext = { ctx })
            val updates = engine.respond(user("Was ist das Hainer Kiezfest?"), AnswerMode.AUTO).toList()
            val last = updates.last() as TurnUpdate.Finished
            val step = last.steps.first()
            assertEquals("wikipedia", step.name)
            assertEquals("web_search", step.via)
            assertEquals(StepState.DONE, step.state)
            assertEquals(ChatEngine.READ_STEP, last.steps[1].name)
            assertEquals(2, last.sources.size)
            assertTrue(llm.prompts[1].contains("Wikipedia had nothing"))
            assertNull(last.steps[1].error)
            assertNotNull(
                ToolStepsJson
                    .decode(ToolStepsJson.encode(last.steps, last.sources))
                    .first
                    .first()
                    .via,
            )
        }

    @Test
    fun researchHintOnlyForThorough() {
        assertNull(ChatEngine.researchHint(AnswerMode.AUTO, "de"))
        assertTrue(ChatEngine.researchHint(AnswerMode.THOROUGH, "de")!!.contains("mindestens 2 Quellen"))
        assertTrue(ChatEngine.researchHint(AnswerMode.THOROUGH, "en")!!.contains("at least 2 sources"))
    }
}
