package app.hain.feature.chat.api

import app.hain.llm.api.GenerationStats
import app.hain.tools.api.Favicons

/**
 * Developer extras for the chat (T-211): speed under each answer, thinking opened right away, and
 * the line "copy debug info" puts on the clipboard. All off outside developer mode.
 */
data class ChatExtras(
    val showStats: Boolean = false,
    val rawThinking: Boolean = false,
    val debugInfo: (GenerationStats) -> String = { "" },
    /** 4b-2: phone actions wait for a tap; `false` = they start after a 3 s countdown with undo. */
    val confirmActions: Boolean = true,
    /** T-064c: site icons at the source chips; `null` = setting "Quellen-Bilder" off, only letters. */
    val favicons: Favicons? = null,
)
