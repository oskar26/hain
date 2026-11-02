package app.hain.ui.components

import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.rememberTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import app.hain.ui.motion.LocalReduceMotion
import app.hain.ui.theme.LocalHainColors
import app.hain.ui.theme.LocalHainShapes
import app.hain.ui.theme.LocalHainSpacing
import app.hain.ui.theme.LocalHainTypography

/**
 * Small menu that opens right at its anchor (2d-2): place it inside the same Box as the button or
 * message it belongs to. Only as wide as its rows, closes on outside tap or back.
 */
@Composable
fun HainPopupMenu(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    alignment: Alignment = Alignment.TopEnd,
    offset: IntOffset = IntOffset.Zero,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = LocalHainColors.current
    val shape = LocalHainShapes.current.card
    val reduce = LocalReduceMotion.current
    val shown = remember { MutableTransitionState(false).apply { targetState = true } }
    val transition = rememberTransition(shown, label = "menu")
    val progress =
        transition.animateFloat({ tween(if (reduce) 0 else OPEN_MS) }, label = "menu-in") { open ->
            if (open) 1f else 0f
        }
    Popup(
        alignment = alignment,
        offset = offset,
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = true),
    ) {
        Column(
            modifier
                .graphicsLayer {
                    val p = progress.value
                    alpha = p
                    scaleX = START_SCALE + (1f - START_SCALE) * p
                    scaleY = START_SCALE + (1f - START_SCALE) * p
                    transformOrigin = TransformOrigin(1f, 0f)
                }.widthIn(min = MIN_WIDTH.dp, max = MAX_WIDTH.dp)
                .shadow(SHADOW.dp, shape)
                .background(colors.bg, shape)
                .border(1.dp, colors.border, shape)
                .padding(vertical = LocalHainSpacing.current.xs),
            content = content,
        )
    }
}

/** One row of a [HainPopupMenu]: icon and label; [danger] paints it red. */
@Composable
fun HainPopupItem(
    icon: Painter?,
    label: String,
    onClick: () -> Unit,
    danger: Boolean = false,
) {
    val colors = LocalHainColors.current
    val spacing = LocalHainSpacing.current
    val tint = if (danger) colors.danger else colors.text
    Row(
        Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = spacing.md, vertical = ITEM_VERTICAL.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Image(icon, null, colorFilter = ColorFilter.tint(tint), modifier = Modifier.size(ICON.dp))
            Spacer(Modifier.width(spacing.md))
        }
        BasicText(label, style = LocalHainTypography.current.bodySmall.copy(color = tint), maxLines = 1)
    }
}

private const val OPEN_MS = 140
private const val START_SCALE = 0.92f
private const val MIN_WIDTH = 168
private const val MAX_WIDTH = 260
private const val SHADOW = 10
private const val ICON = 18
private const val ITEM_VERTICAL = 11
