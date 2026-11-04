package app.hain.feature.chat.internal

import app.hain.cards.api.WeatherDay
import app.hain.tools.api.InfoCard
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Locale

/** 3b-2: weather and Wikipedia results become cards and stay with the answer. */
class InfoCardsTest {
    private val weatherJson =
        """{"place":"Köln","now":{"t":9.6,"wind_kmh":12.4,"sky":"Regen"},"days":[""" +
            """{"date":"2026-11-04","day":"heute","max":11.5,"min":6.2,"rain_chance":70,"sky":"Regen"},""" +
            """{"date":"2026-11-06","max":13,"min":7,"sky":"bewölkt"}]}"""

    @Test
    fun weatherCard() {
        val card = InfoCardParser.weather(InfoCard(InfoCard.WEATHER, weatherJson), Locale.GERMAN)!!
        assertEquals("Köln", card.place)
        assertEquals(10, card.temperature)
        assertEquals("Regen", card.sky)
        assertEquals(12, card.windKmh)
        assertEquals(WeatherDay("Heute", 12, 6, 70, "Regen"), card.days[0])
        assertEquals(WeatherDay("Fr., 6.11.", 13, 7, null, "bewölkt"), card.days[1])
    }

    @Test
    fun wikiCard() {
        val json =
            """{"title":"Kölner Dom","extract":"Der Kölner Dom ist eine Kathedrale.",""" +
                """"url":"https://de.wikipedia.org/wiki/K%C3%B6lner_Dom"}"""
        val card = InfoCardParser.wiki(InfoCard(InfoCard.WIKI, json))!!
        assertEquals("Kölner Dom", card.title)
        assertEquals("Der Kölner Dom ist eine Kathedrale.", card.summary)
        assertEquals("https://de.wikipedia.org/wiki/K%C3%B6lner_Dom", card.url)
        // only https links can be opened from the card
        assertNull(InfoCardParser.wiki(InfoCard(InfoCard.WIKI, """{"title":"X","url":"http://x"}"""))!!.url)
    }

    @Test
    fun brokenDataMeansNoCard() {
        assertNull(InfoCardParser.weather(InfoCard(InfoCard.WEATHER, "kaputt"), Locale.GERMAN))
        assertNull(InfoCardParser.weather(InfoCard(InfoCard.WIKI, weatherJson), Locale.GERMAN))
        assertNull(InfoCardParser.wiki(InfoCard(InfoCard.WIKI, """{"extract":"no title"}""")))
    }

    @Test
    fun cardsAreStoredWithTheAnswer() {
        val cards = listOf(InfoCard(InfoCard.WEATHER, weatherJson), InfoCard(InfoCard.WIKI, """{"title":"X"}"""))
        val json = ToolStepsJson.encode(emptyList(), emptyList(), emptyList(), cards)
        assertEquals(cards, ToolStepsJson.decodeCards(json))
        assertEquals(emptyList<InfoCard>(), ToolStepsJson.decodeCards("""{"steps":[],"sources":[]}"""))
        assertEquals(emptyList<InfoCard>(), ToolStepsJson.decodeCards(null))
    }
}
