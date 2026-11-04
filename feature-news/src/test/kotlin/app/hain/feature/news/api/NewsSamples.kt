package app.hain.feature.news.api

import app.hain.tools.internal.news.Article
import app.hain.tools.internal.news.ArticleBlock
import app.hain.tools.internal.news.NewsFeed
import app.hain.tools.internal.news.NewsItem
import app.hain.tools.internal.news.NewsMode

/** Example headlines for the UI test and the screenshots (texts made up, sources real). */
internal object NewsSamples {
    private const val MINUTE = 60_000L
    private const val HOUR = 60 * MINUTE

    /** Address of the made-up picture of [feed] (the screenshots draw it, nothing is downloaded). */
    fun picture(feed: String) = "https://example.org/img/$feed.png"

    fun state(
        now: Long,
        mode: NewsMode = NewsMode.STANDARD,
        pictures: Boolean = false,
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
            imageUrl = if (pictures && feed != "guardian") picture(feed) else null,
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
                    "Die Gewerkschaft fordert, dass alle Arbeitsplätze bleiben.",
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
            ownFeeds = listOf(NewsFeed("own-1", "Stadtblog", "https://blog.example.org/feed", own = true)),
        )
    }

    /** Today's briefing for the screenshots (T-079). */
    fun briefing(now: Long): BriefingState =
        BriefingState(
            briefing =
                Briefing(
                    day = "2026-11-04",
                    createdAt = now,
                    byBrain = true,
                    points =
                        listOf(
                            BriefingPoint(
                                "Der Bundestag hat den Haushalt für 2027 mit knapper Mehrheit beschlossen.",
                                "Bundestag beschließt Haushalt für 2027",
                                listOf("tagesschau", "Deutschlandfunk", "taz"),
                                "https://example.org/tagesschau",
                            ),
                            BriefingPoint(
                                "Bei Ford legen viele Beschäftigte die Arbeit nieder.",
                                "Beschäftigte bei Ford legen Arbeit nieder",
                                listOf("junge Welt", "nd"),
                                "https://example.org/jungewelt",
                            ),
                            BriefingPoint(
                                "In Großbritannien fallen wegen eines Bahnstreiks viele Züge aus.",
                                "Rail strike set to disrupt services across Britain",
                                listOf("BBC News", "The Guardian"),
                                "https://example.org/bbc",
                            ),
                            BriefingPoint(
                                "Die Klimakonferenz einigt sich auf einen Fonds für Schäden.",
                                "Klimakonferenz: Einigung auf neuen Fonds",
                                listOf("Deutschlandfunk"),
                                "https://example.org/dlf",
                            ),
                            BriefingPoint(
                                "In Südeuropa fallen Hitzerekorde.",
                                "Heatwave records tumble in southern Europe",
                                listOf("The Guardian"),
                                "https://example.org/guardian",
                            ),
                        ),
                ),
            expanded = true,
        )

    /** The article of [item] as the reading view gets it (T-076). */
    fun article(
        item: NewsItem,
        locked: Boolean = false,
    ): Article =
        Article(
            url = item.link.orEmpty(),
            title = item.title,
            siteName = item.source,
            byline = "Anna Beispiel",
            leadImage = item.imageUrl,
            blocks =
                listOf(
                    ArticleBlock.Paragraph(
                        "Nach einer langen Debatte hat der Bundestag am Mittwoch den Haushalt für das kommende Jahr " +
                            "beschlossen. Eine knappe Mehrheit stimmte für den Entwurf der Regierung.",
                    ),
                    ArticleBlock.Paragraph(
                        "Mehr Geld soll vor allem in die Bahn und in Schulen fließen. " +
                            "Bei anderen Posten wird gespart, was die Opposition scharf kritisiert.",
                    ),
                    ArticleBlock.Heading("Was sich ändert"),
                    ArticleBlock.ListItem("Mehr Geld für Schienen und Brücken"),
                    ArticleBlock.ListItem("Ein Programm für Schulgebäude"),
                    ArticleBlock.ListItem("Weniger Geld für einige Förderprogramme"),
                    ArticleBlock.Quote("Wir investieren in das, was das Land zusammenhält."),
                    ArticleBlock.Image(picture("chart"), "So verteilt sich das Geld (Grafik: Beispiel)"),
                    ArticleBlock.Paragraph(
                        "Der Bundesrat berät im Dezember über den Haushalt. Danach kann er in Kraft treten.",
                    ),
                ),
            locked = locked,
        )
}
