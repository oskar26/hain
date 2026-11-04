package app.hain.data.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** One message of a chat (DATA.md `message`). Deleting the chat deletes its messages. */
@Entity(
    tableName = "message",
    foreignKeys = [
        ForeignKey(
            entity = ChatEntity::class,
            parentColumns = ["id"],
            childColumns = ["chat_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("chat_id")],
)
data class MessageEntity(
    @PrimaryKey val id: Long,
    @ColumnInfo(name = "chat_id") val chatId: Long,
    val role: String,
    val content: String,
    val reasoning: String? = null,
    @ColumnInfo(name = "tool_calls_json") val toolCallsJson: String? = null,
    @ColumnInfo(name = "tool_call_id") val toolCallId: String? = null,
    @ColumnInfo(name = "cards_json") val cardsJson: String? = null,
    @ColumnInfo(name = "sources_json") val sourcesJson: String? = null,
    @ColumnInfo(name = "attachments_json") val attachmentsJson: String? = null,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    val tokens: Int? = null,
    @ColumnInfo(name = "parent_id") val parentId: Long? = null,
    @ColumnInfo(name = "thinking_ms") val thinkingMillis: Long? = null,
    /** ok, stopped or failed — how the answer ended. */
    val status: String = STATUS_OK,
    /**
     * T-057 branches: when this message was picked among its siblings (same [parentId]); the newest
     * pick is the version shown. `null` = never picked (the newest sibling by id wins).
     */
    @ColumnInfo(name = "selected_at") val selectedAt: Long? = null,
) {
    companion object {
        const val ROLE_SYSTEM = "system"
        const val ROLE_USER = "user"
        const val ROLE_ASSISTANT = "assistant"
        const val ROLE_TOOL = "tool"
        const val STATUS_OK = "ok"
        const val STATUS_STOPPED = "stopped"
        const val STATUS_FAILED = "failed"
    }
}
