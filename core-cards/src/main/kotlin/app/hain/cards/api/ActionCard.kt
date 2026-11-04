package app.hain.cards.api

import app.hain.tools.api.PhoneAction

/** Where a ConfirmCard stands (T-068, moved here in T-068e). Only [PENDING] has buttons. */
enum class ActionState { PENDING, DONE, CANCELLED, NO_APP }

/** What the user did on a ConfirmCard. */
enum class ActionEvent { CONFIRMED, NO_APP, CANCELLED }

/**
 * A phone action Pim prepared (alarm, timer, calendar event, route) and how far it is. It is only
 * done after the user tapped the card's button and the phone had an app for it. Knows nothing about
 * the chat, so any screen can show a ConfirmCard (T-068e).
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
