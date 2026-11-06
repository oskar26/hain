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
            val llm = FakeLlm(listOf(call), listOf("Ich hab dir den Wecker für 6:30 gestellt."))
            val engine = ChatEngine(llm, { "SYS" }, tools = ToolRouter(listOf(SetAlarmTool())), toolContext = { ctx })
            val last =
                engine
                    .respond(listOf(ChatMessage(Role.USER, "Stell mir einen Wecker um 6:30")), AnswerMode.FAST)
                    .toList()
                    .last() as TurnUpdate.Finished
            assertEquals(listOf(ActionCard(alarm, ActionState.PENDING)), last.actions)
            // 4b-2: nothing is set before the tap, so the answer says "vorbereitet"
            assertEquals("Ich hab dir den Wecker für 6:30 vorbereitet.", last.output.text)
            assertEquals(ToolStep("set_alarm", "06:30", StepState.DONE, online = false), last.steps.single())
            assertTrue(
                last.trace
                    .single()
                    .result
                    .contains("waiting_for_tap"),
            )
        }

    // T-282 (#44): no set_alarm call at all, but a clear request → still a confirm card
    @Test
    fun aClearAlarmRequestWithoutACallStillGetsACard() =
        runTest {
            val llm = FakeLlm(listOf("Klar, ich wecke dich."), listOf("Dein Wecker für 6:30 ist vorbereitet."))
            val engine = ChatEngine(llm, { "SYS" }, tools = ToolRouter(listOf(SetAlarmTool())), toolContext = { ctx })
            val last =
                engine
                    .respond(listOf(ChatMessage(Role.USER, "stell mir nen wecker um halb sieben")), AnswerMode.FAST)
                    .toList()
                    .last() as TurnUpdate.Finished
            assertEquals(listOf(ActionCard(alarm, ActionState.PENDING)), last.actions)
            assertEquals("Dein Wecker für 6:30 ist vorbereitet.", last.output.text)
        }

    @Test
    fun aPlainQuestionGetsNoCard() =
        runTest {
            val llm = FakeLlm(listOf("Öffne die Uhr-App und tippe auf Wecker."))
            val engine = ChatEngine(llm, { "SYS" }, tools = ToolRouter(listOf(SetAlarmTool())), toolContext = { ctx })
            val last =
                engine
                    .respond(listOf(ChatMessage(Role.USER, "Wie stelle ich einen Wecker?")), AnswerMode.FAST)
                    .toList()
                    .last() as TurnUpdate.Finished
            assertTrue(last.actions.isEmpty())
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
