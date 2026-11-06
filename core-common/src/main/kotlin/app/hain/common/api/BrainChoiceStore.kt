package app.hain.common.api

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

/**
 * Which brain you picked in the top bar (T-055), kept in its own small DataStore file
 * (`datastore/brain.preferences_pb`) so it does not touch the main settings. `null` = never picked,
 * Hain then uses the default brain.
 */
class BrainChoiceStore(
    private val store: DataStore<Preferences>,
) {
    // T-291: a broken or unreadable file (disk full, cut off while writing) gives the defaults instead
    // of crashing every screen that reads it; a write that fails is dropped (the old value stays)
    private val safeData: Flow<Preferences> =
        store.data.catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }

    private suspend fun save(change: (MutablePreferences) -> Unit) {
        try {
            store.edit(change)
        } catch (ignored: IOException) {
            // nothing to show: the switch simply stays as it was
        }
    }

    val selectedId: Flow<String?> = safeData.map { it[BRAIN_ID]?.takeIf(String::isNotBlank) }

    suspend fun select(id: String) {
        save { it[BRAIN_ID] = id }
    }

    suspend fun clear() {
        save { it.remove(BRAIN_ID) }
    }

    private companion object {
        val BRAIN_ID = stringPreferencesKey("brain_id")
    }
}
