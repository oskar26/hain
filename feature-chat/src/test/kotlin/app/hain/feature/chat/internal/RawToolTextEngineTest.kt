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
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZonedDateTime

/** T-271: Oskar's device test of 0.1.10.7 – tool calls written as text never reach the chat. */
class RawToolTextEngineTest {
    private val ctx = ToolContext("de", ZonedDateTime.now(), offline = false)

    private class Recorder(
        name: String,
        param: String,
        private val answer: String,
    ) : Tool {
        val runs = mutableListOf<Map<String, Any?>>()
        override val online = false
        override val schema =
            ToolSchema(
                name,
                "d",
                buildJsonObject {
                    put("type", "object")
                    putJsonObject("properties") { putJsonObject(param) { put("type", "string") } }
                    put("required", JsonArray(listOf(JsonPrimitive(param))))
                },
            )

        override suspend fun run(
            args: Map<String, Any?>,
            ctx: ToolContext,
        ): ToolResult {
            runs += args
            return ToolResult(answer, label = answer)
        }
    }

    private fun user(text: String) = listOf(ChatMessage(Role.USER, text))

    private fun allText(updates: List<TurnUpdate>): String =
        updates.joinToString("\n") {
            when (it) {
                is TurnUpdate.Streaming -> it.text
                is TurnUpdate.Finished -> it.output.text
                else -> ""
            }
        }

    @Test
    fun rememberForATopicQuestionIsDroppedAndPimStillAnswers() =
        runTest {
            val remember = Recorder("remember", "text", """{"saved":"x"}""")
            val llm =
                FakeLlm(
                    listOf("remember \"Lerntipps für Mathe: Regelmäßig üben\""),
                    listOf("Übe jeden Tag ein bisschen und erkläre dir Aufgaben selbst."),
                )
            val engine =
                ChatEngine(
                    llm,
                    { "SYS" },
                    tools = ToolRouter(listOf(remember)),
                    toolContext = { ctx },
                    mayRemember = { _, _ -> false },
                )
            val updates = engine.respond(user("Lerntipps für Mathe"), AnswerMode.AUTO).toList()
            val last = updates.last() as TurnUpdate.Finished
            assertEquals(0, remember.runs.size)
            assertEquals("Übe jeden Tag ein bisschen und erkläre dir Aufgaben selbst.", last.output.text)
            assertFalse(allText(updates).contains("remember"))
            // the second try was told to answer directly
            assertTrue(llm.lastPrompt.contains("Antworte jetzt direkt dem Nutzer"))
        }

    @Test
    fun bareCalculatorArgumentsRunTheCalculator() =
        runTest {
            val calc = Recorder("calculate", "expression", """{"result":"4.02 km"}""")
            val reply = "```json\n{\"expression\": \"2.5 mi in km\"}\n```"
            val llm = FakeLlm(listOf(reply), listOf("Das sind etwa 4,02 km."))
            val engine = ChatEngine(llm, { "SYS" }, tools = ToolRouter(listOf(calc)), toolContext = { ctx })
            val updates = engine.respond(user("Wie weit sind 2,5 Meilen ungefähr?"), AnswerMode.AUTO).toList()
            val last = updates.last() as TurnUpdate.Finished
            assertEquals("2.5 mi in km", calc.runs.single()["expression"])
            assertEquals("Das sind etwa 4,02 km.", last.output.text)
            assertFalse(allText(updates).contains("expression"))
        }

    @Test
    fun anAnswerThatIsOnlyToolSyntaxIsAskedAgain() =
        runTest {
            val reply = "<function=unknown_tool>\n<parameter=x>\n1\n</parameter>\n</function>"
            val llm = FakeLlm(listOf(reply), listOf("Hallo!"))
            val engine = ChatEngine(llm, { "SYS" }, tools = ToolRouter(emptyList()), toolContext = { ctx })
            val last = engine.respond(user("hi"), AnswerMode.AUTO).toList().last() as TurnUpdate.Finished
            assertEquals("Hallo!", last.output.text)
        }

    @Test
    fun aLooseCardGetsItsFenceAndARememberLineGoes() =
        runTest {
            val remember = Recorder("remember", "text", """{"saved":"x"}""")
            val reply = "remember \"Bastelidee mit Altpapier\"\nCard title=\"Altpapier\"\nChecklist\n• Schere\n• Kleber"
            val llm = FakeLlm(listOf(reply), listOf("Bastel eine Papierlaterne!"))
            val engine =
                ChatEngine(
                    llm,
                    { "SYS" },
                    tools = ToolRouter(listOf(remember)),
                    toolContext = { ctx },
                    mayRemember = { _, _ -> false },
                )
            val updates = engine.respond(user("Bastelidee mit Altpapier"), AnswerMode.AUTO).toList()
            val last = updates.last() as TurnUpdate.Finished
            assertEquals(0, remember.runs.size)
            assertTrue(last.output.text, last.output.text.startsWith("```card\nCard title=\"Altpapier\""))
            assertFalse(last.output.text.contains("remember"))
        }
}
