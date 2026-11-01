package app.hain.ui.motion

import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalView

/** The three haptic moments of the app (UI_LIBRARIES.md §3 "Haptics"). */
enum class HainHaptic {
    /** Send: light tick. */
    SEND,

    /** Tool confirm: medium tap. */
    TOOL_CONFIRM,

    /** Error: double tick. */
    ERROR,
}

/** Maps a haptic moment to its framework constant; pure for unit testing. */
fun HainHaptic.toConstant(): Int =
    when (this) {
        HainHaptic.SEND -> HapticFeedbackConstants.KEYBOARD_TAP
        HainHaptic.TOOL_CONFIRM -> HapticFeedbackConstants.CONTEXT_CLICK
        HainHaptic.ERROR -> HapticFeedbackConstants.LONG_PRESS
    }

/** Thin wrapper so composables never touch the View directly. */
class HainHaptics(
    private val view: View,
) {
    fun perform(haptic: HainHaptic) {
        when (haptic) {
            HainHaptic.ERROR -> {
                // "double": two ticks back to back
                view.performHapticFeedback(haptic.toConstant())
                view.performHapticFeedback(HainHaptic.SEND.toConstant())
            }
            else -> view.performHapticFeedback(haptic.toConstant())
        }
    }

    fun send() = perform(HainHaptic.SEND)

    fun toolConfirm() = perform(HainHaptic.TOOL_CONFIRM)

    fun error() = perform(HainHaptic.ERROR)
}

@Composable
fun rememberHainHaptics(): HainHaptics = HainHaptics(LocalView.current)
