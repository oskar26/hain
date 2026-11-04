package app.hain.feature.news.api

import app.hain.tools.internal.news.NewsFeed
import app.hain.tools.internal.news.NewsItem
import app.hain.tools.internal.news.NewsMode

/** Example headlines for the UI test and the screenshots (texts made up, sources real). */
internal object NewsSamples {
    private const val MINUTE = 60_000L
    private const val HOUR = 60 * MINUTE

    fun state(
        now: Long,
        mode: NewsMode = NewsMode.STANDARD,
    ): NewsUiState {
        fun item(
            feed: String,
            source: String,
            title: String,
            ago: Long,
            teaser: String,
            paywall: Boolean = false,
        ) = NewsItem(
            feedId = feed,
            source = source,
            title = title,
            link = "https://example.org/$feed",
            guid = "$feed-$ago",
            published = now - ago,
            fetchedAt = now,
            summary = teaser,
            paywall = paywall,
        )
        val items =
            listOf(
                item(
                    "tagesschau",
                    "tagesschau",
                    "Bundestag beschließt Haushalt für 2027",
                    20 * MINUTE,
                    "Nach langer Debatte stimmt eine knappe Mehrheit für den Entwurf der Regierung.",
                ),
                item(
                    "jungewelt",
                    "junge Welt",
                    "Beschäftigte bei Ford legen Arbeit nieder",
                    50 * MINUTE,
                    "Gewerkschaft fordert Erhalt aller Arbeitsplätze am Standort Köln.",
                    paywall = true,
                ),
                item(
                    "bbc",
                    "BBC News",
                    "Rail strike set to disrupt services across Britain",
                    2 * HOUR,
                    "Unions say talks have broken down after months of negotiations.",
                ),
                item(
                    "dlf",
                    "Deutschlandfunk",
                    "Klimakonferenz: Einigung auf neuen Fonds",
                    26 * HOUR,
                    "Die Staaten wollen ärmere Länder bei Schäden stärker unterstützen.",
                ),
                item(
                    "guardian",
                    "The Guardian",
                    "Heatwave records tumble in southern Europe",
                    28 * HOUR,
                    "Scientists link the extreme temperatures to the climate crisis.",
                ),
            )
        return NewsUiState(
            mode = mode,
            items = items,
            ownFeeds = listOf(NewsFeed("own-1", "Kölner Stadtblog", "https://blog.example.org/feed", own = true)),
        )
    }
}
