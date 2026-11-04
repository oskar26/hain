package app.hain.di

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import app.hain.common.api.BrainChoiceStore
import app.hain.common.api.HainSettings
import app.hain.common.api.SettingsRepository
import app.hain.common.api.net.NetworkGate
import app.hain.common.api.net.NetworkGates
import app.hain.data.HainDatabase
import app.hain.data.KeystoreKeyProvider
import app.hain.data.RoomNetworkLogStore
import app.hain.feature.chat.internal.ChatStore
import app.hain.feature.chat.internal.RoomChatPersistence
import app.hain.llm.api.LlmEngine
import app.hain.llm.api.TextEmbedder
import app.hain.llm.internal.LlamaEmbedder
import app.hain.llm.internal.LlamaEngine
import app.hain.memory.api.HainMemory
import app.hain.memory.api.MemoryStore
import app.hain.memory.api.MemoryTurn
import app.hain.memory.api.PackEmbedder
import app.hain.models.api.ModelInfo
import app.hain.models.api.ModelRepository
import app.hain.models.internal.DefaultModelRepository
import app.hain.tools.internal.net.HttpNetworkGate
import app.hain.tools.internal.news.FileNewsStore
import app.hain.tools.internal.news.NewsJson
import app.hain.tools.internal.news.NewsRepository
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
@Suppress("TooManyFunctions") // one provider per app-wide object (Hilt), grows with the merged Häppchen
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

    /** T-055: the brain picked in the top bar, in its own small DataStore file. */
    @Provides
    @Singleton
    fun brainChoiceStore(
        @ApplicationContext context: Context,
    ): BrainChoiceStore =
        BrainChoiceStore(
            PreferenceDataStoreFactory.create(
                scope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
                produceFile = { File(context.filesDir, "datastore/brain.preferences_pb") },
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
        ).also { NetworkGates.current = it } // 3b-2: the brain download worker logs through it too
    }

    /**
     * News (T-071): the fixed standard mix from the assets plus own feeds, kept in one JSON file in
     * the app's private files. Feeds go through the NetworkGate (purpose NEWS).
     */
    @Provides
    @Singleton
    fun newsRepository(
        @ApplicationContext context: Context,
        gate: NetworkGate,
    ): NewsRepository {
        val standard =
            runCatching {
                context.assets.open(STANDARD_FEEDS).use { NewsJson.standardFeeds(it.reader().readText()) }
            }.onFailure { Log.e("Hain", "Standard news feeds unreadable", it) }
                .getOrDefault(emptyList())
        return NewsRepository(gate, FileNewsStore(File(context.filesDir, "news/news.json")), standard)
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

    /** T-110: the small embedding model next to the brain (own thread, own context, loaded on demand). */
    @Provides
    @Singleton
    fun textEmbedder(
        @ApplicationContext context: Context,
    ): TextEmbedder = LlamaEmbedder(context)

    /**
     * The memory (T-112): facts in the encrypted database, only on this phone; in memory if it cannot
     * open. With the pack "Suche nach Bedeutung" (T-110) installed, recall is by meaning too.
     */
    @Provides
    @Singleton
    fun memoryStore(
        @ApplicationContext context: Context,
        database: DatabaseHolder,
        embedder: TextEmbedder,
        models: ModelRepository,
    ): MemoryStore {
        val pack = models.catalog.firstOrNull { it.kind == ModelInfo.KIND_EMBED }
        val packEmbedder =
            pack?.let { info ->
                PackEmbedder(embedder, info.id) { models.mainFile(info).takeIf { models.isInstalled(info) } }
            }
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        return HainMemory.store(context, database.db?.memory(), scope, packEmbedder)
    }

    /** T-112/7b: the memory's part of each turn, one for the app (the chips read its [MemoryTurn.uses]). */
    @Provides
    @Singleton
    fun memoryTurn(memory: MemoryStore): MemoryTurn {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        return MemoryTurn(memory, scope)
    }
}

private const val STANDARD_FEEDS = "news/standard_feeds.json"

/** Wraps the database so Hilt can hand out a missing one. */
class DatabaseHolder(
    val db: HainDatabase?,
)
