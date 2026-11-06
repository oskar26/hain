package app.hain.feature.chat.internal

import app.hain.llm.api.ChatMessage
import app.hain.llm.api.Role
import app.hain.llm.api.StopReason
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZonedDateTime

/** T-277: Oskar's "Was ist Moos?" – after a long research the answer was empty (thinking ran out). */
class EmptyAnswerTest {
    private val ctx = ToolContext("de", ZonedDateTime.now(), offline = false)

    private class Wiki : Tool {
        override val online = false
        override val schema = ToolSchema("wikipedia", "w", JsonObject(emptyMap()))

        override suspend fun run(
            args: Map<String, Any?>,
            ctx: ToolContext,
        ) = ToolResult(EXTRACT, "Moos")
    }

    private val call =
        "<tool_call>\n<function=wikipedia>\n<parameter=topic>\nMoos\n</parameter>\n</function>\n</tool_call>"

    private fun user(text: String) = listOf(ChatMessage(Role.USER, text))

    private fun engine(llm: FakeLlm): ChatEngine {
        val router = ToolRouter(listOf(Wiki()))
        return ChatEngine(llm, { "SYS" }, tools = router, toolContext = { ctx })
    }

    @Test
    fun thinkingThatRunsOutIsNoAnswerAndTheRetryAnswersWithoutThinking() =
        runTest {
            val llm =
                ScriptedLlm(
                    listOf("plan", "</think>", call) to StopReason.END,
                    listOf("We need to answer the question about moss. The user asks …") to StopReason.MAX_TOKENS,
                    listOf("Moose sind grüne Landpflanzen ohne echte Wurzeln [1].") to StopReason.END,
                )
            val updates = engine(llm.fake).respond(user("Was ist Moos?"), AnswerMode.THOROUGH).toList()
            val last = updates.last() as TurnUpdate.Finished
            assertEquals("Moose sind grüne Landpflanzen ohne echte Wurzeln [1].", last.output.text)
            assertFalse(last.output.text.contains("We need"))
            // the retry is told to answer and does not think
            assertTrue(llm.fake.lastPrompt.endsWith("<think></think>"))
            assertTrue(llm.fake.lastPrompt.contains("Antworte jetzt direkt dem Nutzer"))
        }

    @Test
    fun theClosingRoundAnswersWithoutThinking() =
        runTest {
            val replies = List(8) { listOf("plan", "</think>", call.replace("Moos", "Moos $it")) to StopReason.END }
            val all = replies + (listOf("Moose sind Landpflanzen [1].") to StopReason.END)
            val llm = ScriptedLlm(*all.toTypedArray())
            val last =
                engine(llm.fake).respond(user("Was ist Moos?"), AnswerMode.THOROUGH).toList().last()
                    as TurnUpdate.Finished
            assertEquals("Moose sind Landpflanzen [1].", last.output.text)
            assertTrue(
                llm.fake.prompts
                    .first()
                    .endsWith("<think>"),
            )
            assertTrue(llm.fake.lastPrompt.endsWith("<think></think>"))
        }

    @Test
    fun neverAnEmptyBubble() =
        runTest {
            val llm = ScriptedLlm(listOf("") to StopReason.END, listOf(" ") to StopReason.END)
            val last = engine(llm.fake).respond(user("hm"), AnswerMode.AUTO).toList().last() as TurnUpdate.Finished
            assertEquals(ChatEngine.emptyAnswer("de"), last.output.text)
        }

    private companion object {
        const val EXTRACT = """{"extract":"Moose sind grüne Landpflanzen ohne echte Wurzeln."}"""
    }

    /** A [FakeLlm] whose replies each end with their own stop reason. */
    private class ScriptedLlm(
        vararg replies: Pair<List<String>, StopReason>,
    ) {
        val fake = FakeLlm(*replies.map { it.first }.toTypedArray(), ends = replies.map { it.second })
    }
}
