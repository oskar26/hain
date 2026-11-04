package app.hain.data.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One remembered fact (DATA.md `memory_fact`, MEMORY.md L2). Never overwritten by the brain: a newer
 * fact points the old one out of use with [supersededBy]; the user can edit and delete.
 *
 * @property status `active` (used in answers), `suggested` (waits for the user's yes) or `deleted`.
 */
@Entity(tableName = "memory_fact", indices = [Index("status")])
data class MemoryFactEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val text: String,
    val category: String,
    val status: String,
    val sensitive: Boolean,
    val importance: Double,
    @ColumnInfo(name = "origin_chat_id") val originChatId: Long?,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "superseded_by") val supersededBy: Long?,
) {
    companion object {
        const val ACTIVE = "active"
        const val SUGGESTED = "suggested"
        const val DELETED = "deleted"
    }
}
