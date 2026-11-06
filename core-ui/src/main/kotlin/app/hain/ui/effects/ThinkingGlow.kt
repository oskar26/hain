package app.hain.ui.effects

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.translate
import app.hain.ui.motion.LocalReduceMotion
import app.hain.ui.theme.LocalHainColors
import kotlin.math.sin

/** Mint of Pim's body, mixed with Hain green for the glow. */
private val MINT = Color(color = 0xFF7FD6A4)

/**
 * The glow's animated values, shared by every layer that paints a part of it (T-295): the backdrop
 * behind the chat and the band under the message box draw the same glow in screen coordinates, so
 * it reaches the bottom edge (behind the box and the navigation bar) without a seam.
 */
@Stable
class ThinkingGlowState internal constructor(
    internal val alpha: State<Float>,
    internal val phase: State<Float>,
    internal val accent: Color,
) {
    /** Nothing to draw (faded out). */
    val hidden: Boolean get() = alpha.value <= 0f
}

private val STILL = mutableFloatStateOf(0f)

/** The glow for [visible]; fades in and out, moves unless "reduce motion" is on. */
@Composable
fun rememberThinkingGlow(visible: Boolean): ThinkingGlowState {
    // Soft, eased fade (2d-2: no sudden pop-in). Values are read in the draw lambdas only, so the
    // glow never recomposes the screen while it moves.
    val alpha = animateFloatAsState(if (visible) 1f else 0f, tween(FADE_MS, easing = FADE_EASING), label = "glow-alpha")
    val accent = LocalHainColors.current.accent
    val reduce = LocalReduceMotion.current
    val phase =
        if (reduce || (!visible && alpha.value == 0f)) {
            STILL
        } else {
            rememberInfiniteTransition(label = "glow").animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(tween(WANDER_MS, easing = LinearEasing), RepeatMode.Restart),
                label = "glow-phase",
            )
        }
    return remember(alpha, phase, accent) { ThinkingGlowState(alpha, phase, accent) }
}

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
    val glow = rememberThinkingGlow(visible)
    if (!visible && glow.hidden) return
    Box(modifier.drawBehind { drawThinkingGlow(glow, size.height, 0f) })
}

/**
 * Paints the part of the glow that falls into this layer: [fullHeight] is the height of the whole
 * glow (the screen), [top] where this layer starts in it. Clipped to the layer.
 */
fun DrawScope.drawThinkingGlow(
    glow: ThinkingGlowState,
    fullHeight: Float,
    top: Float,
) {
    if (glow.hidden || fullHeight <= 0f) return
    clipRect(0f, 0f, size.width, size.height) {
        translate(top = -top) { drawGlow(glow.alpha.value, glow.phase.value, glow.accent, size.width, fullHeight) }
    }
}

internal fun DrawScope.drawGlow(
    alpha: Float,
    phase: Float,
    accent: Color,
) = drawGlow(alpha, phase, accent, size.width, size.height)

@Suppress("SpreadOperator") // the brush takes its stops as vararg
private fun DrawScope.drawGlow(
    alpha: Float,
    phase: Float,
    accent: Color,
    width: Float,
    h: Float,
) {
    if (alpha <= 0f) return
    // calm base: strongest at the bottom edge, nothing above BASE_TOP. T-295: many eased stops instead
    // of one straight ramp, so the low-alpha gradient shows no bands and no hard start
    drawRect(
        Brush.verticalGradient(*easedRamp(accent, BASE_ALPHA * alpha, BASE_TOP), startY = 0f, endY = h),
        size = Size(width, h),
    )
    // two wandering bands, half a cycle apart, rising and fading out at the top
    listOf(phase, (phase + HALF_CYCLE) % 1f).forEachIndexed { i, p ->
        val centre = h * (1f - p * RISE)
        val band = h * BAND
        val fade = (1f - p) * alpha
        val tint = if (i == 0) MINT else accent
        val sway = sin(p * TWO_PI + i) * width * SWAY
        val peak = BAND_ALPHA * fade
        drawRect(
            Brush.linearGradient(
                0f to Color.Transparent,
                SHOULDER to tint.copy(alpha = peak * SHOULDER_ALPHA),
                HALF_CYCLE to tint.copy(alpha = peak),
                1f - SHOULDER to tint.copy(alpha = peak * SHOULDER_ALPHA),
                1f to Color.Transparent,
                start = Offset(width / 2 + sway, centre - band),
                end = Offset(width / 2 - sway, centre + band),
            ),
            size = Size(width, h),
        )
    }
}

/** Transparent down to [from], then a smoothstep rise to [peak] at the bottom, in RAMP_STEPS stops. */
internal fun easedRamp(
    tint: Color,
    peak: Float,
    from: Float,
): Array<Pair<Float, Color>> =
    Array(RAMP_STEPS + 1) { i ->
        val t = i.toFloat() / RAMP_STEPS
        val y = from + (1f - from) * t
        val eased = t * t * (SMOOTH_A - SMOOTH_B * t)
        y to tint.copy(alpha = peak * eased)
    }.let { arrayOf(0f to Color.Transparent) + it }

private const val FADE_MS = 450
private const val EASE_X1 = 0.4f
private const val EASE_X2 = 0.2f

/** Material-like "standard" curve: starts gently, settles softly. */
private val FADE_EASING = CubicBezierEasing(EASE_X1, 0f, EASE_X2, 1f)
private const val WANDER_MS = 5200
private const val BASE_TOP = 0.45f
private const val BASE_ALPHA = 0.24f
private const val RISE = 0.85f
private const val BAND = 0.2f
private const val BAND_ALPHA = 0.15f
private const val SHOULDER = 0.25f
private const val SHOULDER_ALPHA = 0.55f
private const val SWAY = 0.2f
private const val TWO_PI = 6.2831855f
private const val HALF_CYCLE = 0.5f
private const val RAMP_STEPS = 8
private const val SMOOTH_A = 3f
private const val SMOOTH_B = 2f
