package app.hain.feature.chat.api

import app.hain.feature.chat.internal.UiMessage
import app.hain.ui.pim.PimState
import org.junit.Assert.assertEquals
import org.junit.Test

class AnswerPimTest {
    private val answer = UiMessage(1, isUser = false, text = "Hallo")

    @Test
    fun `Pim at the newest answer shows the right mood`() {
        assertEquals(PimState.ERROR, answerPimMood(answer.copy(failed = true), cheer = true, tooling = false))
        assertEquals(PimState.HAPPY, answerPimMood(answer, cheer = true, tooling = false))
        assertEquals(PimState.THINKING, answerPimMood(answer.copy(streaming = true), cheer = false, tooling = false))
        assertEquals(PimState.THINKING, answerPimMood(answer.copy(streaming = true), cheer = false, tooling = true))
        assertEquals(PimState.IDLE, answerPimMood(answer, cheer = false, tooling = false))
    }
}
