package app.hain.feature.chat.internal

import org.junit.Assert.assertEquals
import org.junit.Test

class ChatStoreTest {
    @Test
    fun upsertUpdatesInPlaceAndSortsByRecency() {
        var now = 0L
        val store = ChatStore { now }
        val a = store.create("a")
        now = 1
        val b = store.create("b")
        now = 2
        store.upsertMessage(a.id, UiMessage(10, true, "hi"))
        now = 3
        store.upsertMessage(a.id, UiMessage(10, true, "hallo"))
        assertEquals(listOf(a.id, b.id), store.chats.value.map { it.id })
        assertEquals(listOf("hallo"), store.chat(a.id)!!.messages.map { it.text })
    }

    @Test
    fun titleIsShortenedOnWordBoundary() {
        val store = ChatStore()
        assertEquals("Kurz", store.titleFrom("  Kurz "))
        assertEquals(
            "Wie kann ich im Alltag am einfachsten…",
            store.titleFrom("Wie kann ich im Alltag am einfachsten Plastik sparen und dabei Geld?"),
        )
    }
}
