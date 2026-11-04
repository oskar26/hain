package app.hain.memory.internal

import app.hain.data.dao.MemoryDao
import app.hain.data.entities.MemoryFactVecEntity
import app.hain.memory.api.EmbedKind
import app.hain.memory.api.Embedder
import app.hain.memory.api.MemoryFact
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** One stored vector: the fact, which model made it, from which text. */
class StoredVector(
    val factId: Long,
    val model: String,
    val textHash: Int,
    val vector: FloatArray,
)

/** Where the vectors live: `memory_fact_vec` in the encrypted database, or plain memory. */
interface VectorSource {
    suspend fun all(): List<StoredVector>

    suspend fun put(vector: StoredVector)

    suspend fun delete(factId: Long)

    suspend fun deleteAll()
}

class RoomVectorSource(
    private val dao: MemoryDao,
) : VectorSource {
    override suspend fun all(): List<StoredVector> =
        dao.vectors().map { StoredVector(it.factId, it.model, it.textHash, Vectors.fromBytes(it.vector)) }

    override suspend fun put(vector: StoredVector) {
        val bytes = Vectors.toBytes(vector.vector)
        dao.upsertVector(MemoryFactVecEntity(vector.factId, vector.model, vector.textHash, vector.vector.size, bytes))
    }

    override suspend fun delete(factId: Long) = dao.deleteVector(factId)

    override suspend fun deleteAll() = dao.deleteAllVectors()
}

class InMemoryVectorSource : VectorSource {
    private val rows = LinkedHashMap<Long, StoredVector>()

    override suspend fun all(): List<StoredVector> = synchronized(rows) { rows.values.toList() }

    override suspend fun put(vector: StoredVector) {
        synchronized(rows) { rows[vector.factId] = vector }
    }

    override suspend fun delete(factId: Long) {
        synchronized(rows) { rows.remove(factId) }
    }

    override suspend fun deleteAll() {
        synchronized(rows) { rows.clear() }
    }
}

/**
 * The facts' vectors in memory (T-110), mirrored from [source]: a few thousand facts × 1024 floats
 * are a few MB, so the cosine search is a plain loop. [sync] embeds what is missing or outdated
 * (new, edited, other model) and drops vectors of facts that are gone.
 */
class VectorIndex(
    private val source: VectorSource,
    private val embedder: Embedder,
) {
    private val lock = Mutex()
    private val syncLock = Mutex()
    private var loaded = false
    private val vectors = HashMap<Long, StoredVector>()

    /** Vectors of [facts] that are up to date (same text, same model). */
    private fun current(fact: MemoryFact): FloatArray? =
        vectors[fact.id]?.takeIf { it.model == embedder.model && it.textHash == Vectors.textHash(fact.text) }?.vector

    /**
     * Brings the index in line with [facts]; returns how many facts got a new vector. Embeds one
     * fact at a time outside the lock, so a question can be ranked while old facts catch up.
     */
    suspend fun sync(facts: List<MemoryFact>): Int =
        syncLock.withLock {
            val todo =
                lock.withLock {
                    load()
                    val ids = facts.mapTo(HashSet()) { it.id }
                    vectors.keys.filter { it !in ids }.forEach { id ->
                        vectors.remove(id)
                        source.delete(id)
                    }
                    if (embedder.ready()) facts.filter { current(it) == null } else emptyList()
                }
            var made = 0
            for (fact in todo) {
                val vector = embedder.embed(fact.text, EmbedKind.DOCUMENT) ?: break
                val stored = StoredVector(fact.id, embedder.model, Vectors.textHash(fact.text), vector)
                lock.withLock {
                    // the fact may have been deleted meanwhile: then its row is gone and the insert fails
                    if (runCatching { source.put(stored) }.isSuccess) {
                        vectors[fact.id] = stored
                        made++
                    }
                }
            }
            made
        }

    /**
     * Raw cosine of each of [facts] to [query] (a vector made with [EmbedKind.QUERY] or
     * [EmbedKind.DOCUMENT]), `null` for facts without an up-to-date vector.
     */
    suspend fun cosines(
        facts: List<MemoryFact>,
        query: FloatArray,
    ): Map<Long, Double> =
        lock.withLock {
            load()
            facts.mapNotNull { f -> current(f)?.let { f.id to Vectors.cosine(query, it) } }.toMap()
        }

    suspend fun forget(factId: Long) =
        lock.withLock {
            vectors.remove(factId)
            source.delete(factId)
        }

    suspend fun clear() =
        lock.withLock {
            vectors.clear()
            source.deleteAll()
        }

    private suspend fun load() {
        if (loaded) return
        source.all().forEach { vectors[it.factId] = it }
        loaded = true
    }
}
