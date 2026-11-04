package app.hain.feature.chat.api

import androidx.compose.runtime.Composable
import app.hain.llm.api.GenerationStats

/**
 * Developer extras for the chat (T-211): speed under each answer, thinking opened right away, and
 * the line "copy debug info" puts on the clipboard. All off outside developer mode. Also the slot
 * under each answer that other features fill ([answerFooter]).
 */
data class ChatExtras(
    val showStats: Boolean = false,
    val rawThinking: Boolean = false,
    val debugInfo: (GenerationStats) -> String = { "" },
    /** 4b-2: phone actions wait for a tap; `false` = they start after a 3 s countdown with undo. */
    val confirmActions: Boolean = true,
    /**
     * 7b: shown under each finished answer (the memory's chips "Gemerkt ✓ · Rückgängig" and
     * "3 Erinnerungen genutzt"). Gets the chat's user messages up to that answer.
     */
    val answerFooter: (@Composable (userMessages: List<String>) -> Unit)? = null,
)
