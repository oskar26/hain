package app.hain.tools.api

import app.hain.llm.api.ToolCall
import app.hain.tools.internal.phone.TimeParser
import java.time.ZonedDateTime
import java.util.Locale

/**
 * Makes the small brain's tool calls usable before they run (4d, T-183):
 *  - wrong tool names ("search", "get_weather") and parameter names ("city", "q") → the real ones,
 *  - enum values in the brain's own words ("zu Fuß", "car") → the fixed value,
 *  - split values joined (`date` + `time` → `start`), relative days ("morgen") → numbers,
 *  - text numbers ("600", "10 min") → integers,
 *  - a missing required value → [Fixed.problem], a short sentence that makes the brain ask the user.
 * Duplicates (same tool, same arguments) are merged by [dedupe].
 */
class ToolCallFixer(
    private val specs: Map<String, ToolSpec> = ToolSpecs.all,
) {
    /** A call after fixing; [problem] is set when it cannot run as it is. */
    data class Fixed(
        val call: ToolCall,
        val problem: String? = null,
    )

    private val byAlias: Map<String, String> =
        specs.values
            .flatMap { s -> s.names.map { it.lowercase() to s.name } }
            .toMap()

    /** The real name of [name], or `null` when no tool fits. */
    fun toolName(name: String): String? {
        val key =
            name
                .trim()
                .lowercase()
                .removePrefix("functions.")
                .replace('-', '_')
        return when {
            key in specs -> key
            key in byAlias -> byAlias[key]
            else -> null
        }
    }

    fun fix(
        call: ToolCall,
        now: ZonedDateTime? = null,
    ): Fixed {
        val name = toolName(call.name) ?: return Fixed(call)
        val spec = specs.getValue(name)
        val args = linkedMapOf<String, Any?>()
        call.arguments.forEach { (rawKey, value) ->
            val key = rawKey.trim().lowercase()
            val real = if (spec.param(key) != null) key else spec.aliases[key] ?: key
            if (real !in args && !isBlank(value)) args[real] = clean(value)
        }
        joinSplitStart(name, call.arguments, args)
        if (name == TIMER) timerSeconds(call.arguments, args)
        spec.params.forEach { p -> args[p.name]?.let { args[p.name] = value(p, it, now) } }
        // unknown keys are dropped: tools never read them and they confuse the stored history
        args.keys.retainAll(spec.params.map { it.name }.toSet() + TOOL_ONLY)
        val fixed = call.copy(name = name, arguments = args)
        val missing = spec.required.firstOrNull { (TOOL_NAMES[it] ?: it) !in args }
        return Fixed(fixed, missing?.let { problem(name, it) })
    }

    private fun value(
        p: ParamSpec,
        v: Any?,
        now: ZonedDateTime?,
    ): Any? =
        when {
            p.enum.isNotEmpty() && v is String -> v.trim().lowercase().let { p.aliases[it] ?: it }
            p.type == "integer" && v is String -> integer(p.name, v) ?: v
            now != null && v is String && p.name in DATE_PARAMS -> isoDate(v, now) ?: v
            else -> v
        }

    /** "morgen 15 Uhr" → "2026-11-05T15:00" relative to the turn's [now]; a whole day → "2026-11-05". */
    private fun isoDate(
        text: String,
        now: ZonedDateTime,
    ): String? {
        val w = TimeParser.dateTime(text, now) ?: return null
        return w.date.toString() + (w.time?.let { "T%02d:%02d".format(Locale.ROOT, it.hour, it.minute) } ?: "")
    }

    /** "600" → 600, "10 min" → 600 for `seconds`, "morgen" → 2 for `days`. */
    private fun integer(
        name: String,
        text: String,
    ): Int? {
        val t = text.trim().lowercase()
        t.toDoubleOrNull()?.let { return it.toInt() }
        return when (name) {
            "seconds" -> TimeParser.durationSeconds(t)
            "days" -> RELATIVE_DAYS.entries.firstOrNull { (word, _) -> word in t }?.value
            else -> null
        }
    }

    /** `add_calendar_event` with `date` and `time` (or `day` + `time`) instead of one `start`. */
    private fun joinSplitStart(
        name: String,
        raw: Map<String, Any?>,
        args: MutableMap<String, Any?>,
    ) {
        if (name != CALENDAR || args.containsKey("start")) return
        val lower = raw.mapKeys { it.key.lowercase() }
        val date = lower["date"] ?: lower["day"]
        val time = lower["time"] ?: lower["hour"]
        if (date != null) args["start"] = listOfNotNull(date, time).joinToString(" ") { it.toString().trim() }
    }

    /**
     * `set_timer`: the brain sees `minutes` (host eval 4d: with `seconds` it sent 8 for "8 min"); the
     * tool gets `seconds`. Also read: `seconds`, `hours`, and a text length ("10 min", "1h 30m").
     */
    private fun timerSeconds(
        raw: Map<String, Any?>,
        args: MutableMap<String, Any?>,
    ) {
        val lower = raw.mapKeys { it.key.trim().lowercase() }

        fun number(vararg keys: String): Double? =
            keys.firstNotNullOfOrNull { k ->
                lower[k]
                    ?.toString()
                    ?.trim()
                    ?.replace(',', '.')
                    ?.toDoubleOrNull()
            }

        fun text(vararg keys: String): Int? =
            keys.firstNotNullOfOrNull { k -> lower[k]?.toString()?.let { TimeParser.durationSeconds(it) } }
        val minutes = number("minutes", "minuten", "min", "mins")
        val seconds = number("seconds", "secs", "sekunden")
        val hours = number("hours", "stunden")
        val total =
            if (minutes != null || seconds != null || hours != null) {
                ((minutes ?: 0.0) * MINUTE + (seconds ?: 0.0) + (hours ?: 0.0) * HOUR).toInt()
            } else {
                text("minutes", "minuten", "seconds", "duration", "length", "time", "dauer")
            }
        args.remove("minutes")
        if (total != null && total > 0) args["seconds"] = total
    }

    private fun problem(
        tool: String,
        param: String,
    ): String {
        val ask = ASK[param] ?: "Ask the user for the missing '$param'."
        return "The call $tool needs '$param'. $ask"
    }

    companion object {
        private const val CALENDAR = "add_calendar_event"
        private val DATE_PARAMS = setOf("start", "end")
        private const val TIMER = "set_timer"
        private const val MINUTE = 60
        private const val HOUR = 3600

        /** Values the tool reads under another name than the brain sees (`minutes` → `seconds`). */
        private val TOOL_NAMES = mapOf("minutes" to "seconds")
        private val TOOL_ONLY = setOf("seconds")

        private val RELATIVE_DAYS =
            linkedMapOf(
                "übermorgen" to 3,
                "day after tomorrow" to 3,
                "morgen" to 2,
                "tomorrow" to 2,
                "heute" to 1,
                "today" to 1,
                "woche" to 7,
                "week" to 7,
            )

        private val ASK =
            mapOf(
                "place" to "Ask the user which city they mean.",
                "time" to "Ask the user for the time.",
                "minutes" to "Ask the user how long the timer should run.",
                "start" to "Ask the user for the day and time.",
                "title" to "Ask the user what the event is.",
                "destination" to "Ask the user where they want to go.",
                "url" to "Ask the user for the link.",
            )

        private fun isBlank(v: Any?): Boolean = v == null || (v is String && (v.isBlank() || v.trim() == "null"))

        /** Quotes the brain put around a plain value ("'Köln'") are removed. */
        private fun clean(v: Any?): Any? =
            if (v is String) {
                v
                    .trim()
                    .removeSurrounding("\"")
                    .removeSurrounding("'")
                    .trim()
            } else {
                v
            }

        /** Calls with the same tool and arguments run once (the first id wins). */
        fun dedupe(calls: List<ToolCall>): List<ToolCall> = calls.distinctBy { key(it) }

        /** Same tool, same arguments (case and spaces ignored). */
        fun key(call: ToolCall): String =
            call.name +
                call.arguments.entries
                    .sortedBy { it.key }
                    .joinToString(prefix = "(", postfix = ")") { "${it.key}=${it.value.toString().trim().lowercase()}" }
    }
}
