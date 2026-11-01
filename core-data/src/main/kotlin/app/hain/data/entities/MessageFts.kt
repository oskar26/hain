package app.hain.data.entities

import androidx.room.Entity
import androidx.room.Fts4
import androidx.room.FtsOptions

/**
 * Full-text index over message text. External content table: Room keeps it in sync with triggers.
 * FTS4 instead of FTS5 because Room generates and migrates it (decision T-020, STATUS.md).
 */
@Fts4(contentEntity = MessageEntity::class, tokenizer = FtsOptions.TOKENIZER_UNICODE61)
@Entity(tableName = "message_fts")
data class MessageFts(
    val content: String,
)
