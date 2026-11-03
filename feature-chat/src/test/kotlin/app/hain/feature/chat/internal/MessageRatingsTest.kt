package app.hain.feature.chat.internal

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MessageRatingsTest {
    @Test
    fun `same thumb again takes it back, the other one replaces it`() {
        assertEquals(Rating.UP, null.toggled(Rating.UP))
        assertNull(Rating.UP.toggled(Rating.UP))
        assertEquals(Rating.DOWN, Rating.UP.toggled(Rating.DOWN))
    }

    @Test
    fun `in-memory store keeps one thumb per answer`() {
        val store = InMemoryRatings()
        store.set(7L, Rating.UP)
        store.set(8L, Rating.DOWN)
        store.set(7L, null)
        assertEquals(mapOf(8L to Rating.DOWN), store.ratings.value)
    }
}
