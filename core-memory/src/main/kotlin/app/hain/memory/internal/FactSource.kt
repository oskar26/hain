package app.hain.memory.internal

import app.hain.data.dao.MemoryDao
import app.hain.data.entities.MemoryFactEntity
import app.hain.data.entities.MemoryTopicEntity
import app.hain.memory.api.MemoryCategory
import app.hain.memory.api.MemoryFact
import app.hain.memory.api.MemoryTopic
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

    /** [id] is replaced by the newer fact [by]: it stays stored but out of use (MEMORY.md §2). */
    suspend fun supersede(
        id: Long,
        by: Long,
    )

    /** Topics (MEMORY.md L1), by title. */
    fun observeTopics(): Flow<List<MemoryTopic>>

    /** Sets the note of the topic [title] (new topic if needed); an empty note removes it. */
    suspend fun setTopic(topic: MemoryTopic)
}

/** Facts in the `memory_fact` table (T-111). */
class RoomFactSource(
    private val dao: MemoryDao,
) : FactSource {
    override fun observe(): Flow<List<MemoryFact>> = dao.observe().map { rows -> rows.map { it.toFact() } }

    override suspend fun insert(fact: MemoryFact): Long = dao.insert(fact.toEntity().copy(id = 0))

    override suspend fun update(fact: MemoryFact) = dao.update(fact.toEntity())

    override suspend fun delete(id: Long) {
        // the vector goes with its fact (foreign key); deleted here too in case foreign keys are off
        dao.deleteVector(id)
        dao.delete(id)
    }

    override suspend fun deleteAll() {
        dao.deleteAllVectors()
        dao.deleteAllFacts()
        dao.deleteAllTopics()
    }

    override suspend fun supersede(
        id: Long,
        by: Long,
    ) = dao.supersede(id, by)

    override fun observeTopics(): Flow<List<MemoryTopic>> =
        dao.observeTopics().map { rows -> rows.map { MemoryTopic(it.title, it.note, it.updatedAt) } }

    override suspend fun setTopic(topic: MemoryTopic) {
        val old = dao.topicByTitle(topic.title)
        when {
            topic.note.isBlank() -> old?.let { dao.deleteTopic(it.id) }
            else -> dao.upsertTopic(MemoryTopicEntity(old?.id ?: 0, topic.title, topic.note, topic.updatedAt))
        }
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
            replaces = replaces,
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
            replaces = replaces,
        )
}

/** Facts only in memory: when the database cannot open, and in tests. */
class InMemoryFactSource : FactSource {
    private val rows = MutableStateFlow<List<MemoryFact>>(emptyList())
    private val topics = MutableStateFlow<List<MemoryTopic>>(emptyList())

    /** Facts put out of use by a newer one (old id → new id), for tests. */
    val superseded = mutableMapOf<Long, Long>()
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
        topics.value = emptyList()
    }

    override suspend fun supersede(
        id: Long,
        by: Long,
    ) {
        superseded[id] = by
        delete(id)
    }

    override fun observeTopics(): Flow<List<MemoryTopic>> = topics

    override suspend fun setTopic(topic: MemoryTopic) {
        val others = topics.value.filterNot { it.title == topic.title }
        topics.value = (if (topic.note.isBlank()) others else others + topic).sortedBy { it.title }
    }

    private companion object {
        val NEWEST_FIRST = compareByDescending<MemoryFact> { it.createdAt }.thenByDescending { it.id }
    }
}
