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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import app.hain.ui.effects.frostedGlass
import app.hain.ui.effects.rememberMenuBackdrop
import app.hain.ui.motion.HainMotion
import app.hain.ui.motion.LocalReduceMotion
import app.hain.ui.motion.hainTween
import app.hain.ui.motion.rememberHainHaptics
import app.hain.ui.theme.LocalHainColors
import app.hain.ui.theme.LocalHainShapes
import app.hain.ui.theme.LocalHainSpacing
import app.hain.ui.theme.LocalHainTypography

/**
 * Compact context menu as a quiet glass tile (T-053, docs/design/DESIGN_LANGUAGE.md "floating"):
 * place it inside the same Box as the button or message it belongs to. It grows out of its anchor
 * with scale + fade in [HainMotion.MENU_MS] ([growFrom] = the corner nearest the anchor), is only as
 * wide as its rows, shrinks back the same way and closes on outside tap or back. No motion when
 * motion is reduced. The tile is frosted glass (T-210).
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
    val haptics = rememberHainHaptics()
    // one soft tick when a ⋯ / long-press menu opens, nothing when it closes
    LaunchedEffect(expanded) { if (expanded) haptics.menu() }
    // T-104/T-210: light dimming behind the open menu; the menu itself is frosted glass
    rememberMenuBackdrop(expanded)
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
        MenuTile(modifier, { progress.value }, growFrom, content)
    }
}

/**
 * Glass popover that opens *above* its anchor, start-aligned (T-084, like the model picker in
 * Gemini or ChatGPT): put it in the same Box as the button. Scale + fade from the bottom-left corner
 * in [HainMotion.MENU_MS]; it stays inside the window on small screens.
 */
@Composable
fun HainPopover(
    expanded: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val reduce = LocalReduceMotion.current
    rememberMenuBackdrop(expanded)
    val shown = remember { MutableTransitionState(false) }
    shown.targetState = expanded
    if (!shown.currentState && !shown.targetState) return
    val transition = rememberTransition(shown, label = "popover")
    val progress =
        transition.animateFloat({ hainTween(HainMotion.MENU_MS, reduce) }, label = "popover-in") { open ->
            if (open) 1f else 0f
        }
    val gap = with(LocalDensity.current) { POPOVER_GAP.dp.roundToPx() }
    Popup(
        popupPositionProvider = remember(gap) { AboveStartPosition(gap) },
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = true),
    ) {
        MenuTile(modifier, { progress.value }, TransformOrigin(0f, 1f), content)
    }
}

/** Places a popup right above the anchor, its left edge on the anchor's left edge, kept on screen. */
internal class AboveStartPosition(
    private val gap: Int,
) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset = popoverOffset(anchorBounds, windowSize, popupContentSize, gap)
}

/** Pure placement math (unit-tested): above the anchor, clamped into the window. */
internal fun popoverOffset(
    anchor: IntRect,
    window: IntSize,
    popup: IntSize,
    gap: Int,
): IntOffset {
    val x = anchor.left.coerceAtMost(window.width - popup.width).coerceAtLeast(0)
    val above = anchor.top - popup.height + gap
    val y = if (above >= 0) above else (anchor.bottom - gap).coerceAtMost(window.height - popup.height)
    return IntOffset(x, y.coerceAtLeast(0))
}

@Composable
private fun MenuTile(
    modifier: Modifier,
    progress: () -> Float,
    growFrom: TransformOrigin,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier
            // room for the soft shadow inside the popup window
            .padding(SHADOW_ROOM.dp)
            .graphicsLayer {
                val p = progress()
                alpha = p
                val s = HainMotion.MENU_START_SCALE + (1f - HainMotion.MENU_START_SCALE) * p
                scaleX = s
                scaleY = s
                transformOrigin = growFrom
            }.widthIn(min = MIN_WIDTH.dp, max = MAX_WIDTH.dp)
            // T-210: frosted glass, the screen behind shows through blurred
            .frostedGlass(LocalHainShapes.current.menu)
            .padding(LocalHainSpacing.current.xs),
        content = content,
    )
}

/**
 * The open menu card without its popup window, for the screenshot tests: there only the main
 * window really blurs, so they show it in the overlay of a `HainMenuBackdrop`.
 */
@Composable
internal fun HainMenuCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    MenuTile(modifier, { 1f }, TransformOrigin.Center, content)
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
        // T-085: long labels (German, big font) wrap to a second line instead of being cut off
        BasicText(
            label,
            style = LocalHainTypography.current.body.copy(color = tint),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private const val MIN_WIDTH = 176
private const val MAX_WIDTH = 320
private const val POPOVER_GAP = 8
private const val ICON = 18
private const val ITEM_VERTICAL = 10
private const val SHADOW_ROOM = 12
