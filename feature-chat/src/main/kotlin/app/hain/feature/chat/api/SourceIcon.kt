package app.hain.feature.chat.api

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.hain.tools.api.FaviconRules
import app.hain.tools.api.Favicons
import app.hain.ui.theme.LocalHainColors
import app.hain.ui.theme.LocalHainTypography
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * T-064c: the small site icon in a source chip. Shows the site's first letter on a soft green dot
 * until the icon is there, when the site has none, in offline mode and when "Quellen-Bilder" is off
 * ([favicons] = `null`). Only decoration: TalkBack reads the chip's text, not the icon.
 */
@Composable
internal fun SourceIcon(
    url: String,
    favicons: Favicons?,
    modifier: Modifier = Modifier,
) {
    val icon by produceState<ImageBitmap?>(null, url, favicons) {
        val bytes = favicons?.icon(url) ?: return@produceState
        value =
            withContext(Dispatchers.Default) {
                runCatching { BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap() }.getOrNull()
            }
    }
    val box = modifier.size(ICON_DP.dp).clearAndSetSemantics {}
    val bitmap = icon
    if (bitmap != null) {
        Image(bitmap, contentDescription = null, modifier = box.clip(RoundedCornerShape(CORNER_DP.dp)))
    } else {
        val colors = LocalHainColors.current
        val letter = remember(url) { FaviconRules.letter(url) }
        Box(box.background(colors.accentSoft, CircleShape), contentAlignment = Alignment.Center) {
            BasicText(
                letter,
                style = LocalHainTypography.current.label.copy(color = colors.accent, fontSize = LETTER_SP.sp),
            )
        }
    }
}

private const val ICON_DP = 16
private const val CORNER_DP = 4
private const val LETTER_SP = 10
