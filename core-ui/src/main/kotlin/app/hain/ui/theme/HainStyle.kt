package app.hain.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * The two visual styles Oskar chooses between (03.11.2026, docs/design/STYLE_OPTIONS.md). Same
 * screens, same components; only the tokens and a few surface recipes differ.
 *
 * - [A] "Nothing Clean": pure white / black, high contrast, hairlines instead of shadows, Geist,
 *   small radii, blur only under bars and popovers, one soft green glow in the empty chat.
 * - [B] "Soft Gradient": large, very soft mint/grey mesh gradients behind everything, surfaces as
 *   translucent layers above them, Hanken Grotesk, slightly warmer but just as tidy.
 */
enum class HainStyle(
    val key: String,
) {
    A("a"),
    B("b"),
    ;

    companion object {
        /** Stored value → style; unknown or missing values fall back to the default [A]. */
        fun fromKey(key: String?): HainStyle = entries.firstOrNull { it.key == key } ?: A
    }
}

/** The style in use; components read it to pick their surface recipe. */
val LocalHainStyle = staticCompositionLocalOf { HainStyle.A }

/** Colour tokens of a style in light or dark. */
fun hainColors(
    style: HainStyle,
    dark: Boolean,
): HainColors =
    when (style) {
        HainStyle.A -> if (dark) NothingDark else NothingLight
        HainStyle.B -> if (dark) SoftDark else SoftLight
    }

/** A: white page, neutral greys without a green tint, ink-black text. */
val NothingLight =
    HainColors(
        bg = Color(color = 0xFFFFFFFF),
        surface = Color(color = 0xFFF5F5F5),
        surfaceHigh = Color(color = 0xFFEBEBEB),
        border = Color(color = 0xFFE6E6E6),
        text = Color(color = 0xFF0A0A0A),
        textMuted = Color(color = 0xFF6B6B6B),
        accent = Color(color = 0xFF16A36B),
        onAccent = Color(color = 0xFFFFFFFF),
        accentSoft = Color(color = 0xFFEAF6F0),
        online = Color(color = 0xFFE8833A),
        danger = Color(color = 0xFFD14343),
        icon = Color(color = 0xFF808080),
        glass = Color(color = 0xE6FFFFFF),
        glassEdge = Color(color = 0xFFE6E6E6),
        shadow = Color(color = 0x14000000),
        scrim = Color(color = 0x33000000),
    )

/** A dark: true black page (OLED), greys from 7 to 20 %, white text. */
val NothingDark =
    HainColors(
        bg = Color(color = 0xFF000000),
        surface = Color(color = 0xFF111111),
        surfaceHigh = Color(color = 0xFF1C1C1C),
        border = Color(color = 0xFF242424),
        text = Color(color = 0xFFF2F2F2),
        textMuted = Color(color = 0xFF8C8C8C),
        accent = Color(color = 0xFF22C17E),
        onAccent = Color(color = 0xFF04120B),
        accentSoft = Color(color = 0xFF0E2A1D),
        online = Color(color = 0xFFF0964F),
        danger = Color(color = 0xFFF07070),
        icon = Color(color = 0xFF8C8C8C),
        glass = Color(color = 0xE60A0A0A),
        glassEdge = Color(color = 0xFF242424),
        shadow = Color(color = 0x66000000),
        scrim = Color(color = 0x99000000),
    )

/** B: a pale grey-green page under the mesh; surfaces are translucent white layers. */
val SoftLight =
    HainColors(
        bg = Color(color = 0xFFF3F6F4),
        surface = Color(color = 0xB3FFFFFF),
        surfaceHigh = Color(color = 0xFFE6ECE8),
        border = Color(color = 0x14102A1D),
        text = Color(color = 0xFF111614),
        textMuted = Color(color = 0xFF66706B),
        accent = Color(color = 0xFF16A36B),
        onAccent = Color(color = 0xFFFFFFFF),
        accentSoft = Color(color = 0xFFDDF1E6),
        online = Color(color = 0xFFE8833A),
        danger = Color(color = 0xFFD14343),
        icon = Color(color = 0xFF7C8681),
        glass = Color(color = 0xCCFFFFFF),
        glassEdge = Color(color = 0xB3FFFFFF),
        shadow = Color(color = 0x140D2A1C),
        scrim = Color(color = 0x330D1A14),
    )

/** B dark: deep green-black page; surfaces are faint white layers over the dark mesh. */
val SoftDark =
    HainColors(
        bg = Color(color = 0xFF0B0F0D),
        surface = Color(color = 0x14FFFFFF),
        surfaceHigh = Color(color = 0xFF1E2622),
        border = Color(color = 0x1AFFFFFF),
        text = Color(color = 0xFFEDF1EF),
        textMuted = Color(color = 0xFF97A29C),
        accent = Color(color = 0xFF22C17E),
        onAccent = Color(color = 0xFF04120B),
        accentSoft = Color(color = 0xFF14301F),
        online = Color(color = 0xFFF0964F),
        danger = Color(color = 0xFFF07070),
        icon = Color(color = 0xFF8E9993),
        glass = Color(color = 0xD9161C19),
        glassEdge = Color(color = 0x1AFFFFFF),
        shadow = Color(color = 0x66000000),
        scrim = Color(color = 0x99000000),
    )
