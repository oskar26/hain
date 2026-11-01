package app.hain.data.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import app.hain.data.entities.ChatEntity
import app.hain.data.entities.ChatSummaryEntity

@Dao
interface ChatDao {
    @Upsert
    suspend fun upsert(chat: ChatEntity)

    @Query("SELECT * FROM chat WHERE archived = 0 ORDER BY pinned DESC, updated_at DESC")
    suspend fun all(): List<ChatEntity>

    @Query("SELECT * FROM chat WHERE id = :id")
    suspend fun byId(id: Long): ChatEntity?

    @Query("UPDATE chat SET title = :title WHERE id = :id")
    suspend fun rename(
        id: Long,
        title: String,
    )

    @Query("UPDATE chat SET pinned = :pinned WHERE id = :id")
    suspend fun setPinned(
        id: Long,
        pinned: Boolean,
    )

    @Query("UPDATE chat SET updated_at = :updatedAt WHERE id = :id")
    suspend fun touch(
        id: Long,
        updatedAt: Long,
    )

    @Query("DELETE FROM chat WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT MAX(id) FROM (SELECT id FROM chat UNION ALL SELECT id FROM message)")
    suspend fun maxId(): Long?

    @Upsert
    suspend fun upsertSummary(summary: ChatSummaryEntity)

    @Query("SELECT * FROM chat_summary WHERE chat_id = :chatId")
    suspend fun summary(chatId: Long): ChatSummaryEntity?
}
