package app.hain.data.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

/**
 * The vector of one fact (DATA.md `memory_fact_vec`, T-110): what the embedder made of its text,
 * as little-endian float32 values. Goes with its fact ([ForeignKey.CASCADE]). [textHash] is the
 * hash of the text it was made from, so an edited fact gets a new vector; [model] names the
 * embedder, so a different one starts over. The cosine search runs in Kotlin (no sqlite-vec).
 */
@Entity(
    tableName = "memory_fact_vec",
    foreignKeys = [
        ForeignKey(
            entity = MemoryFactEntity::class,
            parentColumns = ["id"],
            childColumns = ["fact_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
class MemoryFactVecEntity(
    @PrimaryKey @ColumnInfo(name = "fact_id") val factId: Long,
    val model: String,
    @ColumnInfo(name = "text_hash") val textHash: Int,
    val dim: Int,
    val vector: ByteArray,
)
