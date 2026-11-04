package app.hain.data.entities

import androidx.room.Entity
import androidx.room.Fts4
import androidx.room.FtsOptions

/** Full-text index over remembered facts; Room keeps it in sync with triggers (FTS4, as for messages). */
@Fts4(contentEntity = MemoryFactEntity::class, tokenizer = FtsOptions.TOKENIZER_UNICODE61)
@Entity(tableName = "memory_fact_fts")
data class MemoryFactFts(
    val text: String,
)
