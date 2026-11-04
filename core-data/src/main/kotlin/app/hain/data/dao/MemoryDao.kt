package app.hain.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import app.hain.data.entities.MemoryFactEntity
import app.hain.data.entities.MemoryFactVecEntity
import app.hain.data.entities.MemoryTopicEntity
import kotlinx.coroutines.flow.Flow

/**
 * Remembered facts, their vectors and topics (MEMORY.md). Deleted and superseded facts stay out of
 * every query but [byId].
 */
@Suppress("TooManyFunctions") // one query per memory-page action
@Dao
interface MemoryDao {
    @Insert
    suspend fun insert(fact: MemoryFactEntity): Long

    @Update
    suspend fun update(fact: MemoryFactEntity)

    @Query("SELECT * FROM memory_fact WHERE id = :id")
    suspend fun byId(id: Long): MemoryFactEntity?

    /** Active and suggested facts, newest first; the memory page and the in-memory index read this. */
    @Query(
        "SELECT * FROM memory_fact WHERE status != 'deleted' AND superseded_by IS NULL " +
            "ORDER BY created_at DESC, id DESC",
    )
    fun observe(): Flow<List<MemoryFactEntity>>

    @Query(
        "SELECT * FROM memory_fact WHERE status != 'deleted' AND superseded_by IS NULL " +
            "ORDER BY created_at DESC, id DESC",
    )
    suspend fun all(): List<MemoryFactEntity>

    /** A newer fact ([by]) replaces [id]: the old row stays, but out of use (never overwritten, MEMORY.md §2). */
    @Query("UPDATE memory_fact SET superseded_by = :by WHERE id = :id")
    suspend fun supersede(
        id: Long,
        by: Long,
    )

    @Query("UPDATE memory_fact SET status = :status WHERE id = :id")
    suspend fun setStatus(
        id: Long,
        status: String,
    )

    /** Facts whose text matches [match] (an [FtsQuery] expression), deleted ones left out. */
    @Query(
        """
        SELECT memory_fact.* FROM memory_fact
        JOIN memory_fact_fts ON memory_fact.rowid = memory_fact_fts.rowid
        WHERE memory_fact_fts MATCH :match AND memory_fact.status != 'deleted'
        ORDER BY memory_fact.created_at DESC
        """,
    )
    suspend fun search(match: String): List<MemoryFactEntity>

    /** The user deleted it: gone for good, from the full-text index too (MEMORY.md §4). */
    @Query("DELETE FROM memory_fact WHERE id = :id")
    suspend fun delete(id: Long)

    /** "Alles löschen": the rows go for good (also from the full-text index via its triggers). */
    @Query("DELETE FROM memory_fact")
    suspend fun deleteAllFacts()

    /** T-110: vector of one fact (new or replacing the old one). */
    @Upsert
    suspend fun upsertVector(vector: MemoryFactVecEntity)

    @Query("SELECT * FROM memory_fact_vec")
    suspend fun vectors(): List<MemoryFactVecEntity>

    @Query("DELETE FROM memory_fact_vec WHERE fact_id = :id")
    suspend fun deleteVector(id: Long)

    @Query("DELETE FROM memory_fact_vec")
    suspend fun deleteAllVectors()

    @Upsert
    suspend fun upsertTopic(topic: MemoryTopicEntity)

    @Query("SELECT * FROM memory_topic ORDER BY title")
    suspend fun topics(): List<MemoryTopicEntity>

    /** Topics for the memory page's "Themen" tab (7b). */
    @Query("SELECT * FROM memory_topic ORDER BY title")
    fun observeTopics(): Flow<List<MemoryTopicEntity>>

    @Query("SELECT * FROM memory_topic WHERE title = :title")
    suspend fun topicByTitle(title: String): MemoryTopicEntity?

    @Query("DELETE FROM memory_topic WHERE id = :id")
    suspend fun deleteTopic(id: Long)

    @Query("DELETE FROM memory_topic")
    suspend fun deleteAllTopics()
}
