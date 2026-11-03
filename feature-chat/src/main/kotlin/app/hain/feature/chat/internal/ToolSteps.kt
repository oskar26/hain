package app.hain.feature.chat.internal

import app.hain.tools.api.PhoneAction

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

/** Where a ConfirmCard stands (T-068). Only [PENDING] has buttons. */
enum class ActionState { PENDING, DONE, CANCELLED, NO_APP }

/** What the user did on a ConfirmCard. */
enum class ActionEvent { CONFIRMED, NO_APP, CANCELLED }

/**
 * A phone action Pim prepared (alarm, timer, calendar event, route) and how far it is. It is only
 * done after the user tapped the card's button and the phone had an app for it.
 */
data class ActionCard(
    val action: PhoneAction,
    val state: ActionState = ActionState.PENDING,
) {
    /** The card after [event]; a card that is no longer pending stays as it is (one tap, one action). */
    fun after(event: ActionEvent): ActionCard {
        if (state != ActionState.PENDING) return this
        val next =
            when (event) {
                ActionEvent.CONFIRMED -> ActionState.DONE
                ActionEvent.NO_APP -> ActionState.NO_APP
                ActionEvent.CANCELLED -> ActionState.CANCELLED
            }
        return copy(state = next)
    }
}
