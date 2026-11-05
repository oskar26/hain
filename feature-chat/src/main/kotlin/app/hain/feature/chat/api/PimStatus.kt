@file:Suppress("MatchingDeclarationName") // the phase enum and the status element belong together

package app.hain.feature.chat.api

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.hain.feature.chat.R
import app.hain.feature.chat.internal.StepState
import app.hain.feature.chat.internal.UiMessage
import app.hain.ui.HainIcons
import app.hain.ui.motion.HainMotion
import app.hain.ui.motion.LocalReduceMotion
import app.hain.ui.motion.hainTween
import app.hain.ui.pim.LivePim
import app.hain.ui.pim.PimState
import app.hain.ui.theme.LocalHainColors
import app.hain.ui.theme.LocalHainShapes
import app.hain.ui.theme.LocalHainTypography

/**
 * What Pim is doing for an answer (T-231, Oskar 04.11. 22:18): before, there was a big Pim with
 * "Pim denkt nach…" while waiting, then a two-line "Pim denkt nach… ▸" with a tiny Pim, then a
 * separate tool card. Now it is **one element for all phases**.
 */
internal enum class PimPhase {
    /** Sent, nothing back yet (the brain reads the question). */
    WAITING,

    /** Thinking text streams, or between two tool rounds. */
    THINKING,

    /** A tool runs right now ("schaut nach"). */
    TOOLS,

    /** The answer text streams. */
    WRITING,

    /** Finished (or stopped / failed). */
    DONE,
}

internal fun pimPhase(message: UiMessage): PimPhase =
    when {
        !message.streaming -> PimPhase.DONE
        message.text.isNotEmpty() -> PimPhase.WRITING
        message.steps.any { it.state == StepState.RUNNING } -> PimPhase.TOOLS
        message.reasoning != null || message.steps.isNotEmpty() -> PimPhase.THINKING
        else -> PimPhase.WAITING
    }

/** The status line shows while Pim works, at the newest answer, and wherever there is thinking to open. */
internal fun showsStatusLine(
    message: UiMessage,
    latest: Boolean,
): Boolean = latest || message.streaming || !message.reasoning.isNullOrBlank()

/**
 * One line: Pim (30 dp, his mood follows the phase — thinking, peeking in while tools run, happy
 * when done, a fright on an error), the phase in words next to him, and when there is thinking a
 * chevron that opens it **in the same element**. The tool steps follow below in the same quiet style,
 * hanging from a thin line under Pim. Phases cross-fade; the height never jumps.
 * Older answers show a small brain icon instead of Pim.
 */
