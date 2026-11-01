package app.hain.data.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import app.hain.data.entities.MessageEntity

@Dao
interface MessageDao {
    @Upsert
    suspend fun upsert(message: MessageEntity)

    @Query("SELECT * FROM message ORDER BY chat_id, created_at, id")
    suspend fun all(): List<MessageEntity>

    @Query("SELECT * FROM message WHERE chat_id = :chatId ORDER BY created_at, id")
    suspend fun forChat(chatId: Long): List<MessageEntity>

    @Query("DELETE FROM message WHERE id = :id")
    suspend fun delete(id: Long)

    /**
     * Chats whose messages match an FTS [match] expression (see [FtsQuery.of]), best match first.
     */
    @Query(
        """
        SELECT DISTINCT message.chat_id FROM message
        JOIN message_fts ON message.rowid = message_fts.rowid
        WHERE message_fts MATCH :match
        ORDER BY message.created_at DESC
        """,
    )
    suspend fun searchChatIds(match: String): List<Long>
}
