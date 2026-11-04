package app.hain.common.api

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Which brain you picked in the top bar (T-055), kept in its own small DataStore file
 * (`datastore/brain.preferences_pb`) so it does not touch the main settings. `null` = never picked,
 * Hain then uses the default brain.
 */
class BrainChoiceStore(
    private val store: DataStore<Preferences>,
) {
    val selectedId: Flow<String?> = store.data.map { it[BRAIN_ID]?.takeIf(String::isNotBlank) }

    suspend fun select(id: String) {
        store.edit { it[BRAIN_ID] = id }
    }

    suspend fun clear() {
        store.edit { it.remove(BRAIN_ID) }
    }

    private companion object {
        val BRAIN_ID = stringPreferencesKey("brain_id")
    }
}
