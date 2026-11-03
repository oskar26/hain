package app.hain.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Colour tokens, ported verbatim from DESIGN_SYSTEM.md §2 (+ the glass/shadow tokens of T-010,
 * docs/design/DESIGN_LANGUAGE.md). Do not invent or tweak values here — the spec table is the
 * source of truth.
 */
data class HainColors(
    val bg: Color,
    val surface: Color,
    val surfaceHigh: Color,
    val border: Color,
    val text: Color,
    val textMuted: Color,
    val accent: Color,
    val onAccent: Color,
    val accentSoft: Color,
    val online: Color,
    val danger: Color,
    /** Quiet grey for action icons (copy, share, thumbs). */
    val icon: Color,
    /** Fill of floating glass tiles (menus): almost opaque so text stays crisp. */
    val glass: Color,
    /** Thin light edge on the top of glass tiles. */
    val glassEdge: Color,
    /** Colour of the one soft shadow the app uses. */
    val shadow: Color,
    /** Dim layer behind the drawer and dialogs. */
    val scrim: Color,
)

val LightHainColors =
    HainColors(
        bg = Color(color = 0xFFFFFFFF),
        surface = Color(color = 0xFFF4F5F4),
        surfaceHigh = Color(color = 0xFFECEEEC),
        border = Color(color = 0xFFE4E7E5),
        text = Color(color = 0xFF0D0F0E),
        textMuted = Color(color = 0xFF5F6662),
        accent = Color(color = 0xFF16A36B),
        onAccent = Color(color = 0xFFFFFFFF),
        accentSoft = Color(color = 0xFFE3F5EC),
        online = Color(color = 0xFFE8833A),
        danger = Color(color = 0xFFD14343),
        icon = Color(color = 0xFF7A817D),
        glass = Color(color = 0xF5FFFFFF),
        glassEdge = Color(color = 0xFFFFFFFF),
        shadow = Color(color = 0x1F0D1F16),
        scrim = Color(color = 0x420D0F0E),
    )

val DarkHainColors =
    HainColors(
        bg = Color(color = 0xFF0F1311),
        surface = Color(color = 0xFF1B201D),
        surfaceHigh = Color(color = 0xFF242A26),
        border = Color(color = 0xFF2A302C),
        text = Color(color = 0xFFECEFED),
        textMuted = Color(color = 0xFF9AA39E),
        accent = Color(color = 0xFF22C17E),
        onAccent = Color(color = 0xFF06130C),
        accentSoft = Color(color = 0xFF173326),
        online = Color(color = 0xFFF0964F),
        danger = Color(color = 0xFFF07070),
        icon = Color(color = 0xFF8B948F),
        glass = Color(color = 0xF21E2420),
        glassEdge = Color(color = 0x1FFFFFFF),
        shadow = Color(color = 0x66000000),
        scrim = Color(color = 0x8C000000),
    )
