package app.hain.feature.chat.internal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.hain.llm.api.ChatMessage
import app.hain.llm.api.EngineState
import app.hain.llm.api.LlmEngine
import app.hain.llm.api.Role
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
    )

    val state: StateFlow<ChatUiState> =
        combine(store.chats, local, llm.state, installed, networkActive) { chats, l, engineState, hasBrain, online ->
            val visible = chats.filter { it.messages.isNotEmpty() }
            val hits = l.searchHits
            ChatUiState(
                chats = visible,
                groups = groupChats(if (hits == null) visible else visible.filter { it.id in hits }, clock()),
                searchQuery = l.searchQuery,
                currentChatId = l.currentChatId,
                messages = chats.firstOrNull { it.id == l.currentChatId }?.messages.orEmpty(),
                input = l.input,
                isGenerating = l.generating,
                engine = engineState,
                engineAvailable = llm.isAvailable,
                error = l.error,
                brainInstalled = hasBrain,
                editingId = l.editingId,
                online = online,
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
                    if (llm.load(modelPath())) {
                        engine.warmUp()
                    } else {
                        local.update { it.copy(error = ChatError.MODEL_LOAD) }
                    }
                }
            }
        }
    }

    fun onInputChange(text: String) = local.update { it.copy(input = text, error = null) }

    fun newChat() {
        stop()
        local.update { it.copy(currentChatId = null, input = "", error = null, editingId = null) }
    }

    fun openChat(id: Long) {
        stop()
        local.update { it.copy(currentChatId = id, error = null, editingId = null) }
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

    @Suppress("ReturnCount") // early exits keep the three cases readable
    fun send() {
        val text = local.value.input.trim()
        if (text.isEmpty() || local.value.generating) return
        if (!installed.value) {
            local.update { it.copy(error = ChatError.NO_BRAIN) }
            return
        }
        cancelTitle()
        val editing = local.value.editingId
        val current = local.value.currentChatId
        if (editing != null && current != null && store.chat(current)?.messages?.any { it.id == editing } == true) {
            store.truncateFrom(current, editing, keep = true)
            store.upsertMessage(current, UiMessage(editing, isUser = true, text = text))
            local.update { it.copy(input = "", error = null, editingId = null) }
            answer(current)
            return
        }
        val chatId = current ?: store.create(store.titleFrom(text)).id
        store.upsertMessage(chatId, UiMessage(store.nextId(), isUser = true, text = text))
        local.update { it.copy(currentChatId = chatId, input = "", error = null, editingId = null) }
        answer(chatId)
    }

    /** Drops the answer [answerId] (default: the last one) and everything after it, then asks again. */
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
        if (!target.isUser) store.truncateFrom(chatId, target.id, keep = false)
        answer(chatId)
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
        val shown = live
        live = null
        generation++
        llm.cancel()
        running.cancel()
        shown?.let { (chatId, message) ->
            store.upsertMessage(chatId, message.copy(streaming = false, stopped = true))
        }
        local.update { it.copy(generating = false) }
    }

    private fun answer(chatId: Long) {
        val history =
            store
                .chat(chatId)
                ?.messages
                .orEmpty()
                .toPromptHistory()
        val answerId = store.nextId()
        val placeholder = UiMessage(answerId, isUser = false, text = "", streaming = true)
        store.upsertMessage(chatId, placeholder)
        live = chatId to placeholder
        local.update { it.copy(generating = true) }
        val turn = ++generation
        job =
            viewModelScope.launch {
                var lastShown = 0L
                try {
                    if (!llm.load(modelPath())) {
                        store.removeMessage(chatId, answerId)
                        local.update { it.copy(error = ChatError.MODEL_LOAD) }
                        return@launch
                    }
                    engine.respond(history).collect { update ->
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
                    }
                }
                if (generation == turn) maybeTitle(chatId)
            }
    }

    /**
     * Auto title (2d-2) after the first answer, in the background and without thinking. Only while
     * the chat still has its first-words title: a title the user typed is never replaced.
     */
    @Suppress("ReturnCount")
    private fun maybeTitle(chatId: Long) {
        val chat = store.chat(chatId) ?: return
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
            ),
        )
        when (update.reason) {
            StopReason.CONTEXT_FULL -> local.update { it.copy(error = ChatError.CONTEXT_FULL) }
            StopReason.ERROR -> local.update { it.copy(error = ChatError.GENERATION) }
            else -> Unit
        }
    }

    override fun onCleared() {
        stop()
        cancelTitle()
    }

    private companion object {
        const val STOP_TIMEOUT = 5_000L
        const val STREAM_THROTTLE_MS = 48L
        const val MAX_TITLE_ANSWERS = 3
        const val SEARCH_DEBOUNCE = 150L
        const val MS_PER_S = 1000L
        const val MS_ROUND = 500L
    }
}

private fun List<UiMessage>.toPromptHistory(): List<ChatMessage> =
    filter { it.isUser || (!it.failed && it.text.isNotBlank()) }
        .takeLast(MAX_HISTORY_MESSAGES)
        .map { ChatMessage(if (it.isUser) Role.USER else Role.ASSISTANT, it.text) }

private const val MAX_HISTORY_MESSAGES = 24
