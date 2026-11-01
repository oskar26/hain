package app.hain.feature.chat.internal

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ChatStorePersistenceTest {
    private class FakePersistence(
        saved: List<Chat> = emptyList(),
    ) : ChatPersistence {
        val chats = saved.associateBy { it.id }.toMutableMap()
        val log = mutableListOf<String>()

        override suspend fun load(): List<Chat> = chats.values.toList()

        override suspend fun maxId(): Long {
            val ids = chats.values.flatMap { c -> c.messages.map { it.id } + c.id }
            return ids.maxOrNull() ?: 0
        }

        override suspend fun saveChat(chat: Chat) {
            log += "chat ${chat.id} ${chat.title} pinned=${chat.pinned}"
            chats[chat.id] = chat.copy(messages = chats[chat.id]?.messages.orEmpty())
        }

        override suspend fun saveMessage(
            chatId: Long,
            message: UiMessage,
            updatedAt: Long,
        ) {
            log += "message ${message.id} ${message.text}"
        }

        override suspend fun deleteMessage(id: Long) {
            log += "delete message $id"
        }

        override suspend fun deleteChat(id: Long) {
            log += "delete chat $id"
            chats.remove(id)
        }

        override suspend fun search(query: String): Set<Long> = if (query == "fts") setOf(SAVED_ID) else emptySet()
    }

    private fun TestScope.store(persistence: ChatPersistence) = ChatStore(persistence, backgroundScope) { 5L }

    @Test
    fun restoresSavedChatsAndKeepsIdsUnique() =
        runTest {
            val saved = Chat(SAVED_ID, "Alt", listOf(UiMessage(SAVED_ID + 1, true, "hallo")), updatedAt = 1)
            val store = store(FakePersistence(listOf(saved)))
            runCurrent()
            assertTrue(store.loaded.value)
            assertEquals(listOf("Alt"), store.chats.value.map { it.title })
            assertTrue(store.nextId() > SAVED_ID + 1)
        }

    @Test
    fun writesThroughInOrderButNotWhileStreaming() =
        runTest {
            val fake = FakePersistence()
            val store = store(fake)
            val chat = store.create("Neu")
            store.upsertMessage(chat.id, UiMessage(100, isUser = true, text = "Frage"))
            store.upsertMessage(chat.id, UiMessage(101, isUser = false, text = "An", streaming = true))
            store.upsertMessage(chat.id, UiMessage(101, isUser = false, text = "Antwort"))
            store.rename(chat.id, "  Umbenannt  ")
            store.setPinned(chat.id, true)
            store.removeMessage(chat.id, 101)
            store.delete(chat.id)
            runCurrent()
            assertEquals(
                listOf(
                    "chat ${chat.id} Neu pinned=false",
                    "message 100 Frage",
                    "message 101 Antwort",
                    "chat ${chat.id} Umbenannt pinned=false",
                    "chat ${chat.id} Umbenannt pinned=true",
                    "delete message 101",
                    "delete chat ${chat.id}",
                ),
                fake.log,
            )
        }

    @Test
    fun searchCombinesTextAndFullTextHits() =
        runTest {
            val saved = Chat(SAVED_ID, "Suppe", listOf(UiMessage(SAVED_ID + 1, true, "Rezept")), updatedAt = 1)
            val store = store(FakePersistence(listOf(saved)))
            runCurrent()
            assertEquals(setOf(SAVED_ID), store.search("supp"))
            assertEquals(setOf(SAVED_ID), store.search("rezept"))
            assertEquals(setOf(SAVED_ID), store.search("fts"))
            assertEquals(emptySet<Long>(), store.search("auto"))
            assertEquals(null, store.search("  "))
        }

    private companion object {
        const val SAVED_ID = 10L
    }
}
