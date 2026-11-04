package app.hain.feature.chat.internal

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** T-057: versions of messages (edit / ask again) and which one the chat shows. */
@OptIn(ExperimentalCoroutinesApi::class)
class BranchesTest {
    private fun user(
        id: Long,
        parent: Long?,
        selectedAt: Long? = null,
    ) = UiMessage(id, isUser = true, text = "u$id", parentId = parent, selectedAt = selectedAt)

    private fun answer(
        id: Long,
        parent: Long?,
        selectedAt: Long? = null,
    ) = UiMessage(id, isUser = false, text = "a$id", parentId = parent, selectedAt = selectedAt)

    @Test
    fun oldChatsWithoutParentsStayOneLine() {
        val all = listOf(user(3, null), answer(1, null), user(2, null))
        val fixed = Branches.normalize(all)
        assertEquals(listOf(Branches.ROOT, 1L, 2L), fixed.map { it.parentId })
        assertEquals(listOf(1L, 2L, 3L), Branches.path(fixed).map { it.id })
        assertTrue(Branches.info(fixed, Branches.path(fixed)).isEmpty())
    }

    @Test
    fun newestVersionWinsUnlessAnOlderOneWasPicked() {
        val all =
            listOf(
                user(1, Branches.ROOT),
                answer(2, 1),
                answer(3, 1),
                user(4, 3),
                answer(5, 4),
            )
        assertEquals(listOf(1L, 3L, 4L, 5L), Branches.path(all).map { it.id })
        val picked = all.map { if (it.id == 2L) it.copy(selectedAt = 10) else it }
        assertEquals(listOf(1L, 2L), Branches.path(picked).map { it.id })
        // a later pick of the newer one wins again, and its branch comes back
        val back = picked.map { if (it.id == 3L) it.copy(selectedAt = 11) else it }
        assertEquals(listOf(1L, 3L, 4L, 5L), Branches.path(back).map { it.id })
    }

    @Test
    fun infoCountsVersionsPerShownMessage() {
        val all = listOf(user(1, Branches.ROOT), user(6, Branches.ROOT), answer(2, 1), answer(3, 1), answer(7, 6))
        val path = Branches.path(all)
        assertEquals(listOf(6L, 7L), path.map { it.id })
        val info = Branches.info(all, path)
        assertEquals(BranchInfo(2, 2, 1L, null), info[6L])
        assertEquals(null, info[7L])
        val (shown, hidden) = Branches.split(all)
        assertEquals(path, shown)
        assertEquals(listOf(1L, 2L, 3L), hidden.map { it.id })
    }

    @Test
    fun brokenParentsNeverHang() {
        val loop = listOf(user(1, 2), answer(2, 1))
        assertTrue(Branches.path(loop).isEmpty())
        // a lost parent: the message starts the chat
        assertEquals(listOf(5L), Branches.path(listOf(user(5, 99))).map { it.id })
    }

    @Test
    fun pickTimeAlwaysBeatsTheSiblings() {
        val all = listOf(answer(2, 1, selectedAt = 50), answer(3, 1))
        assertEquals(51L, Branches.pickTime(all, 1, now = 7))
        assertEquals(100L, Branches.pickTime(all, 1, now = 100))
    }

    @Test
    fun storeBranchesAndSwitches() {
        var now = 1L
        val store = ChatStore { now++ }
        val chat = store.create("t")
        store.upsertMessage(chat.id, UiMessage(10, true, "Frage"))
        store.upsertMessage(chat.id, UiMessage(11, false, "Antwort 1"))
        assertTrue(store.branchFrom(chat.id, 11, UiMessage(12, false, "Antwort 2")))
        assertEquals(listOf("Frage", "Antwort 2"), store.chat(chat.id)!!.messages.map { it.text })
        assertEquals(listOf(11L), store.chat(chat.id)!!.hidden.map { it.id })
        store.selectBranch(chat.id, 11)
        assertEquals(listOf("Frage", "Antwort 1"), store.chat(chat.id)!!.messages.map { it.text })
        // removing a version (a failed load) brings the other one back
        store.selectBranch(chat.id, 12)
        store.removeMessage(chat.id, 12)
        assertEquals(listOf("Frage", "Antwort 1"), store.chat(chat.id)!!.messages.map { it.text })
        // an update keeps the message's place in the tree
        store.upsertMessage(chat.id, UiMessage(11, false, "Antwort 1b"))
        assertEquals(
            10L,
            store
                .chat(chat.id)!!
                .messages
                .last()
                .parentId,
        )
        assertTrue(!store.branchFrom(chat.id, 999, UiMessage(13, true, "x")))
    }

    @Test
    fun savedVersionsComeBackAfterARestart() =
        runTest {
            val saved =
                Chat(
                    1,
                    "Alt",
                    listOf(user(10, null), answer(11, 10), answer(12, 10), answer(13, 10, selectedAt = 5)),
                    updatedAt = 1,
                )
            val persistence = ListPersistence(listOf(saved))
            val store = ChatStore(persistence, backgroundScope) { 9L }
            runCurrent()
            val chat = store.chat(1)!!
            assertEquals(listOf(10L, 13L), chat.messages.map { it.id })
            assertEquals(listOf(11L, 12L), chat.hidden.map { it.id }.sorted())
            store.selectBranch(1, 12)
            runCurrent()
            assertEquals(listOf(12L to 9L), persistence.saved.map { it.id to it.selectedAt })
        }

    private class ListPersistence(
        private val chats: List<Chat>,
    ) : ChatPersistence {
        val saved = mutableListOf<UiMessage>()

        override suspend fun load(): List<Chat> = chats

        override suspend fun maxId(): Long = 0

        override suspend fun saveChat(chat: Chat) = Unit

        override suspend fun saveMessage(
            chatId: Long,
            message: UiMessage,
            updatedAt: Long,
        ) {
            saved += message
        }

        override suspend fun deleteMessage(id: Long) = Unit

        override suspend fun deleteChat(id: Long) = Unit

        override suspend fun search(query: String): Set<Long> = emptySet()
    }
}
