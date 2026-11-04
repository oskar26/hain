package app.hain.tools.internal.phone

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZonedDateTime
import java.time.format.DateTimeParseException
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

/**
 * Times and dates as the brain writes them, German and English (T-068): "6:30", "um 7", "15 Uhr",
 * "3 pm", "halb 8", "10 Minuten", "1h 30m", "morgen um 15 Uhr", "tomorrow 3pm", "Freitag 18:00",
 * "4.11. 9 Uhr", "in 10 Minuten", ISO "2026-11-04T15:00". Everything relative is measured from `now`
 * (the ToolContext), so tests are exact. Own code, no library.
 */
@Suppress("TooManyFunctions", "MagicNumber") // one small parser per form; clock numbers are clock numbers
internal object TimeParser {
    /** A date with an optional time; `time == null` means a whole day. */
    data class When(
        val date: LocalDate,
        val time: LocalTime?,
    )

    // ---- clock -------------------------------------------------------------------------------

    private val HALF = Regex("""\bhalb\s+(\d{1,2})\b""", RegexOption.IGNORE_CASE)
    private val QUARTER_PAST = Regex("""\bviertel\s+nach\s+(\d{1,2})\b""", RegexOption.IGNORE_CASE)
    private val QUARTER_TO = Regex("""\bviertel\s+vor\s+(\d{1,2})\b""", RegexOption.IGNORE_CASE)
    private val HH_MM =
        Regex("""(?<![\d.])(\d{1,2})[:.h](\d{2})(?![\d.])(?:\s*(am|pm|a\.m\.|p\.m\.))?""", RegexOption.IGNORE_CASE)
    private val H_UHR_MM = Regex("""(?<![\d.])(\d{1,2})\s*uhr\s*(\d{1,2})?(?!\d)""", RegexOption.IGNORE_CASE)
    private val H_AMPM = Regex("""(?<![\d.])(\d{1,2})\s*(am|pm|a\.m\.|p\.m\.)""", RegexOption.IGNORE_CASE)
    private val AT_H = Regex("""(?:\bum|\bat|\bgegen|@)\s*(\d{1,2})(?![\d.:])""", RegexOption.IGNORE_CASE)
    private val ONLY_H = Regex("""^\s*(\d{1,2})\s*$""")

    /** "6:30" → 06:30; `null` when there is no clock time in [text]. */
    @Suppress("ReturnCount", "CyclomaticComplexMethod")
    fun clock(text: String): LocalTime? {
        val t = text.trim()
        isoDateTime(t)?.let { return it.toLocalTime() }
        HALF.find(t)?.let { m -> return time(m.int(1) - 1, 30) }
        QUARTER_PAST.find(t)?.let { m -> return time(m.int(1), 15) }
        QUARTER_TO.find(t)?.let { m -> return time(m.int(1) - 1, 45) }
        HH_MM.find(t)?.let { m -> return time(ampm(m.int(1), m.groupValues[3]), m.int(2)) }
        H_UHR_MM.find(t)?.let { m -> return time(m.int(1), m.groupValues[2].toIntOrNull() ?: 0) }
        H_AMPM.find(t)?.let { m -> return time(ampm(m.int(1), m.groupValues[2]), 0) }
        AT_H.find(t)?.let { m -> return time(m.int(1), 0) }
        ONLY_H.find(t)?.let { m -> return time(m.int(1), 0) }
        return null
    }

    private fun ampm(
        hour: Int,
        marker: String,
    ): Int {
        val m = marker.lowercase().replace(".", "")
        return when {
            m == "pm" && hour in 1..11 -> hour + 12
            m == "am" && hour == 12 -> 0
            else -> hour
        }
    }

    private fun time(
        hour: Int,
        minute: Int,
    ): LocalTime? {
        val h = if (hour == 24) 0 else hour
        return if (h in 0..23 && minute in 0..59) LocalTime.of(h, minute) else null
    }

    // ---- durations ---------------------------------------------------------------------------

