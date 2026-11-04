package app.hain.feature.news.api

import app.hain.tools.internal.news.NewsItem
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.ZoneId

/** T-073: Heute / Gestern / Älter on the user's clock. */
class NewsGroupsTest {
    private val zone = ZoneId.of("Europe/Berlin")
    private val now = Instant.parse("2026-11-04T16:30:00Z").toEpochMilli()

    private fun at(iso: String): NewsItem {
        val ms = Instant.parse(iso).toEpochMilli()
        return NewsItem("f", "F", iso, null, iso, ms, ms, "")
    }

    @Test fun splitsByLocalDay() {
        val items =
            listOf(
                at("2026-11-04T10:00:00Z"),
                // 23:30 UTC on the 3rd is already the 4th in Berlin
                at("2026-11-03T23:30:00Z"),
                at("2026-11-03T08:00:00Z"),
                at("2026-10-30T08:00:00Z"),
            )
        val groups = NewsGroups.group(items, now, zone)
        assertEquals(listOf(NewsDay.TODAY, NewsDay.YESTERDAY, NewsDay.OLDER), groups.map { it.first })
        assertEquals(2, groups[0].second.size)
        assertEquals(1, groups[1].second.size)
    }

    @Test fun emptyPartsAreLeftOut() {
        val groups = NewsGroups.group(listOf(at("2026-10-30T08:00:00Z")), now, zone)
        assertEquals(listOf(NewsDay.OLDER), groups.map { it.first })
    }
}
