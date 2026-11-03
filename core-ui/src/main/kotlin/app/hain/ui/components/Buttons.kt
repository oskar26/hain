package app.hain.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.hain.ui.motion.LocalReduceMotion
import app.hain.ui.motion.rememberHainHaptics
import app.hain.ui.theme.HainLevel
import app.hain.ui.theme.LocalHainColors
import app.hain.ui.theme.LocalHainShapes
import app.hain.ui.theme.LocalHainSpacing
import app.hain.ui.theme.LocalHainStyle
import app.hain.ui.theme.LocalHainTypography
import app.hain.ui.theme.hainSurface
import app.hain.ui.theme.look

/**
 * Text button (T-011): a domed capsule. Primary = Hain green with a clear inner gradient, a light
 * top edge and a soft green shadow; dark = the same dome in almost black; secondary = raised white
 * pill; ghost = text only. All press in (scale 0.96) with a light tick.
 */
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
    val fg =
        when (style) {
            HainButtonStyle.PRIMARY -> colors.onAccent
            HainButtonStyle.DARK -> colors.bg
            HainButtonStyle.SECONDARY -> colors.text
            HainButtonStyle.GHOST -> colors.accent
        }
    val interaction = remember { MutableInteractionSource() }
    val scale = pressScale(interaction)
    val haptics = rememberHainHaptics()
    Box(
        modifier =
            modifier
                .graphicsLayer {
                    scaleX = scale.value
                    scaleY = scale.value
                }.defaultMinSize(minHeight = 52.dp)
                .alpha(if (enabled) 1f else DISABLED_ALPHA)
                .then(
                    when (style) {
                        HainButtonStyle.PRIMARY -> Modifier.dome(colors.accent, shape)
                        HainButtonStyle.DARK -> Modifier.dome(colors.text, shape)
                        HainButtonStyle.SECONDARY -> Modifier.hainSurface(HainLevel.RAISED, shape)
                        HainButtonStyle.GHOST -> Modifier.clip(shape)
                    },
                ).clickable(
                    interactionSource = interaction,
                    indication = null,
                    enabled = enabled,
                    role = Role.Button,
                    onClick = {
                        if (style != HainButtonStyle.GHOST) haptics.send()
                        onClick()
                    },
                ).padding(horizontal = spacing.lg, vertical = spacing.md),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(
            text,
            style = LocalHainTypography.current.button.copy(color = fg, textAlign = TextAlign.Center),
        )
    }
}

/**
 * Filled button surface in [color] (buttons, send, the Pim knob), calm since the style proposals
 * (docs/design/STYLE_OPTIONS.md): no sheen and no glossy edge any more.
 * - Stil A "Lichtung": flat [color], no shadow.
 * - Stil B "Morgennebel": a gentle gradient (a touch lighter on top) and a soft shadow in [color].
 * [elevation] 0 turns the shadow off (disabled buttons).
 */
@Composable
fun Modifier.dome(
    color: Color,
    shape: Shape,
    elevation: Dp = DOME_ELEVATION.dp,
): Modifier {
    val look = LocalHainStyle.current.look()
    if (!look.buttonGradient) return clip(shape).background(color, shape)
    val lift = if (elevation > 0.dp) look.buttonShadowDp.dp else 0.dp
    val top = lerp(color, Color.White, DOME_LIGHT)
    val bottom = lerp(color, Color.Black, DOME_SHADE)
    return this
        .shadow(
            lift,
            shape,
            clip = false,
            ambientColor = color.copy(alpha = DOME_SHADOW),
            spotColor = color.copy(alpha = DOME_SHADOW),
        ).clip(shape)
        .background(Brush.verticalGradient(listOf(top, color, bottom)), shape)
}

/** Press-in spring (scale 0.96) shared by every tappable dome; still with reduced motion. */
@Composable
fun pressScale(interaction: MutableInteractionSource): State<Float> {
    val pressed by interaction.collectIsPressedAsState()
    val reduce = LocalReduceMotion.current
    return animateFloatAsState(
        if (pressed && !reduce) PRESSED_SCALE else 1f,
        spring(dampingRatio = PRESS_DAMPING, stiffness = PRESS_STIFFNESS),
        label = "press",
    )
}

/** Round domed icon button (send, Pim knob): [color] dome, white icon, presses in with a tick. */
@Suppress("LongParameterList") // optional styling parameters with defaults
@Composable
fun DomeIconButton(
    icon: Painter,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = LocalHainColors.current.accent,
    tint: Color = LocalHainColors.current.onAccent,
    enabled: Boolean = true,
    size: Dp = 44.dp,
) {
    val interaction = remember { MutableInteractionSource() }
    val scale = pressScale(interaction)
    val haptics = rememberHainHaptics()
    val c = LocalHainColors.current
    // disabled: a quiet grey dome instead of a washed-out green one
    val fill = if (enabled) color else c.surfaceHigh
    Box(
        modifier
            .graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
            }.size(size)
            .dome(fill, CircleShape, if (enabled) DOME_ELEVATION.dp else 0.dp)
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onClickLabel = contentDescription,
                onClick = {
                    haptics.send()
                    onClick()
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            icon,
            contentDescription,
            colorFilter = ColorFilter.tint(if (enabled) tint else c.icon),
            modifier = Modifier.size(size * ICON_SHARE),
        )
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
    iconSize: Dp = 20.dp,
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
            modifier = Modifier.size(iconSize),
        )
    }
}

/** Row used by drawers and lists: icon + label, rounded row shape, [selected] = soft grey fill. */
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
                .clip(LocalHainShapes.current.row)
                .background(if (selected) colors.surface else Color.Transparent)
                .clickable(onClick = onClick)
                .padding(horizontal = spacing.md, vertical = spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Image(icon, null, colorFilter = ColorFilter.tint(colors.icon), modifier = Modifier.size(20.dp))
            Box(Modifier.size(spacing.md))
        }
        BasicText(
            label,
            style = LocalHainTypography.current.body.copy(color = colors.text),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

internal const val DISABLED_ALPHA = 0.4f

private const val PRESSED_SCALE = 0.96f
private const val PRESS_DAMPING = 0.5f
private const val PRESS_STIFFNESS = 600f
private const val DOME_LIGHT = 0.10f
private const val DOME_SHADE = 0.06f
private const val DOME_SHADOW = 0.30f
private const val DOME_ELEVATION = 8
private const val ICON_SHARE = 0.45f