    private const val NUMBER = """(?<![\p{L}\d])(\d+(?:[.,]\d+)?|eine?n?|an?|one|half|halbe?n?|anderthalb|eineinhalb)"""
    private val DURATION_PART =
        Regex(
            """$NUMBER\s*(stunden|stunde|std|hours|hour|hrs|hr|h|minuten|minute|mins|min|m|sekunden|sekunde|""" +
                """seconds|second|secs|sec|sek|s)\b""",
            RegexOption.IGNORE_CASE,
        )
    private val HALF_HOUR = Regex("""\b(eine\s+)?halbe\s+stunde\b|\bhalf\s+an?\s+hour\b""", RegexOption.IGNORE_CASE)

    /** "10 Minuten" → 600; a bare number counts as seconds. `null` when nothing fits. */
    @Suppress("ReturnCount")
    fun durationSeconds(text: String): Int? {
        val t = text.trim().lowercase()
        if (t.isEmpty()) return null
        t.toDoubleOrNull()?.let { return it.toInt().takeIf { s -> s > 0 } }
        var total = 0.0
        var found = false
        if (HALF_HOUR.containsMatchIn(t)) {
            total += 1800
            found = true
        }
        val rest = HALF_HOUR.replace(t, " ")
        DURATION_PART.findAll(rest).forEach { m ->
            val n = number(m.groupValues[1]) ?: return@forEach
            total += n * unitSeconds(m.groupValues[2])
            found = true
        }
        return if (found && total >= 1) total.toInt() else null
    }

    private fun number(word: String): Double? =
        when (word.lowercase()) {
            "ein", "eine", "einen", "a", "an", "one" -> 1.0
            "half", "halb", "halbe", "halben" -> 0.5
            "anderthalb", "eineinhalb" -> 1.5
            else -> word.replace(',', '.').toDoubleOrNull()
        }

    private fun unitSeconds(unit: String): Int =
        when (unit.lowercase()) {
            "stunden", "stunde", "std", "hours", "hour", "hrs", "hr", "h" -> 3600
            "minuten", "minute", "mins", "min", "m" -> 60
            else -> 1
        }

    // ---- date and time -----------------------------------------------------------------------

    private val IN_DURATION = Regex("""\b(?:in|nach)\s+(.+)$""", RegexOption.IGNORE_CASE)
    private val ISO_DATE = Regex("""(\d{4})-(\d{2})-(\d{2})""")
    private val DOT_DATE = Regex("""(?<![\d:])(\d{1,2})\.(\d{1,2})\.(\d{2,4})?(?![\d:])""")
    private val MONTH_DATE = Regex("""(?<!\d)(\d{1,2})\.?\s+([a-zäA-ZÄ]{3,})\.?(?:\s+(\d{4}))?""")
    private val EN_MONTH_DATE = Regex("""\b([a-zA-Z]{3,})\.?\s+(\d{1,2})(?:st|nd|rd|th)?\b(?:,?\s+(\d{4}))?""")
    private val NEXT_WORD = word("nächste[nmrs]?|naechste[nmrs]?|kommende[nmrs]?|next")

    // own word edges: from JDK 19, `\b` treats "ü" as no letter, so "übermorgen" would read as "morgen"
    private fun word(alternatives: String) = Regex("""(?iu)(?<!\p{L})(?:$alternatives)(?!\p{L})""")

    private val DAY_WORDS =
        listOf(
            word("übermorgen|uebermorgen|day after tomorrow") to 2L,
            word("morgen|tomorrow") to 1L,
            word("heute|today|tonight") to 0L,
        )

    private val WEEKDAYS =
        mapOf(
            DayOfWeek.MONDAY to listOf("montag", "monday", "mo"),
            DayOfWeek.TUESDAY to listOf("dienstag", "tuesday", "di"),
            DayOfWeek.WEDNESDAY to listOf("mittwoch", "wednesday", "mi"),
            DayOfWeek.THURSDAY to listOf("donnerstag", "thursday", "do"),
            DayOfWeek.FRIDAY to listOf("freitag", "friday", "fr"),
            DayOfWeek.SATURDAY to listOf("samstag", "sonnabend", "saturday", "sa"),
            DayOfWeek.SUNDAY to listOf("sonntag", "sunday", "so"),
        )

