package app.hain.feature.chat.api

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.hain.feature.chat.R
import app.hain.feature.chat.internal.ChatEngine
import app.hain.feature.chat.internal.UiMessage
import app.hain.ui.HainIcons
import app.hain.ui.motion.HainMotion
import app.hain.ui.motion.LocalReduceMotion
import app.hain.ui.motion.hainTween
import app.hain.ui.theme.LocalHainColors
import app.hain.ui.theme.LocalHainShapes
import app.hain.ui.theme.LocalHainTypography

/**
 * T-280 (Oskar 06.11.): the tool steps of an answer fold into one quiet line, "3 Werkzeuge genutzt ›".
 * While Pim works, only the step that runs right now shows (live); when the answer is done the steps
 * fold away by themselves. A tap opens or closes the full list with a soft animation (none with
 * "reduce motion"); the state is kept per message.
 */
@Composable
internal fun StepsFold(
    message: UiMessage,
    modifier: Modifier = Modifier,
    initiallyOpen: Boolean = false,
) {
    val steps = message.steps
    if (steps.isEmpty()) return
    val reduce = LocalReduceMotion.current
    var open by rememberSaveable(message.id, "steps") { mutableStateOf(initiallyOpen) }
    val research = message.research.takeIf { message.streaming }
    Column(modifier.fillMaxWidth().animateContentSize(hainTween(HainMotion.BASE_MS, reduce))) {
        if (message.streaming && !open) {
            // the step that runs now, live; a tap opens all of them
            Column(Modifier.clickable(role = Role.Button) { open = true }) {
                ToolTimeline(listOf(steps.last()), research = research)
            }
            return@Column
        }
        FoldLine(
            text = pluralStringResource(R.plurals.chat_tools_used, toolCount(message), toolCount(message)),
            open = open,
            onToggle = { open = !open },
        )
        AnimatedVisibility(
            visible = open,
            enter =
                fadeIn(hainTween(HainMotion.BASE_MS, reduce)) +
                    expandVertically(hainTween(HainMotion.BASE_MS, reduce)),
            exit =
                fadeOut(hainTween(HainMotion.FAST_MS, reduce)) +
                    shrinkVertically(hainTween(HainMotion.FAST_MS, reduce)),
        ) {
            ToolTimeline(steps, research = research)
        }
    }
}

/** Number of real tools in the steps (the "pages read" and "notes" lines are part of a search). */
internal fun toolCount(message: UiMessage): Int {
    val real = message.steps.count { it.name != ChatEngine.READ_STEP && it.name != ChatEngine.NOTES_STEP }
    return if (real > 0) real else message.steps.size
}

/** One quiet line with a small chevron that turns when it opens; TalkBack says "ausklappen"/"einklappen". */
@Composable
internal fun FoldLine(
    text: String,
    open: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalHainColors.current
    val reduce = LocalReduceMotion.current
    val action = stringResource(if (open) R.string.chat_collapse else R.string.chat_expand)
    val state = stringResource(if (open) R.string.chat_expanded else R.string.chat_collapsed)
    val turn by animateFloatAsState(
        if (open) QUARTER_TURN else 0f,
        hainTween(HainMotion.FAST_MS, reduce),
        label = "fold-chevron",
    )
    Row(
        modifier
            .padding(start = INDENT.dp)
            .clip(LocalHainShapes.current.row)
            .clickable(onClickLabel = action, role = Role.Button, onClick = onToggle)
            .heightIn(min = FOLD_HEIGHT.dp)
            .padding(end = 8.dp)
            .semantics(mergeDescendants = true) { stateDescription = state },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BasicText(
            text,
            style = LocalHainTypography.current.bodySmall.copy(color = colors.textMuted),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Image(
            HainIcons.chevronRight,
            contentDescription = null,
            colorFilter = ColorFilter.tint(colors.textMuted),
            modifier = Modifier.padding(start = 4.dp).size(FOLD_CHEVRON.dp).rotate(turn),
        )
    }
}

private const val FOLD_HEIGHT = 32
private const val FOLD_CHEVRON = 12
private const val QUARTER_TURN = 90f
