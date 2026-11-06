package app.hain.tools.guard

import app.hain.tools.api.CardGuard
import app.hain.tools.api.ToolGuard
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** T-293: Oskar's screenshot 06.11. 17:04 — a checklist of questions instead of an answer. */
class CardGuardTest {
    private val question = "Was hat es mit der Neuwahl des Bundespräsidenten auf sich? Wer kann eigentlich wählen?"

    private val checklistOnly =
        """
        ```card
        Card title="Bundespräsidentenwahl"
        Checklist
        - Wer kann wählen?
        - Wie läuft die Wahl ab?
        - Wer entscheidet über den Kandidaten?
        - Wie viel Geld kostet die Wahl?
        ```
        """.trimIndent()

    @Test
    fun screenshotCaseIsACardInPlaceOfAnAnswer() {
        assertFalse(CardGuard.wanted(question))
        assertTrue(CardGuard.replacesAnswer(checklistOnly))
    }

    @Test
    fun questionChecklistEvenAfterASentenceIsWrong() {
        val text = "Gute Frage! Hier sind ein paar Punkte, die man sich dazu anschauen kann:\n$checklistOnly"
        assertTrue(CardGuard.replacesAnswer(text))
    }

    @Test
    fun realAnswerWithShoppingCardIsFine() {
        val text =
            "Klar, hier ist deine Einkaufsliste für den Kuchen, alles für 12 Stücke:\n" +
                "```card\nCard title=\"Einkauf\"\nChecklist\n- Mehl\n- Eier\n- Zucker\n```"
        assertFalse(CardGuard.replacesAnswer(text))
        assertTrue(CardGuard.wanted("Mach mir eine Einkaufsliste für Apfelkuchen"))
    }

    @Test
    fun answerWithoutCardIsFine() {
        assertFalse(CardGuard.replacesAnswer("Den Bundespräsidenten wählt die Bundesversammlung [1]."))
    }

    @Test
    fun listWishesAreRecognised() {
        assertTrue(CardGuard.wanted("Erstell mir einen Lernplan für Mathe"))
        assertTrue(CardGuard.wanted("Make a packing list for Rome"))
        assertTrue(CardGuard.wanted("Vergleiche iPhone und Pixel in einer Tabelle"))
        assertFalse(CardGuard.wanted("Wer kann den Bundespräsidenten wählen?"))
    }

    @Test
    fun hintAsksForSentencesAndSearch() {
        assertTrue(CardGuard.hint("de").contains("web_search"))
        assertTrue(CardGuard.hint("en").contains("full sentences"))
    }

    @Test
    fun electionQuestionNudgesWebSearch() {
        assertEquals("web_search", ToolGuard.asked(question))
        assertEquals("web_search", ToolGuard.asked("Who can vote in the US election?"))
        assertTrue(ToolGuard.nudge("web_search", "de").contains("web_search"))
    }

    @Test
    fun everydayChoicesAreNoElection() {
        assertNull(ToolGuard.asked("Hilf mir, eine Farbe für mein Zimmer zu wählen"))
        assertNull(ToolGuard.asked("Das war eine gute Wahl, oder?"))
        assertEquals("weather", ToolGuard.asked("Wie wird das Wetter morgen?"))
    }
}
