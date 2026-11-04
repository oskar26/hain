package app.hain.feature.chat.internal

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/** T-220: no fake citations, no tool talk and no "Fakt:" label without tool sources. */
class CitationCleanerTest {
    private fun clean(
        text: String,
        sources: Int = 0,
    ) = CitationCleaner.clean(text, sources)

    @Test
    fun oskarsTreeExample() {
        val raw =
            "**Fakt:** Es gibt keine allgemeine, feste Lebensdauer für Bäume. Viele Arten wie Eichen können " +
                "200+ Jahre alt werden [1]. Manche Kiefern werden sogar über 4000 Jahre alt.\n\n" +
                "[1] Wikipedia-Artikel zu Baum-Lebensdauer (falls verfügbar) – hier kein aktuelles Werkzeug " +
                "nötig für diese allgemeine Frage.\n\n" +
                "Kurz gesagt: Es hängt stark von der Baumart ab."
        val expected =
            "Es gibt keine allgemeine, feste Lebensdauer für Bäume. Viele Arten wie Eichen können " +
                "200+ Jahre alt werden. Manche Kiefern werden sogar über 4000 Jahre alt.\n\n" +
                "Kurz gesagt: Es hängt stark von der Baumart ab."
        assertEquals(expected, clean(raw))
    }

    @Test
    fun removesAllMarkerShapesWithoutSources() {
        assertEquals("Eichen werden alt.", clean("Eichen werden alt [1]."))
        assertEquals("Eichen werden alt.", clean("Eichen werden alt[2]."))
        assertEquals("Eichen werden alt.", clean("Eichen werden alt [2, 3]."))
        assertEquals("Eichen werden alt.", clean("Eichen werden alt [1][2]."))
        assertEquals("Eichen werden alt.", clean("Eichen werden alt [1-3]."))
        assertEquals("Oaks grow old.", clean("Oaks grow old 【1】."))
        assertEquals("Oaks grow old.", clean("Oaks grow old【1†source】."))
        assertEquals("A, B and C.", clean("A [1], B [2] and C [3]."))
    }

    @Test
    fun removesSourceLinesAndBlocks() {
        val de = "Bäume werden alt.\n\nQuellen:\n- [1] Wikipedia\n- [2] Baum-Lexikon\n\nMehr fragen?"
        assertEquals("Bäume werden alt.\n\nMehr fragen?", clean(de))
        val en = "Trees get old.\n\n**Sources:**\n1. Wikipedia\n2. Some site"
        assertEquals("Trees get old.", clean(en))
        assertEquals("Trees get old.", clean("Trees get old.\n### References\n[1] Wikipedia, Tree"))
        assertEquals("Trees get old.", clean("Trees get old.\nQuelle: Wikipedia"))
        assertEquals("Trees get old.", clean("Trees get old.\n[1]: https://example.org"))
    }

    @Test
    fun dropsToolTalkGermanAndEnglish() {
        assertEquals(
            "Die Erde ist rund.",
            clean("Die Erde ist rund. Für diese Frage habe ich kein Werkzeug gebraucht."),
        )
        assertEquals(
            "The Earth is round.",
            clean("The Earth is round. No tool was needed for this general question."),
        )
        assertEquals(
            "The Earth is round.",
            clean("The Earth is round (no web search needed for this question)."),
        )
        val meta = "Dafür war keine Websuche nötig, die Antwort kenne ich.\nParis ist die Hauptstadt."
        assertEquals("Paris ist die Hauptstadt.", clean(meta))
        assertEquals("Fertig.", clean("Fertig. I used no tool for this answer."))
    }

    @Test
    fun keepsRealToolsLikeAHammer() {
        val diy = "Dafür brauchst du kein Werkzeug, eine Münze reicht. Ein Akkuschrauber ist aber praktisch."
        assertSame(diy, clean(diy))
        val en = "You don't need any tool for this, just your hands."
        assertSame(en, clean(en))
        // an honest "I can't know this without a tool" stays: it says nothing about a need
        val honest = "Ohne Websuche weiß ich das nicht aktuell, frag mich gern mit Internet."
        assertSame(honest, clean(honest))
    }

