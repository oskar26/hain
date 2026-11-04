package app.hain.feature.chat.api

import app.hain.llm.api.GenerationStats

/**
 * Developer extras for the chat (T-211): speed under each answer, thinking opened right away, and
 * the line "copy debug info" puts on the clipboard. All off outside developer mode.
 */
data class ChatExtras(
    val showStats: Boolean = false,
    val rawThinking: Boolean = false,
    val debugInfo: (GenerationStats) -> String = { "" },
)
