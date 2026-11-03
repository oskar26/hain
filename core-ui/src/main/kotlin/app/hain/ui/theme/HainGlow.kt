package app.hain.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

/**
 * The soft colours that glow *inside* glass tiles (T-011, docs/design/DESIGN_LANGUAGE.md §4 "Light").
 * Every settings group, tool card and pack gets one hue; the tile stays white (or dark glass) and the
 * hue shines from within, like the widget tiles in Oskar's reference.
 *
 * - [core]: the saturated colour of the light (also tints the soft shadow under the tile)
 * - [light]: the pale halo near the light, used on white glass
 */
enum class GlowHue(
    val core: Color,
    val light: Color,
) {
    MINT(core = Color(color = 0xFF22C17E), light = Color(color = 0xFFC4F2DA)),
    SKY(core = Color(color = 0xFF4C9BFF), light = Color(color = 0xFFD3E6FF)),
    LILAC(core = Color(color = 0xFFA77BFF), light = Color(color = 0xFFE7DCFF)),
    PEACH(core = Color(color = 0xFFFF9A5C), light = Color(color = 0xFFFFE1CE)),
    SUN(core = Color(color = 0xFFF5C431), light = Color(color = 0xFFFFF0BF)),
    ROSE(core = Color(color = 0xFFFF6F91), light = Color(color = 0xFFFFDAE3)),
    TEAL(core = Color(color = 0xFF22B8B0), light = Color(color = 0xFFC8F1EE)),

    /** No colour: a clear glass tile (composer, menus). */
    CLEAR(core = Color(color = 0xFF7A9A88), light = Color(color = 0xFFF1F5F2)),
}

/** How strongly a glass tile shows its light; pure so it is unit-testable. */
data class GlowStrength(
    /** Alpha of the inner light at its brightest point. */
    val inner: Float,
    /** Alpha of the coloured shadow under the tile. */
    val shadow: Float,
    /** Alpha of the white light edge along the top. */
    val edge: Float,
    /** Alpha of the glossy sheen over the upper half. */
    val sheen: Float,
)

/** Light mode: a bright halo on white. Dark mode: the light glows stronger, the edge is fainter. */
fun glowStrength(dark: Boolean): GlowStrength =
    if (dark) {
        GlowStrength(inner = 0.42f, shadow = 0.38f, edge = 0.22f, sheen = 0.07f)
    } else {
        GlowStrength(inner = 0.95f, shadow = 0.30f, edge = 1f, sheen = 0.65f)
    }

/** True while the dark palette is active (read from the page colour, so previews follow too). */
@Composable
fun isHainDark(): Boolean = LocalHainColors.current.bg.luminance() < DARK_LUMINANCE

private const val DARK_LUMINANCE = 0.5f
