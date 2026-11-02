package app.hain.ui.effects

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import app.hain.ui.motion.LocalReduceMotion
import app.hain.ui.theme.LocalHainColors
import kotlin.math.sin

/** Mint of Pim's body, mixed with Hain green for the glow. */
private val MINT = Color(color = 0xFF7FD6A4)

/**
 * Copilot-like "Pim is thinking" glow (2e): a soft green/mint gradient rises
 * slowly from the bottom edge and fades out towards the top. Fades in and out
 * with [visible]; with reduce motion it stays a calm, still glow.
 */
@Composable
fun ThinkingGlow(
    visible: Boolean,
    modifier: Modifier = Modifier,
) {
    val alpha by animateFloatAsState(if (visible) 1f else 0f, tween(FADE_MS), label = "glow-alpha")
    if (alpha == 0f) return
    val reduce = LocalReduceMotion.current
    val accent = LocalHainColors.current.accent
    val phase =
        if (reduce) {
            0f
        } else {
            val p by rememberInfiniteTransition(label = "glow").animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(tween(WANDER_MS, easing = LinearEasing), RepeatMode.Restart),
                label = "glow-phase",
            )
            p
        }
    Box(modifier.drawBehind { drawGlow(alpha, phase, accent) })
}

internal fun DrawScope.drawGlow(
    alpha: Float,
    phase: Float,
    accent: Color,
) {
    val h = size.height
    // calm base: strongest at the bottom edge, transparent above 45 %
    drawRect(
        Brush.verticalGradient(
            0f to Color.Transparent,
            BASE_TOP to Color.Transparent,
            1f to accent.copy(alpha = BASE_ALPHA * alpha),
        ),
    )
    // two wandering bands, half a cycle apart, rising and fading out at the top
    listOf(phase, (phase + HALF_CYCLE) % 1f).forEachIndexed { i, p ->
        val centre = h * (1f - p * RISE)
        val band = h * BAND
        val fade = (1f - p) * alpha
        val tint = if (i == 0) MINT else accent
        val sway = sin(p * TWO_PI + i) * size.width * SWAY
        drawRect(
            Brush.linearGradient(
                listOf(Color.Transparent, tint.copy(alpha = BAND_ALPHA * fade), Color.Transparent),
                start = Offset(size.width / 2 + sway, centre - band),
                end = Offset(size.width / 2 - sway, centre + band),
            ),
        )
    }
}

private const val FADE_MS = 600
private const val WANDER_MS = 5200
private const val BASE_TOP = 0.55f
private const val BASE_ALPHA = 0.22f
private const val RISE = 0.85f
private const val BAND = 0.18f
private const val BAND_ALPHA = 0.16f
private const val SWAY = 0.2f
private const val TWO_PI = 6.2831855f
private const val HALF_CYCLE = 0.5f
