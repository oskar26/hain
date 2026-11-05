package app.hain.tools.api

import java.util.Locale

/**
 * T-263 (Oskar 05.11.: "ich bin neu nach Köln gezogen was machen" → weather without a place, news and
 * Wikipedia at once, then an unrelated headline and "which city?"): the small brain sometimes calls
 * tools the question does not ask for. Weather and news are only run when the user's message is about
 * them (or a short follow-up of a message that was); other tools are left to the brain. Pure, tested.
 */
object ToolGuard {
    private const val WEATHER = "weather"
    private const val NEWS = "news"
    private const val SEARCH = "web_search"

    /** Longest message (in words) that still counts as a follow-up of the one before ("und morgen in Bonn?"). */
    private const val FOLLOW_UP_WORDS = 5

    /**
     * Whether a call of [tool] fits the newest of [userMessages] (oldest first). A web search whose
     * [query] is about the weather counts as a weather call ("Bristol weather" for "I just moved
     * to Bristol, any tips?" in the host test).
     */
    fun fits(
        tool: String,
        userMessages: List<String>,
        query: String? = null,
    ): Boolean {
        // only the weather: "aktuell" or "latest" in a search query says nothing about the news
        val weatherSearch = tool == SEARCH && query != null && TOPICS.getValue(WEATHER).containsMatchIn(normal(query))
        val topic = TOPICS[if (weatherSearch) WEATHER else tool]
        val last = userMessages.lastOrNull()?.let(::normal)
        // "und morgen?" after "Wie wird das Wetter in Köln?"
        val before = userMessages.dropLast(1).lastOrNull()?.let(::normal)
        return topic == null ||
            last == null ||
            topic.containsMatchIn(last) ||
            (before != null && words(last) <= FOLLOW_UP_WORDS && topic.containsMatchIn(before))
    }

    /**
     * The tool the newest message plainly asks for ("Brauche ich heute eine Jacke?" → weather, "Was gibt's
     * Neues?" → news), or `null`. Stricter than [fits]: it adds a nudge to the turn, so only clear words count.
     */
    fun asked(message: String): String? {
        val t = normal(message)
        return CLEAR.entries.firstOrNull { it.value.containsMatchIn(t) }?.key
    }

    /** The nudge for [asked]: call that tool (with the place from the message or the profile). */
    fun nudge(
        tool: String,
        lang: String,
    ): String =
        when {
            tool == WEATHER && lang == "de" ->
                "Die Nachricht fragt nach dem Wetter: rufe weather auf, mit dem Ort aus der Nachricht, sonst aus " +
                    "„Ort“ oder „Über den Nutzer“."
            tool == WEATHER ->
                "The message asks about the weather: call weather, with the place from the message, " +
                    "else from \"Place\" or \"About the user\"."
            lang == "de" -> "Die Nachricht fragt nach Nachrichten: rufe news auf."
            else -> "The message asks for the news: call news."
        }

    /** The line for the brain when every call of a round was left out: answer without them. */
    fun hint(
        tools: List<String>,
        lang: String,
    ): String {
        val names = tools.distinct().joinToString(", ")
        return if (lang == "de") {
            "Die Frage braucht $names nicht. Antworte aus deinem Wissen. Nur wenn dir etwas Wichtiges fehlt, " +
                "nimm web_search mit Ort und Thema."
        } else {
            "The question does not need $names. Answer from what you know. Only if something important is " +
                "missing, use web_search with the place and the topic."
        }
    }

    private fun normal(text: String) = text.lowercase(Locale.GERMAN)

    private fun words(text: String) = text.split(SPACE).count { it.isNotBlank() }

    private val SPACE = Regex("\\s+")

    private val CLEAR =
        mapOf(
            WEATHER to
                Regex(
                    "wetter|regenschirm|regnet es|wird es regnen|jacke|temperatur|wie (?:warm|kalt) (?:ist|wird)|" +
                        "weather|umbrella|forecast|will it rain|jacket",
                ),
            NEWS to
                Regex(
                    "nachrichten|schlagzeilen|neuigkeiten|was gibt'?s neues|was gibt es neues|\\bnews\\b|headlines|" +
                        "what'?s new\\b|what is new\\b",
                ),
        )

    private val TOPICS =
        mapOf(
            WEATHER to
                Regex(
                    "wetter|regen|regnet|regnen|schirm|schnee|schneit|sonnig|sonne scheint|temperatur|grad\\b|" +
                        "\\bwarm|\\bkalt|wind|gewitter|sturm|nebel|frost|glatt|vorhersage|jacke|hitze|weather|rain|" +
                        "umbrella|snow|sunny|temperature|degrees|forecast|\\bcold\\b|\\bhot\\b|storm|jacket|frost",
                ),
            NEWS to
                Regex(
                    "nachricht|news|schlagzeile|neuigkeit|was gibt'?s neues|was gibt es neues|" +
                        "neues (?:aus|von|über|zu)|" +
                        "was ist (?:heute )?passiert|was passiert|aktuell|briefing|presse|zeitung|headline|" +
                        "what'?s new|what happened|latest|today'?s top",
                ),
        )
}
