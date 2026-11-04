package app.hain.tools.internal.weather

import app.hain.common.api.net.GateRequest
import app.hain.common.api.net.NetworkGate
import app.hain.common.api.net.Purpose
import app.hain.tools.api.Args
import app.hain.tools.api.InfoCard
import app.hain.tools.api.Tool
import app.hain.tools.api.ToolContext
import app.hain.tools.api.ToolResult
import app.hain.tools.api.ToolRouter
import app.hain.tools.api.ToolSource
import app.hain.tools.api.ToolSpecs
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import java.io.IOException
import java.net.URLEncoder
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.roundToInt

/** `weather` (T-064, TOOLS.md §4): Open-Meteo geocoding + forecast, no key needed. */
class WeatherTool(
    private val gate: NetworkGate,
) : Tool {
    override val online = true
    override val schema = ToolSpecs.of("weather").schema

    @Suppress("ReturnCount") // no place found, no place allowed
    override suspend fun run(
        args: Map<String, Any?>,
        ctx: ToolContext,
    ): ToolResult {
        val place = Args.string(args, "place")?.takeUnless { HERE.matches(it.trim()) }
        val days = (Args.int(args, "days") ?: DEFAULT_DAYS).coerceIn(1, MAX_DAYS)
        val lang = if (ctx.lang == "de") "de" else "en"
        val hit =
            if (place != null) {
                WeatherParser.firstPlace(get(geocodeUrl(place, lang), Purpose.GEO))
                    ?: return ToolRouter.failure("not_found", "No place called '$place' was found. Ask again.")
            } else {
                // 3b-2: no place = where the phone is, only when the user allowed it; ~1 km is enough
                val here = ctx.here ?: return ToolRouter.failure("no_place", NO_PLACE_HINT)
                GeoPlace(if (lang == "de") HERE_DE else HERE_EN, rounded(here.lat), rounded(here.lon))
            }
        val forecast = get(forecastUrl(hit.lat, hit.lon, days), Purpose.WEATHER)
        val json = WeatherParser.forModel(hit.name, forecast, lang, ctx.now.toLocalDate())
        return ToolResult(
            forModel = json,
            label = hit.name,
            sources = listOf(ToolSource("Open-Meteo", SOURCE_URL)),
            card = InfoCard(InfoCard.WEATHER, json),
        )
    }

    private suspend fun get(
        url: String,
        purpose: Purpose,
    ): String {
        val response = gate.fetch(GateRequest(url, purpose))
        if (!response.ok) throw IOException("HTTP ${response.code}")
        return response.body
    }

    companion object {
        private const val MAX_DAYS = 7

        /** Today, tomorrow and the day after: the brain rarely asks for days by itself. */
        private const val DEFAULT_DAYS = 3
        private const val SOURCE_URL = "https://open-meteo.com/"
        private const val HERE_DE = "Dein Standort"
        private const val HERE_EN = "Your location"
        private const val NO_PLACE_HINT =
            "No place given and no location allowed. Ask the user for the place; they can also allow " +
                "their location for the weather in the settings (Privacy)."
        private val HERE =
            Regex("""(?i)hier|here|standort|mein standort|my location|current location|aktueller standort""")
        private const val ROUND = 100.0

        /** Two decimals (about 1 km): enough for a forecast, less exact than the phone knows. */
        fun rounded(value: Double): Double = Math.round(value * ROUND) / ROUND

        fun geocodeUrl(
            place: String,
            lang: String,
        ) = "https://geocoding-api.open-meteo.com/v1/search?name=${enc(place)}&count=1&language=$lang&format=json"

        fun forecastUrl(
            lat: Double,
            lon: Double,
            days: Int,
        ) = "https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon" +
            "&current=temperature_2m,weather_code,wind_speed_10m,precipitation" +
            "&daily=weather_code,temperature_2m_max,temperature_2m_min,precipitation_probability_max" +
            "&timezone=auto&forecast_days=$days"

        private fun enc(s: String) = URLEncoder.encode(s, "UTF-8")
    }
}

/** A place found by the geocoder. */
data class GeoPlace(
    val name: String,
    val lat: Double,
    val lon: Double,
)

/** Reads Open-Meteo answers (unit tested with fixtures). */
object WeatherParser {
    @Suppress("ReturnCount")
    fun firstPlace(body: String): GeoPlace? {
        val first = (parse(body)["results"] as? JsonArray)?.firstOrNull()?.jsonObject ?: return null
        val name = first["name"]?.jsonPrimitive?.content ?: return null
        val lat = first["latitude"]?.jsonPrimitive?.doubleOrNull ?: return null
        val lon = first["longitude"]?.jsonPrimitive?.doubleOrNull ?: return null
        return GeoPlace(name, lat, lon)
    }

    /** Compact JSON like `{"place":"Köln","now":{"t":9.5,"wind":12,"sky":"Regen"},"days":[…]}`. */
    fun forModel(
        place: String,
        body: String,
        lang: String,
        today: LocalDate? = null,
    ): String {
        val root = parse(body)
        val current = root["current"]?.jsonObject
        val daily = root["daily"]?.jsonObject
        val out =
            buildJsonObject {
                put("place", place)
                if (current != null) {
                    putJsonObject("now") {
                        current.num("temperature_2m")?.let { put("t", it) }
                        current.num("wind_speed_10m")?.let { put("wind_kmh", it) }
                        current.num("precipitation")?.let { put("rain_mm", it) }
                        current["weather_code"]?.jsonPrimitive?.intOrNull?.let {
                            put("sky", WeatherCodes.describe(it, lang))
                        }
                    }
                }
                if (daily != null) {
                    putJsonArray("days") {
                        val dates = daily["time"]?.jsonArray.orEmpty()
                        dates.forEachIndexed { i, date ->
                            add(
                                buildJsonObject {
                                    put("date", date.jsonPrimitive.content)
                                    dayName(date.jsonPrimitive.content, today, lang)?.let { put("day", it) }
                                    daily.at("temperature_2m_max", i)?.let { put("max", it) }
                                    daily.at("temperature_2m_min", i)?.let { put("min", it) }
                                    daily.at("precipitation_probability_max", i)?.let {
                                        put("rain_chance", it.roundToInt())
                                    }
                                    daily.at("weather_code", i)?.let {
                                        put("sky", WeatherCodes.describe(it.toInt(), lang))
                                    }
                                },
                            )
                        }
                    }
                }
            }
        return out.toString()
    }

    /** "heute" / "morgen" / "übermorgen" (or English), so the small brain does not mix up the days. */
    private fun dayName(
        date: String,
        today: LocalDate?,
        lang: String,
    ): String? {
        val day = runCatching { LocalDate.parse(date) }.getOrNull() ?: return null
        val names = if (lang == "de") DAY_NAMES_DE else DAY_NAMES_EN
        return today?.let { names.getOrNull(ChronoUnit.DAYS.between(it, day).toInt()) }
    }

    private val DAY_NAMES_DE = listOf("heute", "morgen", "übermorgen")
    private val DAY_NAMES_EN = listOf("today", "tomorrow", "day after tomorrow")

    private fun parse(body: String): JsonObject =
        runCatching { Json.parseToJsonElement(body).jsonObject }.getOrElse { throw IOException("bad answer") }

    private fun JsonObject.num(key: String): Double? = (this[key] as? JsonPrimitive)?.doubleOrNull

    private fun JsonObject.at(
        key: String,
        index: Int,
    ): Double? = ((this[key] as? JsonArray)?.getOrNull(index) as? JsonPrimitive)?.doubleOrNull
}
