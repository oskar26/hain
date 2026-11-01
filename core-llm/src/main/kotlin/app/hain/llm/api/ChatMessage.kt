package app.hain.llm.api

/** Who wrote a message in a chat (ARCHITECTURE.md §3). */
enum class Role(
    val wire: String,
) {
    SYSTEM("system"),
    USER("user"),
    ASSISTANT("assistant"),
    TOOL("tool"),
}

/**
 * One message of a conversation as the prompt builder sees it.
 *
 * @property reasoning the model's thinking for an assistant turn, without `<think>` tags; `null` = none recorded.
 * @property toolCalls tool calls an assistant turn made, rendered after [content].
 * @property toolCallId for [Role.TOOL] messages: the [ToolCall.id] this result answers.
 */
data class ChatMessage(
    val role: Role,
    val content: String,
    val toolCalls: List<ToolCall> = emptyList(),
    val reasoning: String? = null,
    val toolCallId: String? = null,
)
