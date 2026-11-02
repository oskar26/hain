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

    @Test
    fun truncateFromKeepsOrDropsTheMessage() {
        val store = ChatStore()
        val chat = store.create("t")
        (1L..4L).forEach { store.upsertMessage(chat.id, UiMessage(it, it % 2 == 1L, "m$it")) }
        store.truncateFrom(chat.id, 3, keep = true)
        assertEquals(listOf(1L, 2L, 3L), store.chat(chat.id)!!.messages.map { it.id })
        store.truncateFrom(chat.id, 2, keep = false)
        assertEquals(listOf(1L), store.chat(chat.id)!!.messages.map { it.id })
    }
}
