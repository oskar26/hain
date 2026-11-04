package app.hain.feature.chat.api

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.hain.feature.chat.R
import app.hain.feature.chat.internal.QueuedMessage
import app.hain.ui.HainIcons
import app.hain.ui.motion.HainMotion
import app.hain.ui.motion.LocalReduceMotion
import app.hain.ui.motion.hainTween
import app.hain.ui.theme.LocalHainColors
import app.hain.ui.theme.LocalHainShapes
import app.hain.ui.theme.LocalHainSpacing
import app.hain.ui.theme.LocalHainTypography
import app.hain.ui.theme.calmTile

/**
 * Messages sent while Pim still answers (T-057), right above the message box: each a calm tile on
 * the right with the text, a clock and "wartet", and ✕ to take it back. They go out one by one
 * when the answer is done, then disappear here and show up in the chat.
 */
@Composable
internal fun QueuedMessages(
    queue: List<QueuedMessage>,
    onCancel: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = LocalHainSpacing.current
    val reduce = LocalReduceMotion.current
    Column(
        modifier.fillMaxWidth().padding(horizontal = spacing.md),
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(spacing.xs),
    ) {
        queue.forEach { item ->
            key(item.id) {
                val shown = remember { MutableTransitionState(false).apply { targetState = true } }
                AnimatedVisibility(
                    visibleState = shown,
                    enter =
                        fadeIn(hainTween(HainMotion.PILL_FADE_MS, reduce)) +
                            expandVertically(hainTween(HainMotion.PILL_FADE_MS, reduce)),
                    exit =
                        fadeOut(hainTween(HainMotion.PILL_FADE_MS, reduce)) +
                            shrinkVertically(hainTween(HainMotion.PILL_FADE_MS, reduce)),
                ) {
                    QueuedRow(item, onCancel)
                }
            }
        }
    }
}

@Composable
private fun QueuedRow(
    item: QueuedMessage,
    onCancel: (Long) -> Unit,
) {
    val colors = LocalHainColors.current
    val type = LocalHainTypography.current
    val spacing = LocalHainSpacing.current
    val waiting = stringResource(R.string.chat_queued)
    val described = stringResource(R.string.chat_queued_described, item.text)
    Row(
        Modifier
            .widthIn(max = MAX_WIDTH.dp)
            .calmTile(LocalHainShapes.current.card)
            .semantics { contentDescription = described }
            .padding(start = spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f, fill = false).padding(vertical = spacing.sm)) {
            BasicText(
                item.text,
                style = type.bodySmall.copy(color = colors.text),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    HainIcons.timer,
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(colors.textMuted),
                    modifier = Modifier.size(12.dp),
                )
                Spacer(Modifier.width(4.dp))
                BasicText(waiting, style = type.label.copy(color = colors.textMuted))
            }
        }
        val cancel = stringResource(R.string.chat_queued_cancel)
        // 48 dp touch target, 18 dp icon
        Box(
            Modifier
                .size(TOUCH.dp)
                .clip(CircleShape)
                .clickable(role = Role.Button, onClickLabel = cancel) { onCancel(item.id) }
                .semantics { contentDescription = cancel },
            contentAlignment = Alignment.Center,
        ) {
            Image(
                HainIcons.close,
                contentDescription = null,
                colorFilter = ColorFilter.tint(colors.textMuted),
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

private const val MAX_WIDTH = 320
private const val TOUCH = 48
