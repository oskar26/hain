package app.hain.feature.chat.api

import app.hain.feature.chat.internal.StepState
import app.hain.feature.chat.internal.ToolStep
import app.hain.feature.chat.internal.UiMessage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** T-231: one status element for every phase of an answer. */
class PimStatusTest {
    private val sent = UiMessage(2, false, "", streaming = true)

    @Test
    fun phasesFollowTheAnswer() {
        assertEquals(PimPhase.WAITING, pimPhase(sent))
        assertEquals(PimPhase.THINKING, pimPhase(sent.copy(reasoning = "Hmm")))
        val running = ToolStep("weather", "Köln", StepState.RUNNING, online = true)
        assertEquals(PimPhase.TOOLS, pimPhase(sent.copy(steps = listOf(running))))
        // between two tool rounds Pim thinks again
        assertEquals(PimPhase.THINKING, pimPhase(sent.copy(steps = listOf(running.copy(state = StepState.DONE)))))
        assertEquals(PimPhase.WRITING, pimPhase(sent.copy(text = "Morgen")))
        assertEquals(PimPhase.DONE, pimPhase(sent.copy(text = "Morgen", streaming = false)))
    }

    @Test
    fun lineOnlyWhereItMeansSomething() {
        val old = UiMessage(4, false, "Antwort")
        assertFalse(showsStatusLine(old, latest = false))
        assertTrue(showsStatusLine(old, latest = true))
        assertTrue(showsStatusLine(old.copy(reasoning = "weil"), latest = false))
        assertTrue(showsStatusLine(sent, latest = false))
    }
}
