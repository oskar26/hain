package app.hain.cards.api

/** One day of the forecast on a [WeatherCard]: "morgen", 12° / 6°, 70 % rain, "Regen". */
data class WeatherDay(
    val label: String,
    val max: Int?,
    val min: Int?,
    val rainChance: Int?,
    val sky: String?,
)

/** What a [WeatherCard] shows: the place, the weather right now and a few days. */
data class WeatherCardData(
    val place: String,
    val temperature: Int?,
    val sky: String?,
    val windKmh: Int?,
    val days: List<WeatherDay>,
)

/** What a [WikiCard] shows: the article title, the first lines of its summary and its address. */
data class WikiCardData(
    val title: String,
    val summary: String,
    val url: String?,
)
