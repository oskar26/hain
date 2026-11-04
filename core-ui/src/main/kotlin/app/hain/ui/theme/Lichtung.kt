package app.hain.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.kyant.capsule.ContinuousRoundedRectangle

/**
 * Hain's one style, "Lichtung" (clearing), chosen by Oskar on 03.11.2026 (docs/design/STYLE_OPTIONS.md):
 * clear and minimal. White or near-black pages, hairline edges, almost no shadow, small radii
 * (8/12/16 dp), calm line icons and a flat Pim. Hain green only where you act (send, selection, links).
 */
object Lichtung {
    /** Shadow under cards and tiles: barely there, the hairline does the work. */
    const val TILE_SHADOW_DP = 1

    /** Shadow under menus and popovers. */
    const val FLOATING_SHADOW_DP = 10

    /** Chips and rows. */
    const val RADIUS_SMALL = 8

    /** Cards, bubbles, buttons, menus, tiles. */
    const val RADIUS_CARD = 12

    /** The message box and sheets. */
    const val RADIUS_LARGE = 16
}

/** The shape tokens of Lichtung: calm rounded rectangles, no pills. */
val LichtungShapes: HainShapes =
    run {
        val small = ContinuousRoundedRectangle(Lichtung.RADIUS_SMALL.dp)
        val card = ContinuousRoundedRectangle(Lichtung.RADIUS_CARD.dp)
        val large = ContinuousRoundedRectangle(Lichtung.RADIUS_LARGE.dp)
        HainShapes(
            composer = large,
            userBubble = card,
            card = card,
            chip = small,
            button = card,
            row = small,
            menu = card,
            sheet = large,
            tile = card,
        )
    }

/** Lichtung palette for the light or dark page. */
fun lichtungColors(dark: Boolean): HainColors = if (dark) LichtungDark else LichtungLight

/** Light: pure white, neutral greys, green only where you act. */
val LichtungLight =
    LightHainColors.copy(
        surface = Color(color = 0xFFF5F6F5),
        surfaceHigh = Color(color = 0xFFEDEFEE),
        border = Color(color = 0xFFE5E8E6),
        text = Color(color = 0xFF0E1110),
        textMuted = Color(color = 0xFF5E6561),
        accentSoft = Color(color = 0xFFE8F5EE),
        icon = Color(color = 0xFF6E7571),
        glass = Color(color = 0xFAFFFFFF),
        shadow = Color(color = 0x140D1A14),
    )

val LichtungDark =
    DarkHainColors.copy(
        bg = Color(color = 0xFF0E1110),
        surface = Color(color = 0xFF171B19),
        surfaceHigh = Color(color = 0xFF202523),
        border = Color(color = 0xFF272C2A),
        accent = Color(color = 0xFF1FB877),
        accentSoft = Color(color = 0xFF14271E),
        glass = Color(color = 0xF5171B19),
        glassEdge = Color(color = 0x14FFFFFF),
    )
