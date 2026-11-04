package app.hain.cards.api

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** T-130: the line parser for model cards (CARDS.md §2–3). */
class CardParserTest {
    @Test
    fun specExampleParses() {
        val card =
            CardParser.parse(
                """
                Card title="Einkaufsliste"
                Checklist
                - Tomaten
                - Basilikum [x]
                Button label="In Notizen kopieren" action=copy
                """.trimIndent(),
            )!!
        assertEquals("Einkaufsliste", card.title)
        assertEquals(
            listOf(
                CardBlock.Checklist(listOf(CheckItem("Tomaten", false), CheckItem("Basilikum", true))),
                CardBlock.Button("In Notizen kopieren", CardAction.Copy),
            ),
            card.blocks,
        )
    }

    @Test
    fun listItemsWithDifferentBullets() {
        val card = CardParser.parse("List\n- eins\n* zwei\n• drei\n1. vier\n2) fünf")!!
        assertEquals(listOf(CardBlock.Bullets(listOf("eins", "zwei", "drei", "vier", "fünf"))), card.blocks)
    }

    @Test
    fun tableFirstRowIsHeaderAndSeparatorIsSkipped() {
        val card = CardParser.parse("Table\n| Stadt | Grad |\n|---|:---:|\n| Köln | 9 |\n| Bonn | 10 |")!!
        assertEquals(
            CardBlock.Table(listOf("Stadt", "Grad"), listOf(listOf("Köln", "9"), listOf("Bonn", "10"))),
            card.blocks.single(),
        )
    }

    @Test
    fun shortTableRowsArePaddedAndLongOnesCut() {
        val table = CardParser.parse("| a | b |\n| 1 |\n| 1 | 2 | 3 |")!!.blocks.single() as CardBlock.Table
        assertEquals(listOf(listOf("1", ""), listOf("1", "2")), table.rows)
    }

    @Test
    fun tableAndListWithoutKeywordAreDetected() {
        val card = CardParser.parse("- Milch\n- Brot\n| A | B |\n| 1 | 2 |")!!
        assertTrue(card.blocks[0] is CardBlock.Bullets)
        assertTrue(card.blocks[1] is CardBlock.Table)
    }

    @Test
    fun checkMarksBeforeOrAfterTheText() {
        val body = "Checklist\n- [x] Hausaufgaben\n- [ ] Zimmer\n- Müll [X]\n- ✅ Sport\n- ☐ Lesen"
        assertEquals(
            CardBlock.Checklist(
                listOf(
                    CheckItem("Hausaufgaben", true),
                    CheckItem("Zimmer", false),
                    CheckItem("Müll", true),
                    CheckItem("Sport", true),
                    CheckItem("Lesen", false),
                ),
            ),
            CardParser.parse(body)!!.blocks.single(),
        )
    }

    @Test
    fun markedItemsWithoutKeywordBecomeAChecklist() {
        assertTrue(CardParser.parse("- [ ] a\n- [x] b")!!.blocks.single() is CardBlock.Checklist)
    }

    @Test
    fun progressValueIsClampedAndAcceptsPercentAndFractions() {
        fun first(body: String) = CardParser.parse(body)!!.blocks[0]
        assertEquals(CardBlock.Progress(40, "Vokabeln"), first("Progress value=40 label=\"Vokabeln\""))
        assertEquals(CardBlock.Progress(100, null), first("Progress value=250"))
        assertEquals(CardBlock.Progress(0, null), first("Progress value=-5"))
        assertEquals(CardBlock.Progress(75, null), first("Progress 75%"))
        assertEquals(CardBlock.Progress(50, null), first("Progress value=0.5"))
        assertNull(CardParser.parse("Progress value=viel"))
    }

    @Test
    fun buttonActions() {
        val card =
            CardParser.parse(
                """
                Button label="Kopieren" action=copy
                Button label="Teilen" action=share
                Button label="Mehr" action=ask:"Erklär mir das genauer"
                """.trimIndent(),
            )!!
        assertEquals(
            listOf(
                CardBlock.Button("Kopieren", CardAction.Copy),
                CardBlock.Button("Teilen", CardAction.Share),
                CardBlock.Button("Mehr", CardAction.Ask("Erklär mir das genauer")),
            ),
            card.blocks,
        )
    }

