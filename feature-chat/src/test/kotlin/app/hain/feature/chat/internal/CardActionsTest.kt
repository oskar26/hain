package app.hain.feature.chat.internal

import app.hain.cards.api.CardState
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
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

/** T-131: checklist ticks on a card are saved with the answer, card buttons ask the next question. */
@OptIn(ExperimentalCoroutinesApi::class)
class CardActionsTest {
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

    private val card = "Dein Plan:\n```card\nChecklist\n- Vokabeln\n- Referat [x]\n```"

    @Test
    fun ticksAreSavedOnTheAnswer() {
        val store = ChatStore()
        val vm = viewModel(FakeLlm(listOf(card), listOf("Titel")), store)
        vm.onInputChange("Lernplan")
        vm.send()
        val answer =
            store.chats.value
                .single()
                .messages
                .last()
        val json = CardState().toggled(0, 0, 0, default = false).encode()
        vm.updateCards(answer.id, json)
        assertEquals(
            json,
            store.chats.value
                .single()
                .messages
                .last()
                .cardState,
        )
        vm.updateCards(answer.id, null)
        assertNull(
            store.chats.value
                .single()
                .messages
                .last()
                .cardState,
        )
    }

    @Test
    fun askSendsTheButtonTextAsNextMessage() {
        val store = ChatStore()
        val vm = viewModel(FakeLlm(listOf(card), listOf("Titel"), listOf("Klar!")), store)
        vm.onInputChange("Lernplan")
        vm.send()
        vm.ask("Und morgen?")
        val messages =
            store.chats.value
                .single()
                .messages
        assertEquals("Und morgen?", messages.first { it.isUser && it.text != "Lernplan" }.text)
    }

    @Test
    fun cardStateGoesIntoCardsJson() {
        val message = UiMessage(id = 7, isUser = false, text = card, cardState = "{\"checked\":{\"0.0.0\":true}}")
        val entity = message.toEntity(chatId = 1, createdAt = 0)
        assertEquals(message.cardState, entity.cardsJson)
        assertEquals(message.cardState, entity.toUi().cardState)
    }
}
