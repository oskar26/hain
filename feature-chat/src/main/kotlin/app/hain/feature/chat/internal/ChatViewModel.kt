package app.hain.feature.chat.internal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.hain.docs.api.DocAttachment
import app.hain.docs.api.DocPrompt
import app.hain.llm.api.CONTEXT_SIZE
import app.hain.llm.api.ChatMessage
import app.hain.llm.api.EngineState
import app.hain.llm.api.LlmEngine
import app.hain.llm.api.StopReason
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ChatUiState(
    val chats: List<Chat> = emptyList(),
    /** [chats] after the drawer search, in drawer sections. */
    val groups: List<Pair<ChatGroup, List<Chat>>> = emptyList(),
    val searchQuery: String = "",
    val currentChatId: Long? = null,
    val messages: List<UiMessage> = emptyList(),
    val input: String = "",
    val isGenerating: Boolean = false,
    val engine: EngineState = EngineState.Unloaded,
    val engineAvailable: Boolean = true,
    val error: ChatError? = null,
    /** `false` while the brain is not downloaded yet (onboarding skipped). */
    val brainInstalled: Boolean = true,
    /** Id of the user message being edited; [input] holds its text. */
    val editingId: Long? = null,
    /** `true` while a request is on its way to the internet (orange dot, T-069). */
    val online: Boolean = false,
    /** Private chat (T-056): nothing is saved, the chat is gone once you leave it. */
    val privateChat: Boolean = false,
    /** "‹ 2/3 ›" per shown message that has other versions (T-057). */
    val branches: Map<Long, BranchInfo> = emptyMap(),
    /** Messages sent while Pim was still answering; they go out one by one afterwards (T-057). */
    val queue: List<QueuedMessage> = emptyList(),
    /** Häppchen 9: files waiting in the composer for the next message. */
    val attachments: List<DocAttachment> = emptyList(),
)

/** A message waiting until Pim's current answer is done (T-057, shown as "wartet"). */
data class QueuedMessage(
    val id: Long,
    val chatId: Long,
    val text: String,
)

enum class ChatError { MODEL_LOAD, CONTEXT_FULL, GENERATION, NO_BRAIN }

