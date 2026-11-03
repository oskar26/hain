package app.hain.ui.components

import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.rememberTransition
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import app.hain.ui.motion.HainMotion
import app.hain.ui.motion.LocalReduceMotion
import app.hain.ui.motion.hainTween
import app.hain.ui.theme.HainLevel
import app.hain.ui.theme.LocalHainColors
import app.hain.ui.theme.LocalHainShapes
import app.hain.ui.theme.LocalHainSpacing
import app.hain.ui.theme.LocalHainTypography
import app.hain.ui.theme.hainSurface

/**
 * Compact context menu as a quiet glass tile (T-053, docs/design/DESIGN_LANGUAGE.md "floating"):
 * place it inside the same Box as the button or message it belongs to. It grows out of its anchor
 * with scale + fade in [HainMotion.MENU_MS] ([growFrom] = the corner nearest the anchor), is only as
 * wide as its rows, shrinks back the same way and closes on outside tap or back. No motion when
 * motion is reduced.
 */
@Suppress("LongParameterList") // placement options all have defaults
@Composable
fun HainPopupMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    alignment: Alignment = Alignment.TopEnd,
    offset: IntOffset = IntOffset.Zero,
    growFrom: TransformOrigin = TransformOrigin(1f, 0f),
    content: @Composable ColumnScope.() -> Unit,
) {
    val reduce = LocalReduceMotion.current
    val shown = remember { MutableTransitionState(false) }
    shown.targetState = expanded
    // stays in the window until the fade-out has finished
    if (!shown.currentState && !shown.targetState) return
    val transition = rememberTransition(shown, label = "menu")
    val progress =
        transition.animateFloat({ hainTween(HainMotion.MENU_MS, reduce) }, label = "menu-in") { open ->
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
                // room for the soft shadow inside the popup window
                .padding(SHADOW_ROOM.dp)
                .graphicsLayer {
                    val p = progress.value
                    alpha = p
                    val s = HainMotion.MENU_START_SCALE + (1f - HainMotion.MENU_START_SCALE) * p
                    scaleX = s
                    scaleY = s
                    transformOrigin = growFrom
                }.widthIn(min = MIN_WIDTH.dp, max = MAX_WIDTH.dp)
                .hainSurface(HainLevel.FLOATING, LocalHainShapes.current.menu)
                .padding(LocalHainSpacing.current.xs),
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
            .fillMaxWidth()
            .clip(LocalHainShapes.current.row)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = spacing.md, vertical = ITEM_VERTICAL.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Image(
                icon,
                null,
                colorFilter = ColorFilter.tint(if (danger) colors.danger else colors.icon),
                modifier = Modifier.size(ICON.dp),
            )
            Spacer(Modifier.width(spacing.md))
        }
        BasicText(label, style = LocalHainTypography.current.body.copy(color = tint), maxLines = 1)
    }
}

private const val MIN_WIDTH = 176
private const val MAX_WIDTH = 260
private const val ICON = 18
private const val ITEM_VERTICAL = 10
private const val SHADOW_ROOM = 12
