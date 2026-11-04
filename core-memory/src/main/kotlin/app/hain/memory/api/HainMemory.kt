package app.hain.memory.api

import android.content.Context
import app.hain.data.dao.MemoryDao
import app.hain.memory.internal.DefaultMemoryStore
import app.hain.memory.internal.InMemoryFactSource
import app.hain.memory.internal.MemorySwitch
import app.hain.memory.internal.RoomFactSource
import kotlinx.coroutines.CoroutineScope

/** Builds the memory: facts in the encrypted database ([dao]), or only in memory when it cannot open. */
object HainMemory {
    fun store(
        context: Context,
        dao: MemoryDao?,
        scope: CoroutineScope,
    ): MemoryStore {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val switch =
            object : MemorySwitch {
                override fun get() = prefs.getBoolean(KEY_ON, true)

                override fun set(on: Boolean) = prefs.edit().putBoolean(KEY_ON, on).apply()
            }
        val source = dao?.let { RoomFactSource(it) } ?: InMemoryFactSource()
        return DefaultMemoryStore(source, scope, switch)
    }

    private const val PREFS = "hain_memory"
    private const val KEY_ON = "enabled"
}
