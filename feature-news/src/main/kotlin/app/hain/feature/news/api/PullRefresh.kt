package app.hain.feature.news.api

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import app.hain.ui.motion.LocalReduceMotion
import app.hain.ui.motion.rememberHainHaptics
import app.hain.ui.theme.HainLevel
import app.hain.ui.theme.LocalHainColors
import app.hain.ui.theme.hainSurface
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Pull to refresh for the news list (T-075): pull the list down past its top, a small green ring
 * fills; let go past the mark and [onRefresh] runs while the ring turns. Own code on nested scroll,
 * no Material. With "reduce motion" the ring does not spin. The refresh button in the top bar stays
 * for TalkBack.
 */
@Composable
fun PullRefresh(
    refreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    indicatorTop: Dp = 0.dp,
    content: @Composable BoxScope.() -> Unit,
) {
    val density = LocalDensity.current
    val trigger = with(density) { TRIGGER.dp.toPx() }
    val pull = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val haptics = rememberHainHaptics()
    val refresh by rememberUpdatedState(onRefresh)
    val connection =
        remember(trigger) {
            object : NestedScrollConnection {
                override fun onPreScroll(
                    available: Offset,
                    source: NestedScrollSource,
                ): Offset {
                    if (available.y >= 0f || pull.value <= 0f) return Offset.Zero
                    val used = max(available.y, -pull.value)
                    scope.launch { pull.snapTo(pull.value + used) }
                    return Offset(0f, used)
                }

                override fun onPostScroll(
                    consumed: Offset,
                    available: Offset,
                    source: NestedScrollSource,
                ): Offset {
                    if (source != NestedScrollSource.UserInput || available.y <= 0f) return Offset.Zero
                    val before = pull.value
                    val next = (before + available.y * RESISTANCE).coerceAtMost(trigger * MAX_PULL)
                    if (before < trigger && next >= trigger) haptics.pull()
                    scope.launch { pull.snapTo(next) }
                    return Offset(0f, available.y)
                }

                override suspend fun onPreFling(available: Velocity): Velocity {
                    if (pull.value <= 0f) return Velocity.Zero
                    if (pull.value >= trigger) refresh()
                    pull.animateTo(0f)
                    return Velocity.Zero
                }
            }
        }
    LaunchedEffect(refreshing) { if (!refreshing && pull.value > 0f) pull.animateTo(0f) }
    Box(modifier.nestedScroll(connection)) {
        content()
        val shown = refreshing || pull.value > 0f
        if (shown) {
            val progress = if (refreshing) 1f else (pull.value / trigger).coerceIn(0f, 1f)
            val y = if (refreshing) trigger * REST_SHARE else pull.value * REST_SHARE
            Box(
                Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = indicatorTop)
                    .offset { IntOffset(0, y.roundToInt()) }
                    .graphicsLayer { alpha = progress.coerceAtLeast(MIN_ALPHA) }
                    .size(RING_BOX.dp)
                    .hainSurface(HainLevel.FLOATING, CircleShape, fill = LocalHainColors.current.bg)
                    .clearAndSetSemantics {},
                contentAlignment = Alignment.Center,
            ) {
                Ring(progress, spinning = refreshing)
            }
        }
    }
}

@Composable
private fun Ring(
    progress: Float,
    spinning: Boolean,
) {
    val accent = LocalHainColors.current.accent
    val reduce = LocalReduceMotion.current
    val turn =
        if (spinning && !reduce) {
            val t = rememberInfiniteTransition(label = "pull-ring")
            t
                .animateFloat(
                    0f,
                    FULL_TURN,
                    infiniteRepeatable(tween(SPIN_MS, easing = LinearEasing), RepeatMode.Restart),
                    label = "pull-ring-turn",
                ).value
        } else {
            0f
        }
    Canvas(Modifier.size(RING.dp)) {
        val stroke = RING_STROKE.dp.toPx()
        drawArc(
            color = accent,
            startAngle = turn - QUARTER_TURN,
            sweepAngle = if (spinning) SPIN_SWEEP else FULL_TURN * progress,
            useCenter = false,
            style = Stroke(width = stroke, cap = StrokeCap.Round),
        )
    }
}

private const val TRIGGER = 72
private const val MAX_PULL = 1.6f
private const val RESISTANCE = 0.5f
private const val REST_SHARE = 0.6f
private const val MIN_ALPHA = 0.25f
private const val RING_BOX = 40
private const val RING = 20
private const val RING_STROKE = 2.5f
private const val FULL_TURN = 360f
private const val QUARTER_TURN = 90f
private const val SPIN_SWEEP = 270f
private const val SPIN_MS = 900
