package app.hain.feature.chat.internal

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** T-056: a private chat never reaches the database, the search or the drawer, and is gone on leave. */
@OptIn(ExperimentalCoroutinesApi::class)
class PrivateChatTest {
    /** Records every database call; a private chat must leave it empty. */
    private class RecordingPersistence : ChatPersistence {
        val calls = mutableListOf<String>()

        override suspend fun load(): List<Chat> = emptyList()

        override suspend fun maxId(): Long = 0

        override suspend fun saveChat(chat: Chat) {
            calls += "saveChat ${chat.id}"
        }

        override suspend fun saveMessage(
            chatId: Long,
            message: UiMessage,
            updatedAt: Long,
        ) {
            calls += "saveMessage $chatId ${message.text}"
        }

        override suspend fun deleteMessage(id: Long) {
            calls += "deleteMessage $id"
        }

        override suspend fun deleteChat(id: Long) {
            calls += "deleteChat $id"
        }

        override suspend fun search(query: String): Set<Long> {
            calls += "search $query"
            return emptySet()
        }
    }

    private val watchers = mutableListOf<Job>()

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        watchers.forEach { it.cancel() }
        Dispatchers.resetMain()
    }

    private fun viewModel(
        llm: FakeLlm,
        store: ChatStore,
    ): ChatViewModel {
        val vm =
            ChatViewModel(
                llm = llm,
                engine = ChatEngine(llm, { "S" }),
                store = store,
                modelPath = { "brain.gguf" },
                brainInstalled = flowOf(true),
                streamThrottleMs = 0,
            )
        watchers += CoroutineScope(Dispatchers.Main).launch { vm.state.collect { } }
        return vm
    }

    private fun ChatViewModel.say(text: String) {
        onInputChange(text)
        send()
    }

    @Test
    fun privateChatWritesNothingToTheDatabase() {
        runTest {
            val db = RecordingPersistence()
            val store = ChatStore(db, backgroundScope) { 5L }
            runCurrent()
            val chat = store.createPrivate("Geheim")
            store.upsertMessage(chat.id, UiMessage(100, isUser = true, text = "Frage"))
            store.upsertMessage(chat.id, UiMessage(101, isUser = false, text = "Antwort"))
            store.rename(chat.id, "Neu")
            store.setPinned(chat.id, true)
            store.truncateFrom(chat.id, 100, keep = true)
            store.removeMessage(chat.id, 100)
            store.delete(chat.id)
            runCurrent()
            assertEquals(emptyList<String>(), db.calls)
        }
    }

    @Test
    fun normalChatStillWritesNextToAPrivateOne() {
        runTest {
            val db = RecordingPersistence()
            val store = ChatStore(db, backgroundScope) { 5L }
            runCurrent()
            val secret = store.createPrivate("Geheim")
            val normal = store.create("Offen")
            store.upsertMessage(secret.id, UiMessage(200, isUser = true, text = "privat"))
            store.upsertMessage(normal.id, UiMessage(201, isUser = true, text = "offen"))
            runCurrent()
            assertEquals(listOf("saveChat ${normal.id}", "saveMessage ${normal.id} offen"), db.calls)
            assertTrue(store.isPrivate(secret.id))
            assertFalse(store.isPrivate(normal.id))
        }
    }

    @Test
    fun searchNeverFindsAPrivateChat() {
        runTest {
            val store = ChatStore()
            val secret = store.createPrivate("Apfel geheim")
            store.upsertMessage(secret.id, UiMessage(300, isUser = true, text = "Apfelkuchen"))
            val normal = store.create("Apfel offen")
            store.upsertMessage(normal.id, UiMessage(301, isUser = true, text = "Apfelsaft"))
            assertEquals(setOf(normal.id), store.search("Apfel"))
        }
    }

    @Test
    fun privateChatIsMarkedNotListedAndUsesTheBrainAsUsual() {
        val llm = FakeLlm(listOf("Antwort"))
        val store = ChatStore()
        val vm = viewModel(llm, store)
        vm.togglePrivateChat()
        assertTrue(vm.state.value.privateChat)
        vm.say("Wie wird das Wetter?")
        val chat = store.chats.value.single()
        assertTrue(chat.isPrivate)
        assertEquals("Antwort", chat.messages.last().text)
        assertEquals(chat.id, vm.state.value.currentChatId)
        assertEquals(2, vm.state.value.messages.size)
        // never in the drawer, never an auto title
        assertTrue(
            vm.state.value.chats
                .isEmpty(),
        )
        assertEquals(1, llm.prompts.size)
    }

    @Test
    fun leavingThePrivateChatDeletesIt() {
        val llm = FakeLlm(listOf("Eins"), listOf("Zwei"))
        val store = ChatStore()
        val vm = viewModel(llm, store)
        vm.togglePrivateChat()
        vm.say("Geheim")
        assertEquals(1, store.chats.value.size)
        vm.newChat()
        assertTrue(store.chats.value.isEmpty())
        assertFalse(vm.state.value.privateChat)
        assertNull(vm.state.value.currentChatId)
    }

    @Test
    fun openingAnotherChatOrTogglingAgainDeletesIt() {
        val llm = FakeLlm(listOf("A"), listOf("B"), listOf("C"))
        val store = ChatStore()
        val vm = viewModel(llm, store)
        vm.say("Offen")
        val normal = store.chats.value.single()
        vm.togglePrivateChat()
        vm.say("Geheim")
        assertEquals(2, store.chats.value.size)
        vm.openChat(normal.id)
        assertEquals(listOf(normal.id), store.chats.value.map { it.id })
        assertFalse(vm.state.value.privateChat)
        vm.togglePrivateChat()
        vm.say("Wieder geheim")
        vm.togglePrivateChat()
        assertEquals(listOf(normal.id), store.chats.value.map { it.id })
    }
}
