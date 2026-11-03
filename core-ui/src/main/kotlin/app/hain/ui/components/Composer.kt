package app.hain.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import app.hain.ui.HainIcons
import app.hain.ui.theme.HainLevel
import app.hain.ui.theme.LocalHainColors
import app.hain.ui.theme.LocalHainShapes
import app.hain.ui.theme.LocalHainSpacing
import app.hain.ui.theme.LocalHainTypography
import app.hain.ui.theme.hainSurface

/**
 * The pill-shaped message box (DESIGN_SYSTEM.md §3), a raised white capsule (T-010 redesign):
 * text, then send — which morphs into stop
 * while an answer is streaming.
 */
@Suppress("LongParameterList") // slot-free composable: every label/callback is a parameter
@Composable
fun Composer(
    text: String,
    onTextChange: (String) -> Unit,
    placeholder: String,
    isGenerating: Boolean,
    onSend: () -> Unit,
    onStop: () -> Unit,
    sendLabel: String,
    stopLabel: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val colors = LocalHainColors.current
    val type = LocalHainTypography.current
    val spacing = LocalHainSpacing.current
    val shape = LocalHainShapes.current.composer
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .hainSurface(HainLevel.RAISED, shape)
                .padding(start = spacing.lg, end = spacing.xs, top = spacing.xs, bottom = spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.weight(1f).padding(vertical = spacing.sm), contentAlignment = Alignment.CenterStart) {
            if (text.isEmpty()) {
                BasicText(placeholder, style = type.body.copy(color = colors.textMuted), maxLines = 1)
            }
            BasicTextField(
                value = text,
                onValueChange = onTextChange,
                enabled = enabled,
                textStyle = type.body.copy(color = colors.text),
                cursorBrush = SolidColor(colors.accent),
                maxLines = MAX_LINES,
                modifier = Modifier.fillMaxWidth().heightIn(min = 24.dp),
            )
        }
        AnimatedContent(targetState = isGenerating, label = "send-stop") { generating ->
            if (generating) {
                HainIconButton(
                    icon = HainIcons.stop,
                    contentDescription = stopLabel,
                    onClick = onStop,
                    tint = colors.bg,
                    background = colors.text,
                )
            } else {
                HainIconButton(
                    icon = HainIcons.send,
                    contentDescription = sendLabel,
                    onClick = onSend,
                    enabled = enabled && text.isNotBlank(),
                    tint = colors.onAccent,
                    background = colors.accent,
                )
            }
        }
    }
}

private const val MAX_LINES = 6
