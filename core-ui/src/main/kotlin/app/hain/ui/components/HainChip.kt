package app.hain.ui.components

import android.content.res.Configuration
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.hain.ui.motion.LocalReduceMotion
import app.hain.ui.theme.HainLevel
import app.hain.ui.theme.HainTheme
import app.hain.ui.theme.LocalHainColors
import app.hain.ui.theme.LocalHainShapes
import app.hain.ui.theme.LocalHainSpacing
import app.hain.ui.theme.LocalHainTypography
import app.hain.ui.theme.hainSurface

/**
 * Small rounded label (T-014, DESIGN_SYSTEM.md §3): a source, a filter or a tool step. [selected]
 * fills it in Hain green; [dot] puts a small coloured point in front (orange = went online).
 */
@Suppress("LongParameterList") // all optional, like a Compose chip
@Composable
fun HainChip(
    text: String,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    dot: Color? = null,
    leading: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
) {
    val colors = LocalHainColors.current
    val shape = LocalHainShapes.current.chip
    val spacing = LocalHainSpacing.current
    val background = if (selected) colors.accentSoft else colors.surface
    val textColor = if (selected) colors.accent else colors.text
    Row(
        modifier
            .hainSurface(HainLevel.FLAT, shape, fill = background)
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
            .padding(horizontal = spacing.md, vertical = spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (dot != null) Box(Modifier.size(7.dp).background(dot, CircleShape))
        leading?.invoke()
        BasicText(
            text,
            style = LocalHainTypography.current.label.copy(color = textColor),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.widthIn(max = 220.dp),
        )
    }
}

/** A slowly pulsing point while something runs; stands still when motion is reduced. */
@Composable
fun PulsingDot(
    color: Color,
    modifier: Modifier = Modifier,
) {
    val alpha =
        if (LocalReduceMotion.current) {
            1f
        } else {
            val pulse by rememberInfiniteTransition(label = "pulse").animateFloat(
                initialValue = 1f,
                targetValue = 0.3f,
                animationSpec = infiniteRepeatable(tween(PULSE_MS), RepeatMode.Reverse),
                label = "pulse-alpha",
            )
            pulse
        }
    Box(modifier.size(7.dp).alpha(alpha).background(color, CircleShape))
}

private const val PULSE_MS = 700

@Preview(name = "Light — chips")
@Preview(name = "Dark — chips", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
internal fun HainChipPreview() {
    HainTheme {
        Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            HainChip("Open-Meteo", dot = LocalHainColors.current.online)
            HainChip("Alle", selected = true)
            HainChip("Wikipedia")
        }
    }
}