@Suppress("LongMethod") // one element: line, thinking, tool steps
@Composable
internal fun PimStatus(
    message: UiMessage,
    latest: Boolean,
    cheer: Boolean,
    modifier: Modifier = Modifier,
    initiallyExpanded: Boolean = false,
) {
    val colors = LocalHainColors.current
    val type = LocalHainTypography.current
    val reduce = LocalReduceMotion.current
    val phase = pimPhase(message)
    val reasoning = message.reasoning?.takeIf { it.isNotBlank() }
    val showLine = showsStatusLine(message, latest)
    if (!showLine && message.steps.isEmpty()) return
    var expanded by rememberSaveable(message.id) { mutableStateOf(initiallyExpanded) }
    val label = statusLabel(message, phase)
    val openLabel = stringResource(R.string.chat_status_open_thinking)
    Column(
        modifier
            .fillMaxWidth()
            .padding(bottom = 6.dp)
            .animateContentSize(hainTween(HainMotion.BASE_MS, reduce)),
    ) {
        if (showLine) {
            val canOpen = reasoning != null
            Row(
                Modifier
                    .clip(LocalHainShapes.current.row)
                    .then(
                        if (canOpen) {
                            Modifier.clickable(onClickLabel = openLabel, role = Role.Button) { expanded = !expanded }
                        } else {
                            Modifier
                        },
                    ).heightIn(min = LINE_HEIGHT.dp)
                    .padding(end = 8.dp)
                    .semantics(mergeDescendants = true) { label?.let { contentDescription = it } },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(PIM_SLOT.dp), contentAlignment = Alignment.Center) {
                    if (latest) {
                        StatusPim(message, cheer, phase)
                    } else {
                        Image(
                            HainIcons.brain,
                            contentDescription = null,
                            colorFilter = ColorFilter.tint(colors.icon),
                            modifier = Modifier.size(OLD_ICON.dp),
                        )
                    }
                }
                Spacer(Modifier.width(GAP.dp))
                AnimatedContent(
                    targetState = label,
                    transitionSpec = {
                        fadeIn(hainTween(HainMotion.BASE_MS, reduce)) togetherWith
                            fadeOut(hainTween(HainMotion.FAST_MS, reduce))
                    },
                    label = "pim-status",
                ) { text ->
                    if (text != null) {
                        BasicText(
                            text,
                            style = type.bodySmall.copy(color = colors.textMuted),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                if (canOpen) {
                    val turn by animateFloatAsState(
                        if (expanded) QUARTER else 0f,
                        hainTween(HainMotion.FAST_MS, reduce),
                        label = "pim-chevron",
                    )
                    Image(
                        HainIcons.chevronRight,
                        contentDescription = null,
                        colorFilter = ColorFilter.tint(colors.textMuted),
                        modifier = Modifier.padding(start = 4.dp).size(CHEVRON.dp).rotate(turn),
                    )
                }
            }
        }
        // the thinking, in the same element: muted text hanging from the thread under Pim
        AnimatedVisibility(
            visible = expanded && reasoning != null,
            enter =
                fadeIn(hainTween(HainMotion.BASE_MS, reduce)) +
                    expandVertically(hainTween(HainMotion.BASE_MS, reduce)),
            exit =
                fadeOut(hainTween(HainMotion.FAST_MS, reduce)) +
                    shrinkVertically(hainTween(HainMotion.FAST_MS, reduce)),
        ) {
            BasicText(
                reasoning.orEmpty(),
                style = type.bodySmall.copy(color = colors.textMuted),
                modifier = Modifier.thread().padding(start = INDENT.dp, top = 2.dp, bottom = 8.dp),
            )
        }
        if (message.steps.isNotEmpty()) {
            ToolTimeline(
                message.steps,
                research = message.research.takeIf { message.streaming },
                modifier = Modifier.thread(),
            )
        }
    }
}

/** A thin vertical line under Pim's middle that holds thinking and tool steps together. */
@Composable
private fun Modifier.thread(): Modifier {
    val line = LocalHainColors.current.border
    return drawBehind {
        val x = (PIM_SLOT / 2f).dp.toPx()
        drawLine(line, Offset(x, 0f), Offset(x, size.height), strokeWidth = 1.dp.toPx())
    }
}

/** The words next to Pim for [phase]; `null` = only Pim (a finished answer without thinking). */
@Composable
private fun statusLabel(
    message: UiMessage,
    phase: PimPhase,
): String? =
    when (phase) {
        PimPhase.WAITING -> stringResource(R.string.chat_status_waiting)
        PimPhase.THINKING -> stringResource(R.string.chat_thinking)
        PimPhase.TOOLS -> stringResource(R.string.chat_pim_looking)
        PimPhase.WRITING -> stringResource(R.string.chat_status_writing)
        PimPhase.DONE ->
            when {
                message.reasoning.isNullOrBlank() -> null
                message.thinkingSeconds != null -> stringResource(R.string.chat_thought_for, message.thinkingSeconds)
                else -> stringResource(R.string.chat_status_thought)
            }
    }

/**
 * Pim at the status line (T-213 → T-231): thinking while he works, he leans in from the side while a
 * tool runs, happy for a moment when done ([cheer]), a fright on a failed answer. With "reduce
 * motion" he simply sits there in his mood.
 */
@Composable
private fun StatusPim(
    message: UiMessage,
    cheer: Boolean,
    phase: PimPhase,
) {
    val reduce = LocalReduceMotion.current
    val tooling = phase == PimPhase.TOOLS
    val peek by animateFloatAsState(
        if (tooling && !reduce) 1f else 0f,
        hainTween(HainMotion.PAGE_MS, reduce),
        label = "pim-peek",
    )
    LivePim(
        answerPimMood(message, cheer, tooling),
        PIM_SIZE.dp,
        Modifier.graphicsLayer {
            // a calm peek: leans in a little and tilts
            translationX = -PEEK_SHIFT * size.width * peek
            rotationZ = PEEK_TILT * peek
        },
        tappable = false,
    )
}

/** Pim's mood at the newest answer (T-213): fright on failure, joy when done, thinking while busy. */
internal fun answerPimMood(
    message: UiMessage,
    cheer: Boolean,
    tooling: Boolean,
): PimState =
    when {
        message.failed -> PimState.ERROR
        cheer -> PimState.HAPPY
        tooling || message.streaming -> PimState.THINKING
        else -> PimState.IDLE
    }

/** The room in front of the status text (Pim) and the indent of everything that hangs below. */
internal const val PIM_SLOT = 32
private const val PIM_SIZE = 30
private const val GAP = 10
internal const val INDENT = PIM_SLOT + GAP
private const val LINE_HEIGHT = 40
private const val OLD_ICON = 18
private const val CHEVRON = 12
private const val QUARTER = 90f
private const val PEEK_SHIFT = 0.12f
private const val PEEK_TILT = -8f
