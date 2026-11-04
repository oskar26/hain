package app.hain.feature.chat.api

import app.hain.feature.chat.internal.UiMessage
import org.junit.Assert.assertEquals
import org.junit.Test

/** 7b: the slot under an answer gets the user messages up to that answer (the memory turn's key). */
class AnswerFooterTest {
    @Test
    fun userMessagesUpToTheAnswer() {
        val chat =
            listOf(
                UiMessage(1, isUser = true, text = "Hallo"),
                UiMessage(2, isUser = false, text = "Hi!"),
                UiMessage(3, isUser = true, text = "Merk dir, dass ich Gitarre spiele"),
                UiMessage(4, isUser = false, text = "Gemerkt."),
                UiMessage(5, isUser = true, text = "Und jetzt?"),
                UiMessage(6, isUser = false, text = "…"),
            )
        assertEquals(listOf("Hallo"), userMessagesBefore(chat, 2))
        assertEquals(listOf("Hallo", "Merk dir, dass ich Gitarre spiele"), userMessagesBefore(chat, 4))
        assertEquals(3, userMessagesBefore(chat, 6).size)
    }
}
