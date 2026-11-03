package app.hain.ui

import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.hain.ui.theme.HainTheme
import app.hain.ui.theme.LocalHainColors

/**
 * The app's icon set: Lucide (ISC licence) as vector drawables, per
 * DESIGN_SYSTEM.md §2. Render at 20 dp with tint from the theme.
 */
object HainIcons {
    val menu: Painter @Composable get() = painterResource(R.drawable.ic_menu)
    val plus: Painter @Composable get() = painterResource(R.drawable.ic_plus)
    val mic: Painter @Composable get() = painterResource(R.drawable.ic_mic)
    val send: Painter @Composable get() = painterResource(R.drawable.ic_send)
    val stop: Painter @Composable get() = painterResource(R.drawable.ic_square)
    val copy: Painter @Composable get() = painterResource(R.drawable.ic_copy)
    val volume: Painter @Composable get() = painterResource(R.drawable.ic_volume_2)
    val retry: Painter @Composable get() = painterResource(R.drawable.ic_refresh_cw)
    val share: Painter @Composable get() = painterResource(R.drawable.ic_share_2)
    val settings: Painter @Composable get() = painterResource(R.drawable.ic_settings)
    val search: Painter @Composable get() = painterResource(R.drawable.ic_search)
    val news: Painter @Composable get() = painterResource(R.drawable.ic_newspaper)
    val brain: Painter @Composable get() = painterResource(R.drawable.ic_brain)
    val image: Painter @Composable get() = painterResource(R.drawable.ic_image)
    val document: Painter @Composable get() = painterResource(R.drawable.ic_file_text)
    val offline: Painter @Composable get() = painterResource(R.drawable.ic_wifi_off)
    val check: Painter @Composable get() = painterResource(R.drawable.ic_check)
    val close: Painter @Composable get() = painterResource(R.drawable.ic_x)
    val chevronRight: Painter @Composable get() = painterResource(R.drawable.ic_chevron_right)
    val delete: Painter @Composable get() = painterResource(R.drawable.ic_trash_2)
    val download: Painter @Composable get() = painterResource(R.drawable.ic_download)
    val globe: Painter @Composable get() = painterResource(R.drawable.ic_globe)
    val more: Painter @Composable get() = painterResource(R.drawable.ic_ellipsis)
    val pin: Painter @Composable get() = painterResource(R.drawable.ic_pin)
    val edit: Painter @Composable get() = painterResource(R.drawable.ic_pencil)
    val thumbUp: Painter @Composable get() = painterResource(R.drawable.ic_thumbs_up)
    val thumbDown: Painter @Composable get() = painterResource(R.drawable.ic_thumbs_down)
    val alarm: Painter @Composable get() = painterResource(R.drawable.ic_alarm_clock)
    val timer: Painter @Composable get() = painterResource(R.drawable.ic_timer)
    val calendar: Painter @Composable get() = painterResource(R.drawable.ic_calendar)
    val navigation: Painter @Composable get() = painterResource(R.drawable.ic_navigation)
}

@Preview(name = "Light — icon grid")
@Preview(name = "Dark — icon grid", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
internal fun HainIconsPreview() {
    HainTheme {
        FlowRow(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            listOf(
                HainIcons.menu,
                HainIcons.plus,
                HainIcons.mic,
                HainIcons.send,
                HainIcons.stop,
                HainIcons.copy,
                HainIcons.volume,
                HainIcons.retry,
                HainIcons.share,
                HainIcons.settings,
                HainIcons.search,
                HainIcons.news,
                HainIcons.brain,
                HainIcons.image,
                HainIcons.document,
                HainIcons.offline,
                HainIcons.check,
                HainIcons.close,
                HainIcons.chevronRight,
                HainIcons.delete,
                HainIcons.download,
                HainIcons.globe,
            ).forEach { painter ->
                Image(
                    painter = painter,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    colorFilter = ColorFilter.tint(LocalHainColors.current.text),
                )
            }
        }
    }
}
