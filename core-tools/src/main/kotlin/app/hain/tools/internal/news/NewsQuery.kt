package app.hain.tools.internal.news

import java.util.Locale

/**
 * T-236 (Oskar 04.11.: "Nachrichten" without a topic made Pim ask for a search term): words that
 * only say "news" are no topic. Without a topic the `news` tool returns the top headlines.
 */
object NewsQuery {
    private val GENERIC =
        setOf(
            "news",
            "headlines",
            "top headlines",
            "top news",
            "latest news",
            "today",
            "today's news",
            "current events",
            "world news",
            "breaking news",
            "nachrichten",
            "aktuelle nachrichten",
            "neueste nachrichten",
            "news heute",
            "nachrichten heute",
            "schlagzeilen",
            "neue schlagzeilen",
            "aktuelle schlagzeilen",
            "top-schlagzeilen",
            "heute",
            "aktuelles",
            "was passiert heute",
            "was passiert heute in der welt",
            "neuigkeiten",
        )

    /** The real topic of [raw], or `null` for "no topic" (empty or only a word for news). */
    fun topic(raw: String?): String? {
        val text =
            raw
                ?.trim()
                ?.trim('"', '\'', '„', '“', '?', '!', '.')
                ?.replace(Regex("\\s+"), " ")
                .orEmpty()
        if (text.isBlank()) return null
        return text.takeUnless { it.lowercase(Locale.ROOT) in GENERIC }
    }
}
