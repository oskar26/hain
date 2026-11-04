package app.hain.tools.phone

import app.hain.tools.internal.phone.TimeParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

class TimeParserTest {
    // Tuesday, 3 November 2026, 18:45 in Cologne
    private val now = ZonedDateTime.of(2026, 11, 3, 18, 45, 0, 0, ZoneId.of("Europe/Berlin"))
    private val today = LocalDate.of(2026, 11, 3)

    private fun w(
        date: LocalDate,
        time: LocalTime?,
    ) = TimeParser.When(date, time)

    private fun at(text: String) = TimeParser.dateTime(text, now)

    private fun t(
        h: Int,
        m: Int = 0,
    ) = LocalTime.of(h, m)

    @Test
    fun clockForms() {
        assertEquals(t(6, 30), TimeParser.clock("6:30"))
        assertEquals(t(6, 30), TimeParser.clock("06:30"))
        assertEquals(t(6, 30), TimeParser.clock("06:30:00"))
        assertEquals(t(6, 30), TimeParser.clock("6.30 Uhr"))
        assertEquals(t(7), TimeParser.clock("um 7"))
        assertEquals(t(7), TimeParser.clock("7"))
        assertEquals(t(15), TimeParser.clock("15 Uhr"))
        assertEquals(t(15, 20), TimeParser.clock("15 Uhr 20"))
        assertEquals(t(7, 30), TimeParser.clock("halb 8"))
        assertEquals(t(7, 15), TimeParser.clock("viertel nach 7"))
        assertEquals(t(6, 45), TimeParser.clock("viertel vor 7"))
        assertEquals(t(7), TimeParser.clock("7 am"))
        assertEquals(t(15), TimeParser.clock("3pm"))
        assertEquals(t(19, 30), TimeParser.clock("7:30 pm"))
        assertEquals(t(0), TimeParser.clock("12 am"))
        assertEquals(t(6, 30), TimeParser.clock("2026-11-04T06:30"))
        assertNull(TimeParser.clock("bald"))
        assertNull(TimeParser.clock("25:00"))
    }

    @Test
    fun durations() {
        assertEquals(600, TimeParser.durationSeconds("600"))
        assertEquals(600, TimeParser.durationSeconds("10 Minuten"))
        assertEquals(600, TimeParser.durationSeconds("10 min"))
        assertEquals(5400, TimeParser.durationSeconds("1h 30m"))
        assertEquals(5400, TimeParser.durationSeconds("anderthalb Stunden"))
        assertEquals(1800, TimeParser.durationSeconds("eine halbe Stunde"))
        assertEquals(1800, TimeParser.durationSeconds("half an hour"))
        assertEquals(3600, TimeParser.durationSeconds("an hour"))
        assertEquals(90, TimeParser.durationSeconds("90 seconds"))
        assertEquals(150, TimeParser.durationSeconds("2,5 Minuten"))
        assertNull(TimeParser.durationSeconds("gleich"))
        assertNull(TimeParser.durationSeconds("0"))
    }

    @Test
    fun dayWords() {
        assertEquals(w(today.plusDays(1), t(15)), at("morgen um 15 Uhr"))
        assertEquals(w(today.plusDays(1), t(15)), at("tomorrow 3pm"))
        assertEquals(w(today.plusDays(2), t(9)), at("übermorgen 9:00"))
        assertEquals(w(today, t(20)), at("heute 20 Uhr"))
        assertEquals(w(today.plusDays(1), null), at("morgen"))
    }

    @Test
    fun onlyATimeIsTheNextOne() {
        // 15:00 has passed at 18:45 → tomorrow; 20:00 is still today
        assertEquals(w(today.plusDays(1), t(15)), at("15:00"))
        assertEquals(w(today, t(20)), at("um 20 Uhr"))
    }

    @Test
    fun inSomeMinutes() {
        assertEquals(w(today, t(18, 55)), at("in 10 Minuten"))
        assertEquals(w(today, t(20, 45)), at("in 2 hours"))
    }

    @Test
    fun weekdaysAndDates() {
        assertEquals(w(LocalDate.of(2026, 11, 6), t(19)), at("Freitag 19 Uhr"))
        assertEquals(w(LocalDate.of(2026, 11, 6), t(19)), at("Friday at 7 pm"))
        assertEquals(w(LocalDate.of(2026, 11, 10), t(10)), at("nächsten Dienstag 10:00"))
        assertEquals(w(LocalDate.of(2026, 11, 4), t(9)), at("4.11. 9 Uhr"))
        assertEquals(w(LocalDate.of(2026, 12, 24), null), at("24. Dezember"))
        assertEquals(w(LocalDate.of(2027, 1, 2), t(8)), at("Jan 2 8am"))
        // a day that has passed this year is next year's
        assertEquals(w(LocalDate.of(2027, 3, 1), null), at("1.3."))
    }

    @Test
    fun isoFromTheBrain() {
        assertEquals(w(LocalDate.of(2026, 11, 4), t(15)), at("2026-11-04T15:00"))
        assertEquals(w(LocalDate.of(2026, 11, 4), t(15)), at("2026-11-04T15:00:00"))
        assertEquals(w(LocalDate.of(2026, 11, 4), t(15)), at("2026-11-04 15:00"))
        assertEquals(w(LocalDate.of(2026, 11, 4), t(15)), at("2026-11-04T15:00+01:00"))
        assertEquals(w(LocalDate.of(2026, 11, 4), null), at("2026-11-04"))
        assertNull(at("irgendwann"))
    }

    @Test
    fun repeatDays() {
        val mo = DayOfWeek.MONDAY
        val workdays = listOf(mo, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY)
        val weekend = listOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)
        assertEquals(workdays, TimeParser.weekdays("Mo-Fr"))
        assertEquals(workdays, TimeParser.weekdays("werktags"))
        assertEquals(workdays, TimeParser.weekdays("Montag bis Freitag"))
        assertEquals(workdays, TimeParser.weekdays("weekdays"))
        assertEquals(workdays, TimeParser.weekdays("monday to friday"))
        assertEquals(DayOfWeek.entries.toList(), TimeParser.weekdays("täglich"))
        assertEquals(DayOfWeek.entries.toList(), TimeParser.weekdays("every day"))
        assertEquals(weekend, TimeParser.weekdays("am Wochenende"))
        assertEquals(weekend, TimeParser.weekdays("Sa, So"))
        assertEquals(listOf(mo, DayOfWeek.THURSDAY), TimeParser.weekdays("montags und donnerstags"))
        assertEquals(listOf(mo, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY), TimeParser.weekdays("Mon, Wed, Fri"))
        val monWedFri = listOf(mo, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY)
        assertEquals(monWedFri, TimeParser.weekdays("[\"mon\", \"wed\", \"fri\"]"))
        assertEquals(listOf(mo, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY), TimeParser.weekdays("1,3,5"))
        // a range over the weekend wraps around
        assertEquals(listOf(mo, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY, DayOfWeek.SUNDAY), TimeParser.weekdays("Fr-Mo"))
        assertNull(TimeParser.weekdays("bald"))
        assertNull(TimeParser.weekdays(""))
    }
}
