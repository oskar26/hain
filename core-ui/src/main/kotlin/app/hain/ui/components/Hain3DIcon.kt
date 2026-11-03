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
 * The icons of tools, suggestions, settings groups and the thinking picker. Since the style decision
 * (03.11.2026, STYLE_OPTIONS.md) they are plain Lucide line icons (ISC) in one colour, no glossy 3D
 * any more; the names stay so the call sites keep working. The old 3D renders remain in
 * `assets/illustrations/3d/icons/` for reference only.
 */
enum class Hain3D(
    val res: Int,
) {
    WEATHER(R.drawable.ic_cloud_sun),
    BOOK(R.drawable.ic_book_open),
    SEARCH(R.drawable.ic_search),
    CALCULATOR(R.drawable.ic_calculator),
    DOCUMENT(R.drawable.ic_file_text),
    SPARKLES(R.drawable.ic_sparkles),
    PHONE(R.drawable.ic_smartphone),
    LEAF(R.drawable.ic_leaf),
    RECYCLE(R.drawable.ic_recycle),
    GIFT(R.drawable.ic_gift),
    CALENDAR(R.drawable.ic_calendar),
    PALETTE(R.drawable.ic_palette),
    BUBBLE(R.drawable.ic_message_circle),
    BUBBLE_BLUE(R.drawable.ic_message_square),
    HEART(R.drawable.ic_heart),
    SHIELD(R.drawable.ic_shield),
    TREE(R.drawable.ic_trees),
    BOLT(R.drawable.ic_zap),
    BULB(R.drawable.ic_lightbulb),
    PARCEL(R.drawable.ic_package),
    GLOBE(R.drawable.ic_globe),
    HEAD(R.drawable.ic_user_round),
}

/** One line icon in the quiet icon colour; decorative by default (the row carries the words). */
@Composable
fun Hain3DIcon(
    icon: Hain3D,
    modifier: Modifier = Modifier,
    size: Dp = 28.dp,
    contentDescription: String? = null,
    tint: Color = LocalHainColors.current.text,
) {
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Image(
            painterResource(icon.res),
            contentDescription,
            colorFilter = ColorFilter.tint(tint),
            modifier = Modifier.size(size * LINE_SHARE),
        )
    }
}

/** A line icon in a small quiet square (settings groups, tool cards). [hue] is no longer drawn. */
@Suppress("UnusedParameter") // hue stays for source compatibility of the call sites
@Composable
fun Hain3DBadge(
    icon: Hain3D,
    hue: GlowHue,
    modifier: Modifier = Modifier,
    size: Dp = 36.dp,
) {
    val c = LocalHainColors.current
    val a = LocalHainStyle.current == HainStyle.A
    Box(
        modifier
            .size(size)
            .clip(LocalHainShapes.current.chip)
            .background(if (a) c.surface else c.accentSoft.copy(alpha = BADGE_TINT)),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painterResource(icon.res),
            null,
            colorFilter = ColorFilter.tint(if (a) c.text else c.accent),
            modifier = Modifier.size(size * ICON_SHARE),
        )
    }
}

private const val LINE_SHARE = 0.72f
private const val BADGE_TINT = 0.8f
private const val ICON_SHARE = 0.52f

@Preview(name = "Light — line icons")
@Preview(name = "Dark — line icons", uiMode = Configuration.UI_MODE_NIGHT_YES)
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
