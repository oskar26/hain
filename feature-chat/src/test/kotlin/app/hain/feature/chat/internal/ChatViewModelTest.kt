package app.hain.feature.chat.internal

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModelTest {
    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    private val watchers = mutableListOf<Job>()

    @After
    fun tearDown() {
        watchers.forEach { it.cancel() }
        Dispatchers.resetMain()
    }

    private fun viewModel(
        llm: FakeLlm,
        store: ChatStore = ChatStore(),
        brain: Boolean = true,
    ): ChatViewModel {
        val vm =
            ChatViewModel(
                llm = llm,
                engine = ChatEngine(llm, { "S" }),
                store = store,
                modelPath = { "brain.gguf" },
                brainInstalled = flowOf(brain),
                streamThrottleMs = 0,
            )
        // state is shared while subscribed, like the screen does
        watchers += CoroutineScope(Dispatchers.Main).launch { vm.state.collect { } }
        return vm
    }

    private fun ChatViewModel.say(text: String) {
        onInputChange(text)
        send()
    }

    private fun ChatStore.only(): Chat = chats.value.single()

    @Test
    fun loadsAndWarmsUpTheBrainAtStart() {
        val llm = FakeLlm()
        viewModel(llm)
        assertEquals(1, llm.loads)
        assertEquals(1, llm.prefills.size)
    }

    @Test
    fun stopSwitchesTheAnswerAtOnce() {
        val llm = FakeLlm(null)
        val store = ChatStore()
        val vm = viewModel(llm, store)
        vm.say("Warum ist der Himmel blau?")
        val streaming = store.only().messages.last()
        assertTrue(streaming.streaming)
        vm.stop()
        val stopped = store.only().messages.last()
        assertEquals(1, llm.cancels)
        assertFalse(stopped.streaming)
        assertTrue(stopped.stopped)
        // what was shown stays (as answer or, while thinking, in the thinking block)
        assertEquals("Ich denke", stopped.text + stopped.reasoning.orEmpty())
    }

    @Test
    fun firstAnswerGetsAnAutoTitle() {
        val llm = FakeLlm(listOf("Antwort"), listOf("Himmel und Licht"))
        val store = ChatStore()
        viewModel(llm, store).say("Warum ist der Himmel blau?")
        assertEquals("Himmel und Licht", store.only().title)
    }

    @Test
    fun titleTheUserTypedIsNeverReplaced() {
        val llm = FakeLlm(listOf("A1"), listOf(""), listOf("A2"), listOf("Auto"))
        val store = ChatStore()
        val vm = viewModel(llm, store)
        vm.say("erste Frage")
        assertEquals("erste Frage", store.only().title)
        vm.renameChat(store.only().id, "Mein Titel")
        vm.say("zweite Frage")
        assertEquals("Mein Titel", store.only().title)
        assertEquals(3, llm.prompts.size)
    }

    @Test
    fun editingAMessageAnswersAgainFromThere() {
        val llm = FakeLlm(listOf("A1"), listOf("Titel"), listOf("A2"), listOf("A3"))
        val store = ChatStore()
        val vm = viewModel(llm, store)
        vm.say("eins")
        vm.say("zwei")
        val first = store.only().messages.first()
        vm.startEdit(first.id)
        assertEquals(first.id, vm.state.value.editingId)
        assertEquals("eins", vm.state.value.input)
        vm.say("eins, aber anders")
        val texts = store.only().messages.map { it.text }
        assertEquals(listOf("eins, aber anders", "A3"), texts)
        assertEquals(
            first.id,
            store
                .only()
                .messages
                .first()
                .id,
        )
    }

    @Test
    fun regenerateWorksOnAnOlderAnswer() {
        val llm = FakeLlm(listOf("A1"), listOf("Titel"), listOf("A2"), listOf("neu"))
        val store = ChatStore()
        val vm = viewModel(llm, store)
        vm.say("eins")
        vm.say("zwei")
        val firstAnswer = store.only().messages[1]
        vm.openChat(store.only().id)
        vm.regenerate(firstAnswer.id)
        assertEquals(listOf("eins", "neu"), store.only().messages.map { it.text })
    }

    @Test
    fun withoutBrainSendingShowsAHint() {
        val llm = FakeLlm()
        val store = ChatStore()
        val vm = viewModel(llm, store, brain = false)
        vm.say("Hallo")
        assertEquals(0, llm.loads)
        assertTrue(store.chats.value.isEmpty())
        assertEquals(ChatError.NO_BRAIN, vm.state.value.error)
        assertFalse(vm.state.value.brainInstalled)
        assertEquals("Hallo", vm.state.value.input)
        assertEquals(0, llm.prompts.size)
    }
}
