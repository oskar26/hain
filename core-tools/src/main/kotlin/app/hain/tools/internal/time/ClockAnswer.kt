package app.hain.tools.internal.time

import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * "Wie spät ist es?" / "Welcher Tag ist heute?" answered by code from the turn's clock (4d), like
 * pure sums: right every time, no brain needed. Host eval: the small brain sometimes called the
 * weather tool for "welcher tag ist heute?" although the date is in the system prompt.
 */
internal object ClockAnswer {
    private val TIME =
        listOf(
            "wie spät ist es",
            "wie spaet ist es",
            "wie spät is es",
            "wieviel uhr ist es",
            "wie viel uhr ist es",
            "uhrzeit",
            "wie spät",
            "what time is it",
            "whats the time",
            "what's the time",
            "what is the time",
            "time",
        )
    private val DATE =
        listOf(
            "welcher tag ist heute",
            "welcher tag ist",
            "welches datum ist heute",
            "welches datum haben wir",
            "der wievielte ist heute",
            "was ist heute für ein tag",
            "datum",
            "what day is it",
            "what day is it today",
            "what's the date",
            "whats the date",
            "what is the date",
            "what's today's date",
            "what is today's date",
        )
    private val CLEAN = Regex("""[?!.,]+""")

    fun direct(
        message: String,
        now: ZonedDateTime,
        lang: String,
    ): String? {
        val t = CLEAN.replace(message.trim().lowercase(), "").replace(Regex("\\s+"), " ").trim()
        val de = lang == "de"
        val locale = if (de) Locale.GERMAN else Locale.ENGLISH
        return when (t) {
            in TIME -> {
                val clock = now.format(DateTimeFormatter.ofPattern("HH:mm", locale))
                if (de) "Es ist $clock Uhr." else "It's $clock."
            }
            in DATE -> {
                val pattern = if (de) "EEEE, 'der' d. MMMM yyyy" else "EEEE, d MMMM yyyy"
                val day = now.format(DateTimeFormatter.ofPattern(pattern, locale))
                if (de) "Heute ist $day." else "Today is $day."
            }
            else -> null
        }
    }
}
