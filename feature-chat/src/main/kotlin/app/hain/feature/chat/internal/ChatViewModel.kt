package app.hain.feature.chat.internal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.hain.llm.api.ChatMessage
import app.hain.llm.api.EngineState
import app.hain.llm.api.LlmEngine
import app.hain.llm.api.Role
import app.hain.llm.api.StopReason
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ChatUiState(
    val chats: List<Chat> = emptyList(),
    val currentChatId: Long? = null,
    val messages: List<UiMessage> = emptyList(),
    val input: String = "",
    val isGenerating: Boolean = false,
    val engine: EngineState = EngineState.Unloaded,
    val engineAvailable: Boolean = true,
    val error: ChatError? = null,
)

enum class ChatError { MODEL_LOAD, CONTEXT_FULL, GENERATION }

/** Chat screen state + actions (T-051). */
class ChatViewModel(
    private val llm: LlmEngine,
    private val engine: ChatEngine,
    private val store: ChatStore,
    private val modelPath: () -> String,
) : ViewModel() {
    private val local = MutableStateFlow(LocalState())
    private var job: Job? = null

    private data class LocalState(
        val currentChatId: Long? = null,
        val input: String = "",
        val generating: Boolean = false,
        val error: ChatError? = null,
    )

    val state: StateFlow<ChatUiState> =
        combine(store.chats, local, llm.state) { chats, l, engineState ->
            ChatUiState(
                chats = chats.filter { it.messages.isNotEmpty() },
                currentChatId = l.currentChatId,
                messages = chats.firstOrNull { it.id == l.currentChatId }?.messages.orEmpty(),
                input = l.input,
                isGenerating = l.generating,
                engine = engineState,
                engineAvailable = llm.isAvailable,
                error = l.error,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT), ChatUiState())

    init {
        // Load the brain right away so the first answer starts fast.
        viewModelScope.launch {
            if (!llm.load(modelPath())) local.update { it.copy(error = ChatError.MODEL_LOAD) }
        }
    }

    fun onInputChange(text: String) = local.update { it.copy(input = text, error = null) }

    fun newChat() {
        stop()
        local.update { it.copy(currentChatId = null, input = "", error = null) }
    }

    fun openChat(id: Long) {
        stop()
        local.update { it.copy(currentChatId = id, error = null) }
    }

    fun deleteChat(id: Long) {
        if (local.value.currentChatId == id) newChat()
        store.delete(id)
    }

    fun send() {
        val text = local.value.input.trim()
        if (text.isEmpty() || local.value.generating) return
        val chatId = local.value.currentChatId ?: store.create(store.titleFrom(text)).id
        store.upsertMessage(chatId, UiMessage(store.nextId(), isUser = true, text = text))
        local.update { it.copy(currentChatId = chatId, input = "", error = null) }
        answer(chatId)
    }

    /** Drops the last answer and asks again. */
    fun regenerate() {
        val chatId = local.value.currentChatId
        val last = chatId?.let { store.chat(it)?.messages?.lastOrNull() }
        if (chatId != null && last != null && !local.value.generating) {
            if (!last.isUser) store.removeMessage(chatId, last.id)
            answer(chatId)
        }
    }

    fun stop() {
        if (job?.isActive == true) llm.cancel()
    }

    private fun answer(chatId: Long) {
        val history =
            store
                .chat(chatId)
                ?.messages
                .orEmpty()
                .toPromptHistory()
        val answerId = store.nextId()
        store.upsertMessage(chatId, UiMessage(answerId, isUser = false, text = "", streaming = true))
        local.update { it.copy(generating = true) }
        job =
            viewModelScope.launch {
                try {
                    if (!llm.load(modelPath())) {
                        store.removeMessage(chatId, answerId)
                        local.update { it.copy(error = ChatError.MODEL_LOAD) }
                        return@launch
                    }
                    engine.respond(history).collect { update ->
                        when (update) {
                            is TurnUpdate.Streaming ->
                                store.upsertMessage(
                                    chatId,
                                    UiMessage(
                                        answerId,
                                        isUser = false,
                                        text = update.text,
                                        reasoning = update.reasoning.ifBlank { null },
                                        streaming = true,
                                    ),
                                )
                            is TurnUpdate.Finished -> finish(chatId, answerId, update)
                        }
                    }
                } finally {
                    local.update { it.copy(generating = false) }
                }
            }
    }

    private fun finish(
        chatId: Long,
        answerId: Long,
        update: TurnUpdate.Finished,
    ) {
        val out = update.output
        val text =
            out.text.ifBlank {
                store
                    .chat(chatId)
                    ?.messages
                    ?.lastOrNull()
                    ?.text
                    .orEmpty()
            }
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
    }

    private companion object {
        const val STOP_TIMEOUT = 5_000L
        const val MS_PER_S = 1000L
        const val MS_ROUND = 500L
    }
}

private fun List<UiMessage>.toPromptHistory(): List<ChatMessage> =
    filter { it.isUser || (!it.failed && it.text.isNotBlank()) }
        .takeLast(MAX_HISTORY_MESSAGES)
        .map { ChatMessage(if (it.isUser) Role.USER else Role.ASSISTANT, it.text) }

private const val MAX_HISTORY_MESSAGES = 24
