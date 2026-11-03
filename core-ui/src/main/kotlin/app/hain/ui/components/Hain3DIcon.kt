@file:Suppress("MatchingDeclarationName") // the enum and its composables live together

package app.hain.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.hain.ui.R
import app.hain.ui.theme.GlowHue
import app.hain.ui.theme.HainStyle
import app.hain.ui.theme.HainTheme
import app.hain.ui.theme.LocalHainColors
import app.hain.ui.theme.LocalHainShapes
import app.hain.ui.theme.LocalHainStyle

/**
 * The app's motif icons (T-011): each has a glossy 3D file (own generation, see LICENSES.md) and a
 * calm Lucide line icon; the style proposals draw only the line icon. Used for tools, suggestions,
 * settings groups and the thinking picker.
 * Files: `core-ui/src/main/res/drawable-nodpi/ic3d_*.webp` (192 px, transparent), copies in
 * `assets/illustrations/3d/icons/`.
 */
enum class Hain3D(
    val res: Int,
    /** Calm Lucide line icon used by the style proposals A and B (docs/design/STYLE_OPTIONS.md). */
    val line: Int,
) {
    WEATHER(R.drawable.ic3d_weather, R.drawable.icl_cloud_sun),
    BOOK(R.drawable.ic3d_book, R.drawable.icl_book_open),
    SEARCH(R.drawable.ic3d_search, R.drawable.icl_search),
    CALCULATOR(R.drawable.ic3d_calculator, R.drawable.icl_calculator),
    DOCUMENT(R.drawable.ic3d_document, R.drawable.icl_file_text),
    SPARKLES(R.drawable.ic3d_sparkles, R.drawable.icl_sparkles),
    PHONE(R.drawable.ic3d_phone, R.drawable.icl_smartphone),
    LEAF(R.drawable.ic3d_leaf, R.drawable.icl_leaf),
    RECYCLE(R.drawable.ic3d_recycle, R.drawable.icl_recycle),
    GIFT(R.drawable.ic3d_gift, R.drawable.icl_gift),
    CALENDAR(R.drawable.ic3d_calendar, R.drawable.icl_calendar),
    PALETTE(R.drawable.ic3d_palette, R.drawable.icl_palette),
    BUBBLE(R.drawable.ic3d_bubble, R.drawable.icl_message_circle),
    BUBBLE_BLUE(R.drawable.ic3d_bubble_blue, R.drawable.icl_message_square),
    HEART(R.drawable.ic3d_heart, R.drawable.icl_heart),
    SHIELD(R.drawable.ic3d_shield, R.drawable.icl_shield_check),
    TREE(R.drawable.ic3d_tree, R.drawable.icl_trees),
    BOLT(R.drawable.ic3d_bolt, R.drawable.icl_zap),
    BULB(R.drawable.ic3d_bulb, R.drawable.icl_lightbulb),
    PARCEL(R.drawable.ic3d_parcel, R.drawable.icl_package),
    GLOBE(R.drawable.ic3d_globe, R.drawable.icl_globe),
    HEAD(R.drawable.ic3d_head, R.drawable.icl_user_round),
}

/**
 * One icon of the set; decorative by default (the row next to it carries the words for TalkBack).
 * Since the style proposals every icon is drawn as one calm line icon: Stil A in the text colour,
 * Stil B in Hain green. The glossy 3D files stay in res for a later decision.
 */
@Composable
fun Hain3DIcon(
    icon: Hain3D,
    modifier: Modifier = Modifier,
    size: Dp = 28.dp,
    contentDescription: String? = null,
) {
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Image(
            painterResource(icon.line),
            contentDescription,
            Modifier.size(size * LINE_SHARE),
            colorFilter = ColorFilter.tint(calmIconTint()),
        )
    }
}

/** Tint of the calm icons: quiet text colour (A) or Hain green (B). */
@Composable
fun calmIconTint(): Color {
    val c = LocalHainColors.current
    return if (LocalHainStyle.current == HainStyle.A) c.text.copy(alpha = A_ICON_ALPHA) else c.accent
}

/**
 * An icon on its own small tile (settings groups, tool cards): a soft grey square (A) or a pale
 * green one with a gentle gradient (B). [hue] is kept for the call sites; every badge is calm.
 */
@Suppress("UnusedParameter", "UNUSED_PARAMETER")
@Composable
fun Hain3DBadge(
    icon: Hain3D,
    hue: GlowHue,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
) {
    val c = LocalHainColors.current
    val shape = LocalHainShapes.current.chip
    val fill =
        if (LocalHainStyle.current == HainStyle.A) {
            Modifier.background(c.surface, shape)
        } else {
            Modifier.background(Brush.verticalGradient(listOf(c.accentSoft.copy(alpha = B_TOP), c.accentSoft)), shape)
        }
    Box(
        modifier.size(size).clip(shape).then(fill),
        contentAlignment = Alignment.Center,
    ) {
        Hain3DIcon(icon, size = size * ICON_SHARE)
    }
}

private const val LINE_SHARE = 0.62f
private const val A_ICON_ALPHA = 0.82f
private const val B_TOP = 0.55f
private const val ICON_SHARE = 0.8f

@Preview(name = "Light — 3D icons")
@Preview(name = "Dark — 3D icons", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
internal fun Hain3DIconsPreview() {
    HainTheme {
        FlowRow(
            Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Hain3D.entries.forEachIndexed { i, icon ->
                Hain3DBadge(icon, GlowHue.entries[i % GlowHue.entries.size])
            }
        }
    }
}
