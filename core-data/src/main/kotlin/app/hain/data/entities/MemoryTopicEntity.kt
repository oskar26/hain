package app.hain.data.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** A memory topic (DATA.md `memory_topic`, MEMORY.md L1): a title and a short note about it. */
@Entity(tableName = "memory_topic", indices = [Index(value = ["title"], unique = true)])
data class MemoryTopicEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val note: String,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
)
