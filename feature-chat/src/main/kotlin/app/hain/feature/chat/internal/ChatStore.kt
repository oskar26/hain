package app.hain.feature.chat.internal

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.concurrent.atomic.AtomicLong

/** A message as the chat UI shows it. */
data class UiMessage(
    val id: Long,
    val isUser: Boolean,
    val text: String,
    val reasoning: String? = null,
    val thinkingSeconds: Int? = null,
    val streaming: Boolean = false,
    val stopped: Boolean = false,
    val failed: Boolean = false,
)

data class Chat(
    val id: Long,
    val title: String,
    val messages: List<UiMessage>,
    val updatedAt: Long,
)

/**
 * Chats of this app session. In memory for the MVP; the encrypted Room database (T-020) replaces it
 * behind the same functions.
 */
class ChatStore(
    private val clock: () -> Long = System::currentTimeMillis,
) {
    private val ids = AtomicLong(1)
    private val mutableChats = MutableStateFlow<List<Chat>>(emptyList())
    val chats: StateFlow<List<Chat>> = mutableChats.asStateFlow()

    fun nextId(): Long = ids.getAndIncrement()

    fun chat(id: Long?): Chat? = mutableChats.value.firstOrNull { it.id == id }

    fun create(title: String): Chat {
        val chat = Chat(nextId(), title, emptyList(), clock())
        mutableChats.update { listOf(chat) + it }
        return chat
    }

    fun upsertMessage(
        chatId: Long,
        message: UiMessage,
    ) {
        mutableChats.update { list ->
            list
                .map { chat ->
                    if (chat.id != chatId) {
                        chat
                    } else {
                        val exists = chat.messages.any { it.id == message.id }
                        val messages =
                            if (exists) {
                                chat.messages.map { if (it.id == message.id) message else it }
                            } else {
                                chat.messages + message
                            }
                        chat.copy(messages = messages, updatedAt = clock())
                    }
                }.sortedByDescending { it.updatedAt }
        }
    }

    fun removeMessage(
        chatId: Long,
        messageId: Long,
    ) {
        mutableChats.update { list ->
            list.map { c -> if (c.id == chatId) c.copy(messages = c.messages.filterNot { it.id == messageId }) else c }
        }
    }

    fun delete(chatId: Long) {
        mutableChats.update { list -> list.filterNot { it.id == chatId } }
    }

    /** First words of the first user message, at most [MAX_TITLE] characters. */
    fun titleFrom(text: String): String {
        val clean = text.trim().replace(Regex("\\s+"), " ")
        return if (clean.length <= MAX_TITLE) clean else clean.take(MAX_TITLE).substringBeforeLast(' ') + "…"
    }

    private companion object {
        const val MAX_TITLE = 40
    }
}
