@file:Suppress("MatchingDeclarationName") // the enum and its composables live together

package app.hain.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.hain.ui.R
import app.hain.ui.theme.GlowHue
import app.hain.ui.theme.HainTheme
import app.hain.ui.theme.glassTile

/**
 * Small glossy 3D icons (T-011, own generation, see LICENSES.md): soft clay look, light from the top
 * left, one consistent set. Used for tools, suggestions, settings groups and the thinking picker.
 * Files: `core-ui/src/main/res/drawable-nodpi/ic3d_*.webp` (192 px, transparent), copies in
 * `assets/illustrations/3d/icons/`.
 */
enum class Hain3D(
    val res: Int,
) {
    WEATHER(R.drawable.ic3d_weather),
    BOOK(R.drawable.ic3d_book),
    SEARCH(R.drawable.ic3d_search),
    CALCULATOR(R.drawable.ic3d_calculator),
    DOCUMENT(R.drawable.ic3d_document),
    SPARKLES(R.drawable.ic3d_sparkles),
    PHONE(R.drawable.ic3d_phone),
    LEAF(R.drawable.ic3d_leaf),
    RECYCLE(R.drawable.ic3d_recycle),
    GIFT(R.drawable.ic3d_gift),
    CALENDAR(R.drawable.ic3d_calendar),
    PALETTE(R.drawable.ic3d_palette),
    BUBBLE(R.drawable.ic3d_bubble),
    BUBBLE_BLUE(R.drawable.ic3d_bubble_blue),
    HEART(R.drawable.ic3d_heart),
    SHIELD(R.drawable.ic3d_shield),
    TREE(R.drawable.ic3d_tree),
    BOLT(R.drawable.ic3d_bolt),
    BULB(R.drawable.ic3d_bulb),
    PARCEL(R.drawable.ic3d_parcel),
    GLOBE(R.drawable.ic3d_globe),
    HEAD(R.drawable.ic3d_head),
}

/** One 3D icon; decorative by default (the row next to it carries the words for TalkBack). */
@Composable
fun Hain3DIcon(
    icon: Hain3D,
    modifier: Modifier = Modifier,
    size: Dp = 28.dp,
    contentDescription: String? = null,
) {
    Image(painterResource(icon.res), contentDescription, modifier.size(size))
}

/** A 3D icon sitting in its own small glass tile that glows in [hue] (settings groups, tool cards). */
@Composable
fun Hain3DBadge(
    icon: Hain3D,
    hue: GlowHue,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
) {
    Box(
        modifier
            .size(size)
            .glassTile(hue, elevation = 4.dp, lightAt = BADGE_LIGHT, floorGlow = false),
        contentAlignment = Alignment.Center,
    ) {
        Hain3DIcon(icon, size = size * ICON_SHARE)
    }
}

private const val ICON_SHARE = 0.68f
private val BADGE_LIGHT =
    androidx.compose.ui.geometry
        .Offset(x = 0.5f, y = 0.8f)

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
