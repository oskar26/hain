package app.hain.di

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import app.hain.common.api.HainSettings
import app.hain.common.api.SettingsRepository
import app.hain.common.api.net.NetworkGate
import app.hain.data.HainDatabase
import app.hain.data.KeystoreKeyProvider
import app.hain.data.RoomNetworkLogStore
import app.hain.feature.chat.internal.ChatStore
import app.hain.feature.chat.internal.RoomChatPersistence
import app.hain.llm.api.LlmEngine
import app.hain.llm.internal.LlamaEngine
import app.hain.memory.api.HainMemory
import app.hain.memory.api.MemoryStore
import app.hain.models.api.ModelRepository
import app.hain.models.internal.DefaultModelRepository
import app.hain.tools.internal.net.HttpNetworkGate
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun llmEngine(
        @ApplicationContext context: Context,
    ): LlmEngine = LlamaEngine(context)

    @Provides
    @Singleton
    fun modelRepository(
        @ApplicationContext context: Context,
    ): ModelRepository = DefaultModelRepository(context)

    @Provides
    @Singleton
    fun settingsRepository(
        @ApplicationContext context: Context,
    ): SettingsRepository =
        SettingsRepository(
            PreferenceDataStoreFactory.create(
                scope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
                produceFile = { File(context.filesDir, "datastore/settings.preferences_pb") },
            ),
        )

    /**
     * The one way to the internet (T-060). Reads offline mode and the log detail from the settings,
     * which are kept up to date in the background.
     */
    @Provides
    @Singleton
    fun networkGate(
        settings: SettingsRepository,
        database: DatabaseHolder,
    ): NetworkGate {
        val current = AtomicReference(HainSettings())
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch { settings.settings.collect { current.set(it) } }
        return HttpNetworkGate(
            offline = { current.get().offlineMode },
            detailedLog = { current.get().networkLogDetail },
            store = database.db?.let { RoomNetworkLogStore(it.networkLog()) },
        )
    }

    /** The one encrypted database, opened once; null when it cannot open (chats then stay in memory). */
    @Provides
    @Singleton
    fun databaseHolder(
        @ApplicationContext context: Context,
    ): DatabaseHolder =
        DatabaseHolder(
            runCatching { HainDatabase.open(context, KeystoreKeyProvider(context)) }
                .onFailure { Log.e("Hain", "Chat database unavailable, chats stay in memory", it) }
                .getOrNull(),
        )

    /** Chats survive app restarts in the encrypted database; if it cannot open, chats stay in memory. */
    @Provides
    @Singleton
    fun chatStore(database: DatabaseHolder): ChatStore {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val persistence =
            database.db?.let { RoomChatPersistence(it) }
        return ChatStore(persistence = persistence, scope = scope)
    }

    /** The memory (T-112): facts in the encrypted database, only on this phone; in memory if it cannot open. */
    @Provides
    @Singleton
    fun memoryStore(
        @ApplicationContext context: Context,
        database: DatabaseHolder,
    ): MemoryStore = HainMemory.store(context, database.db?.memory(), CoroutineScope(SupervisorJob() + Dispatchers.IO))
}

/** Wraps the database so Hilt can hand out a missing one. */
class DatabaseHolder(
    val db: HainDatabase?,
)
