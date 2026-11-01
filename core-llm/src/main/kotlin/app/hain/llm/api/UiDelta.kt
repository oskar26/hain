package app.hain.llm.api

/** A live piece of a streaming answer for the UI (LLM.md §4). */
sealed interface UiDelta {
    /** More thinking text ("Pim denkt nach…"). */
    data class Reasoning(
        val text: String,
    ) : UiDelta

    /** More visible answer text. */
    data class Text(
        val text: String,
    ) : UiDelta

    /** The model started a tool call; [name] once it is known, `null` if the block never named one. */
    data class ToolCallStarted(
        val name: String?,
    ) : UiDelta
}
