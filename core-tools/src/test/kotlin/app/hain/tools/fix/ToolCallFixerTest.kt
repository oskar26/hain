package app.hain.tools.fix

import app.hain.llm.api.ToolCall
import app.hain.tools.api.ToolCallFixer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

class ToolCallFixerTest {
    private val fixer = ToolCallFixer()

    // Wednesday, 4 November 2026, 10:00 in Berlin
    private val now = ZonedDateTime.of(2026, 11, 4, 10, 0, 0, 0, ZoneId.of("Europe/Berlin"))

    private fun fix(
        name: String,
        vararg args: Pair<String, Any?>,
    ) = fixer.fix(ToolCall("c", name, mapOf(*args)), now)

    @Test
    fun wrongToolNamesBecomeRealOnes() {
        assertEquals("web_search", fix("search", "q" to "Deutschlandticket").call.name)
        assertEquals("weather", fix("get_weather", "city" to "Köln").call.name)
        assertEquals("calculate", fix("Calculator", "expr" to "2*3").call.name)
        assertEquals("wikipedia", fix("functions.wiki", "query" to "Ada").call.name)
        assertEquals("nope", fix("nope").call.name)
        assertNull(fixer.toolName("nope"))
    }

    @Test
    fun parameterAliasesAndUnknownKeys() {
        val c = fix("weather", "City" to "Köln", "mood" to "happy").call
        assertEquals(mapOf("place" to "Köln"), c.arguments)
        assertEquals(mapOf("query" to "COP30 Belém"), fix("web_search", "search_query" to "COP30 Belém").call.arguments)
        assertEquals(mapOf("topic" to "Ada Lovelace"), fix("wikipedia", "q" to "'Ada Lovelace'").call.arguments)
    }

    @Test
    fun realNameWinsOverAlias() {
        val c = fix("web_search", "query" to "a", "q" to "b").call
        assertEquals("a", c.arguments["query"])
    }

    @Test
    fun enumValuesInOwnWords() {
        assertEquals("bike", fix("navigate", "destination" to "Hbf", "mode" to "Fahrrad").call.arguments["mode"])
        assertEquals("walk", fix("navigate", "ziel" to "Dom", "mode" to "zu Fuß").call.arguments["mode"])
        assertEquals("transit", fix("navigate", "destination" to "Dom", "mode" to "Bus").call.arguments["mode"])
        assertEquals("de", fix("wikipedia", "topic" to "Köln", "language" to "German").call.arguments["lang"])
    }

    @Test
    fun numbersFromText() {
        assertEquals(600, fix("set_timer", "seconds" to "600").call.arguments["seconds"])
        assertEquals(600, fix("set_timer", "duration" to "10 min").call.arguments["seconds"])
        assertEquals(1500, fix("set_timer", "minutes" to "25").call.arguments["seconds"])
        assertEquals(5400, fix("set_timer", "hours" to 1, "minutes" to 30).call.arguments["seconds"])
        assertEquals(480, fix("set_timer", "minutes" to 8).call.arguments["seconds"])
        assertEquals(30, fix("set_timer", "minutes" to 0.5).call.arguments["seconds"])
        val timer = fix("set_timer", "min" to "10", "title" to "Nudeln").call.arguments
        assertEquals(mapOf("seconds" to 600, "label" to "Nudeln"), timer)
        assertNull(fix("set_timer", "minutes" to 8).problem)
        assertEquals(true, fix("set_timer", "label" to "x").problem?.contains("how long"))
        assertEquals(2, fix("weather", "place" to "Köln", "days" to "morgen").call.arguments["days"])
        assertEquals(3, fix("weather", "place" to "Köln", "day" to "übermorgen").call.arguments["days"])
    }

    @Test
    fun datesRelativeToNow() {
        val tomorrow = fix("add_calendar_event", "title" to "Zahnarzt", "start" to "morgen 15 Uhr").call
        assertEquals("2026-11-05T15:00", tomorrow.arguments["start"])
        val friday = fix("add_calendar_event", "title" to "Kino", "start" to "Freitag 18:00").call
        assertEquals("2026-11-06T18:00", friday.arguments["start"])
        val split = fix("add_calendar_event", "title" to "Kino", "date" to "2026-11-06", "time" to "18:00").call
        assertEquals("2026-11-06T18:00", split.arguments["start"])
        val unreadable = fix("add_calendar_event", "title" to "X", "start" to "irgendwann").call
        assertEquals("irgendwann", unreadable.arguments["start"])
    }

    @Test
    fun missingRequiredValueAsksTheUser() {
        assertNotNull(fix("set_alarm").problem)
        assertNotNull(fix("wikipedia").problem)
        assertNull(fix("weather", "place" to "Köln").problem)
        // 3b-2: weather without a place is allowed; the tool uses the location or asks for the city
        assertNull(fix("weather", "days" to 2).problem)
    }

    @Test
    fun duplicatesRunOnce() {
        val a = ToolCall("1", "weather", mapOf("place" to "Köln"))
        val b = ToolCall("2", "weather", mapOf("place" to " köln "))
        val c = ToolCall("3", "weather", mapOf("place" to "Bonn"))
        assertEquals(listOf("1", "3"), ToolCallFixer.dedupe(listOf(a, b, c)).map { it.id })
        assertEquals(ToolCallFixer.key(a), ToolCallFixer.key(b))
    }
}
