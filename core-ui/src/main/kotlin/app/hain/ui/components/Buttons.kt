package app.hain.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import app.hain.ui.motion.LocalReduceMotion
import app.hain.ui.theme.LocalHainColors
import app.hain.ui.theme.LocalHainShapes
import app.hain.ui.theme.LocalHainSpacing
import app.hain.ui.theme.LocalHainTypography

/** Text button (DESIGN_SYSTEM.md §3): primary = green, secondary = outlined, ghost = text only. */
@Composable
fun HainButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: HainButtonStyle = HainButtonStyle.PRIMARY,
    enabled: Boolean = true,
) {
    val colors = LocalHainColors.current
    val shape = LocalHainShapes.current.button
    val spacing = LocalHainSpacing.current
    val (bg, fg) =
        when (style) {
            HainButtonStyle.PRIMARY -> colors.accent to colors.onAccent
            HainButtonStyle.SECONDARY -> Color.Transparent to colors.text
            HainButtonStyle.GHOST -> Color.Transparent to colors.accent
        }
    // light 3D touch (2e): primary pills are slightly domed and press in on tap
    val fill =
        if (style == HainButtonStyle.PRIMARY) {
            Brush.verticalGradient(listOf(lerp(bg, Color.White, DOME_LIGHT), bg, lerp(bg, Color.Black, DOME_SHADE)))
        } else {
            SolidColor(bg)
        }
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val reduce = LocalReduceMotion.current
    val scale by animateFloatAsState(
        if (pressed && !reduce) PRESSED_SCALE else 1f,
        spring(dampingRatio = PRESS_DAMPING, stiffness = PRESS_STIFFNESS),
        label = "button-press",
    )
    Box(
        modifier =
            modifier
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }.defaultMinSize(minHeight = 48.dp)
                .alpha(if (enabled) 1f else DISABLED_ALPHA)
                .clip(shape)
                .background(fill, shape)
                .then(if (style == HainButtonStyle.SECONDARY) Modifier.border(1.dp, colors.border, shape) else Modifier)
                .clickable(
                    interactionSource = interaction,
                    indication = null,
                    enabled = enabled,
                    role = Role.Button,
                    onClick = onClick,
                ).padding(horizontal = spacing.lg, vertical = spacing.md),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(text, style = LocalHainTypography.current.label.copy(color = fg))
    }
}

/** 40 dp round icon button with a 20 dp Lucide icon. */
@Suppress("LongParameterList") // optional styling parameters with defaults
@Composable
fun HainIconButton(
    icon: Painter,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = LocalHainColors.current.text,
    background: Color = Color.Transparent,
    enabled: Boolean = true,
) {
    Box(
        modifier =
            modifier
                .size(40.dp)
                .alpha(if (enabled) 1f else DISABLED_ALPHA)
                .clip(LocalHainShapes.current.composer)
                .background(background)
                .clickable(enabled = enabled, role = Role.Button, onClickLabel = contentDescription, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = icon,
            contentDescription = contentDescription,
            colorFilter = ColorFilter.tint(tint),
            modifier = Modifier.size(20.dp),
        )
    }
}

/** Small row helper used by menus and drawers: icon + label, full width, clickable. */
@Composable
fun HainMenuRow(
    icon: Painter?,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
) {
    val colors = LocalHainColors.current
    val spacing = LocalHainSpacing.current
    Row(
        modifier =
            modifier
                .clip(LocalHainShapes.current.button)
                .background(if (selected) colors.surfaceHigh else Color.Transparent)
                .clickable(onClick = onClick)
                .padding(horizontal = spacing.md, vertical = spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Image(icon, null, colorFilter = ColorFilter.tint(colors.textMuted), modifier = Modifier.size(20.dp))
            Box(Modifier.size(spacing.md))
        }
        BasicText(label, style = LocalHainTypography.current.bodySmall.copy(color = colors.text), maxLines = 1)
    }
}

internal const val DISABLED_ALPHA = 0.4f

private const val PRESSED_SCALE = 0.96f
private const val PRESS_DAMPING = 0.5f
private const val PRESS_STIFFNESS = 600f
private const val DOME_LIGHT = 0.14f
private const val DOME_SHADE = 0.08f
