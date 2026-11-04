package app.hain.feature.chat.api

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.AlarmClock
import android.provider.CalendarContract
import app.hain.feature.chat.internal.ActionEvent
import app.hain.tools.api.PhoneAction

/**
 * The Android intent for a confirmed phone action (T-068). Only called after the tap on a
 * ConfirmCard. No calendar or location permission: the calendar app opens its own "new event"
 * screen filled in, and `geo:` lets the phone pick the map app.
 */
internal object PhoneIntents {
    fun of(action: PhoneAction): Intent =
        when (action) {
            is PhoneAction.Alarm ->
                Intent(AlarmClock.ACTION_SET_ALARM)
                    .putExtra(AlarmClock.EXTRA_HOUR, action.hour)
                    .putExtra(AlarmClock.EXTRA_MINUTES, action.minute)
                    .putExtra(AlarmClock.EXTRA_SKIP_UI, false)
                    .apply { action.label?.let { putExtra(AlarmClock.EXTRA_MESSAGE, it) } }
                    // 4b-2: a repeating alarm (Calendar day numbers, Sunday = 1)
                    .apply {
                        if (action.days.isNotEmpty()) {
                            putIntegerArrayListExtra(AlarmClock.EXTRA_DAYS, ArrayList(action.calendarDays))
                        }
                    }
            is PhoneAction.Timer ->
                Intent(AlarmClock.ACTION_SET_TIMER)
                    .putExtra(AlarmClock.EXTRA_LENGTH, action.seconds)
                    .putExtra(AlarmClock.EXTRA_SKIP_UI, false)
                    .apply { action.label?.let { putExtra(AlarmClock.EXTRA_MESSAGE, it) } }
            is PhoneAction.CalendarEvent ->
                Intent(Intent.ACTION_INSERT)
                    .setData(CalendarContract.Events.CONTENT_URI)
                    .putExtra(CalendarContract.Events.TITLE, action.title)
                    .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, action.startMillis)
                    .putExtra(CalendarContract.EXTRA_EVENT_END_TIME, action.endMillis)
                    .putExtra(CalendarContract.EXTRA_EVENT_ALL_DAY, action.allDay)
                    .apply { action.location?.let { putExtra(CalendarContract.Events.EVENT_LOCATION, it) } }
            is PhoneAction.Navigate ->
                Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=" + Uri.encode(action.destination)))
        }

    /** Starts [action]; [ActionEvent.NO_APP] when the phone has no app for it. */
    @Suppress("SwallowedException") // no app is an answer for the card, not an error
    fun launch(
        context: Context,
        action: PhoneAction,
    ): ActionEvent =
        try {
            context.startActivity(of(action))
            ActionEvent.CONFIRMED
        } catch (e: ActivityNotFoundException) {
            ActionEvent.NO_APP
        } catch (e: SecurityException) {
            ActionEvent.NO_APP
        }
}
