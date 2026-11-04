package app.hain.tools.api

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray

/**
 * Something Pim wants to do on the phone (T-068): set an alarm or a timer, put an event in the
 * calendar, start a route. A tool only *suggests* it; the chat shows a ConfirmCard and only the
 * user's tap starts the Android intent (TOOLS.md §2, "Intent tools never fire directly").
 */
sealed interface PhoneAction {
    /** Tool name, also the key in the stored JSON. */
    val kind: String

    data class Alarm(
        val hour: Int,
        val minute: Int,
        val label: String? = null,
        /** 4b-2: days the alarm repeats on, ISO numbers 1 = Monday … 7 = Sunday, sorted; empty = once. */
        val days: List<Int> = emptyList(),
    ) : PhoneAction {
        override val kind = SET_ALARM

        /** "06:30". */
        val clock: String get() = "%02d:%02d".format(hour, minute)

        /** The [days] as `java.util.Calendar` numbers (Sunday = 1 … Saturday = 7) for `AlarmClock.EXTRA_DAYS`. */
        val calendarDays: List<Int> get() = days.map { it % DAYS_PER_WEEK + 1 }
    }

    data class Timer(
        val seconds: Int,
        val label: String? = null,
    ) : PhoneAction {
        override val kind = SET_TIMER
    }

    data class CalendarEvent(
        val title: String,
        val startMillis: Long,
        val endMillis: Long,
        val location: String? = null,
        val allDay: Boolean = false,
    ) : PhoneAction {
        override val kind = ADD_EVENT
    }

    data class Navigate(
        val destination: String,
        /** walk, drive, bike or transit; `null` = let the map app choose. */
        val mode: String? = null,
    ) : PhoneAction {
        override val kind = NAVIGATE
    }

    companion object {
        const val SET_ALARM = "set_alarm"
        const val SET_TIMER = "set_timer"
        const val ADD_EVENT = "add_calendar_event"
        const val NAVIGATE = "navigate"
        const val DAYS_PER_WEEK = 7

        fun toJson(action: PhoneAction): JsonObject =
            buildJsonObject {
                put("kind", action.kind)
                when (action) {
                    is Alarm -> {
                        put("hour", action.hour)
                        put("minute", action.minute)
                        action.label?.let { put("label", it) }
                        if (action.days.isNotEmpty()) {
                            putJsonArray("days") { action.days.forEach { add(JsonPrimitive(it)) } }
                        }
                    }
                    is Timer -> {
                        put("seconds", action.seconds)
                        action.label?.let { put("label", it) }
                    }
                    is CalendarEvent -> {
                        put("title", action.title)
                        put("start", action.startMillis)
                        put("end", action.endMillis)
                        action.location?.let { put("location", it) }
                        put("allDay", action.allDay)
                    }
                    is Navigate -> {
                        put("destination", action.destination)
                        action.mode?.let { put("mode", it) }
                    }
                }
            }

        /** Back from [toJson]; `null` for anything unknown or broken. */
        @Suppress("ReturnCount", "CyclomaticComplexMethod") // one line per kind
        fun fromJson(o: JsonObject): PhoneAction? {
            fun str(k: String) = (o[k] as? JsonPrimitive)?.takeIf { it.isString }?.content

            fun int(k: String) = (o[k] as? JsonPrimitive)?.intOrNull

            fun long(k: String) = (o[k] as? JsonPrimitive)?.longOrNull
            return when (str("kind")) {
                SET_ALARM ->
                    Alarm(int("hour") ?: return null, int("minute") ?: return null, str("label"), days(o["days"]))
                SET_TIMER -> Timer(int("seconds") ?: return null, str("label"))
                ADD_EVENT ->
                    CalendarEvent(
                        str("title") ?: return null,
                        long("start") ?: return null,
                        long("end") ?: return null,
                        str("location"),
                        (o["allDay"] as? JsonPrimitive)?.booleanOrNull == true,
                    )
                NAVIGATE -> Navigate(str("destination") ?: return null, str("mode"))
                else -> null
            }
        }

        /** Repeat days from the stored JSON; anything that is not a day number 1–7 is left out. */
        private fun days(element: Any?): List<Int> =
            (element as? JsonArray)
                ?.mapNotNull { (it as? JsonPrimitive)?.intOrNull }
                ?.filter { it in 1..DAYS_PER_WEEK }
                ?.distinct()
                ?.sorted()
                .orEmpty()

        fun fromJson(text: String): PhoneAction? {
            val o = runCatching { Json.parseToJsonElement(text).jsonObject }.getOrNull() ?: return null
            return fromJson(o)
        }
    }
}
