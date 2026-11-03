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
)

/** A page an answer is based on; shown as a source chip. */
data class SourceRef(
    val title: String,
    val url: String,
)
