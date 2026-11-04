package app.hain.feature.chat.internal

import app.hain.cards.api.WeatherCardData
import app.hain.cards.api.WeatherDay
import app.hain.cards.api.WikiCardData
import app.hain.tools.api.InfoCard
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonObject
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Turns the JSON a tool found into what the cards in `core-cards` show (3b-2). Model output never
 * gets here: only tool results. Anything missing or broken means no card, never a crash.
 */
@Suppress("ReturnCount") // each missing piece means no card
object InfoCardParser {
    private const val MAX_DAYS = 7
    private const val MAX_SUMMARY = 400

    fun weather(
        card: InfoCard,
        locale: Locale,
    ): WeatherCardData? {
        if (card.kind != InfoCard.WEATHER) return null
        val root = parse(card.data) ?: return null
        val place = root.text("place")?.takeIf { it.isNotBlank() } ?: return null
        val now = root["now"] as? JsonObject
        val days =
            (root["days"] as? JsonArray)
                .orEmpty()
                .mapNotNull { it as? JsonObject }
                .take(MAX_DAYS)
                .map { d ->
                    WeatherDay(
                        label = dayLabel(d.text("day"), d.text("date"), locale),
                        max = d.whole("max"),
                        min = d.whole("min"),
                        rainChance = d.whole("rain_chance"),
                        sky = d.text("sky"),
                    )
                }
        return WeatherCardData(
            place = place,
            temperature = now?.whole("t"),
            sky = now?.text("sky"),
            windKmh = now?.whole("wind_kmh"),
            days = days,
        )
    }

    fun wiki(card: InfoCard): WikiCardData? {
        if (card.kind != InfoCard.WIKI) return null
        val root = parse(card.data) ?: return null
        val title = root.text("title")?.takeIf { it.isNotBlank() } ?: return null
        val url = root.text("url")?.takeIf { it.startsWith("https://") }
        return WikiCardData(title, root.text("extract").orEmpty().take(MAX_SUMMARY), url)
    }

    /** "heute" → "Heute"; without a day word the date as "Fr., 6.11." / "Fri, Nov 6". */
    private fun dayLabel(
        word: String?,
        date: String?,
        locale: Locale,
    ): String {
        if (!word.isNullOrBlank()) return word.replaceFirstChar { it.titlecase(locale) }
        val day = date?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: return date.orEmpty()
        val pattern = if (locale.language == "de") "EEE, d.M." else "EEE, MMM d"
        return day.format(DateTimeFormatter.ofPattern(pattern, locale))
    }

    private fun parse(text: String): JsonObject? = runCatching { Json.parseToJsonElement(text).jsonObject }.getOrNull()

    private fun JsonObject.text(key: String): String? = (this[key] as? JsonPrimitive)?.takeIf { it.isString }?.content

    private fun JsonObject.whole(key: String): Int? = (this[key] as? JsonPrimitive)?.doubleOrNull?.roundToInt()
}
