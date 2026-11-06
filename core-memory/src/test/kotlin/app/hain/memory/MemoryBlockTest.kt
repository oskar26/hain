package app.hain.memory

import app.hain.memory.internal.MemoryBlock
import org.junit.Assert.assertEquals
import org.junit.Test

class MemoryBlockTest {
    @Test
    fun verbFactsGetTheUserAsSubject() {
        assertEquals("Der Nutzer lernt Spanisch.", MemoryBlock.sentence("lernt Spanisch", de = true))
        assertEquals("The user lives in Cologne.", MemoryBlock.sentence("lives in Cologne.", de = false))
    }

    @Test
    fun ownWordsAndNounsKeepTheirWording() {
        assertEquals("Ich mag keine Pilze.", MemoryBlock.sentence("ich mag keine Pilze", de = true))
        assertEquals("Lieblingsfarbe: Blau.", MemoryBlock.sentence("Lieblingsfarbe: Blau", de = true))
        assertEquals("Hat er einen Hund?", MemoryBlock.sentence("Hat er einen Hund?", de = true))
    }
}
