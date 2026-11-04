package app.hain.feature.chat.internal

import app.hain.llm.api.ChatMessage
import app.hain.llm.api.Role
import app.hain.llm.api.ToolCall
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FollowUpTest {
    private val weatherTurn =
        listOf(
            ChatMessage(Role.USER, "Wie wird das Wetter morgen in Köln?"),
            ChatMessage(Role.ASSISTANT, "", listOf(ToolCall("c0", "weather", mapOf("place" to "Köln")))),
            ChatMessage(Role.TOOL, "{\"place\":\"Köln\"}", toolCallId = "c0"),
            ChatMessage(Role.ASSISTANT, "Morgen regnet es in Köln [1]."),
        )

    private fun ask(text: String) = weatherTurn + ChatMessage(Role.USER, text)

    @Test
    fun shortFollowUpNamesTheTool() {
        assertEquals(
            "Das ist eine Folgefrage: rufe weather neu auf, mit den Werten aus dieser Nachricht.",
            FollowUp.hint(ask("und in München?"), "de"),
        )
        assertEquals(
            "This is a follow-up: call weather again with the values from this message.",
            FollowUp.hint(ask("and in Paris?"), "en"),
        )
        assertEquals(true, FollowUp.hint(ask("mach lieber Bonn draus"), "de")?.contains("weather"))
    }

    @Test
    fun thanksLongMessagesAndFirstQuestionsGetNone() {
        assertNull(FollowUp.hint(ask("danke dir!"), "de"))
        assertNull(FollowUp.hint(ask("ok cool?"), "de"))
        assertNull(FollowUp.hint(ask("Kannst du mir bitte eine lange Mail an meinen Chef schreiben?"), "de"))
        assertNull(FollowUp.hint(ask("Schreib ein Gedicht"), "de"))
        assertNull(FollowUp.hint(listOf(ChatMessage(Role.USER, "und in München?")), "de"))
    }

    @Test
    fun onlyTheLastTurnCounts() {
        val chat = weatherTurn + ChatMessage(Role.USER, "Erzähl einen Witz") + ChatMessage(Role.ASSISTANT, "Haha.")
        assertNull(FollowUp.hint(chat + ChatMessage(Role.USER, "und noch einer?"), "de"))
    }

    @Test
    fun linkInTheMessageAsksForReadPage() {
        val chat = listOf(ChatMessage(Role.USER, "lies mal https://example.org/artikel, und fass zusammen"))
        val expected = "Die Nachricht enthält einen Link: lies https://example.org/artikel mit read_page."
        assertEquals(expected, FollowUp.link(chat, "de"))
        assertNull(FollowUp.link(listOf(ChatMessage(Role.USER, "kein Link hier")), "en"))
    }
}
