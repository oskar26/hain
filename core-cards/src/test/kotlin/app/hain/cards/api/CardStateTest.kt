package app.hain.cards.api

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** T-131: the ticks a user sets on a checklist survive in `cards_json`. */
class CardStateTest {
    @Test
    fun toggleAndBackStoresNothing() {
        val once = CardState().toggled(0, 1, 2, default = false)
        assertTrue(once.isChecked(0, 1, 2, default = false))
        assertEquals("{\"checked\":{\"0.1.2\":true}}", once.encode())
        assertNull(once.toggled(0, 1, 2, default = false).encode())
    }

    @Test
    fun untickingWhatTheBrainTicked() {
        val state = CardState().toggled(1, 0, 0, default = true)
        assertFalse(state.isChecked(1, 0, 0, default = true))
        assertEquals(state, CardState.decode(state.encode()))
    }

    @Test
    fun brokenJsonMeansNoChanges() {
        assertEquals(CardState(), CardState.decode("{kaputt"))
        assertEquals(CardState(), CardState.decode(null))
    }
}
