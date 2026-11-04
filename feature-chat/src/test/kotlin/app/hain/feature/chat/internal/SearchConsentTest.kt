package app.hain.feature.chat.internal

import app.hain.llm.api.ChatMessage
import app.hain.llm.api.Role
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** 3b-2, "Websuche: erst fragen": only a yes right after Pim's question lets the search run. */
class SearchConsentTest {
    private val asked =
        listOf(
            ChatMessage(Role.USER, "Was gibt es Neues bei der Bahn?"),
            ChatMessage(Role.TOOL, """{"error":"ask_first","hint":"…"}""", toolCallId = "1"),
            ChatMessage(Role.ASSISTANT, "Darf ich dafür kurz im Web suchen?"),
        )

    @Test
    fun yesAfterTheQuestion() {
        assertTrue(SearchConsent.approved(asked + ChatMessage(Role.USER, "Ja, gerne")))
        assertTrue(SearchConsent.approved(asked + ChatMessage(Role.USER, "ok")))
        assertTrue(SearchConsent.approved(asked + ChatMessage(Role.USER, "Yes please")))
    }

    @Test
    fun noOrNewQuestionAsksAgain() {
        assertFalse(SearchConsent.approved(asked + ChatMessage(Role.USER, "Nein, lieber nicht")))
        assertFalse(SearchConsent.approved(asked + ChatMessage(Role.USER, "Wie wird das Wetter?")))
        // a yes without Pim's question before it
        assertFalse(SearchConsent.approved(listOf(ChatMessage(Role.USER, "Ja"))))
        // the question was two turns ago
        val later = asked + ChatMessage(Role.USER, "Egal") + ChatMessage(Role.ASSISTANT, "Gut.")
        assertFalse(SearchConsent.approved(later + ChatMessage(Role.USER, "ja")))
    }
}
