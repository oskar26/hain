package app.hain.feature.chat.internal

/** How far one tool call is (T-059). */
enum class StepState { RUNNING, DONE, FAILED }

/**
 * One line of the tool timeline under an answer: which tool, a few words about what it looked at
 * ("Köln"), and whether it went online (orange dot).
 */
data class ToolStep(
    val name: String,
    val detail: String?,
    val state: StepState,
    val online: Boolean,
    val error: String? = null,
    /** The tool that answered instead when this one had nothing (4c): "Wikipedia hatte nichts → Web". */
    val via: String? = null,
)

/** A page an answer is based on; shown as a source chip. */
data class SourceRef(
    val title: String,
    val url: String,
)

/**
 * One tool call of an answer as the brain saw it (T-062 fix): the call and a short copy of its result.
 * Kept with the answer, so the next turns show the brain real calls instead of only the answer text,
 * which it would otherwise copy ("laut Wikipedia …") without calling the tool again.
 */
data class ToolTrace(
    val name: String,
    val args: Map<String, String>,
    val result: String,
)

// T-068e: the ConfirmCard state lives in :core-cards now; the old names stay for the chat code.
typealias ActionState = app.hain.cards.api.ActionState
typealias ActionEvent = app.hain.cards.api.ActionEvent
typealias ActionCard = app.hain.cards.api.ActionCard
