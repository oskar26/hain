package app.hain.feature.news.api

import app.hain.tools.internal.news.NewsItem
import java.time.Instant
import java.time.ZoneId

/** The three parts of the news list (NEWS.md §6). */
enum class NewsDay { TODAY, YESTERDAY, OLDER }

/** Splits headlines (newest first) into Heute / Gestern / Älter in [zone]; empty parts are left out. */
object NewsGroups {
    fun group(
        items: List<NewsItem>,
        now: Long,
        zone: ZoneId,
    ): List<Pair<NewsDay, List<NewsItem>>> {
        val today = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
        val yesterday = today.minusDays(1)
        val byDay =
            items.groupBy { item ->
                when (Instant.ofEpochMilli(item.time).atZone(zone).toLocalDate()) {
                    today -> NewsDay.TODAY
                    yesterday -> NewsDay.YESTERDAY
                    else -> if (item.time > now) NewsDay.TODAY else NewsDay.OLDER
                }
            }
        return NewsDay.entries.mapNotNull { day -> byDay[day]?.let { day to it } }
    }
}
