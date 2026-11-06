package app.hain.ui.motion

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

/**
 * T-295: a calm entrance for blocks that appear together (settings groups, cards of an answer, list
 * rows): each fades in and rises a few dp, [index] steps of [HainMotion.STAGGER_MS] after the first
 * (at most [MAX_STAGGERED] steps, so long lists do not keep you waiting). Values are read in the
 * layer only, so the entrance never recomposes. With "reduce motion" the block is simply there.
 */
fun Modifier.staggeredEnter(index: Int): Modifier =
    composed {
        val reduce = LocalReduceMotion.current
        if (reduce) return@composed this
        val rise = with(LocalDensity.current) { RISE_DP.dp.toPx() }
        val progress = remember { Animatable(0f) }
        LaunchedEffect(Unit) {
            progress.animateTo(
                1f,
                tween(
                    durationMillis = HainMotion.BASE_MS,
                    delayMillis = staggerDelay(index),
                    easing = HainEase,
                ),
            )
        }
        graphicsLayer {
            val p = progress.value
            alpha = p
            translationY = (1f - p) * rise
        }
    }

/** Delay of the block at [index]: [HainMotion.STAGGER_MS] per step, capped at [MAX_STAGGERED] steps. */
fun staggerDelay(index: Int): Int = index.coerceIn(0, MAX_STAGGERED) * HainMotion.STAGGER_MS

/** After this many blocks the rest come in together. */
const val MAX_STAGGERED = 6

private const val RISE_DP = 8
