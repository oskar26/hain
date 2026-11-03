package app.hain.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.kyant.capsule.ContinuousCapsule
import com.kyant.capsule.ContinuousRoundedRectangle

/**
 * The two style proposals Oskar picks from (docs/design/STYLE_OPTIONS.md). Both keep Hain green,
 * small radii, calm line icons and only gentle depth; they differ in how surfaces are lit.
 * - [A] "Lichtung": clear and minimal. White or near-black pages, hairline edges, almost no shadow,
 *   green only as an accent (send, selection, links).
 * - [B] "Morgennebel": soft green light. Faintly tinted pages, surfaces with a soft vertical
 *   gradient and a blurred green glow, a little more radius, pill buttons.
 */
enum class HainStyle { A, B }

/** Style in use; read by the shared primitives (tiles, buttons, icons, Pim). */
val LocalHainStyle = staticCompositionLocalOf { HainStyle.A }

/** Depth and light of a style; pure values so they are unit-testable. */
data class StyleLook(
    /** Shadow under cards and tiles. */
    val tileShadowDp: Int,
    /** Shadow under menus and popovers. */
    val floatingShadowDp: Int,
    /** Shadow under buttons. */
    val buttonShadowDp: Int,
    /** Surfaces get a soft vertical gradient and a blurred green glow instead of a flat fill. */
    val mist: Boolean,
    /** Alpha of the green glow inside a surface (0 = none). */
    val glowAlpha: Float,
    /** Buttons get a gentle top-to-bottom gradient. */
    val buttonGradient: Boolean,
)

fun HainStyle.look(): StyleLook =
    when (this) {
        HainStyle.A ->
            StyleLook(
                tileShadowDp = 1,
                floatingShadowDp = 10,
                buttonShadowDp = 0,
                mist = false,
                glowAlpha = 0f,
                buttonGradient = false,
            )
        HainStyle.B ->
            StyleLook(
                tileShadowDp = 4,
                floatingShadowDp = 14,
                buttonShadowDp = 3,
                mist = true,
                glowAlpha = 0.10f,
                buttonGradient = true,
            )
    }

/** Corner radii per style: cards 12 (A) or 16 dp (B), never the old 20–28 dp. */
object StyleRadius {
    fun small(style: HainStyle) = if (style == HainStyle.A) A_SMALL else B_SMALL

    fun card(style: HainStyle) = if (style == HainStyle.A) A_CARD else B_CARD

    fun large(style: HainStyle) = if (style == HainStyle.A) A_LARGE else B_LARGE

    private const val A_SMALL = 8
    private const val A_CARD = 12
    private const val A_LARGE = 16
    private const val B_SMALL = 10
    private const val B_CARD = 16
    private const val B_LARGE = 20
}

fun shapesFor(style: HainStyle): HainShapes {
    val small = ContinuousRoundedRectangle(StyleRadius.small(style).dp)
    val card = ContinuousRoundedRectangle(StyleRadius.card(style).dp)
    val large = ContinuousRoundedRectangle(StyleRadius.large(style).dp)
    return HainShapes(
        composer = large,
        userBubble = card,
        card = card,
        chip = small,
        // A: calm rounded rectangles; B: soft pills like the Copilot references
        button = if (style == HainStyle.A) card else ContinuousCapsule,
        row = small,
        menu = card,
        sheet = large,
        tile = card,
    )
}

fun colorsFor(
    style: HainStyle,
    dark: Boolean,
): HainColors =
    when (style) {
        HainStyle.A -> if (dark) LichtungDark else LichtungLight
        HainStyle.B -> if (dark) NebelDark else NebelLight
    }

/** Stil A "Lichtung" light: pure white, neutral greys, green only where you act. */
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

/** Stil B "Morgennebel" light: a breath of green in the page, misty surfaces. */
val NebelLight =
    LightHainColors.copy(
        bg = Color(color = 0xFFFAFCFB),
        surface = Color(color = 0xFFF0F5F2),
        surfaceHigh = Color(color = 0xFFE6EEE9),
        border = Color(color = 0xFFDDE6E1),
        text = Color(color = 0xFF0F1513),
        textMuted = Color(color = 0xFF5A6660),
        accentSoft = Color(color = 0xFFDCF1E5),
        icon = Color(color = 0xFF66736C),
        glass = Color(color = 0xEBFFFFFF),
        shadow = Color(color = 0x1A0F3A26),
    )

val NebelDark =
    DarkHainColors.copy(
        bg = Color(color = 0xFF0C1210),
        surface = Color(color = 0xFF151F1A),
        surfaceHigh = Color(color = 0xFF1D2923),
        border = Color(color = 0xFF24322B),
        accentSoft = Color(color = 0xFF133325),
        glass = Color(color = 0xE6151F1A),
        glassEdge = Color(color = 0x1AFFFFFF),
    )
