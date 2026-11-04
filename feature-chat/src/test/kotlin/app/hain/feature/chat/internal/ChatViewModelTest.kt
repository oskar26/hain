package app.hain.feature.chat.internal

import app.hain.llm.api.GenerationEvent
import app.hain.llm.api.LlmEngine
import app.hain.llm.api.SamplingParams
import app.hain.llm.api.StopReason
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
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
    fun editingAMessageAnswersAgainFromThereAndKeepsTheOldVersion() {
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
        assertEquals(listOf("eins, aber anders", "A3"), store.only().messages.map { it.text })
        // T-057: the old version is still there, "‹ 2/2 ›" under the edited message
        val edited = store.only().messages.first()
        assertEquals(BranchInfo(2, 2, first.id, null), vm.state.value.branches[edited.id])
        vm.selectBranch(first.id)
        assertEquals(listOf("eins", "A1", "zwei", "A2"), store.only().messages.map { it.text })
        assertEquals(BranchInfo(1, 2, null, edited.id), vm.state.value.branches[first.id])
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
        // the brain did not see the old answer or what followed it
        assertFalse(llm.lastPrompt.contains("A1"))
        assertFalse(llm.lastPrompt.contains("zwei"))
    }

    @Test
    fun askingAgainKeepsEveryAnswerAsAVersion() {
        val llm = FakeLlm(listOf("A1"), listOf("Titel"), listOf("A2"), listOf("A3"))
        val store = ChatStore()
        val vm = viewModel(llm, store)
        vm.say("Frage")
        val a1 = store.only().messages.last()
        vm.regenerate()
        vm.regenerate()
        val a3 = store.only().messages.last()
        assertEquals("A3", a3.text)
        assertEquals(BranchInfo(3, 3, store.only().hidden.maxOf { it.id }, null), vm.state.value.branches[a3.id])
        vm.selectBranch(a1.id)
        assertEquals(listOf("Frage", "A1"), store.only().messages.map { it.text })
        // switching is not possible while Pim answers
        val stuck = FakeLlm(null)
        val other = viewModel(stuck, ChatStore())
        other.say("x")
        other.selectBranch(1L)
        assertTrue(other.state.value.isGenerating)
    }

    @Test
    fun messageSentWhileAnsweringWaitsAndGoesOutAfterwards() {
        val gate = CompletableDeferred<Unit>()
        val llm = GatedLlm(gate, FakeLlm(listOf("A2"), listOf("Titel")))
        val store = ChatStore()
        val vm =
            ChatViewModel(llm, ChatEngine(llm, { "S" }), store, { "b.gguf" }, streamThrottleMs = 0)
        watchers += CoroutineScope(Dispatchers.Main).launch { vm.state.collect { } }
        vm.say("erste")
        assertTrue(vm.state.value.isGenerating)
        vm.say("zweite")
        assertEquals(
            listOf("zweite"),
            vm.state.value.queue
                .map { it.text },
        )
        assertEquals("", vm.state.value.input)
        // not part of the chat yet, only waiting
        assertEquals(
            listOf("erste"),
            store
                .only()
                .messages
                .filter { it.isUser }
                .map { it.text },
        )
        gate.complete(Unit)
        assertTrue(
            vm.state.value.queue
                .isEmpty(),
        )
        assertEquals(listOf("erste", "A1", "zweite", "A2"), store.only().messages.map { it.text })
    }

    @Test
    fun waitingMessageCanBeTakenBackAndStopStillSendsTheRest() {
        val llm = FakeLlm(null, listOf("A2"))
        val store = ChatStore()
        val vm = viewModel(llm, store)
        vm.say("erste")
        vm.say("zweite")
        vm.say("dritte")
        val queued = vm.state.value.queue
        assertEquals(listOf("zweite", "dritte"), queued.map { it.text })
        vm.cancelQueued(queued[0].id)
        assertEquals(
            listOf("dritte"),
            vm.state.value.queue
                .map { it.text },
        )
        vm.stop()
        assertTrue(
            vm.state.value.queue
                .isEmpty(),
        )
        assertEquals(
            listOf("erste", "dritte"),
            store
                .only()
                .messages
                .filter { it.isUser }
                .map { it.text },
        )
        assertEquals(
            "A2",
            store
                .only()
                .messages
                .last()
                .text,
        )
    }

    @Test
    fun sharedTextOpensANewChatWithoutSending() {
        val llm = FakeLlm(listOf("A1"))
        val store = ChatStore()
        val vm = viewModel(llm, store)
        vm.say("alt")
        vm.newChatWith("https://example.org/artikel")
        assertEquals(null, vm.state.value.currentChatId)
        assertEquals("https://example.org/artikel", vm.state.value.input)
        assertEquals(1, store.chats.value.size)
    }

    @Test
    fun newChatDropsTheQueue() {
        val llm = FakeLlm(null)
        val store = ChatStore()
        val vm = viewModel(llm, store)
        vm.say("erste")
        vm.say("zweite")
        vm.newChat()
        assertTrue(
            vm.state.value.queue
                .isEmpty(),
        )
        assertEquals(1, store.only().messages.count { it.isUser })
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

    @Test
    fun thumbsToggleAndStayPerAnswer() {
        val vm = viewModel(FakeLlm(listOf("A1")))
        vm.rate(5L, Rating.UP)
        assertEquals(Rating.UP, vm.ratings.value[5L])
        vm.rate(5L, Rating.DOWN)
        assertEquals(Rating.DOWN, vm.ratings.value[5L])
        vm.rate(5L, Rating.DOWN)
        assertEquals(null, vm.ratings.value[5L])
    }

    @Test
    fun confirmCardTapIsKeptOnTheMessage() {
        val store = ChatStore()
        val chat = store.create("Wecker")
        val alarm =
            app.hain.tools.api.PhoneAction
                .Alarm(6, 30)
        store.upsertMessage(chat.id, UiMessage(1, isUser = true, text = "Wecker 6:30"))
        val answer = UiMessage(2, isUser = false, text = "Tipp auf Stellen.", actions = listOf(ActionCard(alarm)))
        store.upsertMessage(chat.id, answer)
        val vm = viewModel(FakeLlm(), store)
        vm.openChat(chat.id)
        vm.resolveAction(2, 0, ActionEvent.CONFIRMED)
        assertEquals(
            ActionState.DONE,
            store
                .chat(chat.id)!!
                .messages
                .last()
                .actions
                .single()
                .state,
        )
        // one tap, one action
        vm.resolveAction(2, 0, ActionEvent.CANCELLED)
        assertEquals(
            ActionState.DONE,
            store
                .chat(chat.id)!!
                .messages
                .last()
                .actions
                .single()
                .state,
        )
    }
}

/** The first answer ("A1") waits for [gate]; everything after comes from [rest]. */
private class GatedLlm(
    private val gate: CompletableDeferred<Unit>,
    private val rest: FakeLlm,
) : LlmEngine by rest {
    private var first = true

    override fun generate(
        prompt: String,
        params: SamplingParams,
        checkpoints: List<Int>,
    ): Flow<GenerationEvent> {
        if (!first) return rest.generate(prompt, params, checkpoints)
        first = false
        return flow {
            emit(GenerationEvent.Token("A1"))
            gate.await()
            emit(GenerationEvent.Done(StopReason.END))
        }
    }
}