/** Chat screen state + actions (T-051). */
@Suppress("TooManyFunctions", "LongParameterList") // one small function per chat action; test hooks
class ChatViewModel(
    private val llm: LlmEngine,
    private val engine: ChatEngine,
    private val store: ChatStore,
    private val modelPath: () -> String,
    private val clock: () -> Long = System::currentTimeMillis,
    brainInstalled: Flow<Boolean> = flowOf(true),
    /** Minimum time between two UI updates while an answer streams (fewer recompositions). */
    private val streamThrottleMs: Long = STREAM_THROTTLE_MS,
    networkActive: Flow<Boolean> = flowOf(false),
    /** Thumbs up/down on answers, kept on the phone (T-057). */
    private val messageRatings: MessageRatings = InMemoryRatings(),
    /** Context size for the next load (developer mode, T-211); a change loads the brain again. */
    private val contextSize: () -> Int = { CONTEXT_SIZE },
    /** 7b: a chat got a finished answer (the memory looks for facts in it once the chat is quiet). */
    private val onAnswered: (chatId: Long) -> Unit = {},
    /** Häppchen 9: the attached documents as prompt blocks (whole or as a search note). */
    private val documentPrompt: suspend (List<DocAttachment>) -> DocPrompt.ForChat = { DocPrompt.ForChat() },
) : ViewModel() {
    private val local = MutableStateFlow(LocalState())
    private var job: Job? = null
    private var titleJob: Job? = null
    private var searchJob: Job? = null
    private var generation = 0

    /** The answer currently streaming: (chat id, message). Lets [stop] switch the UI at once. */
    private var live: Pair<Long, UiMessage>? = null
    private val installed = MutableStateFlow(true)

    private data class LocalState(
        val currentChatId: Long? = null,
        val input: String = "",
        val generating: Boolean = false,
        val error: ChatError? = null,
        val searchQuery: String = "",
        val searchHits: Set<Long>? = null,
        val editingId: Long? = null,
        val privateMode: Boolean = false,
        val queue: List<QueuedMessage> = emptyList(),
        val attachments: List<DocAttachment> = emptyList(),
    )

    val state: StateFlow<ChatUiState> =
        combine(store.chats, local, llm.state, installed, networkActive) { chats, l, engineState, hasBrain, online ->
            val visible = chats.filter { it.messages.isNotEmpty() && !it.isPrivate }
            val hits = l.searchHits
            val current = chats.firstOrNull { it.id == l.currentChatId }
            ChatUiState(
                chats = visible,
                groups = groupChats(if (hits == null) visible else visible.filter { it.id in hits }, clock()),
                searchQuery = l.searchQuery,
                currentChatId = l.currentChatId,
                messages = current?.messages.orEmpty(),
                input = l.input,
                isGenerating = l.generating,
                engine = engineState,
                engineAvailable = llm.isAvailable,
                error = l.error,
                brainInstalled = hasBrain,
                editingId = l.editingId,
                online = online,
                privateChat = l.privateMode,
                branches = current?.let { Branches.info(it.all, it.messages) }.orEmpty(),
                queue = l.queue.filter { it.chatId == l.currentChatId },
                attachments = l.attachments,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT), ChatUiState())

    init {
        // Load the brain in the background as soon as it is there, then read the system prompt
        // ahead of time, so the first answer starts fast.
        viewModelScope.launch {
            brainInstalled.distinctUntilChanged().collect { has ->
                installed.value = has
                if (has) {
                    local.update { if (it.error == ChatError.NO_BRAIN) it.copy(error = null) else it }
                    if (llm.load(modelPath(), contextSize())) {
                        engine.warmUp()
                    } else {
                        local.update { it.copy(error = ChatError.MODEL_LOAD) }
                    }
                }
            }
        }
    }

    /**
     * T-055: another brain was picked in the top bar. A running answer stops, then the new brain
     * loads in the background ([modelPath] already points at it); the top bar shows the progress.
     */
    fun switchBrain() {
        if (local.value.generating) stop()
        if (!installed.value) return
        viewModelScope.launch {
            local.update { if (it.error == ChatError.MODEL_LOAD) it.copy(error = null) else it }
            if (llm.load(modelPath(), contextSize())) {
                engine.warmUp()
            } else {
                local.update { it.copy(error = ChatError.MODEL_LOAD) }
            }
        }
    }

    /** Thumb per answer id; read by the action row under each answer. */
    val ratings: StateFlow<Map<Long, Rating>> = messageRatings.ratings

    /** Thumb tapped under an answer: the same thumb again takes it back. */
    fun rate(
        messageId: Long,
        tap: Rating,
    ) = messageRatings.set(messageId, messageRatings.ratings.value[messageId].toggled(tap))

    /**
     * The user tapped a ConfirmCard (T-068): [event] is what happened (the intent started, the
     * phone had no app for it, or "Abbrechen"). The card keeps its new state, also after a restart.
     */
    @Suppress("ReturnCount")
    fun resolveAction(
        messageId: Long,
        index: Int,
        event: ActionEvent,
    ) {
        val chatId = local.value.currentChatId ?: return
        val message = store.chat(chatId)?.messages?.firstOrNull { it.id == messageId } ?: return
        val card = message.actions.getOrNull(index) ?: return
        val next = card.after(event)
        if (next == card) return
        val actions = message.actions.toMutableList().also { it[index] = next }
        store.upsertMessage(chatId, message.copy(actions = actions))
    }

    /** T-131: saves the checklist ticks of answer [messageId] (`cards_json`, null = as the brain wrote). */
    fun updateCards(
        messageId: Long,
        cardsJson: String?,
    ) {
        val chatId = local.value.currentChatId ?: return
        val message = store.chat(chatId)?.messages?.firstOrNull { it.id == messageId && !it.isUser && !it.streaming }
        if (message != null && message.cardState != cardsJson) {
            store.upsertMessage(chatId, message.copy(cardState = cardsJson))
        }
    }

    /** T-131: a card button `ask:"…"` sends its text as the next message (not while Pim answers). */
    fun ask(text: String) {
        if (text.isBlank() || local.value.generating) return
        onInputChange(text)
        send()
    }

    fun onInputChange(text: String) = local.update { it.copy(input = text, error = null) }

    /** Häppchen 9: a file from "Datei oder Foto" or "Meine Dateien" waits in the composer. */
    fun attach(doc: DocAttachment) =
        local.update { s ->
            if (s.attachments.any { it.id == doc.id } || s.attachments.size >= MAX_ATTACHMENTS) {
                s
            } else {
                s.copy(attachments = s.attachments + doc)
            }
        }

    fun detach(id: Long) = local.update { s -> s.copy(attachments = s.attachments.filterNot { it.id == id }) }

    fun newChat() {
        dropQueue()
        stop()
        leavePrivate()
        local.update {
            it.copy(
                currentChatId = null,
                input = "",
                error = null,
                editingId = null,
                privateMode = false,
                attachments = emptyList(),
            )
        }
    }

    /** T-144: a new chat with [text] in the message box (shared from another app); nothing is sent. */
    fun newChatWith(text: String) {
        newChat()
        local.update { it.copy(input = text) }
    }

    fun openChat(id: Long) {
        if (id != local.value.currentChatId) dropQueue()
        stop()
        if (id != local.value.currentChatId) leavePrivate()
        local.update {
            it.copy(
                currentChatId = id,
                error = null,
                editingId = null,
                privateMode = store.isPrivate(id),
                attachments = emptyList(),
            )
        }
    }

    /**
     * Starts an empty private chat (T-056): it is not saved, not searchable, not read by the memory,
     * and deleted when you leave it. Tools work as usual. Tapping again while private ends it.
     */
    fun togglePrivateChat() {
        if (local.value.privateMode) {
            newChat()
            return
        }
        stop()
        leavePrivate()
        local.update {
            it.copy(currentChatId = null, input = "", error = null, editingId = null, privateMode = true)
        }
    }

    /** Forgets the open chat if it is private (leaving it = deleting it). */
    private fun leavePrivate() {
        val id = local.value.currentChatId ?: return
        if (store.isPrivate(id)) store.discardPrivate(id)
    }

    /** Puts a sent message back into the composer; sending replaces it and answers again from there. */
    fun startEdit(messageId: Long) {
        val chatId = local.value.currentChatId ?: return
        val message = store.chat(chatId)?.messages?.firstOrNull { it.id == messageId && it.isUser } ?: return
        stop()
        local.update { it.copy(editingId = message.id, input = message.text, error = null) }
    }

    fun cancelEdit() = local.update { it.copy(editingId = null, input = "") }

    fun deleteChat(id: Long) {
        if (local.value.currentChatId == id) newChat()
        store.delete(id)
    }

    fun renameChat(
        id: Long,
        title: String,
    ) = store.rename(id, title)

    fun setPinned(
        id: Long,
        pinned: Boolean,
    ) = store.setPinned(id, pinned)

    /** Drawer search: filters the chat list by title, text and the full-text index. */
    fun onSearchChange(query: String) {
        local.update { it.copy(searchQuery = query) }
        searchJob?.cancel()
        searchJob =
            viewModelScope.launch {
                if (query.isNotBlank()) delay(SEARCH_DEBOUNCE)
                val hits = store.search(query)
                local.update { if (it.searchQuery == query) it.copy(searchHits = hits) else it }
            }
    }

    @Suppress("ReturnCount") // early exits keep the cases readable
    fun send() {
        val text = local.value.input.trim()
        val files = local.value.attachments
        if (text.isEmpty() && files.isEmpty()) return
        if (local.value.generating) {
            // T-057: a message sent while Pim answers waits; attached files stay in the box for later
            if (text.isNotEmpty()) enqueue(text)
            return
        }
        if (!installed.value) {
            local.update { it.copy(error = ChatError.NO_BRAIN) }
            return
        }
        cancelTitle()
        val editing = local.value.editingId
        val current = local.value.currentChatId
        // T-057: an edit becomes a new version next to the old one; "‹ 2/2 ›" switches back.
        // Häppchen 9: the new version keeps the files of the old one plus the newly attached ones
        if (editing != null && current != null) {
            val before =
                store
                    .chat(current)
                    ?.all
                    ?.firstOrNull { it.id == editing }
                    ?.attachments
                    .orEmpty()
            val kept = (before + files).distinctBy { it.id }
            val version = UiMessage(store.nextId(), isUser = true, text = text, attachments = kept)
            if (store.branchFrom(current, editing, version)) {
                local.update { it.copy(input = "", error = null, editingId = null, attachments = emptyList()) }
                answer(current)
                return
            }
        }
        val title = store.titleFrom(text.ifEmpty { files.first().name })
        val chatId =
            current ?: if (local.value.privateMode) {
                store.createPrivate(title).id
            } else {
                store.create(title).id
            }
        store.upsertMessage(chatId, UiMessage(store.nextId(), isUser = true, text = text, attachments = files))
        local.update {
            it.copy(currentChatId = chatId, input = "", error = null, editingId = null, attachments = emptyList())
        }
        answer(chatId)
    }

    /** T-057: sent while Pim answers → waits ("wartet") and goes out after the answer. */
    private fun enqueue(text: String) {
        val chatId = local.value.currentChatId ?: return
        if (local.value.editingId != null) return
        local.update {
            it.copy(input = "", error = null, queue = it.queue + QueuedMessage(store.nextId(), chatId, text))
        }
    }

    /** Takes a waiting message back out of the queue (its ✕). */
    fun cancelQueued(id: Long) = local.update { s -> s.copy(queue = s.queue.filterNot { it.id == id }) }

    private fun dropQueue() = local.update { it.copy(queue = emptyList()) }

    /**
     * Sends the next waiting message once Pim is free. After an error nothing is sent on its own:
     * the waiting texts go back into the message box, so nothing is lost.
     */
    @Suppress("ReturnCount")
    private fun sendQueued() {
        val now = local.value
        val next = now.queue.firstOrNull() ?: return
        if (now.generating) return
        if (next.chatId != now.currentChatId) {
            dropQueue()
            return
        }
        if (now.error != null || !installed.value) {
            val back = (listOf(now.input) + now.queue.map { it.text }).filter { it.isNotBlank() }
            local.update { it.copy(input = back.joinToString("\n\n"), queue = emptyList()) }
            return
        }
        local.update { it.copy(queue = it.queue.drop(1)) }
        cancelTitle()
        store.upsertMessage(next.chatId, UiMessage(next.id, isUser = true, text = next.text))
        answer(next.chatId)
    }

    /** T-057: shows another version of a message ("‹ 2/3 ›"); not while Pim answers. */
    fun selectBranch(messageId: Long) {
        val chatId = local.value.currentChatId ?: return
        if (local.value.generating) return
        store.selectBranch(chatId, messageId)
    }

    /**
     * Asks again for the answer [answerId] (default: the last one). The old answer and what followed
     * it stay as another version (T-057); without an answer at the end it simply answers.
     */
    @Suppress("ReturnCount")
    fun regenerate(answerId: Long? = null) {
        val chatId = local.value.currentChatId ?: return
        if (local.value.generating) return
        val messages = store.chat(chatId)?.messages.orEmpty()
        val target = messages.firstOrNull { it.id == answerId && !it.isUser } ?: messages.lastOrNull() ?: return
        if (!installed.value) {
            local.update { it.copy(error = ChatError.NO_BRAIN) }
            return
        }
        cancelTitle()
        if (!target.isUser && target.failed && target.text.isBlank()) {
            // an empty failed answer is not worth keeping as a version
            store.truncateFrom(chatId, target.id, keep = false)
            answer(chatId)
            return
        }
        answer(chatId, replacing = target.id.takeIf { !target.isUser })
    }

    private fun cancelTitle() {
        val running = titleJob ?: return
        if (running.isActive) {
            llm.cancel()
            running.cancel()
        }
        titleJob = null
    }

    /** Stops the answer: the UI switches at once, the brain stops at its next step (even mid-prompt). */
    fun stop() {
        val running = job
        if (running == null || !running.isActive) return
        // T-069: during a research Stop ends only the looking-up; Pim answers with what it found
        if (engine.stopResearch()) return
        val shown = live
        live = null
        generation++
        llm.cancel()
        running.cancel()
        shown?.let { (chatId, message) ->
            store.upsertMessage(chatId, message.copy(streaming = false, stopped = true))
        }
        local.update { it.copy(generating = false) }
        // T-057: Stop ends this answer only; a waiting message goes out once the brain has let go
        if (local.value.queue.isNotEmpty()) {
            viewModelScope.launch {
                running.join()
                sendQueued()
            }
        }
    }

    /** Answers the newest message; with [replacing] the answer becomes a new version of that one. */
    @Suppress("LongMethod") // placeholder, history, stream, finish: one answer
    private fun answer(
        chatId: Long,
        replacing: Long? = null,
    ) {
        val answerId = store.nextId()
        val placeholder = UiMessage(answerId, isUser = false, text = "", streaming = true)
        if (replacing == null || !store.branchFrom(chatId, replacing, placeholder)) {
            store.upsertMessage(chatId, placeholder)
        }
        // what the brain reads: the shown path up to this answer (documents are added in the coroutine)
        val messages =
            store
                .chat(chatId)
                ?.messages
                .orEmpty()
                .takeWhile { it.id != answerId }
        live = chatId to (store.chat(chatId)?.messages?.lastOrNull { it.id == answerId } ?: placeholder)
        local.update { it.copy(generating = true) }
        val turn = ++generation
        job =
            viewModelScope.launch {
                var lastShown = 0L
                try {
                    if (!llm.load(modelPath(), contextSize())) {
                        store.removeMessage(chatId, answerId)
                        local.update { it.copy(error = ChatError.MODEL_LOAD) }
                        return@launch
                    }
                    // Häppchen 9: attached documents go into their message (small) or the search tool (long)
                    val attached = messages.filter { it.isUser }.flatMap { it.attachments }
                    val docs = if (attached.isEmpty()) DocPrompt.ForChat() else documentPrompt(attached)
                    val history = DocMessages.withDocuments(messages, docs.blocks).toPromptHistory()
                    engine.respond(history, documents = docs.search).collect { update ->
                        when (update) {
                            is TurnUpdate.Streaming -> {
                                val message =
                                    UiMessage(
                                        answerId,
                                        isUser = false,
                                        text = update.text,
                                        reasoning = update.reasoning.ifBlank { null },
                                        streaming = true,
                                        steps = update.steps,
                                        research = update.research,
                                    )
                                live = chatId to message
                                val now = clock()
                                if (now - lastShown >= streamThrottleMs) {
                                    lastShown = now
                                    store.upsertMessage(chatId, message)
                                }
                            }
                            is TurnUpdate.Finished -> {
                                live = null
                                finish(chatId, answerId, update)
                            }
                        }
                    }
                } finally {
                    if (generation == turn) {
                        live = null
                        local.update { it.copy(generating = false) }
                        // the brain did not load: waiting messages go back into the box (T-057)
                        if (local.value.error != null) sendQueued()
                    }
                }
                if (generation == turn) {
                    if (local.value.queue.isEmpty()) maybeTitle(chatId) else sendQueued()
                }
            }
    }

    /**
     * Auto title (2d-2) after the first answer, in the background and without thinking. Only while
     * the chat still has its first-words title: a title the user typed is never replaced.
     */
    @Suppress("ReturnCount")
    private fun maybeTitle(chatId: Long) {
        val chat = store.chat(chatId) ?: return
        if (chat.isPrivate) return // T-056: no title run, the chat is never listed anyway
        val firstUser = chat.messages.firstOrNull { it.isUser } ?: return
        val answers = chat.messages.count { !it.isUser && !it.failed && !it.stopped && it.text.isNotBlank() }
        if (answers !in 1..MAX_TITLE_ANSWERS || chat.title != store.titleFrom(firstUser.text)) return
        val history = chat.messages.toPromptHistory()
        titleJob =
            viewModelScope.launch {
                val title = runCatching { engine.title(history) }.getOrNull() ?: return@launch
                val now = store.chat(chatId) ?: return@launch
                if (now.title == store.titleFrom(firstUser.text)) store.rename(chatId, title)
            }
    }

    private fun finish(
        chatId: Long,
        answerId: Long,
        update: TurnUpdate.Finished,
    ) {
        val out = update.output
        val text = out.text
        store.upsertMessage(
            chatId,
            UiMessage(
                id = answerId,
                isUser = false,
                text = text,
                reasoning = out.reasoning?.ifBlank { null },
                thinkingSeconds = update.thinkingMillis?.let { ((it + MS_ROUND) / MS_PER_S).toInt().coerceAtLeast(1) },
                streaming = false,
                stopped = update.reason == StopReason.CANCELLED,
                failed = update.reason == StopReason.ERROR && text.isBlank(),
                steps = update.steps,
                sources = update.sources,
                trace = update.trace,
                actions = update.actions,
                news = update.news,
                stats = update.stats,
                cards = update.cards,
            ),
        )
        when (update.reason) {
            StopReason.CONTEXT_FULL -> local.update { it.copy(error = ChatError.CONTEXT_FULL) }
            StopReason.ERROR -> local.update { it.copy(error = ChatError.GENERATION) }
            StopReason.END, StopReason.MAX_TOKENS -> onAnswered(chatId)
            else -> Unit
        }
    }

    override fun onCleared() {
        dropQueue()
        stop()
        cancelTitle()
        leavePrivate()
    }

    private companion object {
        const val STOP_TIMEOUT = 5_000L
        const val STREAM_THROTTLE_MS = 50L
        const val MAX_TITLE_ANSWERS = 3
        const val SEARCH_DEBOUNCE = 150L
        const val MS_PER_S = 1000L
        const val MS_ROUND = 500L
        const val MAX_ATTACHMENTS = 5
    }
}

private fun List<UiMessage>.toPromptHistory(): List<ChatMessage> = PromptHistory.of(this)
