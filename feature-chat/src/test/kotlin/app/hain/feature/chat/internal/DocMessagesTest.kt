package app.hain.feature.chat.internal

import app.hain.docs.api.DocAttachment
import app.hain.docs.api.DocKind
import org.junit.Assert.assertEquals
import org.junit.Test

class DocMessagesTest {
    private val a = DocAttachment(1, "a.pdf", DocKind.PDF, 2, 100)
    private val b = DocAttachment(2, "b.txt", DocKind.TEXT, 1, 50)

    @Test
    fun blocksGoBeforeTheWordsOfTheMessageThatBroughtThem() {
        val messages =
            listOf(
                UiMessage(1, isUser = true, text = "Fass zusammen", attachments = listOf(a)),
                UiMessage(2, isUser = false, text = "Gern."),
                UiMessage(3, isUser = true, text = "Und das?", attachments = listOf(a, b)),
            )
        val out = DocMessages.withDocuments(messages, mapOf(1L to "<A>", 2L to "<B>"))
        assertEquals("<A>\n\nFass zusammen", out[0].text)
        assertEquals("Gern.", out[1].text)
        assertEquals("<B>\n\nUnd das?", out[2].text)
    }

    @Test
    fun deletedDocumentIsLeftOut() {
        val messages = listOf(UiMessage(1, isUser = true, text = "Hi", attachments = listOf(a)))
        assertEquals("Hi", DocMessages.withDocuments(messages, mapOf(9L to "<X>"))[0].text)
        assertEquals(messages, DocMessages.withDocuments(messages, emptyMap()))
    }
}
