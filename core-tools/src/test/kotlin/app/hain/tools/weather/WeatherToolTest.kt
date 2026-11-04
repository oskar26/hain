package app.hain.tools.weather

import app.hain.common.api.net.GateRequest
import app.hain.common.api.net.GateResponse
import app.hain.common.api.net.Purpose
import app.hain.tools.api.InfoCard
import app.hain.tools.api.ToolContext
import app.hain.tools.internal.net.HttpNetworkGate
import app.hain.tools.internal.weather.WeatherCodes
import app.hain.tools.internal.weather.WeatherParser
import app.hain.tools.internal.weather.WeatherTool
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZonedDateTime

class WeatherToolTest {
    private val geo =
        """{"results":[{"id":2886242,"name":"Köln","latitude":50.93333,"longitude":6.95,"country":"Deutschland"}],""" +
            """"generationtime_ms":0.5}"""
    private val forecast =
        """{"latitude":50.94,"longitude":6.96,"current":{"time":"2026-11-03T11:00","temperature_2m":9.5,""" +
            """"weather_code":61,"wind_speed_10m":12.3,"precipitation":0.4},"daily":{"time":["2026-11-03",""" +
            """"2026-11-04"],"weather_code":[61,3],"temperature_2m_max":[11.2,10.0],"temperature_2m_min":[6.1,""" +
            """5.0],"precipitation_probability_max":[70,20]}}"""

    @Test
    fun parsesPlace() {
        val p = WeatherParser.firstPlace(geo)!!
        assertEquals("Köln", p.name)
        assertEquals(50.93333, p.lat, 1e-9)
        assertNull(WeatherParser.firstPlace("""{"generationtime_ms":0.1}"""))
    }

    @Test
    fun compactJsonForModel() {
        val json = WeatherParser.forModel("Köln", forecast, "de")
        assertEquals(
            """{"place":"Köln","now":{"t":9.5,"wind_kmh":12.3,"rain_mm":0.4,"sky":"leichter Regen"},""" +
                """"days":[{"date":"2026-11-03","max":11.2,"min":6.1,"rain_chance":70,"sky":"leichter Regen"},""" +
                """{"date":"2026-11-04","max":10.0,"min":5.0,"rain_chance":20,"sky":"bedeckt"}]}""",
            json,
        )
    }

    @Test
    fun daysAreNamed() {
        val json = WeatherParser.forModel("Köln", forecast, "de", java.time.LocalDate.parse("2026-11-03"))
        assertTrue(json.contains(""""date":"2026-11-03","day":"heute""""))
        assertTrue(json.contains(""""date":"2026-11-04","day":"morgen""""))
    }

    @Test
    fun codes() {
        assertEquals("Gewitter", WeatherCodes.describe(95, "de"))
        assertEquals("overcast", WeatherCodes.describe(3, "en"))
        assertEquals("unknown", WeatherCodes.describe(42, "en"))
    }

    @Test
    fun runsThroughGate() =
        runTest {
            val seen = mutableListOf<GateRequest>()
            val gate =
                HttpNetworkGate(transport = { r ->
                    seen += r
                    GateResponse(200, if (r.purpose == Purpose.GEO) geo else forecast)
                })
            val ctx = ToolContext("de", ZonedDateTime.now(), false)
            val result = WeatherTool(gate).run(mapOf("place" to "Köln", "days" to "2"), ctx)
            assertEquals("Köln", result.label)
            assertEquals("Open-Meteo", result.sources.single().title)
            // 3b-2: the same compact JSON becomes the WeatherCard
            assertEquals(InfoCard(InfoCard.WEATHER, result.forModel), result.card)
            assertEquals(listOf(Purpose.GEO, Purpose.WEATHER), seen.map { it.purpose })
            assertTrue(seen[0].url.contains("name=K%C3%B6ln"))
            assertTrue(seen[1].url.contains("forecast_days=2"))
            assertEquals(2, gate.log.value.size)
        }

    @Test
    fun unknownPlaceIsNotFound() =
        runTest {
            val gate = HttpNetworkGate(transport = { GateResponse(200, "{}") })
            val r = WeatherTool(gate).run(mapOf("place" to "Xyz"), ToolContext("de", ZonedDateTime.now(), false))
            assertEquals("not_found", r.error)
        }
}
