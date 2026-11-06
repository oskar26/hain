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
    fun streamingIntoAnOlderChatMovesItToTheFrontAndKeepsTheRestInOrder() {
        // T-290: no sort per streamed word; the changed chat goes to the front, the others keep their order
        var now = 0L
        val store = ChatStore { now }
        val ids =
            (1..5).map {
                now = it.toLong()
                store.create("c$it").id
            }
        now = 10
        store.upsertMessage(ids[1], UiMessage(100, false, "a", streaming = true))
        assertEquals(listOf(ids[1], ids[4], ids[3], ids[2], ids[0]), store.chats.value.map { it.id })
        now = 11
        store.upsertMessage(ids[1], UiMessage(100, false, "ab", streaming = true))
        assertEquals(listOf(ids[1], ids[4], ids[3], ids[2], ids[0]), store.chats.value.map { it.id })
        assertEquals(
            "ab",
            store
                .chat(ids[1])!!
                .messages
                .single()
                .text,
        )
        store.upsertMessage(-1, UiMessage(101, false, "nobody"))
        assertEquals(5, store.chats.value.size)
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
