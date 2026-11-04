package app.hain.memory.api

import android.content.Context
import app.hain.data.dao.MemoryDao
import app.hain.memory.internal.DefaultMemoryStore
import app.hain.memory.internal.InMemoryFactSource
import app.hain.memory.internal.InMemoryVectorSource
import app.hain.memory.internal.MemorySwitch
import app.hain.memory.internal.RoomFactSource
import app.hain.memory.internal.RoomVectorSource
import kotlinx.coroutines.CoroutineScope

/**
 * Builds the memory: facts (and their vectors) in the encrypted database ([dao]), or only in memory
 * when it cannot open. [embedder] is the optional pack for recall by meaning (T-110).
 */
object HainMemory {
    fun store(
        context: Context,
        dao: MemoryDao?,
        scope: CoroutineScope,
        embedder: Embedder? = null,
    ): MemoryStore {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val switch =
            object : MemorySwitch {
                override fun get() = prefs.getBoolean(KEY_ON, true)

                override fun set(on: Boolean) = prefs.edit().putBoolean(KEY_ON, on).apply()
            }
        val source = dao?.let { RoomFactSource(it) } ?: InMemoryFactSource()
        val vectors = dao?.let { RoomVectorSource(it) } ?: InMemoryVectorSource()
        return DefaultMemoryStore(source, scope, switch, embedder = embedder, vectors = vectors)
    }

    private const val PREFS = "hain_memory"
    private const val KEY_ON = "enabled"
}