    private val MONTHS =
        listOf(
            listOf("jan", "januar", "january", "jänner"),
            listOf("feb", "februar", "february"),
            listOf("mär", "mar", "märz", "maerz", "march"),
            listOf("apr", "april"),
            listOf("mai", "may"),
            listOf("jun", "juni", "june"),
            listOf("jul", "juli", "july"),
            listOf("aug", "august"),
            listOf("sep", "sept", "september"),
            listOf("okt", "oct", "oktober", "october"),
            listOf("nov", "november"),
            listOf("dez", "dec", "dezember", "december"),
        )

    /**
     * Date and time in [text] relative to [now]. With only a time, the next such time (today, or
     * tomorrow when it has passed). With only a day, the whole day. `null` when nothing fits.
     */
    @Suppress("ReturnCount")
    fun dateTime(
        text: String,
        now: ZonedDateTime,
    ): When? {
        val t = text.trim()
        if (t.isEmpty()) return null
        isoDateTime(t)?.let { return When(it.toLocalDate(), it.toLocalTime().truncatedTo(ChronoUnit.MINUTES)) }
        IN_DURATION.find(t)?.let { m ->
            durationSeconds(m.groupValues[1])?.let { s ->
                val at = now.plusSeconds(s.toLong()).truncatedTo(ChronoUnit.MINUTES)
                return When(at.toLocalDate(), at.toLocalTime())
            }
        }
        val (date, rest) = date(t, now.toLocalDate())
        val time = clock(rest)
        return when {
            date != null -> When(date, time)
            time != null -> {
                val today = now.toLocalDate()
                val day = if (time.isBefore(now.toLocalTime())) today.plusDays(1) else today
                When(day, time)
            }
            else -> null
        }
    }

    /** The date in [text] and the text without it (so "4.11. 9 Uhr" does not read 4 as the hour). */
    @Suppress("ReturnCount", "CyclomaticComplexMethod")
    private fun date(
        text: String,
        today: LocalDate,
    ): Pair<LocalDate?, String> {
        ISO_DATE.find(text)?.let { m ->
            val d = runCatching { LocalDate.of(m.int(1), m.int(2), m.int(3)) }.getOrNull()
            if (d != null) return d to text.removeRange(m.range)
        }
        DOT_DATE.find(text)?.let { m ->
            val d = dayMonth(m.int(1), m.int(2), m.groupValues[3].toIntOrNull(), today)
            if (d != null) return d to text.removeRange(m.range)
        }
        MONTH_DATE.findAll(text).forEach { m ->
            val month = month(m.groupValues[2]) ?: return@forEach
            val d = dayMonth(m.int(1), month, m.groupValues[3].toIntOrNull(), today)
            if (d != null) return d to text.removeRange(m.range)
        }
        EN_MONTH_DATE.findAll(text).forEach { m ->
            val month = month(m.groupValues[1]) ?: return@forEach
            val d = dayMonth(m.int(2), month, m.groupValues[3].toIntOrNull(), today)
            if (d != null) return d to text.removeRange(m.range)
        }
        DAY_WORDS.forEach { (regex, days) ->
            regex.find(text)?.let { m -> return today.plusDays(days) to text.removeRange(m.range) }
        }
        val words = text.lowercase().split(Regex("""[^a-zäöüß]+""")).filter { it.isNotEmpty() }
        for ((day, names) in WEEKDAYS) {
            // two-letter names ("Mo") only when written like a day, never inside other words
            val hit = words.firstOrNull { w -> names.any { n -> w == n && (n.length > 2 || words.size <= 3) } }
            if (hit != null) {
                val next = NEXT_WORD.containsMatchIn(text)
                val adjust = if (next) TemporalAdjusters.next(day) else TemporalAdjusters.nextOrSame(day)
                val d = today.with(adjust)
                return d to text.replace(Regex("""\b$hit\b""", RegexOption.IGNORE_CASE), " ")
            }
        }
        return null to text
    }

    // ---- repeat days (4b-2) -------------------------------------------------------------------

    private val EVERY_DAY = word("täglich|taeglich|jeden\\s+tag|alle\\s+tage|every\\s*day|each\\s+day|daily")
    private val WORKDAYS =
        word("werktags?|werktage|wochentags?|wochentage|arbeitstage|weekdays?|workdays?|work\\s+days|business\\s+days")
    private val WEEKEND = word("wochenende[ns]?|weekends?")
    private val DAY_RANGE =
        Regex("""(?iu)(\p{L}+)\.?(?:\s*[-–—]\s*|\s+(?:bis|to|through|thru|until)\s+)(\p{L}+)""")

