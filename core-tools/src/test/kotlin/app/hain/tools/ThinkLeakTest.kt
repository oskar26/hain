package app.hain.tools

import app.hain.tools.api.ThinkLeak
import org.junit.Assert.assertEquals
import org.junit.Test

/** T-278: thinking without tags in the answer (LFM3 o5 "Was ist Moos?", LFM3-Omni). */
class ThinkLeakTest {
    @Test
    fun englishTaskTalkBeforeTheGermanAnswerGoes() {
        val raw =
            "We need to answer \"Was ist Moos?\" in German. The user asks what moss is.\n\n" +
                "Moose sind grüne Landpflanzen ohne echte Wurzeln [1]."
        assertEquals("Moose sind grüne Landpflanzen ohne echte Wurzeln [1].", ThinkLeak.strip(raw))
    }

    @Test
    fun severalThinkingParagraphsAndAStrayCloseTagGo() {
        val raw =
            "The user wants vocab.\n\nLet me search for it with a tool.\n\n" +
                "Hier sind drei Wörter: hola, gracias, adiós."
        assertEquals("Hier sind drei Wörter: hola, gracias, adiós.", ThinkLeak.strip(raw))
        assertEquals("Hallo!", ThinkLeak.strip("Okay, so the user greets me.</think>\n\nHallo!"))
    }

    @Test
    fun onlyThinkingBecomesEmpty() {
        assertEquals("", ThinkLeak.strip("We need to answer the question. Let's search."))
    }

    @Test
    fun realAnswersStay() {
        val english = "Let me explain: moss is a small green plant without real roots."
        assertEquals(english, ThinkLeak.strip(english))
        val german = "Wir brauchen dafür nur Papier und Kleber.\n\nSo geht's: …"
        assertEquals(german, ThinkLeak.strip(german))
        assertEquals("Okay, here are three tips.", ThinkLeak.strip("Okay, here are three tips."))
    }

    @Test
    fun liveHoldsWhatMayStillBeThinking() {
        assertEquals("", ThinkLeak.live("We ne"))
        assertEquals("", ThinkLeak.live("The user asks what moss is."))
        assertEquals("", ThinkLeak.live("We need to answer.\n\nMoo".substringBefore("Moo")))
        assertEquals("Moose sind", ThinkLeak.live("We need to answer.\n\nMoose sind"))
        assertEquals("Hallo, schön", ThinkLeak.live("Hallo, schön"))
    }
}
