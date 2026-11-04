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
import app.hain.ui.theme.HainTheme
import app.hain.ui.theme.LocalHainColors
import app.hain.ui.theme.LocalHainShapes

/**
 * The app's motif icons (T-011, name kept from the 3D days): one calm Lucide line icon each
 * (ISC licence, LICENSES.md), Lichtung style (docs/design/STYLE_OPTIONS.md). The glossy 3D files of
 * #24 are gone. Used for tools, suggestions, settings groups and the thinking picker.
 */
enum class Hain3D(
    /** Lucide line icon, `core-ui/src/main/res/drawable/icl_*.xml`. */
    val line: Int,
) {
    WEATHER(R.drawable.icl_cloud_sun),
    BOOK(R.drawable.icl_book_open),
    SEARCH(R.drawable.icl_search),
    CALCULATOR(R.drawable.icl_calculator),
    DOCUMENT(R.drawable.icl_file_text),
    SPARKLES(R.drawable.icl_sparkles),
    PHONE(R.drawable.icl_smartphone),
    LEAF(R.drawable.icl_leaf),
    RECYCLE(R.drawable.icl_recycle),
    GIFT(R.drawable.icl_gift),
    CALENDAR(R.drawable.icl_calendar),
    PALETTE(R.drawable.icl_palette),
    BUBBLE(R.drawable.icl_message_circle),
    BUBBLE_BLUE(R.drawable.icl_message_square),
    HEART(R.drawable.icl_heart),
    SHIELD(R.drawable.icl_shield_check),
    TREE(R.drawable.icl_trees),
    BOLT(R.drawable.icl_zap),
    BULB(R.drawable.icl_lightbulb),
    PARCEL(R.drawable.icl_package),
    GLOBE(R.drawable.icl_globe),
    HEAD(R.drawable.icl_user_round),
    NEWS(R.drawable.icl_newspaper),
}

/** One icon of the set in a quiet text colour; decorative by default (the row next to it speaks for TalkBack). */
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

/** Tint of the calm icons: the text colour, a little quieter. */
@Composable
fun calmIconTint(): Color = LocalHainColors.current.text.copy(alpha = A_ICON_ALPHA)

/** An icon on its own small soft grey tile (settings groups, tool cards); [hue] is kept for the call sites. */
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
    Box(
        modifier.size(size).clip(shape).background(c.surface, shape),
        contentAlignment = Alignment.Center,
    ) {
        Hain3DIcon(icon, size = size * ICON_SHARE)
    }
}

private const val LINE_SHARE = 0.62f
private const val A_ICON_ALPHA = 0.82f
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
