package app.hain.tools.time

import app.hain.tools.api.HainTools
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

class ClockAnswerTest {
    private val now = ZonedDateTime.of(2026, 11, 4, 10, 5, 0, 0, ZoneId.of("Europe/Berlin"))

    @Test
    fun timeAndDateFromTheClock() {
        assertEquals("Es ist 10:05 Uhr.", HainTools.clockAnswer("Wie spät ist es?", now, "de"))
        assertEquals("It's 10:05.", HainTools.clockAnswer("what time is it", now, "en"))
        val day = HainTools.clockAnswer("welcher tag ist heute?", now, "de")
        assertEquals("Heute ist Mittwoch, der 4. November 2026.", day)
        assertEquals("Today is Wednesday, 4 November 2026.", HainTools.clockAnswer("What day is it today?", now, "en"))
    }

    @Test
    fun otherQuestionsGoToTheBrain() {
        assertNull(HainTools.clockAnswer("Wie spät ist es in Tokio?", now, "de"))
        assertNull(HainTools.clockAnswer("Welcher Tag ist heute in der Geschichte passiert?", now, "de"))
        assertNull(HainTools.clockAnswer("Hallo", now, "de"))
    }
}
