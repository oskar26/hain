package app.hain.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Every "object" of the app (settings groups, tool and answer cards, packs, menus) sits on one of
 * these. Since Lichtung (docs/design/STYLE_OPTIONS.md) there is no coloured glass any more:
 * [hue] and [floorGlow] stay for the call sites, but every tile is a calm [calmTile].
 * The chat text itself never sits on one.
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

/** A calm Lichtung surface: page-coloured (dark: surface) fill, one hairline edge, a 1 dp shadow. */
@Suppress("UnusedParameter", "UNUSED_PARAMETER") // lightAt: kept so call sites stay stable
@Composable
fun Modifier.calmTile(
    shape: Shape = LocalHainShapes.current.tile,
    lightAt: Offset = Offset(x = 0.5f, y = 0f),
    shadow: Dp? = null,
    fill: Color? = null,
): Modifier {
    val c = LocalHainColors.current
    val lift = shadow ?: Lichtung.TILE_SHADOW_DP.dp
    return this
        .shadow(lift, shape, clip = false, ambientColor = c.shadow, spotColor = c.shadow)
        .clip(shape)
        .background(fill ?: if (isHainDark()) c.surface else c.bg, shape)
        .border(EDGE_WIDTH.dp, c.border, shape)
}

private const val TILE_ELEVATION = 10
private const val EDGE_WIDTH = 1
