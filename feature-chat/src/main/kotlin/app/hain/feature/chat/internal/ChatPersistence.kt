package app.hain.feature.chat.internal

/** Where [ChatStore] keeps chats between app starts. */
interface ChatPersistence {
    suspend fun load(): List<Chat>

    /** Highest chat or message id ever stored, 0 when empty. */
    suspend fun maxId(): Long

    /** Saves title, pin and timestamps of [chat] (not its messages). */
    suspend fun saveChat(chat: Chat)

    suspend fun saveMessage(
        chatId: Long,
        message: UiMessage,
        updatedAt: Long,
    )

    suspend fun deleteMessage(id: Long)

    suspend fun deleteChat(id: Long)

    /** Chat ids whose messages match [query] in the full-text index. */
    suspend fun search(query: String): Set<Long>
}
