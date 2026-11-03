package app.hain.tools.api

import app.hain.common.api.net.NetworkGate
import app.hain.tools.internal.calc.CalculatorTool
import app.hain.tools.internal.phone.AddCalendarEventTool
import app.hain.tools.internal.phone.NavigateTool
import app.hain.tools.internal.phone.SetAlarmTool
import app.hain.tools.internal.phone.SetTimerTool
import app.hain.tools.internal.search.PageReader
import app.hain.tools.internal.search.ReadPageTool
import app.hain.tools.internal.search.SearchTool
import app.hain.tools.internal.weather.WeatherTool
import app.hain.tools.internal.wiki.WikipediaTool

/**
 * The tools: calculator and the phone actions (offline; alarm, timer, calendar, route only after a
 * tap on their card); weather, Wikipedia, web search and page reading (online, all
 * through [gate], so every request shows in the network log).
 */
object HainTools {
    fun router(gate: NetworkGate): ToolRouter {
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
            )
        return ToolRouter(tools, pages = reader)
    }

    /** A pure sum or conversion answered without the brain ("12 × 7 = 84"); `null` otherwise. */
    fun quickAnswer(
        message: String,
        lang: String,
    ): String? = CalculatorTool.directAnswer(message, lang)
}
