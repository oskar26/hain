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
 * @property replaces a suggestion from the background extraction (T-114) that would replace this
 *   older fact once the user accepts it; the old one then gets [supersededBy] (DB version 5).
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
    val replaces: Long? = null,
) {
    companion object {
        const val ACTIVE = "active"
        const val SUGGESTED = "suggested"
        const val DELETED = "deleted"
    }
}
