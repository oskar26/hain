package app.hain.tools

import app.hain.llm.api.ToolSchema
import app.hain.tools.api.RawToolText
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** T-271: tool calls written as plain text (Oskar's device test of 0.1.10.7 with LFM3-Omni). */
class RawToolTextTest {
    private fun schema(
        name: String,
        vararg params: String,
    ) = ToolSchema(
        name,
        "d",
        buildJsonObject {
            put("type", "object")
            putJsonObject("properties") { params.forEach { p -> putJsonObject(p) { put("type", "string") } } }
            put("required", JsonArray(listOf(JsonPrimitive(params.first()))))
        },
    )

    private val tools =
        listOf(
            schema("calculate", "expression"),
            schema("weather", "place", "days"),
            schema("wikipedia", "topic"),
            schema("web_search", "query"),
            schema("remember", "text", "category"),
        )
    private val names = tools.map { it.name }

    @Test
    fun rememberAsTextBecomesACall() {
        val r = RawToolText.extract("remember \"Ich lerne Spanisch\"", tools)
        assertEquals("", r.text)
        assertEquals("remember", r.calls.single().name)
        assertEquals("Ich lerne Spanisch", r.calls.single().arguments["text"])
    }

    @Test
    fun bareArgumentsInACodeBlockBecomeTheirTool() {
        val r = RawToolText.extract("```json\n{\"expression\": \"2.5 * 1609.34\"}\n```", tools)
        assertEquals("", r.text)
        assertEquals("calculate", r.calls.single().name)
        assertEquals("2.5 * 1609.34", r.calls.single().arguments["expression"])
        assertEquals(
            "calculate",
            RawToolText
                .extract("{\"expression\": \"2+2\"}", tools)
                .calls
                .single()
                .name,
        )
    }

    @Test
    fun pythonLikeAndXmlCallsWithoutTheirBlock() {
        val py = RawToolText.extract("weather(place=\"Köln\", days=2)", tools).calls.single()
        assertEquals("weather", py.name)
        assertEquals("Köln", py.arguments["place"])
        assertEquals(2, py.arguments["days"])
        val xml = RawToolText.extract("<function=wikipedia>\n<parameter=topic>\nMoos\n</parameter>\n</function>", tools)
        assertEquals("Moos", xml.calls.single().arguments["topic"])
        assertEquals("", xml.text)
    }

    @Test
    fun theAnswerAroundACallLineStays() {
        val raw = "remember \"Bastelidee mit Altpapier\"\nCard title=\"Altpapier\"\nChecklist\n- Schere"
        val r = RawToolText.extract(raw, tools)
        assertEquals("remember", r.calls.single().name)
        assertEquals("Card title=\"Altpapier\"\nChecklist\n- Schere", r.text)
    }

    @Test
    fun normalTextIsLeftAlone() {
        val text = "Klar! Hier sind Lerntipps:\n- Übe jeden Tag\n- Mach Pausen\n\nViel Erfolg mit „Mathe“!"
        assertEquals(text, RawToolText.extract(text, tools).text)
        assertTrue(RawToolText.extract(text, tools).calls.isEmpty())
        // JSON in the middle of an answer is no call
        val json = "So sieht das aus:\n{\"expression\": \"1+1\"}"
        assertTrue(RawToolText.extract(json, tools).calls.isEmpty())
        // a word that happens to be a tool name in a sentence
        assertTrue(RawToolText.extract("weather is nice today", tools).calls.isEmpty())
    }

    @Test
    fun unknownToolNamesAreNotRun() {
        assertTrue(RawToolText.extract("delete_all(\"x\")", tools).calls.isEmpty())
        assertTrue(RawToolText.extract("remember \"x\"", emptyList()).calls.isEmpty())
    }

    @Test
    fun stripRemovesLeftoverSyntax() {
        val raw =
            "Gerne!\n<tool_call>\n<function=weather>\n<parameter=place>\nKöln\n" +
                "</parameter>\n</function>\n</tool_call>"
        assertEquals("Gerne!", RawToolText.strip(raw, names))
        assertEquals("Hallo", RawToolText.strip("remember \"x\"\nHallo", names))
        assertEquals("Hallo <3", RawToolText.strip("Hallo <3", names))
    }

    @Test
    fun liveTextHoldsBackWhatMayBecomeACall() {
        assertEquals("", RawToolText.liveText("remem", names))
        assertEquals("", RawToolText.liveText("remember \"Ich ler", names))
        assertEquals("", RawToolText.liveText("{\"expres", names))
        assertEquals("Hallo! Schön", RawToolText.liveText("Hallo! Schön", names))
        assertFalse(RawToolText.liveText("Klar, gern.\nHier", names).isEmpty())
        // a code block or JSON with a call at the start is skipped, also over several lines
        assertEquals("", RawToolText.liveText("```json\n{\"expression\": \"2.5", names))
        assertEquals("Das sind", RawToolText.liveText("```json\n{\"expression\": \"2+2\"}\n```\nDas sind", names))
        assertEquals("", RawToolText.liveText("{\n  \"expression\": \"1", names))
        // real code stays
        assertEquals("```kotlin\nval x = 1", RawToolText.liveText("```kotlin\nval x = 1", names))
    }
}
