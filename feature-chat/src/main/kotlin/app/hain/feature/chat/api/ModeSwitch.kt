package app.hain.feature.chat.api

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import app.hain.common.api.AnswerMode
import app.hain.feature.chat.R
import app.hain.ui.motion.HainMotion
import app.hain.ui.motion.hainTween
import app.hain.ui.theme.LocalHainColors
import app.hain.ui.theme.LocalHainSpacing
import app.hain.ui.theme.LocalHainTypography

/** Small switch above the message box: Auto / Fast / Thorough. Changes apply to the next message. */
@Composable
fun ModeSwitch(
    mode: AnswerMode,
    onMode: (AnswerMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = LocalHainSpacing.current
    Row(
        modifier.padding(horizontal = spacing.md),
        horizontalArrangement = Arrangement.spacedBy(spacing.xs),
    ) {
        ModeChip(R.string.chat_mode_auto, mode == AnswerMode.AUTO) { onMode(AnswerMode.AUTO) }
        ModeChip(R.string.chat_mode_fast, mode == AnswerMode.FAST) { onMode(AnswerMode.FAST) }
        ModeChip(R.string.chat_mode_thorough, mode == AnswerMode.THOROUGH) { onMode(AnswerMode.THOROUGH) }
    }
}

@Composable
private fun ModeChip(
    label: Int,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val colors = LocalHainColors.current
    val spacing = LocalHainSpacing.current
    val bg by animateColorAsState(
        if (selected) colors.accent else Color.Transparent,
        hainTween(HainMotion.FAST_MS),
        label = "mode-bg",
    )
    BasicText(
        stringResource(label),
        style = LocalHainTypography.current.label.copy(color = if (selected) colors.onAccent else colors.textMuted),
        modifier =
            Modifier
                .clip(RoundedCornerShape(percent = 50))
                .background(bg)
                .semantics {
                    role = Role.RadioButton
                    this.selected = selected
                }.clickable(onClick = onClick)
                .padding(horizontal = spacing.md, vertical = spacing.xs),
    )
}
