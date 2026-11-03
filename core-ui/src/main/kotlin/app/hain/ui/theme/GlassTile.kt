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
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.max

/**
 * Every "object" of the app (settings groups, tool and answer cards, packs, menus) sits on one of
 * these. Since the style proposals (docs/design/STYLE_OPTIONS.md) there is no coloured glass any
 * more: [hue] and [floorGlow] stay for the call sites, but every tile is lit the calm way of the
 * active [HainStyle] (see [calmTile]). The chat text itself never sits on one.
 */
@Suppress("UnusedParameter", "UNUSED_PARAMETER") // hue/elevation/floorGlow: kept so call sites stay stable
@Composable
fun Modifier.glassTile(
    hue: GlowHue = GlowHue.CLEAR,
    shape: Shape = LocalHainShapes.current.tile,
    elevation: Dp = TILE_ELEVATION.dp,
    lightAt: Offset = Offset(x = 0.5f, y = 1f),
    floorGlow: Boolean = true,
): Modifier = calmTile(shape, lightAt)

/**
 * A calm surface in the active style.
 * - Stil A "Lichtung": page-coloured (dark: surface) fill, one hairline edge, a 1 dp shadow.
 * - Stil B "Morgennebel": a soft vertical gradient into pale green, a blurred green light around
 *   [lightAt] (fractions of the tile), a faint light edge on top and a soft green-grey shadow.
 */
@Composable
fun Modifier.calmTile(
    shape: Shape = LocalHainShapes.current.tile,
    lightAt: Offset = Offset(x = 0.5f, y = 0f),
    shadow: Dp? = null,
    fill: Color? = null,
): Modifier {
    val c = LocalHainColors.current
    val look = LocalHainStyle.current.look()
    val dark = isHainDark()
    val lift = shadow ?: look.tileShadowDp.dp
    val base =
        this
            .shadow(lift, shape, clip = false, ambientColor = c.shadow, spotColor = c.shadow)
            .clip(shape)
    if (!look.mist) {
        return base.background(fill ?: if (dark) c.surface else c.bg, shape).border(EDGE_WIDTH.dp, c.border, shape)
    }
    val top = fill ?: if (dark) c.surface else Color.White
    val bottom = lerp(top, c.accentSoft, MIST_SHARE)
    val glow = c.accent.copy(alpha = look.glowAlpha * if (dark) DARK_GLOW else 1f)
    val edge =
        Brush.verticalGradient(
            listOf(c.glassEdge.copy(alpha = if (dark) DARK_EDGE else 1f), c.border),
        )
    return base
        .background(Brush.verticalGradient(listOf(top, bottom)), shape)
        .drawBehind {
            drawRect(
                Brush.radialGradient(
                    listOf(glow, Color.Transparent),
                    center = Offset(size.width * lightAt.x, size.height * lightAt.y),
                    radius = max(size.width, size.height) * GLOW_RADIUS,
                ),
            )
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

private const val TILE_ELEVATION = 10
private const val MIST_SHARE = 0.55f
private const val DARK_GLOW = 1.6f
private const val DARK_EDGE = 0.08f
private const val GLOW_RADIUS = 0.9f
private const val EDGE_WIDTH = 1
private const val FLOOR_RADIUS = 0.42f
private const val FLOOR_EXTRA = 14
private const val FLOOR_CENTRE = 0.72f
private const val FLOOR_WIDTH = 0.56f
