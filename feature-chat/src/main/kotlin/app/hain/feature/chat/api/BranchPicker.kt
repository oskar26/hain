package app.hain.feature.chat.api

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.hain.feature.chat.R
import app.hain.feature.chat.internal.BranchInfo
import app.hain.ui.HainIcons
import app.hain.ui.motion.rememberHainHaptics
import app.hain.ui.theme.LocalHainColors
import app.hain.ui.theme.LocalHainTypography

/**
 * "‹ 2/3 ›" under a message with other versions (T-057, like ChatGPT): an edit or "Nochmal" keeps
 * the old version, the arrows switch. Quiet grey; arrows are 40 dp touch targets (T-272: the row under an
 * answer stays one line at 360 dp) and greyed out at the ends; not usable while Pim answers ([enabled]).
 */
@Composable
internal fun BranchPicker(
    info: BranchInfo,
    enabled: Boolean,
    onSelect: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalHainColors.current
    val haptics = rememberHainHaptics()
    val position = stringResource(R.string.chat_branch_position, info.index, info.count)
    Row(modifier.semantics { contentDescription = position }, verticalAlignment = Alignment.CenterVertically) {
        BranchArrow(
            label = stringResource(R.string.chat_branch_previous),
            target = info.previousId,
            enabled = enabled,
            turn = HALF_TURN,
        ) {
            haptics.tick()
            onSelect(it)
        }
        // T-272: plain text, no cross-fade: two numbers on top of each other ("2" over "3") looked broken
        BasicText(
            "${info.index}/${info.count}",
            style = LocalHainTypography.current.label.copy(color = colors.textMuted),
            maxLines = 1,
            softWrap = false,
        )
        BranchArrow(
            label = stringResource(R.string.chat_branch_next),
            target = info.nextId,
            enabled = enabled,
            turn = 0f,
        ) {
            haptics.tick()
            onSelect(it)
        }
    }
}

@Composable
private fun BranchArrow(
    label: String,
    target: Long?,
    enabled: Boolean,
    turn: Float,
    onSelect: (Long) -> Unit,
) {
    val usable = enabled && target != null
    Box(
        Modifier
            .size(TOUCH.dp)
            .clip(CircleShape)
            .clickable(enabled = usable, role = Role.Button, onClickLabel = label) { target?.let(onSelect) }
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        Image(
            HainIcons.chevronRight,
            contentDescription = null,
            colorFilter = ColorFilter.tint(LocalHainColors.current.textMuted),
            modifier = Modifier.size(ICON.dp).rotate(turn).alpha(if (usable) 1f else DIMMED),
        )
    }
}

private const val TOUCH = 40
private const val ICON = 16
private const val HALF_TURN = 180f
private const val DIMMED = 0.35f
