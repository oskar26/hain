package app.hain.feature.chat.internal

import app.hain.llm.api.GenerationStats
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
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
    /** Tools this answer used, in order (T-059). */
    val steps: List<ToolStep> = emptyList(),
    /** Pages the answer is based on, shown as chips. */
    val sources: List<SourceRef> = emptyList(),
    /** Tool calls and short results, for the brain's view of later turns (T-062 fix). */
    val trace: List<ToolTrace> = emptyList(),
    /** Phone actions waiting for (or done after) the user's tap on their card (T-068). */
    val actions: List<ActionCard> = emptyList(),
    /** Research clock while a thorough answer is still looking things up (T-069; not stored). */
    val research: ResearchClock? = null,
    /** Speed of this answer for developer mode (T-211; not stored). */
    val stats: GenerationStats? = null,
)

data class Chat(
    val id: Long,
    val title: String,
    val messages: List<UiMessage>,
    val updatedAt: Long,
    val pinned: Boolean = false,
    val createdAt: Long = updatedAt,
    /**
     * Private chat (T-056): lives only in memory, never written to the database, never found by the
     * search or read by the memory, and gone once you leave it.
     */
    val isPrivate: Boolean = false,
)

/**
 * All chats, held in memory for the UI and written through to [persistence] (the encrypted Room
 * database, T-020) in order on [scope]. Streaming answers stay in memory until they are finished.
 */
