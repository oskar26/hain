package app.hain.ui.pim

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import app.hain.ui.motion.LocalReduceMotion
import app.hain.ui.motion.rememberHainHaptics
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Pull gesture with Pim (T-089): pull a list down past its top and Pim peeks in from above, eyes
 * wide once you pull far; let go and he springs back out of sight. The list itself does not move.
 * Off with "reduce motion".
 */
@Composable
fun PullPim(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val reduce = LocalReduceMotion.current
    val maxPull = with(LocalDensity.current) { MAX_PULL.dp.toPx() }
    val pull = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val haptics = rememberHainHaptics()
    // one soft tick when the pull goes past the point where Pim looks surprised, not on every pixel
    var armed by remember { mutableStateOf(false) }
    LaunchedEffect(pull, maxPull) {
        snapshotFlow { pull.value / maxPull > WIDE_FROM }.collect { past ->
            if (past && !armed) haptics.pull()
            armed = past
        }
    }
    val connection =
        remember(maxPull) {
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
                    scope.launch { pull.snapTo((pull.value + available.y * RESISTANCE).coerceAtMost(maxPull)) }
                    return Offset(0f, available.y)
                }

                override suspend fun onPreFling(available: Velocity): Velocity {
                    if (pull.value > 0f) pull.animateTo(0f, spring(dampingRatio = BOUNCE, stiffness = STIFFNESS))
                    return Velocity.Zero
                }
            }
        }
    Box(if (reduce) modifier else modifier.nestedScroll(connection)) {
        content()
        if (!reduce) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(PIM.dp)
                    .offset { IntOffset(0, (pull.value - PIM.dp.toPx()).roundToInt()) }
                    .graphicsLayer { alpha = (pull.value / (PIM.dp.toPx())).coerceIn(0f, 1f) },
                contentAlignment = Alignment.Center,
            ) {
                LivePim(
                    PimState.IDLE,
                    PIM.dp,
                    tappable = false,
                    overlay = { base ->
                        val p = (pull.value / maxPull).coerceIn(0f, 1f)
                        if (p > WIDE_FROM) {
                            base.copy(eyes = PimEyes.WIDE, mouth = PimMouth.OPEN, mouthOpen = p)
                        } else {
                            base
                        }
                    },
                )
            }
        }
    }
}

private const val MAX_PULL = 96
private const val PIM = 44
private const val RESISTANCE = 0.5f
private const val BOUNCE = 0.5f
private const val STIFFNESS = 400f
private const val WIDE_FROM = 0.7f
