package app.hain.ui.motion

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp

/**
 * Which word indices in a freshly grown word list should animate.
 * Only appended words count, and at most [maxInFlight] of them — the most
 * recent ones win, older new words appear immediately (UI_LIBRARIES.md §3
 * "max 30 animated words at once").
 */
internal fun animatedWordIndices(
    previousWordCount: Int,
    currentWordCount: Int,
    maxInFlight: Int = HainMotion.MAX_ANIMATED_WORDS,
): IntRange {
    if (previousWordCount >= currentWordCount) return IntRange.EMPTY
    val newWords = currentWordCount - previousWordCount
    val animatable = minOf(newWords, maxInFlight)
    return (currentWordCount - animatable) until currentWordCount
}

/**
 * Streaming answer text where only newly appended words fade in and rise
 * 2 dp (160 ms ease-out). Non-append edits (regenerate) snap every word in.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WordFadeIn(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = TextStyle.Default,
    color: Color = Color.Unspecified,
) {
    val words = remember(text) { text.split(' ').filter { it.isNotBlank() } }
    var lastText by remember { mutableStateOf<String?>(null) }

    val animateRange =
        remember(text) {
            val previous = lastText
            val appended = previous != null && text.startsWith(previous)
            val previousCount = previous?.split(' ')?.count { it.isNotBlank() } ?: 0
            animatedWordIndices(
                previousWordCount = if (appended) previousCount else words.size,
                currentWordCount = words.size,
            )
        }
    SideEffect { lastText = text }

    FlowRow(modifier = modifier) {
        words.forEachIndexed { index, word ->
            key(index) {
                FadingWord(
                    word = word,
                    animate = index in animateRange,
                    last = index == words.lastIndex,
                    style = if (color.isSpecified) style.copy(color = color) else style,
                )
            }
        }
    }
}

@Composable
private fun FadingWord(
    word: String,
    animate: Boolean,
    last: Boolean,
    style: TextStyle,
) {
    val reduceMotion = LocalReduceMotion.current
    val progress = remember { Animatable(if (animate && !reduceMotion) 0f else 1f) }
    LaunchedEffect(animate, reduceMotion) {
        if (animate && !reduceMotion) {
            progress.animateTo(1f, tween(durationMillis = HainMotion.TOKEN_FADE_MS, easing = EaseOut))
        } else {
            progress.snapTo(1f)
        }
    }
    BasicText(
        // trailing space keeps words apart inside the FlowRow
        text = if (last) word else "$word ",
        modifier =
            Modifier.graphicsLayer {
                alpha = progress.value
                translationY = (1f - progress.value) * WORD_RISE.toPx()
            },
        style = style,
    )
}

// 2 dp rise for streaming words (UI_LIBRARIES.md §3 "Tokens stream").
private val WORD_RISE = 2.dp
