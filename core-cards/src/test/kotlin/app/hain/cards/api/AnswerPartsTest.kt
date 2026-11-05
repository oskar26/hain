package app.hain.cards.api

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** T-130: splitting an answer into text and cards, also while it streams. */
class AnswerPartsTest {
    private val answer =
        "Hier dein Plan:\n\n```card\nCard title=\"Einkauf\"\nChecklist\n- Milch\n- Brot [x]\n```\n\nViel Spaß!"

    @Test
    fun plainAnswerIsOneTextPart() {
        assertEquals(listOf(AnswerPart.Text("Hallo **du**")), AnswerParts.split("Hallo **du**"))
        assertTrue(AnswerParts.split("").isEmpty())
    }

    @Test
    fun cardBetweenTexts() {
        val parts = AnswerParts.split(answer)
        assertEquals(3, parts.size)
        assertEquals(AnswerPart.Text("Hier dein Plan:"), parts[0])
        val card = parts[1] as AnswerPart.Card
        assertEquals("Einkauf", card.card.title)
        assertTrue(card.complete)
        assertEquals(0, card.index)
        assertEquals(AnswerPart.Text("Viel Spaß!"), parts[2])
    }

    @Test
    fun brokenCardStaysAsCodeText() {
        val text = "Vorher\n```card\nnur Quatsch\n```\nNachher"
        assertEquals(listOf(AnswerPart.Text(text)), AnswerParts.split(text))
    }

    @Test
    fun openCardWhileStreamingIsPartial() {
        val parts = AnswerParts.split("Plan:\n```card\nList\n- a\n- b", streaming = true)
        val card = parts.last() as AnswerPart.Card
        assertFalse(card.complete)
        assertEquals(listOf(CardBlock.Bullets(listOf("a", "b"))), card.card.blocks)
    }

    @Test
    fun emptyOpenCardIsHeldBackWhileStreamingButShownAfter() {
        val open = "Plan:\n```card\nCard title=\"X\""
        assertEquals(listOf(AnswerPart.Text("Plan:")), AnswerParts.split(open, streaming = true))
        assertEquals(listOf(AnswerPart.Text(open)), AnswerParts.split(open, streaming = false))
    }

    @Test
    fun halfWrittenFenceIsHeldBackWhileStreaming() {
        assertEquals(listOf(AnswerPart.Text("Plan:")), AnswerParts.split("Plan:\n```car", streaming = true))
    }

    @Test
    fun cardFenceInsideOtherCodeBlockIsLeftAlone() {
        val nested = "So geht's:\n````markdown\n```card\nList\n- a\n```\n````"
        assertTrue(AnswerParts.split(nested).none { it is AnswerPart.Card })
        val after = AnswerParts.split("So geht's:\n```markdown\nList\n```\n```card\nList\n- a\n```")
        assertEquals(2, after.size)
        assertTrue(after[1] is AnswerPart.Card)
    }

    @Test
    fun severalCardsAreCounted() {
        val parts = AnswerParts.split("```card\nList\n- a\n```\nund\n```card\nList\n- b\n```")
        assertEquals(listOf(0, 1), parts.filterIsInstance<AnswerPart.Card>().map { it.index })
    }

    @Test
    fun plainTextForCopyAndShare() {
        val state = CardState().toggled(0, 0, 0, default = false)
        assertEquals(
            "Hier dein Plan:\n\nEinkauf\n- [x] Milch\n- [x] Brot\n\nViel Spaß!",
            AnswerParts.plainText(answer, state),
        )
        assertTrue(AnswerParts.hasCards(answer))
        assertFalse(AnswerParts.hasCards("```kotlin\nval card = 1\n```"))
    }

    @Test
    fun cardTextForTableAndProgress() {
        val body = "| A | B |\n| 1 | 2 |\nProgress value=30 label=\"Fertig\"\nButton label=\"x\" action=copy"
        val card = CardParser.parse(body)!!
        assertEquals("A | B\n1 | 2\nFertig: 30 %", CardText.plain(card))
    }

    @Test
    fun looseCardSyntaxGetsItsFence() {
        // T-271: Oskar's device test, "Bastelidee mit Altpapier" (LFM3-Omni wrote the card without fence)
        val raw =
            "Card title=\"Altpapier Bastelidee\"\nChecklist\n• Altpapier\n• Stifte\n" +
                "• Papierbügel Button label=\"Kopieren\" action=copy"
        val fixed = AnswerParts.fenceLooseCards(raw)
        val card =
            AnswerParts
                .split(fixed)
                .filterIsInstance<AnswerPart.Card>()
                .single()
                .card
        assertEquals("Altpapier Bastelidee", card.title)
        val list = card.blocks.filterIsInstance<CardBlock.Checklist>().single()
        assertEquals(listOf("Altpapier", "Stifte", "Papierbügel"), list.items.map { it.text })
        assertTrue(card.blocks.any { it is CardBlock.Button })
        assertFalse(fixed.contains("•"))
    }

    @Test
    fun textAroundALooseCardStaysAndRealCardsAreUntouched() {
        val raw = "Hier ist deine Liste:\n\nCard title=\"Einkauf\"\nList\n- Milch\n- Brot\n\nViel Spaß!"
        val parts = AnswerParts.split(AnswerParts.fenceLooseCards(raw))
        assertEquals(3, parts.size)
        assertEquals("Viel Spaß!", (parts.last() as AnswerPart.Text).markdown)
        val fenced = "```card\nCard title=\"A\"\nList\n- x\n```"
        assertEquals(fenced, AnswerParts.fenceLooseCards(fenced))
        assertEquals("Eine Karte für dich.", AnswerParts.fenceLooseCards("Eine Karte für dich."))
    }
}
