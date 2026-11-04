package app.hain.tools.api

import app.hain.common.api.net.NetworkGate
import app.hain.tools.internal.calc.CalculatorTool
import app.hain.tools.internal.news.NewsRepository
import app.hain.tools.internal.news.NewsTool
import app.hain.tools.internal.phone.AddCalendarEventTool
import app.hain.tools.internal.phone.NavigateTool
import app.hain.tools.internal.phone.SetAlarmTool
import app.hain.tools.internal.phone.SetTimerTool
import app.hain.tools.internal.search.PageReader
import app.hain.tools.internal.search.ReadPageTool
import app.hain.tools.internal.search.SearchTool
import app.hain.tools.internal.time.ClockAnswer
import app.hain.tools.internal.weather.WeatherTool
import app.hain.tools.internal.wiki.WikipediaTool
import java.time.ZonedDateTime

/**
 * The tools: calculator and the phone actions (offline; alarm, timer, calendar, route only after a
 * tap on their card); weather, Wikipedia, web search, page reading and, with [news], the headlines
 * of the user's news mode (T-072) (online, all through [gate], so every request shows in the network log).
 */
object HainTools {
    /** All tools; [extra] are tools of other modules, e.g. `remember` from the memory (T-112). */
    fun router(
        gate: NetworkGate,
        news: NewsRepository? = null,
        extra: List<Tool> = emptyList(),
    ): ToolRouter {
        val reader = PageReader(gate)
        val tools =
            listOf(
                CalculatorTool(),
                WeatherTool(gate),
                WikipediaTool(gate),
                SearchTool(gate),
                ReadPageTool(reader),
                SetAlarmTool(),
                SetTimerTool(),
                AddCalendarEventTool(),
                NavigateTool(),
            ) + listOfNotNull(news?.let(::NewsTool)) + extra
        return ToolRouter(tools, pages = reader, fallbacks = FALLBACKS)
    }

    /**
     * 4c: an empty Wikipedia answer becomes a web search with the same words. (The web search's
     * own fallback, Wikipedia in German and English, lives inside [SearchTool].)
     */
    val FALLBACKS =
        mapOf(
            "wikipedia" to
                Fallback(
                    tool = SearchTool.NAME,
                    note = "Wikipedia had nothing about this. Here is a web search instead.",
                ) { args -> mapOf("query" to (args["topic"] ?: args["query"])) },
            // T-072: no headline about the topic in the feeds → a web search (NEWS.md §4)
            NewsTool.NAME to
                Fallback(
                    tool = SearchTool.NAME,
                    note = "The news feeds had no headline about this. Here is a web search instead.",
                ) { args -> mapOf("query" to (args["query"] ?: args["topic"] ?: "news")) },
        )

    /** A pure sum or conversion answered without the brain ("12 × 7 = 84"); `null` otherwise. */
    fun quickAnswer(
        message: String,
        lang: String,
    ): String? = CalculatorTool.directAnswer(message, lang)

    /** "Wie spät ist es?" / "Welcher Tag ist heute?" answered from [now] without the brain (4d); `null` otherwise. */
    fun clockAnswer(
        message: String,
        now: ZonedDateTime,
        lang: String,
    ): String? = ClockAnswer.direct(message, now, lang)
}
