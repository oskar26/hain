package app.hain.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * The three depth levels of the whole app (T-010, docs/design/DESIGN_LANGUAGE.md). Every surface
 * picks one of these instead of its own shadow or border, so Hain feels like one calm piece.
 * - [FLAT]: soft grey fill, no edge (bubbles, search field, chips at rest)
 * - [RAISED]: page-coloured card with a light top edge and a soft shadow (secondary pills, cards)
 * - [FLOATING]: clear glass tile ([glassTile]) with sheen, light edge and a deep soft shadow (menus, popovers)
 * Coloured glass tiles with inner light use [glassTile] directly (T-011).
 */
enum class HainLevel { FLAT, RAISED, FLOATING }

/** Shadow size of each level; pure so it is unit-testable. */
fun HainLevel.shadowDp(): Dp =
    when (this) {
        HainLevel.FLAT -> 0.dp
        HainLevel.RAISED -> RAISED_SHADOW.dp
        HainLevel.FLOATING -> FLOATING_SHADOW.dp
    }

/**
 * Paints a surface of the given [level] in [shape]. [fill] overrides the level's colour
 * (for example the accent-soft selection).
 */
@Composable
fun Modifier.hainSurface(
    level: HainLevel,
    shape: Shape = LocalHainShapes.current.card,
    fill: Color? = null,
): Modifier {
    val c = LocalHainColors.current
    return when (level) {
        HainLevel.FLAT -> clip(shape).background(fill ?: c.surface, shape)
        // T-011 "calm, but with depth": a real soft shadow and a light top edge instead of a flat hairline
        HainLevel.RAISED ->
            shadow(level.shadowDp(), shape, clip = false, ambientColor = c.shadow, spotColor = c.shadow)
                .clip(shape)
                .background(fill ?: c.bg, shape)
                .border(HAIRLINE.dp, Brush.verticalGradient(listOf(c.glassEdge, c.border)), shape)
        HainLevel.FLOATING ->
            if (fill == null) {
                glassTile(GlowHue.CLEAR, shape, level.shadowDp(), floorGlow = false)
            } else {
                shadow(level.shadowDp(), shape, clip = false, ambientColor = c.shadow, spotColor = c.shadow)
                    .clip(shape)
                    .background(fill, shape)
                    .border(HAIRLINE.dp, Brush.verticalGradient(listOf(c.glassEdge, c.border)), shape)
            }
    }
}

private const val HAIRLINE = 1
private const val RAISED_SHADOW = 6
private const val FLOATING_SHADOW = 18
