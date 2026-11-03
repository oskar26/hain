@file:Suppress("MagicNumber") // WMO code table

package app.hain.tools.internal.weather

/** WMO weather codes (Open-Meteo `weather_code`) as plain words in German and English (T-064). */
object WeatherCodes {
    private val words: Map<Int, Pair<String, String>> =
        mapOf(
            0 to ("klar" to "clear sky"),
            1 to ("überwiegend klar" to "mainly clear"),
            2 to ("teils bewölkt" to "partly cloudy"),
            3 to ("bedeckt" to "overcast"),
            45 to ("Nebel" to "fog"),
            48 to ("Reifnebel" to "freezing fog"),
            51 to ("leichter Nieselregen" to "light drizzle"),
            53 to ("Nieselregen" to "drizzle"),
            55 to ("starker Nieselregen" to "heavy drizzle"),
            56 to ("gefrierender Nieselregen" to "freezing drizzle"),
            57 to ("starker gefrierender Nieselregen" to "heavy freezing drizzle"),
            61 to ("leichter Regen" to "light rain"),
            63 to ("Regen" to "rain"),
            65 to ("starker Regen" to "heavy rain"),
            66 to ("gefrierender Regen" to "freezing rain"),
            67 to ("starker gefrierender Regen" to "heavy freezing rain"),
            71 to ("leichter Schneefall" to "light snow"),
            73 to ("Schneefall" to "snow"),
            75 to ("starker Schneefall" to "heavy snow"),
            77 to ("Schneegriesel" to "snow grains"),
            80 to ("leichte Regenschauer" to "light showers"),
            81 to ("Regenschauer" to "showers"),
            82 to ("heftige Regenschauer" to "violent showers"),
            85 to ("leichte Schneeschauer" to "light snow showers"),
            86 to ("Schneeschauer" to "snow showers"),
            95 to ("Gewitter" to "thunderstorm"),
            96 to ("Gewitter mit Hagel" to "thunderstorm with hail"),
            99 to ("Gewitter mit starkem Hagel" to "thunderstorm with heavy hail"),
        )

    /** Plain words for [code]; "unbekannt" / "unknown" for codes outside the table. */
    fun describe(
        code: Int,
        lang: String,
    ): String {
        val pair = words[code] ?: ("unbekannt" to "unknown")
        return if (lang == "de") pair.first else pair.second
    }
}
