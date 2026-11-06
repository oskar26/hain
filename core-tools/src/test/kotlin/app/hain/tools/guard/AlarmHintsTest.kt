package app.hain.tools.guard

import app.hain.llm.api.ToolCall
import app.hain.tools.api.AlarmHints
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

/** T-267: repeat days and "15 Minuten früher" from the user's words. */
class AlarmHintsTest {
    private fun alarm(vararg args: Pair<String, Any?>) = ToolCall("1", "set_alarm", mapOf(*args))

    @Test
    fun repeatDaysFromTheMessage() {
        assertEquals("6,7", AlarmHints.days("Weck mich jeden Samstag und Sonntag um 8"))
        assertEquals("1,2,3,4,5", AlarmHints.days("weck mich werktags um 6:30"))
        assertEquals("1,2,3,4,5", AlarmHints.days("Wecker Mo-Fr immer um 7"))
        assertEquals("1,4", AlarmHints.days("montags und donnerstags um 7 wecken"))
        assertEquals("6,7", AlarmHints.days("wake me every Saturday and Sunday at 9"))
        // once only: no repeat word, and "so" is an ordinary word
        assertNull(AlarmHints.days("Weck mich morgen um 7"))
        assertNull(AlarmHints.days("weck mich so um 7 am Samstag"))
    }

    @Test
    fun shiftsAreCountedFromThePreviousAlarm() {
        assertEquals(-15, AlarmHints.shiftMinutes("und 15 Minuten früher"))
        assertEquals(30, AlarmHints.shiftMinutes("lieber eine halbe Stunde später"))
        assertEquals(-60, AlarmHints.shiftMinutes("one more: 1 hour earlier"))
        assertEquals(-15, AlarmHints.shiftMinutes("a quarter of an hour earlier"))
        assertNull(AlarmHints.shiftMinutes("um 7 bitte"))
        assertEquals("06:45", AlarmHints.shifted("07:00", -15))
        assertEquals("23:50", AlarmHints.shifted("00:05", -15))
    }

    @Test
    fun theCallGetsDaysAndTheCountedTime() {
        val fixed = AlarmHints.fix(alarm("time" to "07:15"), "und 15 Minuten früher", previousTime = "07:00")
        assertEquals("06:45", fixed.arguments["time"])
        val days = AlarmHints.fix(alarm("time" to "08:00"), "Weck mich jeden Samstag und Sonntag um 8", null)
        assertEquals("6,7", days.arguments["days"])
        // days the brain wrote stay; other tools are not touched
        val own = alarm("time" to "08:00", "days" to "Sa")
        assertSame(own, AlarmHints.fix(own, "jeden Samstag und Sonntag", null))
        val timer = ToolCall("2", "set_timer", mapOf("seconds" to 60))
        assertSame(timer, AlarmHints.fix(timer, "15 Minuten früher", "07:00"))
        assertEquals("07:00", AlarmHints.lastTime(listOf(alarm("time" to "07:00"), timer)))
    }

    // T-282 (#44): the brain called no set_alarm at all
    @Test
    fun aClearAlarmRequestBecomesACallFromTheWords() {
        val halfSeven = AlarmHints.request("stell mir nen wecker um halb sieben", null)
        assertEquals(mapOf("time" to "06:30"), halfSeven?.arguments)
        assertEquals(mapOf("time" to "07:00"), AlarmHints.request("weck mich morgen um 7", null)?.arguments)
        assertEquals(mapOf("time" to "06:45"), AlarmHints.request("set an alarm for 6:45am", null)?.arguments)
        assertEquals("set_alarm", AlarmHints.request("Kannst du mich um 6 Uhr wecken?", null)?.name)
        assertEquals(
            mapOf("time" to "08:00", "days" to "6,7"),
            AlarmHints.request("Weck mich jeden Samstag und Sonntag um 8", null)?.arguments,
        )
    }

    @Test
    fun aFollowUpMovesTheLastAlarm() {
        assertEquals(mapOf("time" to "06:45"), AlarmHints.request("und 15 Minuten früher", "07:00")?.arguments)
        assertEquals(
            mapOf("time" to "07:30"),
            AlarmHints.request("stell den Wecker eine halbe Stunde später", "07:00")?.arguments,
        )
        assertNull(AlarmHints.request("und 15 Minuten früher", null))
    }

    @Test
    fun noRequestNoCall() {
        assertNull(AlarmHints.request("Wie stelle ich einen Wecker auf dem Handy?", null))
        assertNull(AlarmHints.request("lösch den Wecker um 7", null))
        assertNull(AlarmHints.request("weck mich bald", null))
        assertNull(AlarmHints.request("stell einen Timer auf 10 Minuten", null))
        assertNull(AlarmHints.request("Was ist um 7 im Fernsehen?", null))
    }
}
