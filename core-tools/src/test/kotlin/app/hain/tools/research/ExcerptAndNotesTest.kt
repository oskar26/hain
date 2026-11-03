package app.hain.tools.research

import app.hain.tools.api.ResearchNotes
import app.hain.tools.internal.search.Excerpt
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExcerptAndNotesTest {
    private val page =
        listOf(
            "Willkommen auf unserer Seite über Heizungen und Energie im Haus, schön dass du da bist.",
            "Unser Newsletter erscheint jeden Monat und bringt dir Tipps rund ums Wohnen und Garten.",
            "Eine Wärmepumpe braucht etwa ein Viertel des Stroms, den sie als Wärme abgibt. Sie spart so Energie.",
            "Gasheizungen verbrennen Erdgas und stoßen dabei Kohlendioxid aus, das dem Klima schadet.",
            "Die Jahresarbeitszahl einer Wärmepumpe liegt meist zwischen 3 und 4, im Altbau etwas darunter.",
            "Kontakt, Impressum und Datenschutz findest du ganz unten auf jeder unserer vielen Seiten.",
        ).joinToString("\n")

    @Test
    fun excerptKeepsTheParagraphsThatFitTheQuestionInPageOrder() {
        val out = Excerpt.select(page, "Wie viel Strom spart eine Wärmepumpe?", 230)
        assertTrue(out.contains("Viertel des Stroms"))
        assertTrue(out.contains("Jahresarbeitszahl"))
        assertFalse(out.contains("Newsletter"))
        assertFalse(out.contains("Impressum"))
        assertTrue(out.length <= 230)
        assertTrue(out.indexOf("Viertel") < out.indexOf("Jahresarbeitszahl"))
    }

    @Test
    fun excerptFallsBackToThePageStartWhenNothingMatches() {
        val out = Excerpt.select(page, "Quantenchromodynamik", 120)
        assertTrue(out.startsWith("Willkommen"))
        assertTrue(out.length <= 121)
    }

    @Test
    fun excerptCutsOneHugeParagraphIntoSentences() {
        val huge =
            "Füller ohne Inhalt steht hier. ".repeat(40) + "Die Zugspitze ist 2962 Meter hoch. " +
                "Noch mehr Füller steht hier. ".repeat(40)
        val out = Excerpt.select(huge, "Wie hoch ist die Zugspitze", 300)
        assertTrue(out.contains("2962 Meter"))
        assertTrue(out.length <= 300)
    }

    @Test
    fun notesTakeTheBestSentencesWithTheirSourceNumber() {
        val notes = ResearchNotes()
        notes.add(1, Excerpt.select(page, "Strom Wärmepumpe", 400), "Strom Wärmepumpe")
        val repeat = "Eine Wärmepumpe braucht etwa ein Viertel des Stroms, den sie als Wärme abgibt."
        notes.add(3, repeat, "Strom Wärmepumpe")
        notes.add(3, "Doppelt gemeldet wird nicht noch einmal aufgenommen, auch wenn es lang genug ist.", "x")
        // source 3 only repeats a fact of source 1: no new note, but the fact now has two sources
        assertEquals(2, notes.facts)
        assertEquals(2, notes.sources)
        assertTrue(notes.lines().any { it.startsWith("[1][3] ") })
        assertTrue(notes.forModel(1).startsWith("{\"notes\":[\"[1]"))
        notes.add(2, "Gas kostet 2026 im Schnitt 11 Cent je Kilowattstunde, sagt das Vergleichsportal.", "Gas Preis")
        assertEquals(3, notes.sources)
        assertEquals(1, notes.lines(2).size)
    }
}
