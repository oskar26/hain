// Confetti palette as plain RGB ints (Konfetti API).
@file:Suppress("MagicNumber")

package app.hain.ui.effects

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import app.hain.ui.motion.LocalReduceMotion
import nl.dionsegijn.konfetti.compose.KonfettiView
import nl.dionsegijn.konfetti.core.Party
import nl.dionsegijn.konfetti.core.Position
import nl.dionsegijn.konfetti.core.emitter.Emitter
import java.util.concurrent.TimeUnit

/** Only Hain greens and white (style proposals: fewer colours). */
private val CONFETTI_COLORS = listOf(0x16A36B, 0x7FD6A4, 0x3FAF77, 0xDDF1E6, 0xFFFFFF)

/** One burst of confetti from the upper middle (download done, 2e). Off with reduce motion. */
@Composable
fun Celebration(modifier: Modifier = Modifier) {
    if (LocalReduceMotion.current) return
    val parties =
        remember {
            listOf(
                Party(
                    speed = 0f,
                    maxSpeed = MAX_SPEED,
                    damping = DAMPING,
                    spread = FULL_SPREAD,
                    colors = CONFETTI_COLORS,
                    emitter = Emitter(duration = BURST_MS, TimeUnit.MILLISECONDS).max(PIECES),
                    position = Position.Relative(CENTRE_X, BURST_Y),
                ),
            )
        }
    KonfettiView(modifier = modifier, parties = parties)
}

private const val MAX_SPEED = 32f
private const val DAMPING = 0.9f
private const val FULL_SPREAD = 360
private const val BURST_MS = 120L
private const val PIECES = 70
private const val CENTRE_X = 0.5
private const val BURST_Y = 0.3
