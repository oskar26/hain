package app.hain.llm.internal

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RepairingToolCallParserTest {
    private val parser = RepairingToolCallParser()

    @Test
    fun validOutputIsUnchanged() {
        val raw =
            "</think>Ich schaue.<tool_call>\n<function=weather>\n<parameter=place>\nKöln\n" +
                "</parameter>\n</function>\n</tool_call>"
        assertEquals(XmlToolCallParser().parse(raw), parser.parse(raw))
    }

    @Test
    fun missingCloseTags() {
        val out =
            parser.parse(
                "</think><tool_call>\n<function=weather>\n<parameter=place>\nKöln\n<parameter=days>\n2\n</function>",
            )
        val call = out.toolCalls.single()
        assertEquals("weather", call.name)
        assertEquals(mapOf("place" to "Köln", "days" to 2), call.arguments)
        assertTrue(out.errors.isEmpty())
        assertEquals("", out.text)
    }

    @Test
    fun jsonBody() {
        val out =
            parser.parse(
                "<tool_call>{\"name\": \"web_search\", \"arguments\": {\"query\": \"COP30\", \"n\": 3}}</tool_call>",
            )
        assertEquals("web_search", out.toolCalls.single().name)
        assertEquals(mapOf("query" to "COP30", "n" to 3), out.toolCalls.single().arguments)
    }

    @Test
    fun jsonArgumentsAsString() {
        val out =
            parser.parse(
                "<tool_call>{\"name\": \"weather\", \"arguments\": \"{\\\"place\\\": \\\"Bonn\\\"}\"}</tool_call>",
            )
        assertEquals(mapOf("place" to "Bonn"), out.toolCalls.single().arguments)
    }

    @Test
    fun pythonLikeCallAndLfmTags() {
        val out = parser.parse("Moment.<|tool_call_start|>[weather(place=\"Köln\", days=2)]<|tool_call_end|>")
        assertEquals("weather", out.toolCalls.single().name)
        assertEquals(mapOf("place" to "Köln", "days" to 2), out.toolCalls.single().arguments)
        assertEquals("Moment.", out.text)
    }

    @Test
    fun badBlockIsDroppedOthersRun() {
        val good = "<tool_call>\n<function=weather>\n<parameter=place>\nKöln\n</parameter>\n</function>\n</tool_call>"
        val out = parser.parse("</think>$good<tool_call>\nweather Köln\n</tool_call>")
        assertEquals(1, out.toolCalls.size)
    }

    @Test
    fun nothingReadableKeepsTheStrictError() {
        val out = parser.parse("</think><tool_call>\nweather Köln\n</tool_call>")
        assertTrue(out.toolCalls.isEmpty())
        assertTrue(out.errors.isNotEmpty())
    }

    @Test
    fun oddNamesAreRefused() {
        val out = parser.parse("<tool_call>{\"name\": \"rm -rf\", \"arguments\": {}}</tool_call>")
        assertTrue(out.toolCalls.isEmpty())
    }
}