    private val DAY_NAMES: Map<String, DayOfWeek> =
        WEEKDAYS.flatMap { (day, names) -> names.map { it to day } }.toMap() +
            mapOf(
                "mon" to DayOfWeek.MONDAY,
                "tue" to DayOfWeek.TUESDAY,
                "tues" to DayOfWeek.TUESDAY,
                "tu" to DayOfWeek.TUESDAY,
                "wed" to DayOfWeek.WEDNESDAY,
                "we" to DayOfWeek.WEDNESDAY,
                "thu" to DayOfWeek.THURSDAY,
                "thur" to DayOfWeek.THURSDAY,
                "thurs" to DayOfWeek.THURSDAY,
                "th" to DayOfWeek.THURSDAY,
                "fri" to DayOfWeek.FRIDAY,
                "sat" to DayOfWeek.SATURDAY,
                "sun" to DayOfWeek.SUNDAY,
            )

    /**
     * Repeat days of an alarm as the brain writes them (4b-2): "Mo-Fr", "werktags", "täglich",
     * "montags und donnerstags", "Mon, Wed, Fri", "weekends", "Montag bis Freitag", "1,3,5" (ISO
     * numbers). Sorted Monday first; `null` when no day is in [text].
     */
    fun weekdays(text: String): List<DayOfWeek>? {
        val t = text.lowercase()
        if (EVERY_DAY.containsMatchIn(t)) return DayOfWeek.entries.toList()
        val days = sortedSetOf<DayOfWeek>()
        if (WORKDAYS.containsMatchIn(t)) days += DayOfWeek.entries.take(WORK_DAYS)
        if (WEEKEND.containsMatchIn(t)) days += listOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)
        var rest = t
        DAY_RANGE.findAll(t).forEach { m ->
            val from = dayName(m.groupValues[1])
            val to = dayName(m.groupValues[2])
            if (from != null && to != null) {
                val steps = (to.value - from.value + 7) % 7
                days += (0..steps).map { from.plus(it.toLong()) }
                rest = rest.replace(m.value, " ")
            }
        }
        rest.split(Regex("""[^\p{L}\d]+""")).forEach { w ->
            val day = dayName(w) ?: w.toIntOrNull()?.takeIf { it in 1..7 }?.let { DayOfWeek.of(it) }
            if (day != null) days += day
        }
        return days.toList().ifEmpty { null }
    }

    /** "montag", "montags", "mondays", "mo", "thurs" → the day; `null` for other words. */
    private fun dayName(word: String): DayOfWeek? = DAY_NAMES[word] ?: DAY_NAMES[word.removeSuffix("s")]

    private const val WORK_DAYS = 5

    private fun month(word: String): Int? {
        val w = word.lowercase().trimEnd('.')
        val i = MONTHS.indexOfFirst { names -> names.any { it == w } }
        return if (i < 0) null else i + 1
    }

    /** Day and month without a year: the next such date (this year, or next year when it has passed). */
    private fun dayMonth(
        day: Int,
        month: Int,
        year: Int?,
        today: LocalDate,
    ): LocalDate? {
        val y = year?.let { if (it < 100) it + 2000 else it }
        val d = runCatching { LocalDate.of(y ?: today.year, month, day) }.getOrNull() ?: return null
        return if (y == null && d.isBefore(today)) d.plusYears(1) else d
    }

    /** ISO forms the brain likes: "2026-11-04T15:00", "…:00", "…Z", "+01:00", or with a space. */
    @Suppress("SwallowedException") // not ISO is a normal answer here
    private fun isoDateTime(text: String): LocalDateTime? {
        val t = text.trim().replace(' ', 'T')
        return try {
            OffsetDateTime.parse(t).toLocalDateTime()
        } catch (e: DateTimeParseException) {
            try {
                LocalDateTime.parse(t.removeSuffix("Z"))
            } catch (e2: DateTimeParseException) {
                null
            }
        }
    }

    private fun MatchResult.int(group: Int): Int = groupValues[group].toInt()
}