    @Test
    fun askWithGermanQuotesAndUnclosedQuote() {
        val a = CardParser.parse("Button label=„Weiter“ action=ask:„Nächster Schritt?“")!!.blocks.single()
        assertEquals(CardBlock.Button("Weiter", CardAction.Ask("Nächster Schritt?")), a)
        val b = CardParser.parse("Button label=\"Weiter\" action=ask:\"Noch eine Fra")!!.blocks.single()
        assertEquals(CardBlock.Button("Weiter", CardAction.Ask("Noch eine Fra")), b)
        val c = CardParser.parse("Button action=ask:\"Und dann?\" label=\"Weiter\"")!!.blocks.single()
        assertEquals(CardBlock.Button("Weiter", CardAction.Ask("Und dann?")), c)
    }

    @Test
    fun unknownActionsAndButtonsWithoutLabelAreDropped() {
        assertNull(CardParser.parse("Button label=\"Öffnen\" action=open:https://example.com"))
        assertNull(CardParser.parse("Button action=copy"))
    }

    @Test
    fun atMostThreeButtons() {
        val body = (1..5).joinToString("\n") { "Button label=\"B$it\" action=copy" }
        assertEquals(CardParser.MAX_BUTTONS, CardParser.parse(body)!!.blocks.size)
    }

    @Test
    fun unknownLinesAreIgnored() {
        val card = CardParser.parse("Hallo du\nImage src=attachment:1\nList\n- a\nirgendwas\n- b")!!
        assertEquals(listOf(CardBlock.Bullets(listOf("a", "b"))), card.blocks)
    }

    @Test
    fun nothingUsableIsNull() {
        assertNull(CardParser.parse(""))
        assertNull(CardParser.parse("Card title=\"Nur ein Titel\""))
        assertNull(CardParser.parse("Das ist einfach Text."))
        assertNull(CardParser.parse("List\nChecklist\nTable"))
    }

    @Test
    fun itemsAndLinesAreCapped() {
        val body = "List\n" + (1..50).joinToString("\n") { "- $it" }
        val items = (CardParser.parse(body)!!.blocks.single() as CardBlock.Bullets).items
        assertEquals(CardParser.MAX_ITEMS, items.size)
        val two =
            "List\n" + (1..15).joinToString("\n") { "- $it" } +
                "\nList\n" + (1..30).joinToString("\n") { "- x$it" }
        val blocks = CardParser.parse(two)!!.blocks
        assertEquals(CardParser.MAX_LINES - 2, blocks.sumOf { (it as CardBlock.Bullets).items.size })
    }

    @Test
    fun textIsCleanedAndCapped() {
        val card = CardParser.parse("List\n- **fett** `code`\u0007\n- " + "x".repeat(500))!!
        val items = (card.blocks.single() as CardBlock.Bullets).items
        assertEquals("fett code", items[0])
        assertEquals(CardParser.MAX_TEXT, items[1].length)
    }

    @Test
    fun germanKeywords() {
        val body =
            "Karte title=\"Plan\"\nCheckliste\n- a\nTabelle\n| x | y |\n" +
                "Fortschritt value=10\nKnopf label=\"K\" action=copy"
        val card = CardParser.parse(body)!!
        assertEquals("Plan", card.title)
        assertEquals(4, card.blocks.size)
    }

    @Test
    fun streamingPrefixesNeverThrow() {
        val full =
            "Card title=\"Lernplan\"\nTable\n| Tag | Fach |\n|---|---|\n| Mo | Mathe |\n" +
                "Checklist\n- Vokabeln [x]\n- Referat\nProgress value=60 label=\"Woche\"\n" +
                "Button label=\"Weiter\" action=ask:\"Und Dienstag?\""
        for (n in 0..full.length) CardParser.parse(full.take(n))
        assertEquals(4, CardParser.parse(full)!!.blocks.size)
    }

    @Test
    fun attributesWithEscapedQuotes() {
        assertEquals(
            mapOf("title" to "Er sagt \"hi\"", "x" to "1"),
            CardParser.attributes("title=\"Er sagt \\\"hi\\\"\" x=1"),
        )
    }
}
