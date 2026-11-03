package app.hain.tools.phone

import app.hain.llm.api.ToolCall
import app.hain.tools.api.PhoneAction
import app.hain.tools.api.ToolContext
import app.hain.tools.api.ToolRouter
import app.hain.tools.internal.phone.AddCalendarEventTool
import app.hain.tools.internal.phone.NavigateTool
import app.hain.tools.internal.phone.SetAlarmTool
import app.hain.tools.internal.phone.SetTimerTool
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

class PhoneToolsTest {
    private val zone = ZoneId.of("Europe/Berlin")
    private val now = ZonedDateTime.of(2026, 11, 3, 18, 45, 0, 0, zone)
    private val ctx = ToolContext("de", now, offline = true)
    private val router = ToolRouter(listOf(SetAlarmTool(), SetTimerTool(), AddCalendarEventTool(), NavigateTool()))

    private fun millis(
        day: Int,
        hour: Int,
    ) = ZonedDateTime.of(2026, 11, day, hour, 0, 0, 0, zone).toInstant().toEpochMilli()

    @Test
    fun phoneToolsWorkOfflineAndAreInThePrompt() {
        val names = router.schemas(offline = true).map { it.name }
        assertEquals(listOf("set_alarm", "set_timer", "add_calendar_event", "navigate"), names)
    }

    @Test
    fun alarmIsOnlyPrepared() =
        runTest {
            val r = router.run(ToolCall("1", "set_alarm", mapOf("time" to "6:30")), ctx)
            assertNull(r.error)
            assertEquals(PhoneAction.Alarm(6, 30), r.action)
            assertEquals("06:30", r.label)
            assertTrue(r.forModel.contains("waiting_for_tap"))
        }

    @Test
    fun timerFromSecondsOrWords() =
        runTest {
            val a = router.run(ToolCall("1", "set_timer", mapOf("seconds" to 600)), ctx)
            assertEquals(PhoneAction.Timer(600), a.action)
            assertEquals("10 Min.", a.label)
            val b = router.run(ToolCall("2", "set_timer", mapOf("seconds" to "10 Minuten", "label" to "Nudeln")), ctx)
            assertEquals(PhoneAction.Timer(600, "Nudeln"), b.action)
            val c = router.run(ToolCall("3", "set_timer", mapOf("minutes" to "25")), ctx)
            assertEquals(PhoneAction.Timer(1500), c.action)
            assertEquals("1 Std. 30 Min.", SetTimerTool.durationLabel(5400, "de"))
            assertEquals("45 s", SetTimerTool.durationLabel(45, "en"))
        }

    @Test
    fun calendarTomorrowAtThree() =
        runTest {
            val r =
                router.run(
                    ToolCall("1", "add_calendar_event", mapOf("title" to "Zahnarzt", "start" to "morgen um 15 Uhr")),
                    ctx,
                )
            assertEquals(PhoneAction.CalendarEvent("Zahnarzt", millis(4, 15), millis(4, 16)), r.action)
            assertEquals("Zahnarzt · Mi. 4.11. 15:00", r.label)
            val iso =
                router.run(
                    ToolCall(
                        "2",
                        "add_calendar_event",
                        mapOf("title" to "Zahnarzt", "start" to "2026-11-04T15:00", "end" to "2026-11-04T15:30"),
                    ),
                    ctx,
                )
            val event = iso.action as PhoneAction.CalendarEvent
            assertEquals(millis(4, 15), event.startMillis)
            assertEquals(millis(4, 15) + 30 * 60_000, event.endMillis)
        }

    @Test
    fun calendarWholeDay() =
        runTest {
            val r = router.run(ToolCall("1", "add_calendar_event", mapOf("title" to "Oma", "start" to "morgen")), ctx)
            val event = r.action as PhoneAction.CalendarEvent
            assertTrue(event.allDay)
            assertEquals(24 * 3600 * 1000L, event.endMillis - event.startMillis)
        }

    @Test
    fun badTimeIsAShortError() =
        runTest {
            val r = router.run(ToolCall("1", "set_alarm", mapOf("time" to "bald")), ctx)
            assertEquals("bad_arguments", r.error)
            assertNull(r.action)
        }

    @Test
    fun navigateToTheCathedral() =
        runTest {
            val r = router.run(ToolCall("1", "navigate", mapOf("destination" to "Kölner Dom", "mode" to "Walk")), ctx)
            assertEquals(PhoneAction.Navigate("Kölner Dom", "walk"), r.action)
            assertEquals("Kölner Dom", r.label)
        }

    @Test
    fun actionsSurviveJson() {
        val all =
            listOf(
                PhoneAction.Alarm(6, 30, "Joggen"),
                PhoneAction.Timer(600),
                PhoneAction.CalendarEvent("Zahnarzt", millis(4, 15), millis(4, 16), "Ringstraße 1"),
                PhoneAction.Navigate("Kölner Dom", null),
            )
        all.forEach { assertEquals(it, PhoneAction.fromJson(PhoneAction.toJson(it).toString())) }
        assertNull(PhoneAction.fromJson("""{"kind":"launch_rocket"}"""))
    }
}
