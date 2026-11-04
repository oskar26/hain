package app.hain.tools.internal.phone

import app.hain.tools.api.Args
import app.hain.tools.api.BadArgumentsException
import app.hain.tools.api.PhoneAction
import app.hain.tools.api.Tool
import app.hain.tools.api.ToolContext
import app.hain.tools.api.ToolResult
import app.hain.tools.api.ToolSpecs
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.put
import java.time.DayOfWeek
import java.time.Duration
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

/*
 * Phone actions (T-068): alarm, timer, calendar event, route. All offline. None of them does
 * anything by itself: the result carries a [PhoneAction], the chat shows it as a ConfirmCard, and
 * only the user's tap starts the Android intent. The brain is told so in the result.
 */

/**
 * What the brain reads back: what was prepared, that the user still has to tap, and the sentence
 * to answer with (4d: [say], in the user's language). Host eval: with only the hint, the small
 * brain still wrote "Wecker eingestellt"; with a ready sentence it copies it.
 */
private fun prepared(
    fields: Map<String, String>,
    label: String,
    action: PhoneAction,
    lang: String,
): ToolResult {
    val json =
        JsonObject(
            mapOf("prepared" to JsonPrimitive(action.kind)) +
                fields.mapValues { JsonPrimitive(it.value) } +
                mapOf(
                    "status" to JsonPrimitive("waiting_for_tap"),
                    "say" to JsonPrimitive(say(action.kind, label, lang)),
                    "hint" to JsonPrimitive(TAP_HINT),
                ),
        )
    return ToolResult(forModel = json.toString(), label = label, action = action)
}

/** "Wecker für 06:30 ist vorbereitet – tipp auf der Karte auf „Stellen“." */
internal fun say(
    kind: String,
    label: String,
    lang: String,
): String {
    val de = lang == "de"
    val what =
        when (kind) {
            PhoneAction.SET_ALARM -> if (de) "Der Wecker für $label" else "The alarm for $label"
            PhoneAction.SET_TIMER -> if (de) "Der Timer über $label" else "The $label timer"
            PhoneAction.ADD_EVENT -> if (de) "Der Termin $label" else "The event $label"
            else -> if (de) "Die Route zu $label" else "The route to $label"
        }
    return if (de) {
        "$what ist vorbereitet – tipp auf der Karte auf den Knopf."
    } else {
        "$what is ready – tap the button on the card."
    }
}

private const val TAP_HINT = "Nothing is done yet. Answer with the sentence in say; never say it is set or added."

/** `set_alarm`: AlarmClock.ACTION_SET_ALARM after the tap. */
class SetAlarmTool : Tool {
    override val online = false
    override val schema = ToolSpecs.of(PhoneAction.SET_ALARM).schema

    override suspend fun run(
        args: Map<String, Any?>,
        ctx: ToolContext,
    ): ToolResult {
        val raw = Args.required(args, "time")
        val time = TimeParser.clock(raw) ?: throw BadArgumentsException("Cannot read the time '$raw'. Use HH:mm.")
        // 4b-2: repeating alarms; days the brain wrote but nobody can read are an error, not "once"
        val days =
            Args
                .string(args, "days")
                ?.let { rawDays ->
                    TimeParser.weekdays(rawDays) ?: throw BadArgumentsException("Cannot read the days '$rawDays'.")
                }.orEmpty()
        val action = PhoneAction.Alarm(time.hour, time.minute, Args.string(args, "label"), days.map { it.value })
        val fields =
            listOfNotNull(
                "time" to action.clock,
                action.label?.let { "label" to it },
                days
                    .takeIf { it.isNotEmpty() }
                    ?.let { d -> "repeats" to d.joinToString(",") { it.name.take(SHORT_DAY) } },
            ).toMap()
        val label = if (days.isEmpty()) action.clock else action.clock + " · " + daysLabel(days, ctx.lang)
        return prepared(fields, label, action, ctx.lang)
    }

    companion object {
        private const val WORK_DAYS = 5
        private const val SHORT_DAY = 3

        /** "täglich", "Mo–Fr", "Sa, So", "Mo, Mi" / "daily", "Mon–Fri", "Sat, Sun", "Mon, Wed". */
        fun daysLabel(
            days: List<DayOfWeek>,
            lang: String,
        ): String {
            val de = lang == "de"
            return when (days) {
                DayOfWeek.entries -> if (de) "täglich" else "daily"
                DayOfWeek.entries.take(WORK_DAYS) -> if (de) "Mo–Fr" else "Mon–Fri"
                listOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY) -> if (de) "Sa, So" else "Sat, Sun"
                else -> {
                    val locale = if (de) Locale.GERMAN else Locale.ENGLISH
                    days.joinToString(", ") { it.getDisplayName(TextStyle.SHORT, locale).trimEnd('.') }
                }
            }
        }
    }
}

/** `set_timer`: AlarmClock.ACTION_SET_TIMER after the tap. */
class SetTimerTool : Tool {
    override val online = false
    override val schema = ToolSpecs.of(PhoneAction.SET_TIMER).schema

