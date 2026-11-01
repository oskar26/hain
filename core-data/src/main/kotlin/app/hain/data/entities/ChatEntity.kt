package app.hain.data.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** One conversation (DATA.md `chat`). Ids are assigned by the app, not by SQLite. */
@Entity(tableName = "chat", indices = [Index("updated_at")])
data class ChatEntity(
    @PrimaryKey val id: Long,
    val title: String,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
    @ColumnInfo(name = "model_id") val modelId: String? = null,
    val pinned: Boolean = false,
    @ColumnInfo(name = "private") val isPrivate: Boolean = false,
    val archived: Boolean = false,
)
