package app.hain.data.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

/** Rolling summary of a long chat (DATA.md `chat_summary`), written by the memory layer later. */
@Entity(
    tableName = "chat_summary",
    foreignKeys = [
        ForeignKey(
            entity = ChatEntity::class,
            parentColumns = ["id"],
            childColumns = ["chat_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class ChatSummaryEntity(
    @PrimaryKey @ColumnInfo(name = "chat_id") val chatId: Long,
    val summary: String,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
)
