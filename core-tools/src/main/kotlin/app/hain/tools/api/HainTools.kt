package app.hain.tools.api

import app.hain.common.api.net.NetworkGate
import app.hain.tools.internal.calc.CalculatorTool
import app.hain.tools.internal.weather.WeatherTool
import app.hain.tools.internal.wiki.WikipediaTool

/** The tools of Werkzeuge I: calculator (offline), weather and Wikipedia (online, through [gate]). */
object HainTools {
    fun router(gate: NetworkGate): ToolRouter {
        val tools = listOf(CalculatorTool(), WeatherTool(gate), WikipediaTool(gate))
        return ToolRouter(tools)
    }

    /** A pure sum or conversion answered without the brain ("12 × 7 = 84"); `null` otherwise. */
    fun quickAnswer(
        message: String,
        lang: String,
    ): String? = CalculatorTool.directAnswer(message, lang)
}
