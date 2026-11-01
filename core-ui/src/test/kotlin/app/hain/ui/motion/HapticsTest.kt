package app.hain.ui.motion

import android.view.HapticFeedbackConstants
import org.junit.Assert.assertEquals
import org.junit.Test

class HapticsTest {
    @Test
    fun `haptic moments map to distinct framework constants`() {
        val send = HainHaptic.SEND.toConstant()
        val confirm = HainHaptic.TOOL_CONFIRM.toConstant()
        val error = HainHaptic.ERROR.toConstant()
        assertEquals(HapticFeedbackConstants.KEYBOARD_TAP, send)
        assertEquals(HapticFeedbackConstants.CONTEXT_CLICK, confirm)
        assertEquals(HapticFeedbackConstants.LONG_PRESS, error)
    }
}
