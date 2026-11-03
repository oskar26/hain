package app.hain.feature.chat.internal

import app.hain.llm.api.ChatMessage
import app.hain.llm.api.Role
import app.hain.llm.api.ToolCall

/**
 * The saved chat as the brain reads it (T-062 fix). An answer that used tools comes back as
 * call → tool result → answer, so the brain sees that facts came from a real call and calls again
 * for the next question. Results of the last answer keep [RECENT_RESULT_CHARS], older ones only
 * [OLD_RESULT_CHARS], so long chats still fit.
 */
internal object PromptHistory {
    const val MAX_MESSAGES = 24
    const val RECENT_RESULT_CHARS = 600
    const val OLD_RESULT_CHARS = 200

    /** What is stored per call; the prompt cuts it further. */
    const val STORED_RESULT_CHARS = 1200

    fun of(messages: List<UiMessage>): List<ChatMessage> {
        val kept = messages.filter { it.isUser || (!it.failed && it.text.isNotBlank()) }.takeLast(MAX_MESSAGES)
        val lastAnswer = kept.indexOfLast { !it.isUser }
        return kept.flatMapIndexed { index, m ->
            when {
                m.isUser -> listOf(ChatMessage(Role.USER, m.text))
                m.trace.isEmpty() -> listOf(ChatMessage(Role.ASSISTANT, m.text))
                else -> {
                    val limit = if (index == lastAnswer) RECENT_RESULT_CHARS else OLD_RESULT_CHARS
                    expand(m, limit, index)
                }
            }
        }
    }

    private fun expand(
        m: UiMessage,
        limit: Int,
        index: Int,
    ): List<ChatMessage> =
        m.trace.flatMapIndexed { i, t ->
            val id = "call_${index}_$i"
            listOf(
                ChatMessage(Role.ASSISTANT, "", listOf(ToolCall(id, t.name, t.args))),
                ChatMessage(Role.TOOL, cut(t.result, limit), toolCallId = id),
            )
        } + ChatMessage(Role.ASSISTANT, m.text)

    fun cut(
        text: String,
        max: Int,
    ): String = if (text.length <= max) text else text.take(max).trimEnd() + "…"
}
