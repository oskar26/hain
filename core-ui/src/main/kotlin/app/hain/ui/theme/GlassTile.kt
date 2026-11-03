package app.hain.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
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
 * A panel: the calm surface for every "object" of the app (settings groups, tool and answer cards,
 * packs, suggestions). Since the style decision (03.11.2026, STYLE_OPTIONS.md) panels are
 * monochrome: [hue] and [lightAt] are kept for the call sites but no colour shines from inside any
 * more, and there is no glass edge or sheen.
 *
 * - style A: page-coloured fill (or the soft grey in dark) with a 1 dp hairline, no shadow
 * - style B: translucent white layer over the mesh gradient, a faint white edge, a very soft shadow
 */
@Suppress("UnusedParameter") // hue/lightAt/floorGlow stay for source compatibility of the call sites
@Composable
fun Modifier.glassTile(
    hue: GlowHue = GlowHue.CLEAR,
    shape: Shape = LocalHainShapes.current.tile,
    elevation: Dp = TILE_ELEVATION.dp,
    lightAt: Offset = Offset(x = 0.5f, y = 1f),
    floorGlow: Boolean = true,
): Modifier = hainPanel(shape, raised = elevation > 0.dp)

/** The panel recipe of the current style. [raised] is kept for later (a lifted panel). */
@Suppress("UnusedParameter")
@Composable
fun Modifier.hainPanel(
    shape: Shape = LocalHainShapes.current.tile,
    raised: Boolean = true,
    fill: Color? = null,
): Modifier {
    val c = LocalHainColors.current
    val dark = isHainDark()
    return when (LocalHainStyle.current) {
        HainStyle.A ->
            clip(shape)
                .background(fill ?: if (dark) c.surface else c.bg, shape)
                .border(HAIRLINE.dp, c.border, shape)
        // no drop shadow: it would shine through the translucent layer and muddy it
        HainStyle.B ->
            clip(shape)
                .background(fill ?: c.surface, shape)
                .border(HAIRLINE.dp, c.glassEdge, shape)
    }
}

/**
 * The page behind a screen. A: the plain page colour. B: the page colour with large, very soft
 * mint and grey blobs (mesh-like, low saturation) that stay still while content scrolls over them.
 */
@Composable
fun Modifier.hainBackdrop(): Modifier {
    val c = LocalHainColors.current
    if (LocalHainStyle.current == HainStyle.A) return background(c.bg)
    val dark = isHainDark()
    val blobs = if (dark) DARK_BLOBS else LIGHT_BLOBS
    return background(c.bg).drawBehind { blobs.forEach { drawBlob(it) } }
}

/**
 * The one soft green glow of style A (empty chat, thinking): a wide, low radial light rising from
 * the bottom edge. [strength] 0..1. Style B has its mesh instead and draws nothing here.
 */
@Composable
fun Modifier.hainAmbientGlow(strength: Float = 1f): Modifier {
    if (LocalHainStyle.current != HainStyle.A || strength <= 0f) return this
    val accent = LocalHainColors.current.accent
    val alpha = (if (isHainDark()) GLOW_DARK else GLOW_LIGHT) * strength
    return drawBehind {
        drawFloorGlow(
            accent.copy(alpha = alpha),
            centreY = size.height * GLOW_Y,
            widthShare = GLOW_W,
        )
    }
}

/** One mesh blob: centre and radius as fractions of the screen, colour with its alpha. */
internal data class Blob(
    val x: Float,
    val y: Float,
    val r: Float,
    val color: Color,
)

private fun DrawScope.drawBlob(b: Blob) {
    val centre = Offset(size.width * b.x, size.height * b.y)
    val radius = max(size.width, size.height) * b.r
    drawCircle(
        Brush.radialGradient(
            listOf(b.color, b.color.copy(alpha = 0f)),
            center = centre,
            radius = radius,
        ),
        radius = radius,
        center = centre,
    )
}

/** A soft glow on the "floor": wider than tall, centred at [centreY]. */
internal fun DrawScope.drawFloorGlow(
    color: Color,
    centreY: Float = size.height * FLOOR_CENTRE,
    widthShare: Float = FLOOR_WIDTH,
) {
    val w = size.width
    val r = size.height * FLOOR_RADIUS + FLOOR_EXTRA.dp.toPx()
    val centre = Offset(w / 2, centreY)
    val stretch = max(1f, (w * widthShare) / r)
    scale(scaleX = stretch, scaleY = 1f, pivot = centre) {
        drawCircle(
            Brush.radialGradient(listOf(color, Color.Transparent), center = centre, radius = r),
            radius = r,
            center = centre,
        )
    }
}

private val LIGHT_BLOBS =
    listOf(
        Blob(x = 0.05f, y = 0.02f, r = 0.62f, color = Color(color = 0x99CDEBDB)),
        Blob(x = 1.0f, y = 0.38f, r = 0.55f, color = Color(color = 0x80DCE3E0)),
        Blob(x = 0.25f, y = 1.0f, r = 0.6f, color = Color(color = 0x8CD3EEDF)),
    )

private val DARK_BLOBS =
    listOf(
        Blob(x = 0.0f, y = 0.0f, r = 0.65f, color = Color(color = 0x6614402B)),
        Blob(x = 1.0f, y = 0.45f, r = 0.55f, color = Color(color = 0x4D1C2622)),
        Blob(x = 0.3f, y = 1.0f, r = 0.6f, color = Color(color = 0x59123A27)),
    )

private const val TILE_ELEVATION = 10
private const val HAIRLINE = 1
private const val FLOOR_RADIUS = 0.42f
private const val FLOOR_EXTRA = 14
private const val FLOOR_CENTRE = 0.72f
private const val FLOOR_WIDTH = 0.56f
private const val GLOW_LIGHT = 0.16f
private const val GLOW_DARK = 0.22f
private const val GLOW_Y = 1.0f
private const val GLOW_W = 0.9f
