package app.hain.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.hain.ui.HainIcons
import app.hain.ui.theme.LocalHainColors
import app.hain.ui.theme.LocalHainShapes
import app.hain.ui.theme.LocalHainSpacing
import app.hain.ui.theme.LocalHainTypography
import app.hain.ui.theme.isHainDark

/**
 * The message box (T-011): a calm card in the active style (see composerCard). The text grows up
 * to [MAX_LINES] lines and then scrolls inside; below it a row with [leading] controls (the thinking
 * picker) on the left and the round send button on the right, which turns into stop while an
 * answer streams. The buttons never scroll away, however long the text gets (T-085).
 */
@Suppress("LongParameterList") // slot-light composable: every label/callback is a parameter
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
    leading: @Composable RowScope.() -> Unit = {},
    // Häppchen 10: the mic sits right next to send
    trailing: @Composable RowScope.() -> Unit = {},
) {
    val colors = LocalHainColors.current
    val type = LocalHainTypography.current
    val spacing = LocalHainSpacing.current
    val shape = LocalHainShapes.current.composer
    Column(modifier.fillMaxWidth().composerCard(shape)) {
        Box(
            Modifier
                .fillMaxWidth()
                .padding(start = spacing.lg, end = spacing.lg, top = spacing.md, bottom = spacing.xs),
            contentAlignment = Alignment.TopStart,
        ) {
            if (text.isEmpty()) {
                BasicText(
                    placeholder,
                    style = type.body.copy(color = colors.textMuted),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            BasicTextField(
                value = text,
                onValueChange = onTextChange,
                enabled = enabled,
                textStyle = type.body.copy(color = colors.text),
                cursorBrush = SolidColor(colors.accent),
                minLines = 1,
                maxLines = MAX_LINES,
                modifier = Modifier.fillMaxWidth().heightIn(min = 24.dp),
            )
        }
        Row(
            Modifier.fillMaxWidth().padding(start = spacing.sm, end = spacing.sm, bottom = spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            leading()
            Spacer(Modifier.weight(1f))
            trailing()
            AnimatedContent(targetState = isGenerating, label = "send-stop") { generating ->
                if (generating) {
                    DomeIconButton(
                        icon = HainIcons.stop,
                        contentDescription = stopLabel,
                        onClick = onStop,
                        color = colors.text,
                        tint = colors.bg,
                    )
                } else {
                    DomeIconButton(
                        icon = HainIcons.send,
                        contentDescription = sendLabel,
                        onClick = onSend,
                        enabled = enabled && text.isNotBlank(),
                    )
                }
            }
        }
    }
}

/** The message box: page-coloured card, hairline edge, a faint 2 dp shadow (Lichtung). */
@Composable
private fun Modifier.composerCard(shape: Shape): Modifier {
    val colors = LocalHainColors.current
    return this
        .shadow(A_ELEVATION.dp, shape, clip = false, ambientColor = colors.shadow, spotColor = colors.shadow)
        .clip(shape)
        .background(if (isHainDark()) colors.surface else colors.bg, shape)
        .border(1.dp, colors.border, shape)
}

private const val MAX_LINES = 6
private const val A_ELEVATION = 2
