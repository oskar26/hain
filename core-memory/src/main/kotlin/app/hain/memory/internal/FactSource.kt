package app.hain.memory.internal

import app.hain.data.dao.MemoryDao
import app.hain.data.entities.MemoryFactEntity
import app.hain.memory.api.MemoryCategory
import app.hain.memory.api.MemoryFact
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/** Where the facts live: the encrypted database on the phone, or plain memory (tests, no database). */
interface FactSource {
    /** Active and suggested facts, newest first. */
    fun observe(): Flow<List<MemoryFact>>

    suspend fun insert(fact: MemoryFact): Long

    suspend fun update(fact: MemoryFact)

    suspend fun delete(id: Long)

    suspend fun deleteAll()
}

/** Facts in the `memory_fact` table (T-111). */
class RoomFactSource(
    private val dao: MemoryDao,
) : FactSource {
    override fun observe(): Flow<List<MemoryFact>> = dao.observe().map { rows -> rows.map { it.toFact() } }

    override suspend fun insert(fact: MemoryFact): Long = dao.insert(fact.toEntity().copy(id = 0))

    override suspend fun update(fact: MemoryFact) = dao.update(fact.toEntity())

    override suspend fun delete(id: Long) = dao.delete(id)

    override suspend fun deleteAll() {
        dao.deleteAllFacts()
        dao.deleteAllTopics()
    }

    private fun MemoryFactEntity.toFact() =
        MemoryFact(
            id = id,
            text = text,
            category = MemoryCategory.fromKey(category),
            createdAt = createdAt,
            suggested = status == MemoryFactEntity.SUGGESTED,
            sensitive = sensitive,
            importance = importance,
            originChatId = originChatId,
        )

    private fun MemoryFact.toEntity() =
        MemoryFactEntity(
            id = id,
            text = text,
            category = category.key,
            status = if (suggested) MemoryFactEntity.SUGGESTED else MemoryFactEntity.ACTIVE,
            sensitive = sensitive,
            importance = importance,
            originChatId = originChatId,
            createdAt = createdAt,
            supersededBy = null,
        )
}

/** Facts only in memory: when the database cannot open, and in tests. */
class InMemoryFactSource : FactSource {
    private val rows = MutableStateFlow<List<MemoryFact>>(emptyList())
    private var next = 1L

    override fun observe(): Flow<List<MemoryFact>> = rows

    override suspend fun insert(fact: MemoryFact): Long {
        val id = next++
        rows.value = (rows.value + fact.copy(id = id)).sortedWith(NEWEST_FIRST)
        return id
    }

    override suspend fun update(fact: MemoryFact) {
        rows.value = rows.value.map { if (it.id == fact.id) fact else it }.sortedWith(NEWEST_FIRST)
    }

    override suspend fun delete(id: Long) {
        rows.value = rows.value.filterNot { it.id == id }
    }

    override suspend fun deleteAll() {
        rows.value = emptyList()
    }

    private companion object {
        val NEWEST_FIRST = compareByDescending<MemoryFact> { it.createdAt }.thenByDescending { it.id }
    }
}
