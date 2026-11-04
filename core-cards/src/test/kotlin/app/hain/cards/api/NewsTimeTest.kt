package app.hain.cards.api

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant
import java.time.ZoneId
import java.util.Locale

/** T-072: the short time on the NewsCard. */
class NewsTimeTest {
    private val zone = ZoneId.of("Europe/Berlin")
    private val now = Instant.parse("2026-11-04T16:30:00Z").toEpochMilli()

    @Test fun todayShowsTheClock() {
        val at = Instant.parse("2026-11-04T08:05:00Z").toEpochMilli()
        assertEquals("09:05", NewsTime.label(at, now, zone, Locale.GERMAN))
    }

    @Test fun earlierDaysShowTheDate() {
        val at = Instant.parse("2026-11-02T08:05:00Z").toEpochMilli()
        assertEquals("2. Nov.", NewsTime.label(at, now, zone, Locale.GERMAN))
        assertEquals("2 Nov", NewsTime.label(at, now, zone, Locale.ENGLISH))
    }

    @Test fun noDateNoLabel() {
        assertNull(NewsTime.label(null, now, zone, Locale.ENGLISH))
    }
}