    override suspend fun run(
        args: Map<String, Any?>,
        ctx: ToolContext,
    ): ToolResult {
        val raw = listOf("seconds", "duration", "length", "time").firstNotNullOfOrNull { Args.string(args, it) }
        val minutes = Args.string(args, "minutes")?.let { TimeParser.durationSeconds("$it min") }
        val seconds =
            raw?.let { TimeParser.durationSeconds(it) } ?: minutes
                ?: throw BadArgumentsException("Missing 'seconds'.")
        if (seconds > MAX_TIMER_S) throw BadArgumentsException("A timer can run at most 24 hours.")
        val action = PhoneAction.Timer(seconds, Args.string(args, "label"))
        val label = durationLabel(seconds, ctx.lang)
        val fields = listOfNotNull("seconds" to "$seconds", action.label?.let { "label" to it }).toMap()
        return prepared(fields, label, action, ctx.lang)
    }

    companion object {
        private const val MAX_TIMER_S = 24 * 3600

        /** "10 Min.", "1 Std. 30 Min.", "45 Sek." / "10 min", "1 h 30 min", "45 s". */
        fun durationLabel(
            seconds: Int,
            lang: String,
        ): String {
            val d = Duration.ofSeconds(seconds.toLong())
            val de = lang == "de"
            return buildList {
                if (d.toHours() > 0) add("${d.toHours()} ${if (de) "Std." else "h"}")
                if (d.toMinutesPart() > 0) add("${d.toMinutesPart()} ${if (de) "Min." else "min"}")
                if (d.toSecondsPart() > 0) add("${d.toSecondsPart()} ${if (de) "Sek." else "s"}")
            }.joinToString(" ")
        }
    }
}

/** `add_calendar_event`: opens the calendar app's new-event screen, filled in (no calendar permission). */
class AddCalendarEventTool : Tool {
    override val online = false
    override val schema = ToolSpecs.of(PhoneAction.ADD_EVENT).schema

    @Suppress("CyclomaticComplexMethod") // optional arguments, each with its fallback
    override suspend fun run(
        args: Map<String, Any?>,
        ctx: ToolContext,
    ): ToolResult {
        val title = Args.required(args, "title").take(MAX_TITLE)
        val rawStart = Args.string(args, "start") ?: Args.required(args, "date")
        val start =
            TimeParser.dateTime(rawStart, ctx.now)
                ?: throw BadArgumentsException("Cannot read the start '$rawStart'. Use 2026-11-04T15:00.")
        val zone = ctx.now.zone
        val location = Args.string(args, "location")
        val action =
            if (start.time == null) {
                // a whole day: the calendar wants midnight to midnight in UTC
                val begin =
                    start.date
                        .atStartOfDay(java.time.ZoneOffset.UTC)
                        .toInstant()
                        .toEpochMilli()
                PhoneAction.CalendarEvent(title, begin, begin + DAY_MS, location, allDay = true)
            } else {
                val begin = start.date.atTime(start.time).atZone(zone)
                val end =
                    Args
                        .string(args, "end")
                        ?.let { TimeParser.dateTime(it, begin) }
                        ?.let { e -> (e.time ?: start.time).let { e.date.atTime(it).atZone(zone) } }
                        ?.takeIf { it.isAfter(begin) }
                        ?: begin.plusHours(1)
                PhoneAction.CalendarEvent(
                    title,
                    begin.toInstant().toEpochMilli(),
                    end.toInstant().toEpochMilli(),
                    location,
                )
            }
        val locale = if (ctx.lang == "de") Locale.GERMAN else Locale.ENGLISH
        val day = DateTimeFormatter.ofPattern(if (ctx.lang == "de") "EEE d.M." else "EEE MMM d", locale)
        val whenText = start.date.format(day) + (start.time?.let { " " + "%02d:%02d".format(it.hour, it.minute) } ?: "")
        val fields =
            buildMap {
                put("title", title)
                put("start", start.date.toString() + (start.time?.let { " $it" } ?: " (all day)"))
                location?.let { put("location", it) }
            }
        return prepared(fields, "$title · $whenText", action, ctx.lang)
    }

    private companion object {
        const val MAX_TITLE = 120
        const val DAY_MS = 24 * 3600 * 1000L
    }
}

/** `navigate`: `geo:0,0?q=<destination>`; the phone lets the user pick the map app. */
class NavigateTool : Tool {
    override val online = false
    override val schema = ToolSpecs.of(PhoneAction.NAVIGATE).schema

    override suspend fun run(
        args: Map<String, Any?>,
        ctx: ToolContext,
    ): ToolResult {
        val destination = (Args.string(args, "destination") ?: Args.required(args, "place")).take(MAX_PLACE)
        val mode = Args.string(args, "mode")?.lowercase()?.takeIf { it in MODES }
        val action = PhoneAction.Navigate(destination, mode)
        val fields = listOfNotNull("destination" to destination, mode?.let { "mode" to it }).toMap()
        return prepared(fields, destination, action, ctx.lang)
    }

    private companion object {
        const val MAX_PLACE = 160
        val MODES = setOf("walk", "drive", "bike", "transit")
    }
}
