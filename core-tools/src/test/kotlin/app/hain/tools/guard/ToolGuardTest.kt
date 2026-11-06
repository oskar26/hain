package app.hain.tools.guard

import app.hain.tools.api.ToolGuard
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** T-263: weather and news only when the message asks for them; other tools are the brain's call. */
class ToolGuardTest {
    @Test
    fun weatherAndNewsOffTopicAreLeftOut() {
        val moved = listOf("ich bin neu nach Köln gezogen was machen")
        assertFalse(ToolGuard.fits("weather", moved))
        assertFalse(ToolGuard.fits("news", moved))
        assertTrue(ToolGuard.fits("wikipedia", moved))
        assertTrue(ToolGuard.fits("web_search", moved))
        assertFalse(ToolGuard.fits("weather", listOf("Was kann man in Hamburg am Wochenende machen?")))
        assertFalse(ToolGuard.fits("news", listOf("I just moved to Bristol, any tips?")))
    }

    @Test
    fun ownKnowledgeAsksNeedNoLookup() {
        assertFalse(ToolGuard.fits("web_search", listOf("Gib mir 3 Vokabeln für heute")))
        assertFalse(ToolGuard.fits("wikipedia", listOf("Übersetze „Guten Morgen“ ins Spanische")))
        assertFalse(ToolGuard.fits("web_search", listOf("tell me a joke")))
        assertTrue(ToolGuard.fits("web_search", listOf("Such im Internet nach spanischen Vokabel-Apps")))
        assertTrue(ToolGuard.fits("calculate", listOf("Gib mir 3 Vokabeln für heute")))
        assertFalse(ToolGuard.hint(listOf("web_search"), "de").contains("nimm web_search"))
        assertTrue(ToolGuard.hint(listOf("weather"), "de").contains("nimm web_search"))
    }

    @Test
    fun askedForWeatherOrNews() {
        assertTrue(ToolGuard.fits("weather", listOf("Wie wird das Wetter morgen in Köln?")))
        assertTrue(ToolGuard.fits("weather", listOf("brauch ich heute nen Regenschirm")))
        assertTrue(ToolGuard.fits("weather", listOf("Do I need a jacket today?")))
        assertTrue(ToolGuard.fits("news", listOf("Was gibt's Neues?")))
        assertTrue(ToolGuard.fits("news", listOf("Nachrichten von heute bitte")))
        assertTrue(ToolGuard.fits("news", listOf("what's the latest news")))
    }

    @Test
    fun aShortFollowUpKeepsTheTopic() {
        assertTrue(ToolGuard.fits("weather", listOf("Wie wird das Wetter in Köln?", "und morgen?")))
        assertTrue(ToolGuard.fits("weather", listOf("weather in Leeds", "and in York?")))
        assertFalse(
            ToolGuard.fits(
                "weather",
                listOf("Wie wird das Wetter in Köln?", "Erklär mir bitte ausführlich, wie Photosynthese funktioniert"),
            ),
        )
    }

    @Test
    fun aWebSearchForTheWeatherCountsAsWeather() {
        val moved = listOf("I just moved to Bristol, any tips?")
        assertFalse(ToolGuard.fits("web_search", moved, "Bristol weather November 4 2026"))
        assertTrue(ToolGuard.fits("web_search", moved, "things to do in Bristol"))
        assertTrue(ToolGuard.fits("web_search", listOf("Wird es morgen in Köln regnen?"), "Köln Wetter morgen"))
    }

    @Test
    fun plainWeatherAndNewsQuestionsGetANudge() {
        assertEquals("weather", ToolGuard.asked("Ich bin neu nach Köln gezogen. Brauche ich heute eine Jacke?"))
        assertEquals("weather", ToolGuard.asked("wie wird das wetter morgen?"))
        assertEquals("news", ToolGuard.asked("Was gibt's Neues?"))
        assertEquals("news", ToolGuard.asked("any news today?"))
        assertNull(ToolGuard.asked("ich bin neu nach köln gezogen was machen"))
        assertNull(ToolGuard.asked("Ich mag warmes Essen"))
        assertTrue(ToolGuard.nudge("weather", "de").contains("rufe weather auf"))
        assertTrue(ToolGuard.nudge("news", "en").contains("call news"))
    }

    @Test
    fun noMessageMeansNoGuard() {
        assertTrue(ToolGuard.fits("weather", emptyList()))
        assertTrue(ToolGuard.fits("calculate", listOf("hallo")))
    }

    @Test
    fun theHintNamesTheToolsInTheRightLanguage() {
        val de = ToolGuard.hint(listOf("weather", "news", "weather"), "de")
        assertTrue(de.startsWith("Die Frage braucht weather, news nicht."))
        assertTrue(ToolGuard.hint(listOf("news"), "en").startsWith("The question does not need news."))
    }
}
