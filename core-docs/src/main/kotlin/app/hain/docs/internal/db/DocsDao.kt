package app.hain.docs.internal.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
abstract class DocsDao {
    @Query("SELECT * FROM document ORDER BY added_at DESC")
    abstract fun all(): Flow<List<DocumentEntity>>

    @Query("SELECT * FROM document WHERE id = :id")
    abstract suspend fun byId(id: Long): DocumentEntity?

    @Query("SELECT * FROM document WHERE id IN (:ids)")
    abstract suspend fun byIds(ids: List<Long>): List<DocumentEntity>

    @Insert
    abstract suspend fun insert(document: DocumentEntity): Long

    @Insert
    abstract suspend fun insertChunks(chunks: List<DocChunkEntity>)

    @Transaction
    open suspend fun add(
        document: DocumentEntity,
        chunks: List<DocChunkEntity>,
    ): Long {
        val id = insert(document)
        insertChunks(chunks.map { it.copy(documentId = id) })
        return id
    }

    @Query("SELECT * FROM doc_chunk WHERE document_id IN (:ids) ORDER BY document_id, chunk_index")
    abstract suspend fun chunks(ids: List<Long>): List<DocChunkEntity>

    @Query("DELETE FROM document WHERE id = :id")
    abstract suspend fun delete(id: Long)
}
