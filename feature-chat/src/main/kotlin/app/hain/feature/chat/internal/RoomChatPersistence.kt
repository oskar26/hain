package app.hain.feature.chat.internal

import app.hain.data.HainDatabase
import app.hain.data.dao.FtsQuery
import app.hain.data.entities.ChatEntity
import app.hain.data.entities.MessageEntity
import app.hain.docs.api.DocAttachment

/** [ChatPersistence] on the encrypted database (T-020). */
class RoomChatPersistence(
    private val db: HainDatabase,
) : ChatPersistence {
    override suspend fun load(): List<Chat> {
        val messages = db.messages().all().groupBy { it.chatId }
        return db.chats().all().map { entity ->
            entity.toChat(messages[entity.id].orEmpty().map { it.toUi() })
        }
    }

    override suspend fun maxId(): Long = db.chats().maxId() ?: 0

    override suspend fun saveChat(chat: Chat) {
        val existing = db.chats().byId(chat.id)
        db.chats().upsert(
            ChatEntity(
                id = chat.id,
                title = chat.title,
                createdAt = existing?.createdAt ?: chat.createdAt,
                updatedAt = maxOf(chat.updatedAt, existing?.updatedAt ?: 0),
                modelId = existing?.modelId,
                pinned = chat.pinned,
                isPrivate = existing?.isPrivate ?: false,
                archived = existing?.archived ?: false,
            ),
        )
    }

    override suspend fun saveMessage(
        chatId: Long,
        message: UiMessage,
        updatedAt: Long,
    ) {
        if (db.chats().byId(chatId) == null) return
        db.messages().upsert(message.toEntity(chatId, createdAt = updatedAt))
        db.chats().touch(chatId, updatedAt)
    }

    override suspend fun deleteMessage(id: Long) = db.messages().delete(id)

    override suspend fun deleteChat(id: Long) = db.chats().delete(id)

    override suspend fun search(query: String): Set<Long> {
        val match = FtsQuery.of(query) ?: return emptySet()
        return db.messages().searchChatIds(match).toSet()
    }
}

internal fun ChatEntity.toChat(messages: List<UiMessage>) =
    Chat(id = id, title = title, messages = messages, updatedAt = updatedAt, pinned = pinned, createdAt = createdAt)

internal fun MessageEntity.toUi(): UiMessage {
    val (steps, sources) = ToolStepsJson.decode(sourcesJson)
    return UiMessage(
        id = id,
        isUser = role == MessageEntity.ROLE_USER,
        text = content,
        reasoning = reasoning,
        thinkingSeconds = thinkingMillis?.let { (it / MS_PER_S).toInt() },
        stopped = status == MessageEntity.STATUS_STOPPED,
        failed = status == MessageEntity.STATUS_FAILED,
        steps = steps,
        sources = sources,
        trace = ToolStepsJson.decodeTrace(toolCallsJson),
        actions = ToolStepsJson.decodeActions(sourcesJson),
        cards = ToolStepsJson.decodeCards(sourcesJson),
        attachments = DocAttachment.decode(attachmentsJson),
    )
}

internal fun UiMessage.toEntity(
    chatId: Long,
    createdAt: Long,
) = MessageEntity(
    id = id,
    chatId = chatId,
    role = if (isUser) MessageEntity.ROLE_USER else MessageEntity.ROLE_ASSISTANT,
    content = text,
    reasoning = reasoning,
    sourcesJson = ToolStepsJson.encode(steps, sources, actions, cards),
    toolCallsJson = ToolStepsJson.encodeTrace(trace),
    attachmentsJson = DocAttachment.encode(attachments),
    createdAt = createdAt,
    thinkingMillis = thinkingSeconds?.let { it * MS_PER_S },
    status =
        when {
            failed -> MessageEntity.STATUS_FAILED
            stopped -> MessageEntity.STATUS_STOPPED
            else -> MessageEntity.STATUS_OK
        },
)

private const val MS_PER_S = 1000L
