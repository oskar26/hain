package app.hain.ui.motion

import android.view.Choreographer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

/**
 * Frame-time watcher (UI_LIBRARIES.md §3 performance budget): if a frame takes
 * longer than 16 ms twice in a row, blur is switched off for the rest of the
 * session — never re-enabled mid-session, cheap surfaces stay cheap.
 *
 * Pure frame logic is separated from [Choreographer] so it is unit-testable.
 */
class MotionGovernor(
    private val frameBudgetMs: Float = DEFAULT_FRAME_BUDGET_MS,
    private val slowFrameStreakLimit: Int = DEFAULT_SLOW_FRAME_STREAK_LIMIT,
) {
    private var lastFrameNanos = NO_LAST_FRAME
    private var slowStreak = 0

    /** False once the device proved too slow for blur effects this session. */
    var blurEnabled: Boolean by mutableStateOf(true)
        private set

    /** Feed consecutive frame timestamps; deltas are the measured frame times. */
    fun onFrame(frameTimeNanos: Long) {
        if (lastFrameNanos != NO_LAST_FRAME) {
            val frameMs = (frameTimeNanos - lastFrameNanos) / NANOS_PER_MILLI
            slowStreak = if (frameMs > frameBudgetMs) slowStreak + 1 else 0
            if (slowStreak >= slowFrameStreakLimit) {
                blurEnabled = false
            }
        }
        lastFrameNanos = frameTimeNanos
    }

    companion object {
        // UI_LIBRARIES.md §3: 60 fps budget; two slow frames in a row kill blur.
        const val DEFAULT_FRAME_BUDGET_MS = 16f
        const val DEFAULT_SLOW_FRAME_STREAK_LIMIT = 2
        const val NANOS_PER_MILLI = 1_000_000f
        const val NO_LAST_FRAME = -1L
    }
}

/**
 * Attaches a [MotionGovernor] to the live Choreographer while this composable
 * is in the hierarchy. Read `governor.blurEnabled` before applying hainGlass.
 */
@Composable
fun rememberMotionGovernor(): MotionGovernor {
    val governor = remember { MotionGovernor() }
    DisposableEffect(governor) {
        val choreographer = Choreographer.getInstance()
        val callback =
            object : Choreographer.FrameCallback {
                override fun doFrame(frameTimeNanos: Long) {
                    governor.onFrame(frameTimeNanos)
                    choreographer.postFrameCallback(this)
                }
            }
        choreographer.postFrameCallback(callback)
        onDispose { choreographer.removeFrameCallback(callback) }
    }
    return governor
}
