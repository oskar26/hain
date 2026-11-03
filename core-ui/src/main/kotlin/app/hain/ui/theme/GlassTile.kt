package app.hain.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.max

/**
 * Glass tile with an inner coloured light (T-011, DESIGN_LANGUAGE.md §4): strongly rounded, white
 * (or dark) glass, the [hue] shines from inside around [lightAt] (fractions of the tile, default
 * bottom centre), a bright light edge along the top, a glossy sheen over the upper half and a soft
 * shadow in the same colour below. Light always comes from the top left.
 *
 * Use it for every "object" of the app: settings groups, tool and answer cards, packs, the composer,
 * menus. The chat text itself never sits on one. [floorGlow] off for popups, whose window would cut it.
 */
@Composable
fun Modifier.glassTile(
    hue: GlowHue = GlowHue.CLEAR,
    shape: Shape = LocalHainShapes.current.tile,
    elevation: Dp = TILE_ELEVATION.dp,
    lightAt: Offset = Offset(x = 0.5f, y = 1f),
    floorGlow: Boolean = true,
): Modifier {
    val dark = isHainDark()
    val s = glowStrength(dark)
    val base = if (dark) DARK_GLASS else Color.White
    val edge =
        Brush.verticalGradient(
            listOf(
                Color.White.copy(alpha = s.edge),
                hue.core.copy(alpha = EDGE_TINT),
            ),
        )
    return this
        .then(if (floorGlow) Modifier.drawBehind { drawFloorGlow(hue.core.copy(alpha = s.shadow)) } else Modifier)
        .shadow(
            elevation,
            shape,
            clip = false,
            ambientColor = hue.core.copy(alpha = s.shadow),
            spotColor = hue.core.copy(alpha = s.shadow),
        ).clip(shape)
        .background(base, shape)
        .drawBehind {
            drawInnerLight(hue, s, dark, lightAt)
            drawSheen(s.sheen)
        }.border(EDGE_WIDTH.dp, edge, shape)
}

/** The soft coloured glow on the "floor" below a tile: wider than tall, a little lower than the tile. */
internal fun DrawScope.drawFloorGlow(color: Color) {
    val w = size.width
    val h = size.height
    val r = h * FLOOR_RADIUS + FLOOR_EXTRA.dp.toPx()
    val centre = Offset(w / 2, h * FLOOR_CENTRE)
    val stretch = max(1f, (w * FLOOR_WIDTH) / r)
    scale(scaleX = stretch, scaleY = 1f, pivot = centre) {
        drawCircle(
            Brush.radialGradient(listOf(color, Color.Transparent), center = centre, radius = r),
            radius = r,
            center = centre,
        )
    }
}

/** The coloured light inside the glass: a saturated core that fades through the pale halo. */
internal fun DrawScope.drawInnerLight(
    hue: GlowHue,
    s: GlowStrength,
    dark: Boolean,
    lightAt: Offset,
) {
    val centre = Offset(size.width * lightAt.x, size.height * lightAt.y)
    val radius = max(size.width, size.height) * INNER_RADIUS
    val brush =
        if (dark) {
            Brush.radialGradient(
                0f to hue.core.copy(alpha = s.inner),
                HALO_STOP to hue.core.copy(alpha = s.inner * DARK_HALO),
                1f to Color.Transparent,
                center = centre,
                radius = radius,
            )
        } else {
            Brush.radialGradient(
                0f to hue.core.copy(alpha = s.inner * CORE_ON_WHITE),
                HALO_STOP to hue.light.copy(alpha = s.inner),
                1f to hue.light.copy(alpha = 0f),
                center = centre,
                radius = radius,
            )
        }
    drawRect(brush)
}

/** Glossy sheen: white fading out over the upper part, like light on a glass dome. */
internal fun DrawScope.drawSheen(alpha: Float) {
    if (alpha <= 0f) return
    drawRect(
        Brush.verticalGradient(
            0f to Color.White.copy(alpha = alpha),
            SHEEN_END to Color.White.copy(alpha = 0f),
        ),
    )
}

private val DARK_GLASS = Color(color = 0xF51A211D)
private const val TILE_ELEVATION = 10
private const val EDGE_WIDTH = 1
private const val EDGE_TINT = 0.16f
private const val FLOOR_RADIUS = 0.42f
private const val FLOOR_EXTRA = 14
private const val FLOOR_CENTRE = 0.72f
private const val FLOOR_WIDTH = 0.56f
private const val INNER_RADIUS = 0.85f
private const val HALO_STOP = 0.38f
private const val CORE_ON_WHITE = 0.38f
private const val DARK_HALO = 0.35f
private const val SHEEN_END = 0.5f
