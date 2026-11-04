package app.hain.docs.fix

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** T-122: the examples of DOCS_OCR.md on the real word list (assets/dict/de_words.txt). */
class GermanOcrFixerTest {
    private val fixer = GermanOcrFixer(words)

    @Test
    fun uberAtWordStartBecomesUeber() {
        assertEquals("Überprüfung", fixer.fixWord("Uberprüfung"))
        assertEquals("ÜBER", fixer.fixWord("UBER"))
    }

    @Test
    fun capitalBBetweenLowerCaseBecomesEszett() {
        assertEquals("Größe", fixer.fixWord("GröBe"))
        assertEquals("Straße", fixer.fixWord("StraBe"))
    }

    @Test
    fun umlautAtWordStartOnlyWithDictionaryHit() {
        assertEquals("Ölwechsel", fixer.fixWord("Olwechsel"))
        assertEquals("Übung", fixer.fixWord("Ubung"))
        assertEquals("Ober", fixer.fixWord("Ober"))
    }

    @Test
    fun knownWordsStayAsTheyAre() {
        assertEquals("BUS", fixer.fixWord("BUS"))
        assertEquals("Bus", fixer.fixWord("Bus"))
        assertEquals("Apfel", fixer.fixWord("Apfel"))
        assertEquals("Grobe", fixer.fixWord("Grobe"))
    }

    @Test
    fun uberStaysInEnglishSentence() {
        assertEquals("I took an Uber to the station.", fixer.fix("I took an Uber to the station."))
        assertEquals("Die Überprüfung ist fertig.", fixer.fix("Die Uberprüfung ist fertig."))
    }

    @Test
    fun hyphenatedLineBreakIsJoined() {
        assertEquals("Die Versicherung zahlt.", fixer.fix("Die Ver-\nsicherung zahlt."))
        assertEquals("Baden-Württemberg", fixer.fix("Baden-\nWürttemberg"))
    }

    @Test
    fun wholeOcrLineIsFixed() {
        assertEquals(
            "Vielen Dank für Ihre Bestellung über 3 Größen.",
            fixer.fix("Vielen Dank für Ihre Bestellung über 3 GröBen."),
        )
        assertEquals(
            "Die Überprüfung der Straße dauert 14 Tage.",
            fixer.fix("Die Uberprüfung der StraBe dauert 14 Tage."),
        )
    }

    @Test
    fun wordListLooksUpAnyCase() {
        assertTrue(words.size > 200_000)
        assertTrue("Größe" in words)
        assertTrue("STRASSE" in words || "straße" in words)
        assertFalse("strabe" in words)
    }

    @Test
    fun wordListSkipsCommentsAndBlankLines() {
        val list = WordList.read("# Kommentar\n\nHaus\nbaum\n".byteInputStream())
        assertEquals(2, list.size)
        assertTrue("haus" in list)
        assertTrue("Baum" in list)
    }

    private companion object {
        val words: WordList by lazy {
            File("src/main/assets/dict/de_words.txt").inputStream().use { WordList.read(it) }
        }
    }
}
