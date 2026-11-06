package app.hain.feature.chat.api

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.hain.feature.chat.R
import app.hain.ui.HainIcons
import app.hain.ui.components.HainIconButton
import app.hain.ui.motion.HainEase
import app.hain.ui.motion.HainMotion
import app.hain.ui.motion.LocalReduceMotion
import app.hain.ui.motion.hainTween
import app.hain.ui.theme.LocalHainColors
import app.hain.ui.theme.LocalHainSpacing
import app.hain.voice.api.DictationState
import kotlin.math.PI
import kotlin.math.cos

/**
 * T-296: while Pim listens, the message box becomes this pill (after Grok's recording bar): a soft
 * mint → Hain green gradient, × on the left cancels, in the middle a dotted line from which rounded
 * vertical bars rise with the real microphone level (tallest in the middle), on the right a round dark
 * ✓ ends the dictation. With "reduce motion" the bars keep their shape and only fade with the voice.
 */
@Composable
internal fun DictationPill(
    state: DictationState.Recording,
    onCancel: () -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalHainColors.current
    val spacing = LocalHainSpacing.current
    val listening = stringResource(R.string.chat_dictation_listening)
    val clock = dictationClock(state.elapsedMs)
    val gradient =
        Brush.horizontalGradient(
            listOf(colors.accentSoft, lerp(colors.accentSoft, colors.accent, GRADIENT_END)),
        )
    Row(
        modifier
            .fillMaxWidth()
            .height(PILL_HEIGHT.dp)
            .clip(RoundedCornerShape(percent = HALF))
            .background(gradient)
            .padding(horizontal = spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.xs),
    ) {
        HainIconButton(HainIcons.close, stringResource(R.string.chat_dictation_cancel), onCancel, tint = colors.text)
        Waveform(
            levels = state.levels,
            modifier =
                Modifier
                    .weight(1f)
                    .height(WAVE_HEIGHT.dp)
                    .semantics {
                        contentDescription = "$listening, $clock"
                        liveRegion = LiveRegionMode.Polite
                    },
        )
        DoneButton(onDone)
    }
}

/** The round dark ✓ (40 dp, 48 dp touch target). */
@Composable
private fun DoneButton(onDone: () -> Unit) {
    val colors = LocalHainColors.current
    val done = stringResource(R.string.chat_dictation_done)
    Box(
        Modifier
            .size(TOUCH.dp)
            .clip(CircleShape)
            .clickable(role = Role.Button, onClick = onDone)
            .semantics { contentDescription = done },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier.size(DONE.dp).clip(CircleShape).background(colors.text),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                HainIcons.check,
                contentDescription = null,
                colorFilter = ColorFilter.tint(colors.bg),
                modifier = Modifier.size(ICON.dp),
            )
        }
    }
}

/** Dotted line plus bars from the microphone level; bars only where the voice is. */
@Composable
private fun Waveform(
    levels: List<Float>,
    modifier: Modifier = Modifier,
) {
    val colors = LocalHainColors.current
    val reduce = LocalReduceMotion.current
    val latest = levels.lastOrNull() ?: 0f
    // smooth the loudness a little so the bars breathe instead of flickering
    val loud by animateFloatAsState(latest, hainTween(HainMotion.FAST_MS, reduce), label = "pill-level")
    val heights = remember(levels, reduce) { pillBarHeights(levels, BARS, steady = reduce) }
    Canvas(modifier) {
        val ink = colors.text
        val mid = size.height / 2
        val dot = DOT.dp.toPx()
        val step = DOT_STEP.dp.toPx()
        var x = dot / 2
        while (x < size.width) {
            drawCircle(ink.copy(alpha = DOT_ALPHA), radius = dot / 2, center = Offset(x, mid))
            x += step
        }
        val barWidth = BAR_WIDTH.dp.toPx()
        val gap = BAR_GAP.dp.toPx()
        val total = BARS * barWidth + (BARS - 1) * gap
        val left = (size.width - total) / 2
        heights.forEachIndexed { i, h ->
            val alpha = if (reduce) (BAR_MIN_ALPHA + (1f - BAR_MIN_ALPHA) * loud) else 1f
            val height = (h * size.height).coerceAtLeast(dot)
            if (h > 0f && alpha > 0f) {
                drawRoundRect(
                    color = ink.copy(alpha = alpha),
                    topLeft = Offset(left + i * (barWidth + gap), mid - height / 2),
                    size = Size(barWidth, height),
                    cornerRadius = CornerRadius(barWidth / 2, barWidth / 2),
                )
            }
        }
    }
}

/**
 * Bar heights 0..1 for the pill: a bell (tallest in the middle) scaled by the newest level and lightly
 * varied by older levels, so the shape moves with the voice. Silence (all 0) gives no bars, only the
 * dotted line. [steady]: reduce motion, the bell alone (the pill fades it with the voice instead).
 */
internal fun pillBarHeights(
    levels: List<Float>,
    count: Int,
    steady: Boolean = false,
): List<Float> {
    val bell = List(count.coerceAtLeast(0)) { i -> bell(i, count) }
    val latest = levels.lastOrNull()?.coerceIn(0f, 1f) ?: 0f
    return when {
        steady -> bell.map { it * STEADY }
        latest <= SILENCE -> bell.map { 0f }
        else ->
            bell.mapIndexed { i, b ->
                val older = levels.getOrNull(levels.size - 1 - (i % VARIETY + 1))?.coerceIn(0f, 1f) ?: latest
                val voice = latest * (1f - MIX) + older * MIX
                (b * (BASE + (1f - BASE) * voice)).coerceIn(0f, 1f)
            }
    }
}

/** Raised cosine: 1 in the middle, [EDGE] at both ends. */
private fun bell(
    i: Int,
    count: Int,
): Float {
    if (count == 1) return 1f
    val t = i.toFloat() / (count - 1) * 2f - 1f // -1 … 1
    val c = ((cos(PI * t) + 1) / 2).toFloat()
    return EDGE + (1f - EDGE) * c
}

/** Pulse of the send button after a dictation lands in the box (scale, or only opacity with reduce motion). */
@Composable
internal fun Modifier.dictationHandoff(tick: Int): Modifier {
    val reduce = LocalReduceMotion.current
    val pulse = remember { Animatable(1f) }
    LaunchedEffect(tick) {
        if (tick == 0) return@LaunchedEffect
        pulse.snapTo(if (reduce) HANDOFF_ALPHA else HANDOFF_SCALE)
        pulse.animateTo(1f, tween(HainMotion.BASE_MS, easing = HainEase))
    }
    return graphicsLayer {
        if (reduce) {
            alpha = pulse.value
        } else {
            scaleX = pulse.value
            scaleY = pulse.value
        }
    }
}

private const val PILL_HEIGHT = 56
private const val WAVE_HEIGHT = 32
private const val HALF = 50
private const val TOUCH = 48
private const val DONE = 40
private const val ICON = 20
private const val BARS = 15
private const val BAR_WIDTH = 4
private const val BAR_GAP = 3
private const val DOT = 2
private const val DOT_STEP = 6
private const val DOT_ALPHA = 0.35f
private const val BAR_MIN_ALPHA = 0.25f
private const val GRADIENT_END = 0.45f
private const val EDGE = 0.18f
private const val BASE = 0.15f
private const val MIX = 0.35f
private const val VARIETY = 4
private const val SILENCE = 0.02f
private const val STEADY = 0.6f
private const val HANDOFF_SCALE = 0.7f
private const val HANDOFF_ALPHA = 0.3f
