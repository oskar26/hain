package app.hain.feature.chat.internal

import app.hain.docs.api.DocAttachment
import app.hain.docs.api.DocKind
import app.hain.docs.api.DocPrompt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** Häppchen 9: files in the message box, on the sent message and in the prompt. */
@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModelDocsTest {
    private val watchers = mutableListOf<Job>()
    private val doc = DocAttachment(4, "Rechnung.pdf", DocKind.PDF, 3, 120)

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
        asked: MutableList<List<DocAttachment>>,
    ): ChatViewModel {
        val vm =
            ChatViewModel(
                llm = llm,
                engine = ChatEngine(llm, { "S" }),
                store = store,
                modelPath = { "brain.gguf" },
                streamThrottleMs = 0,
                documentPrompt = { list ->
                    asked += list
                    DocPrompt.ForChat(mapOf(4L to "<document name=\"Rechnung.pdf\">Betrag 249,90 €</document>"))
                },
            )
        watchers += CoroutineScope(Dispatchers.Main).launch { vm.state.collect { } }
        return vm
    }

    @Test
    fun attachedFileGoesWithTheMessageAndIntoThePrompt() {
        val llm = FakeLlm(listOf("Es sind 249,90 €."), listOf("Rechnung"))
        val store = ChatStore()
        val asked = mutableListOf<List<DocAttachment>>()
        val vm = viewModel(llm, store, asked)
        vm.attach(doc)
        vm.attach(doc)
        assertEquals(listOf(doc), vm.state.value.attachments)
        vm.onInputChange("Wie viel muss ich zahlen?")
        vm.send()
        val sent =
            store.chats.value
                .single()
                .messages
                .first()
        assertEquals(listOf(doc), sent.attachments)
        assertEquals("Wie viel muss ich zahlen?", sent.text)
        assertTrue(
            vm.state.value.attachments
                .isEmpty(),
        )
        assertEquals(listOf(listOf(doc)), asked)
        assertTrue("Betrag 249,90 €" in llm.prompts.first())
    }

    @Test
    fun fileCanBeTakenOffAgain() {
        val vm = viewModel(FakeLlm(), ChatStore(), mutableListOf())
        vm.attach(doc)
        vm.detach(doc.id)
        assertTrue(
            vm.state.value.attachments
                .isEmpty(),
        )
    }

    @Test
    fun noFilesNoDocumentWork() {
        val asked = mutableListOf<List<DocAttachment>>()
        val vm = viewModel(FakeLlm(listOf("Hallo"), listOf("T")), ChatStore(), asked)
        vm.onInputChange("Hallo")
        vm.send()
        assertTrue(asked.isEmpty())
    }
}
