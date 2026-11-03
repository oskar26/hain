package app.hain.feature.chat.internal

import app.hain.llm.api.ChatMessage
import app.hain.llm.api.Role
import app.hain.llm.internal.AnswerMode
import app.hain.tools.api.PhoneAction
import app.hain.tools.api.ToolContext
import app.hain.tools.api.ToolRouter
import app.hain.tools.internal.phone.SetAlarmTool
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZonedDateTime

/** T-068: a phone action becomes a ConfirmCard; only the user's tap moves it on. */
class PhoneActionsTest {
    private val ctx = ToolContext("de", ZonedDateTime.now(), offline = false)
    private val alarm = PhoneAction.Alarm(6, 30)

    @Test
    fun alarmCallEndsAsPendingCard() =
        runTest {
            val call =
                "<tool_call>\n<function=set_alarm>\n<parameter=time>\n06:30\n</parameter>\n" +
                    "</function>\n</tool_call>"
            val llm = FakeLlm(listOf(call), listOf("Ich hab dir den Wecker für 6:30 vorbereitet."))
            val engine = ChatEngine(llm, { "SYS" }, tools = ToolRouter(listOf(SetAlarmTool())), toolContext = { ctx })
            val last =
                engine
                    .respond(listOf(ChatMessage(Role.USER, "Stell mir einen Wecker um 6:30")), AnswerMode.FAST)
                    .toList()
                    .last() as TurnUpdate.Finished
            assertEquals(listOf(ActionCard(alarm, ActionState.PENDING)), last.actions)
            assertEquals(ToolStep("set_alarm", "06:30", StepState.DONE, online = false), last.steps.single())
            assertTrue(
                last.trace
                    .single()
                    .result
                    .contains("waiting_for_tap"),
            )
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
    fun cardsAreStoredWithTheAnswer() {
        val cards =
            listOf(
                ActionCard(alarm, ActionState.DONE),
                ActionCard(PhoneAction.Navigate("Kölner Dom"), ActionState.PENDING),
                ActionCard(PhoneAction.CalendarEvent("Zahnarzt", 1L, 2L, "Köln"), ActionState.NO_APP),
            )
        val json = ToolStepsJson.encode(emptyList(), emptyList(), cards)
        assertEquals(cards, ToolStepsJson.decodeActions(json))
        assertEquals(emptyList<ActionCard>(), ToolStepsJson.decodeActions("""{"steps":[],"sources":[]}"""))
        assertEquals(emptyList<ActionCard>(), ToolStepsJson.decodeActions(null))
    }
}