    @Test
    fun stripsTheFactLabel() {
        assertEquals("Wasser kocht bei 100 °C.", clean("**Fakt:** Wasser kocht bei 100 °C."))
        assertEquals("Wasser kocht bei 100 °C.", clean("Fakt: Wasser kocht bei 100 °C."))
        assertEquals("Water boils at 100 °C.", clean("**Fact**: Water boils at 100 °C."))
        assertEquals("- Water boils.", clean("- **Fact:** Water boils."))
        assertEquals("Water boils.", clean("Fact: Water boils.", sources = 2))
        val word = "Faktisch ist das so."
        assertSame(word, clean(word))
    }

    @Test
    fun keepsValidMarkersWithSources() {
        assertEquals("Köln hat 1,1 Mio. Einwohner [1].", clean("Köln hat 1,1 Mio. Einwohner [1].", sources = 2))
        assertEquals("A [1][2].", clean("A [1][2].", sources = 2))
        assertEquals("A [2].", clean("A [2][5].", sources = 2))
        assertEquals("A [1, 2].", clean("A [1, 2, 7].", sources = 2))
        assertEquals("A.", clean("A [3].", sources = 2))
        assertEquals("A [1-2].", clean("A [1-2].", sources = 2))
        assertEquals("A [1, 2].", clean("A [1-3].", sources = 2))
    }

    @Test
    fun withSourcesKeepsValidSourceLinesAndToolWords() {
        val text = "Es regnet [1].\n\nQuellen:\n[1] DWD\n[4] Erfunden"
        assertEquals("Es regnet [1].\n\nQuellen:\n[1] DWD", clean(text, sources = 1))
        val tools = "Laut Websuche ist das nicht aktuell [1]."
        assertSame(tools, clean(tools, sources = 1))
    }

    @Test
    fun neverTouchesCodeBlocks() {
        val text = "So geht's:\n\n```kotlin\nval x = list[1]\n// Quellen:\n[2] keep\n```\n\nFertig [1]."
        val expected = "So geht's:\n\n```kotlin\nval x = list[1]\n// Quellen:\n[2] keep\n```\n\nFertig."
        assertEquals(expected, clean(text))
        val tilde = "~~~\n[1] a\n\n\n[2] b\n~~~"
        assertSame(tilde, clean(tilde))
    }

    @Test
    fun unclosedCodeBlockWhileStreamingStaysUntouched() {
        val text = "Code:\n```python\nprint(a[1])\n[1] x"
        assertSame(text, clean(text))
    }

    @Test
    fun neverTouchesInlineCodeOrLinks() {
        assertEquals("Nimm `arr[1]` dafür.", clean("Nimm `arr[1]` dafür [1]."))
        val link = "Siehe [1](https://example.org) und [Wiki](https://de.wikipedia.org/wiki/Baum)."
        assertSame(link, clean(link))
        val spaced = "Nimm `a  b` hier [1]."
        assertEquals("Nimm `a  b` hier.", clean(spaced))
    }

    @Test
    fun leavesOtherBracketsAlone() {
        val years = "Das war [2024] so, und [x] ist erledigt."
        assertSame(years, clean(years))
        val plain = "Ganz normale Antwort ohne alles."
        assertSame(plain, clean(plain))
        assertEquals("", clean(""))
    }

    @Test
    fun isStableOnItsOwnOutput() {
        val raw = "**Fakt:** A [1].\n\nQuellen:\n- [1] X\n\nB."
        val once = clean(raw)
        assertEquals(once, clean(once))
        assertFalse(once.contains("["))
        assertTrue(once.startsWith("A."))
    }

    @Test
    fun streamingPartialMarkerIsLeftUntilComplete() {
        // "[1" without "]" is not a marker yet; it goes once the "]" has streamed in
        val partial = "Eichen werden alt [1"
        assertSame(partial, clean(partial))
        assertEquals("Eichen werden alt", clean("$partial]"))
    }

    @Test
    fun numbersOfSpreadsRanges() {
        assertEquals(listOf(1, 2, 3), CitationCleaner.numbersOf("1-3"))
        assertEquals(listOf(2, 3), CitationCleaner.numbersOf("2, 3"))
        assertEquals(listOf(4), CitationCleaner.numbersOf("4"))
    }
}
