package app.hain.feature.chat.api

import android.content.Intent
import android.provider.AlarmClock
import android.provider.CalendarContract
import app.hain.tools.api.PhoneAction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** 4b-2: the Android intents behind the ConfirmCard carry the right extras (T-068). */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PhoneIntentsTest {
    @Test
    fun alarmOnceAndRepeating() {
        val once = PhoneIntents.of(PhoneAction.Alarm(6, 30, "Joggen"))
        assertEquals(AlarmClock.ACTION_SET_ALARM, once.action)
        assertEquals(6, once.getIntExtra(AlarmClock.EXTRA_HOUR, -1))
        assertEquals(30, once.getIntExtra(AlarmClock.EXTRA_MINUTES, -1))
        assertEquals("Joggen", once.getStringExtra(AlarmClock.EXTRA_MESSAGE))
        // the clock app shows the alarm before it is saved
        assertFalse(once.getBooleanExtra(AlarmClock.EXTRA_SKIP_UI, true))
        assertFalse(once.hasExtra(AlarmClock.EXTRA_DAYS))

        val weekdays = PhoneIntents.of(PhoneAction.Alarm(7, 0, days = listOf(1, 2, 3, 4, 5)))
        // Calendar numbers: Monday = 2 … Friday = 6
        assertEquals(arrayListOf(2, 3, 4, 5, 6), weekdays.getIntegerArrayListExtra(AlarmClock.EXTRA_DAYS))
        assertFalse(weekdays.hasExtra(AlarmClock.EXTRA_MESSAGE))
        val sunday = PhoneIntents.of(PhoneAction.Alarm(9, 0, days = listOf(7)))
        assertEquals(arrayListOf(1), sunday.getIntegerArrayListExtra(AlarmClock.EXTRA_DAYS))
    }

    @Test
    fun timer() {
        val i = PhoneIntents.of(PhoneAction.Timer(600, "Nudeln"))
        assertEquals(AlarmClock.ACTION_SET_TIMER, i.action)
        assertEquals(600, i.getIntExtra(AlarmClock.EXTRA_LENGTH, -1))
        assertEquals("Nudeln", i.getStringExtra(AlarmClock.EXTRA_MESSAGE))
        assertFalse(i.getBooleanExtra(AlarmClock.EXTRA_SKIP_UI, true))
    }

    @Test
    fun calendarEventOpensTheFilledInForm() {
        val i = PhoneIntents.of(PhoneAction.CalendarEvent("Zahnarzt", 1_000L, 4_600_000L, "Ringstraße 1"))
        assertEquals(Intent.ACTION_INSERT, i.action)
        assertEquals(CalendarContract.Events.CONTENT_URI, i.data)
        assertEquals("Zahnarzt", i.getStringExtra(CalendarContract.Events.TITLE))
        assertEquals(1_000L, i.getLongExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, -1))
        assertEquals(4_600_000L, i.getLongExtra(CalendarContract.EXTRA_EVENT_END_TIME, -1))
        assertFalse(i.getBooleanExtra(CalendarContract.EXTRA_EVENT_ALL_DAY, true))
        assertEquals("Ringstraße 1", i.getStringExtra(CalendarContract.Events.EVENT_LOCATION))
    }

    @Test
    fun routeLetsThePhonePickTheMapApp() {
        val i = PhoneIntents.of(PhoneAction.Navigate("Kölner Dom"))
        assertEquals(Intent.ACTION_VIEW, i.action)
        assertEquals("geo:0,0?q=K%C3%B6lner%20Dom", i.data.toString())
    }
}
