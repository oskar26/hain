package app.hain.tools.api

import app.hain.llm.api.ToolCall
import app.hain.tools.internal.phone.TimeParser
import java.util.Locale

/**
 * T-267 (#44): what the user's own words say about an alarm, so a `set_alarm` call of the small brain
 * gets it right even when the brain forgets or miscounts it:
 * - "Weck mich jeden Samstag und Sonntag um 8" → `days` = Saturday + Sunday, when the call has none;
 * - "und 15 Minuten früher" after an alarm for 07:00 → `time` = 06:45 (the brain often wrote 07:15 or 06:15).
 * Pure, tested; only `set_alarm` calls are touched.
 */
object AlarmHints {
    private const val ALARM = "set_alarm"
    private const val MINUTES_PER_HOUR = 60
    private const val MINUTES_PER_DAY = 1440
    private const val QUARTER = 15
    private const val HALF = 30

    /** The call with days and time from [message] (the user's newest message) and the [previousTime] alarm. */
    fun fix(
        call: ToolCall,
        message: String,
        previousTime: String?,
    ): ToolCall {
        if (call.name != ALARM) return call
        val args = call.arguments.toMutableMap()
        if (args["days"]?.toString().isNullOrBlank()) days(message)?.let { args["days"] = it }
        val shift = shiftMinutes(message)
        val moved = if (shift != null && previousTime != null) shifted(previousTime, shift) else null
        if (moved != null) args["time"] = moved
        return if (args == call.arguments) call else call.copy(arguments = args)
    }

    /** The time of the newest `set_alarm` call in [calls] (oldest first), or `null`. */
    fun lastTime(calls: List<ToolCall>): String? =
        calls
            .lastOrNull { it.name == ALARM }
            ?.arguments
            ?.get("time")
            ?.toString()
            ?.takeIf { it.isNotBlank() }

    /**
     * Repeat days the user asked for, as ISO numbers ("6,7"), or `null` when the message does not say
     * the alarm repeats ("jeden", "immer", "montags", "werktags", "every" …). Short forms like "so" or
     * "mo" only count in a range ("Mo-Fr"): "so" is also an ordinary word.
     */
    fun days(message: String): String? {
        val t = message.lowercase(Locale.GERMAN)
        if (!REPEAT.containsMatchIn(t)) return null
        // numbers are times here ("um 7"), not ISO day numbers
        val words = t.replace(DIGITS, " ")
        val text = if (RANGE.containsMatchIn(words)) words else words.replace(SHORT_DAY, " ")
        return TimeParser.weekdays(text)?.joinToString(",") { it.value.toString() }
    }

    /** "15 Minuten früher" → −15, "eine halbe Stunde später" → +30, "1 hour earlier" → −60; else `null`. */
    fun shiftMinutes(message: String): Int? {
        val m = SHIFT.find(message.lowercase(Locale.GERMAN)) ?: return null
        val (rawAmount, unit, direction) = m.destructured
        val amount = rawAmount.trim()
        val minutes =
            when {
                amount.contains("halb") || amount.contains("half") -> HALF
                amount.contains("viertel") || amount.contains("quarter") -> QUARTER
                else -> amount.toIntOrNull() ?: 1
            }
        val total =
            if (unit.startsWith("stund") || unit.startsWith("hour")) {
                if (minutes == HALF || minutes == QUARTER) minutes else minutes * MINUTES_PER_HOUR
            } else {
                minutes
            }
        return if (EARLIER.containsMatchIn(direction)) -total else total
    }

    /** "07:00" shifted by −15 minutes → "06:45" (over midnight too); `null` when [time] cannot be read. */
    fun shifted(
        time: String,
        minutes: Int,
    ): String? {
        val t = TimeParser.clock(time) ?: return null
        val raw = t.hour * MINUTES_PER_HOUR + t.minute + minutes
        val total = (raw % MINUTES_PER_DAY + MINUTES_PER_DAY) % MINUTES_PER_DAY
        return "%02d:%02d".format(Locale.ROOT, total / MINUTES_PER_HOUR, total % MINUTES_PER_HOUR)
    }

    private val REPEAT =
        Regex(
            "\\b(?:jeden|jede|jedes|immer|alle|every|each)\\b|täglich|werktags|wochenende|weekend|weekday|" +
                "daily|\\b(?:mon|diens|donners|frei|sams|sonn)tags\\b|\\bmittwochs\\b|" +
                "\\b(?:mon|tues|wednes|thurs|fri|satur|sun)days\\b",
        )
    private val RANGE = Regex("\\b(?:mo|mon)\\.?\\s*(?:-|–|bis|to)\\s*(?:fr|fri|sa|sat|so|sun)\\b")
    private val SHORT_DAY = Regex("\\b(?:mo|di|mi|do|fr|sa|so|mon|tue|wed|thu|fri|sat|sun)\\b")
    private val SHIFT =
        Regex(
            "(\\d{1,3}|eine (?:viertel|halbe)|einer (?:viertel|halben)|eine|einer|" +
                "an? (?:quarter of an|half an)|an?|half an)\\s*" +
                "(minuten|minute|min|stunden|stunde|hours|hour|minutes|mins)\\.?\\s*(früher|eher|später|earlier|later)",
        )
    private val EARLIER = Regex("früher|eher|earlier")
    private val DIGITS = Regex("\\d+")
}
