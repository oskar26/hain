package app.hain.docs.internal.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One document in "Meine Dateien" (DATA.md `document`, T-123). Hain keeps only the text and a
 * small preview, never the file itself; everything sits in the encrypted `hain_docs.db`.
 */
@Entity(tableName = "document")
data class DocumentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    /** pdf, text or image */
    val kind: String,
    val mime: String?,
    val pages: Int,
    val chars: Int,
    @ColumnInfo(name = "added_at") val addedAt: Long,
    @ColumnInfo(name = "ocr_pages") val ocrPages: Int = 0,
    @ColumnInfo(name = "missing_pages") val missingPages: Int = 0,
    /** Small PNG of the first page or the photo. */
    @ColumnInfo(typeAffinity = ColumnInfo.BLOB) val thumbnail: ByteArray? = null,
    val status: String = STATUS_READY,
) {
    override fun equals(other: Any?): Boolean = other is DocumentEntity && id == other.id && addedAt == other.addedAt

    override fun hashCode(): Int = id.hashCode()

    companion object {
        const val STATUS_READY = "ready"
    }
}

/** One part of a document (DATA.md `doc_chunk`): about 400 tokens of one page. */
@Entity(
    tableName = "doc_chunk",
    foreignKeys = [
        ForeignKey(
            entity = DocumentEntity::class,
            parentColumns = ["id"],
            childColumns = ["document_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("document_id")],
)
data class DocChunkEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "document_id") val documentId: Long,
    val page: Int,
    /** Order inside the document. */
    @ColumnInfo(name = "chunk_index") val index: Int,
    val text: String,
    val ocr: Boolean = false,
    val confidence: Float? = null,
)
