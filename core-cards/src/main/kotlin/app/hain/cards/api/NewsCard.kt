package app.hain.cards.api

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.hain.cards.R
import app.hain.ui.components.HainCard
import app.hain.ui.theme.HainLevel
import app.hain.ui.theme.LocalHainColors
import app.hain.ui.theme.LocalHainShapes
import app.hain.ui.theme.LocalHainSpacing
import app.hain.ui.theme.LocalHainTypography
import app.hain.ui.theme.hainSurface
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** One headline on the [NewsCard] (T-072). [publishedAt] is epoch millis; `null` = no date. */
data class NewsCardItem(
    val source: String,
    val title: String,
    val url: String?,
    val publishedAt: Long?,
    val paywall: Boolean = false,
)

/**
 * Headlines the `news` tool found, under the answer (NEWS.md §4): headline, source and time, a
 * small "Paywall" badge, and a tap opens the article (only on the user's tap, never by itself).
 * Shows at most [maxItems]; the rest is counted ("+3 weitere").
 */
@Composable
fun NewsCard(
    items: List<NewsCardItem>,
    onOpen: (String) -> Unit,
    modifier: Modifier = Modifier,
    maxItems: Int = DEFAULT_ITEMS,
    now: Long = System.currentTimeMillis(),
) {
    if (items.isEmpty()) return
    val colors = LocalHainColors.current
    val type = LocalHainTypography.current
    val locale = LocalConfiguration.current.locales[0] ?: Locale.getDefault()
    HainCard(modifier.padding(top = LocalHainSpacing.current.sm), title = stringResource(R.string.cards_news_title)) {
        items.take(maxItems).forEach { item ->
            NewsCardRow(item, NewsTime.label(item.publishedAt, now, ZoneId.systemDefault(), locale), onOpen)
        }
        val more = items.size - maxItems
        if (more > 0) {
            BasicText(
                stringResource(R.string.cards_news_more, more),
                style = type.label.copy(color = colors.textMuted),
            )
        }
    }
}

@Composable
private fun NewsCardRow(
    item: NewsCardItem,
    time: String?,
    onOpen: (String) -> Unit,
) {
    val colors = LocalHainColors.current
    val type = LocalHainTypography.current
    val url = item.url
    val openLabel = stringResource(R.string.cards_news_open)
    Column(
        Modifier
            .fillMaxWidth()
            .then(
                if (url != null) {
                    Modifier.clickable(role = Role.Button, onClickLabel = openLabel) { onOpen(url) }
                } else {
                    Modifier
                },
            ).padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        BasicText(
            item.title,
            style = type.bodySmall.copy(color = colors.text),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            val meta = listOfNotNull(item.source.ifBlank { null }, time).joinToString(" · ")
            BasicText(meta, style = type.label.copy(color = colors.textMuted), maxLines = 1)
            if (item.paywall) PaywallBadge()
        }
    }
}

/** Small quiet "Paywall" badge (NEWS.md §1); also used by the news screen. */
@Composable
fun PaywallBadge(modifier: Modifier = Modifier) {
    val colors = LocalHainColors.current
    BasicText(
        stringResource(R.string.cards_news_paywall),
        style = LocalHainTypography.current.label.copy(color = colors.textMuted),
        modifier =
            modifier
                .hainSurface(HainLevel.FLAT, LocalHainShapes.current.chip)
                .padding(horizontal = 6.dp, vertical = 1.dp),
    )
}

/** Short time of a headline: "17:30" today, else "4. Nov." / "4 Nov". */
object NewsTime {
    fun label(
        publishedAt: Long?,
        now: Long,
        zone: ZoneId,
        locale: Locale,
    ): String? {
        val at = publishedAt?.let { Instant.ofEpochMilli(it).atZone(zone) } ?: return null
        val today = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
        val pattern =
            when {
                at.toLocalDate() == today -> "HH:mm"
                locale.language == "de" -> "d. MMM"
                else -> "d MMM"
            }
        return DateTimeFormatter.ofPattern(pattern, locale).format(at)
    }
}

private const val DEFAULT_ITEMS = 5
