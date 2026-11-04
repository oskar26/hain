package app.hain.cards.api

import app.hain.tools.api.PhoneAction
import org.junit.Assert.assertEquals
import org.junit.Test

/** T-068 / T-068e: the ConfirmCard state, now chat-free in :core-cards; only the user's tap moves it on. */
class ActionCardTest {
    private val alarm = PhoneAction.Alarm(6, 30)

    @Test
    fun aNewCardWaitsForTheTap() {
        assertEquals(ActionState.PENDING, ActionCard(alarm).state)
    }

    @Test
    fun cardStates() {
        val card = ActionCard(alarm)
        assertEquals(ActionState.DONE, card.after(ActionEvent.CONFIRMED).state)
        assertEquals(ActionState.CANCELLED, card.after(ActionEvent.CANCELLED).state)
        assertEquals(ActionState.NO_APP, card.after(ActionEvent.NO_APP).state)
        // one tap, one action: a card that is done or cancelled stays so
        val done = card.after(ActionEvent.CONFIRMED)
        assertEquals(done, done.after(ActionEvent.CANCELLED))
        assertEquals(done, done.after(ActionEvent.CONFIRMED))
        val cancelled = card.after(ActionEvent.CANCELLED)
        assertEquals(cancelled, cancelled.after(ActionEvent.CONFIRMED))
    }

    @Test
    fun theActionStaysTheSame() {
        val route = ActionCard(PhoneAction.Navigate("Kölner Dom"))
        assertEquals(route.action, route.after(ActionEvent.CONFIRMED).action)
    }
}
