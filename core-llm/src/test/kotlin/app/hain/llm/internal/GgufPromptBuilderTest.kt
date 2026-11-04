package app.hain.llm.internal

import app.hain.llm.api.ChatMessage
import app.hain.llm.api.Role
import org.junit.Assert.assertEquals
import org.junit.Test

class GgufPromptBuilderTest {
    private val history =
        listOf(
            ChatMessage(Role.USER, "Hallo"),
            ChatMessage(Role.ASSISTANT, "Hi!", reasoning = "denk"),
            ChatMessage(Role.TOOL, "{\"x\":1}", toolCallId = "1"),
            ChatMessage(Role.USER, "Wie geht's?"),
        )

    @Test
    fun `messages keep system, user and assistant and drop tool turns`() {
        assertEquals(
            listOf("system" to "Sys", "user" to "Hallo", "assistant" to "Hi!", "user" to "Wie geht's?"),
            GgufPromptBuilder.messagesOf("Sys", history),
        )
    }

    @Test
    fun `uses the model template when llama cpp knows it`() {
        val b = GgufPromptBuilder { msgs, add -> "T:${msgs.size}:$add" }
        assertEquals("T:4:true", b.build("Sys", emptyList(), history))
    }

    @Test
    fun `falls back to ChatML`() {
        val b = GgufPromptBuilder { _, _ -> null }
        assertEquals(
            "<|im_start|>system\nS<|im_end|>\n<|im_start|>user\nHallo<|im_end|>\n<|im_start|>assistant\n",
            b.build("S", emptyList(), listOf(ChatMessage(Role.USER, "Hallo"))),
        )
    }
}