@Suppress("TooManyFunctions") // one small function per chat action
class ChatStore(
    private val persistence: ChatPersistence? = null,
    private val scope: CoroutineScope? = null,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    // Ids start at the clock so they stay unique across app starts even before the database is read.
    private val ids = AtomicLong(maxOf(clock(), 1))
    private val mutableChats = MutableStateFlow<List<Chat>>(emptyList())
    val chats: StateFlow<List<Chat>> = mutableChats.asStateFlow()
    private val mutableLoaded = MutableStateFlow(persistence == null || scope == null)

    /** `true` once the saved chats are in [chats]. */
    val loaded: StateFlow<Boolean> = mutableLoaded.asStateFlow()

    private val writes = Channel<suspend (ChatPersistence) -> Unit>(Channel.UNLIMITED)

    init {
        if (persistence != null && scope != null) {
            scope.launch {
                runCatching { restore(persistence) }
                mutableLoaded.value = true
                for (write in writes) runCatching { write(persistence) }
            }
        }
    }

    private suspend fun restore(persistence: ChatPersistence) {
        val saved = persistence.load()
        val maxId = maxOf(persistence.maxId(), saved.maxOfOrNull { it.id } ?: 0)
        ids.updateAndGet { maxOf(it, maxId + 1) }
        mutableChats.update { current ->
            val fresh = current.map { it.id }.toSet()
            (current + saved.filterNot { it.id in fresh }).sortedByDescending { it.updatedAt }
        }
    }

    private fun write(block: suspend (ChatPersistence) -> Unit) {
        if (persistence != null && scope != null) writes.trySend(block)
    }

    /** Writes [block] unless [chatId] is a private chat (T-056: those never reach the database). */
    private fun writeFor(
        chatId: Long,
        block: suspend (ChatPersistence) -> Unit,
    ) {
        if (!isPrivate(chatId)) write(block)
    }

    /** `true` for a private chat (T-056); the memory and any export must skip it. */
    fun isPrivate(chatId: Long?): Boolean = chat(chatId)?.isPrivate == true

    fun nextId(): Long = ids.getAndIncrement()

    fun chat(id: Long?): Chat? = mutableChats.value.firstOrNull { it.id == id }

    fun create(title: String): Chat {
        val now = clock()
        val chat = Chat(nextId(), title, emptyList(), now, createdAt = now)
        mutableChats.update { listOf(chat) + it }
        write { it.saveChat(chat) }
        return chat
    }

    /** A private chat (T-056): only in memory, see [Chat.isPrivate]. */
    fun createPrivate(title: String): Chat {
        val now = clock()
        val chat = Chat(nextId(), title, emptyList(), now, createdAt = now, isPrivate = true)
        mutableChats.update { listOf(chat) + it }
        return chat
    }

    /** Forgets a private chat completely; does nothing for a normal chat. */
    fun discardPrivate(chatId: Long) {
        mutableChats.update { list -> list.filterNot { it.id == chatId && it.isPrivate } }
    }

    fun upsertMessage(
        chatId: Long,
        message: UiMessage,
    ) {
        val now = clock()
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
                        chat.copy(messages = messages, updatedAt = now)
                    }
                }.sortedByDescending { it.updatedAt }
        }
        if (!message.streaming) writeFor(chatId) { it.saveMessage(chatId, message, now) }
    }

    fun removeMessage(
        chatId: Long,
        messageId: Long,
    ) {
        mutableChats.update { list ->
            list.map { c -> if (c.id == chatId) c.copy(messages = c.messages.filterNot { it.id == messageId }) else c }
        }
        writeFor(chatId) { it.deleteMessage(messageId) }
    }

    /**
     * Removes every message after [messageId] (and [messageId] itself unless [keep]). Used to edit a
     * sent message or to ask again from an older answer.
     */
    fun truncateFrom(
        chatId: Long,
        messageId: Long,
        keep: Boolean,
    ) {
        val messages = chat(chatId)?.messages.orEmpty()
        val index = messages.indexOfFirst { it.id == messageId }
        val dropped = if (index < 0) emptyList() else messages.drop(if (keep) index + 1 else index)
        if (dropped.isEmpty()) return
        val gone = dropped.map { it.id }.toSet()
        mutableChats.update { list ->
            list.map { c -> if (c.id == chatId) c.copy(messages = c.messages.filterNot { it.id in gone }) else c }
        }
        writeFor(chatId) { p -> gone.forEach { p.deleteMessage(it) } }
    }

    fun delete(chatId: Long) {
        val inMemoryOnly = isPrivate(chatId)
        mutableChats.update { list -> list.filterNot { it.id == chatId } }
        if (!inMemoryOnly) write { it.deleteChat(chatId) }
    }

    fun rename(
        chatId: Long,
        title: String,
    ) {
        val clean = title.trim().replace(Regex("\\s+"), " ").take(MAX_RENAME)
        if (clean.isEmpty()) return
        updateChat(chatId) { it.copy(title = clean) }
    }

    fun setPinned(
        chatId: Long,
        pinned: Boolean,
    ) = updateChat(chatId) { it.copy(pinned = pinned) }

    private fun updateChat(
        chatId: Long,
        change: (Chat) -> Chat,
    ) {
        var changed: Chat? = null
        mutableChats.update { list ->
            list.map { c -> if (c.id == chatId) change(c).also { changed = it } else c }
        }
        changed?.let { chat -> if (!chat.isPrivate) write { it.saveChat(chat) } }
    }

    /**
     * Ids of chats matching [query]: title or message text contains it, plus full-text hits from the
     * database (word prefixes). Blank query → `null` (no filter). Private chats are never found.
     */
    suspend fun search(query: String): Set<Long>? {
        val q = query.trim()
        if (q.isEmpty()) return null
        val local =
            mutableChats.value
                .filter { chat ->
                    chat.title.contains(q, ignoreCase = true) ||
                        chat.messages.any { it.text.contains(q, ignoreCase = true) }
                }.map { it.id }
        val indexed = runCatching { persistence?.search(q) }.getOrNull().orEmpty()
        val hidden =
            mutableChats.value
                .filter { it.isPrivate }
                .map { it.id }
                .toSet()
        return (local + indexed).toSet() - hidden
    }

    /** First words of the first user message, at most [MAX_TITLE] characters. */
    fun titleFrom(text: String): String {
        val clean = text.trim().replace(Regex("\\s+"), " ")
        return if (clean.length <= MAX_TITLE) clean else clean.take(MAX_TITLE).substringBeforeLast(' ') + "…"
    }

    private companion object {
        const val MAX_TITLE = 40
        const val MAX_RENAME = 80
    }
}
