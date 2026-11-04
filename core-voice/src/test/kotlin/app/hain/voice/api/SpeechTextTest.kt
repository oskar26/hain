package app.hain.voice.api

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SpeechTextTest {
    private val de = SpeechLabels(codeSkipped = "Codeblock übersprungen", formulaSkipped = "Formel übersprungen")
    private val en = SpeechLabels(codeSkipped = "Code block skipped", formulaSkipped = "Formula skipped")

    private fun say(
        md: String,
        labels: SpeechLabels = de,
    ) = SpeechText.speakable(md, labels)

    @Test
    fun boldItalicAndHeadingsLoseTheirMarks() {
        val out = say("# Wetter heute\nEs wird **sonnig** und *warm*, ~~kein~~ Regen.")
        assertEquals(listOf("Wetter heute.", "Es wird sonnig und warm, kein Regen."), out)
    }

    @Test
    fun codeBlockIsSkippedWithALabel() {
        val md = "So geht's:\n\n```kotlin\nval x = 1\nprintln(x)\n```\n\nFertig."
        assertEquals(listOf("So geht's:", "Codeblock übersprungen.", "Fertig."), say(md))
        assertEquals(listOf("Code block skipped."), say("```\nrm -rf /\n```", en))
    }

    @Test
    fun inlineCodeKeepsItsText() {
        assertEquals(listOf("Tipp ls ein."), say("Tipp `ls` ein."))
    }

    @Test
    fun linksKeepTextAndUrlsOnlyTheDomain() {
        val md = "Mehr bei [der Tagesschau](https://www.tagesschau.de/x?y=1) oder https://www.wikipedia.org/wiki/Köln."
        val out = say(md)
        assertEquals(listOf("Mehr bei der Tagesschau oder wikipedia.org."), out)
    }

    @Test
    fun imagesKeepTheirDescription() {
        assertEquals(listOf("Ein Baum."), say("![Ein Baum](https://x.de/baum.png)"))
    }

    @Test
    fun listItemsBecomeSentences() {
        val md = "Du brauchst:\n- Mehl\n- Eier\n* Milch\n1. Rühren\n2) Backen!"
        assertEquals(listOf("Du brauchst:", "Mehl.", "Eier.", "Milch.", "Rühren.", "Backen!"), say(md))
    }

    @Test
    fun checklistBoxesAreDropped() {
        assertEquals(listOf("Einkaufen.", "Lernen."), say("- [ ] Einkaufen\n- [x] Lernen"))
    }

    @Test
    fun tablesAreReadRowByRow() {
        val md = "| Tag | Grad |\n|---|---:|\n| Mo | 12 |\n| Di | 14 |"
        assertEquals(listOf("Tag, Grad.", "Mo, 12.", "Di, 14."), say(md))
    }

    @Test
    fun tablesCanBeSkippedWithALabel() {
        val labels = de.copy(tableSkipped = "Tabelle übersprungen")
        assertEquals(listOf("Tabelle übersprungen."), say("| a | b |\n|---|---|\n| 1 | 2 |", labels))
    }

    @Test
    fun citationsQuotesRulesAndHtmlGo() {
        val md = "> Ein Zitat [1].\n\n---\n\nText<br>weiter [2, 3]."
        assertEquals(listOf("Ein Zitat.", "Text weiter."), say(md))
    }

    @Test
    fun formulasAreSkippedOrUnwrapped() {
        assertEquals(listOf("Formel übersprungen."), say("$$\\int_0^1 x\\,dx$$"))
        assertEquals(listOf("Es gilt a = b."), say("Es gilt \$a = b\$."))
    }

    @Test
    fun sentencesSplitAtTheEnd() {
        val out = SpeechText.sentences("Hallo! Wie geht's? Mir geht's gut. Und dir…")
        assertEquals(listOf("Hallo!", "Wie geht's?", "Mir geht's gut.", "Und dir…"), out)
    }

    @Test
    fun abbreviationsNumbersAndDatesDoNotSplit() {
        val out = SpeechText.sentences("Das kostet ca. 3.50 Euro, z.B. am 1. Mai bei Dr. Meier. Danach geht es weiter.")
        assertEquals(listOf("Das kostet ca. 3.50 Euro, z.B. am 1. Mai bei Dr. Meier.", "Danach geht es weiter."), out)
    }

    @Test
    fun englishSentencesSplitToo() {
        val out = SpeechText.sentences("It's 5 p.m. now. Mr. Smith said \"Go!\" Then he left.")
        assertEquals(listOf("It's 5 p.m. now.", "Mr. Smith said \"Go!\"", "Then he left."), out)
    }

    @Test
    fun ellipsisAndDoublePunctuationStayTogether() {
        assertEquals(listOf("Hm... na gut.", "Echt?!", "Ja."), SpeechText.sentences("Hm... na gut. Echt?! Ja."))
    }

    @Test
    fun longSentencesAreCutAtACommaOrSpace() {
        val long = (1..60).joinToString(", ") { "Wort $it" } + "."
        val out = SpeechText.sentences(long, maxChars = 100)
        assertTrue(out.size > 1)
        assertTrue(out.all { it.length <= 101 })
        assertEquals(long.replace(" ", ""), out.joinToString("").replace(" ", ""))
    }

    @Test
    fun emptyAndSymbolOnlyTextSaysNothing() {
        assertTrue(say("").isEmpty())
        assertTrue(say("---\n\n***").isEmpty())
        assertFalse(say("🌳 Baum").isEmpty())
    }

    @Test
    fun paragraphLinesAreJoined() {
        assertEquals(listOf("Eine Zeile und noch eine."), say("Eine Zeile\nund noch eine."))
    }
}
