package app.hain.ui.pim

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import app.hain.ui.motion.LocalReduceMotion
import app.hain.ui.motion.rememberHainHaptics
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin
import kotlin.random.Random

/**
 * Pim, alive (2e, "juicy"): breathes like jelly, blinks now and then, hops when
 * he celebrates and squishes when you tap him. With reduce motion he stays still.
 */
@Composable
fun JuicyPim(
    state: PimState,
    size: Dp,
    modifier: Modifier = Modifier,
    onTap: (() -> Unit)? = null,
) {
    val reduce = LocalReduceMotion.current
    val base = poseFor(state)
    if (reduce) {
        PimCanvas(pose = base, size = size, modifier = modifier, shadowLift = 0f)
        return
    }
    val phase by rememberInfiniteTransition(label = "pim-life").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(periodFor(state), easing = LinearEasing), RepeatMode.Restart),
        label = "pim-phase",
    )
    val squish = remember { Animatable(0f) }
    var blinking by remember { mutableStateOf(false) }
    var delighted by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val haptics = rememberHainHaptics()

    LaunchedEffect(Unit) {
        while (true) {
            delay(Random.nextLong(BLINK_MIN_MS, BLINK_MAX_MS))
            blinking = true
            delay(BLINK_MS)
            blinking = false
        }
    }

    val pose = livePose(base, state, phase, squish.value, blinking, delighted)
    val tap =
        Modifier.pointerInput(onTap) {
            detectTapGestures {
                haptics.send()
                onTap?.invoke()
                scope.launch {
                    delighted = true
                    squish.snapTo(1f)
                    squish.animateTo(0f, spring(dampingRatio = JELLY_DAMPING, stiffness = JELLY_STIFFNESS))
                    delay(DELIGHT_MS)
                    delighted = false
                }
            }
        }
    PimCanvas(pose = pose, size = size, modifier = modifier.then(tap), shadowLift = 0f)
}

/** Pure pose math so it can be unit-tested: [phase] runs 0→1 per cycle. */
@Suppress("LongParameterList")
internal fun livePose(
    base: PimPose,
    state: PimState,
    phase: Float,
    squish: Float,
    blinking: Boolean,
    delighted: Boolean,
): PimPose {
    val angle = phase * 2f * PI.toFloat()
    val hops = state == PimState.CELEBRATE
    val hop = if (hops) abs(sin(angle)) else 0f
    // jelly breathing: wider when low, taller when high
    val breathe = if (hops) 0f else sin(angle) * BREATHE
    // landing squash: strongest when the hop is near the ground
    val landing = if (hops) (1f - hop) * (1f - hop) * LANDING_SQUASH else 0f
    val sx = 1f + breathe + landing + squish * TAP_SQUASH
    val sy = 1f - breathe - landing - squish * TAP_SQUASH + hop * HOP_STRETCH
    val eyes =
        when {
            delighted -> PimEyes.HAPPY
            blinking && base.eyes == PimEyes.PILL -> PimEyes.CLOSED
            else -> base.eyes
        }
    return base.copy(
        y = base.y - hop * HOP_HEIGHT,
        sx = sx,
        sy = sy,
        tilt = base.tilt + if (hops) sin(angle * 2f) * HOP_TILT else 0f,
        leaf = base.leaf + sin(angle + LEAF_LAG) * LEAF_SWAY,
        eyes = eyes,
    )
}

private fun periodFor(state: PimState): Int = if (state == PimState.CELEBRATE) HOP_PERIOD_MS else BREATHE_PERIOD_MS

private const val BREATHE_PERIOD_MS = 2600
private const val HOP_PERIOD_MS = 900
private const val BREATHE = 0.03f
private const val LANDING_SQUASH = 0.12f
private const val HOP_STRETCH = 0.08f
private const val HOP_HEIGHT = 16f
private const val HOP_TILT = 4f
private const val LEAF_SWAY = 6f
private const val LEAF_LAG = 0.8f
private const val TAP_SQUASH = 0.16f
private const val JELLY_DAMPING = 0.3f
private const val JELLY_STIFFNESS = 260f
private const val DELIGHT_MS = 600L
private const val BLINK_MS = 130L
private const val BLINK_MIN_MS = 2400L
private const val BLINK_MAX_MS = 5200L
