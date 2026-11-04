package app.hain.di

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
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
import app.hain.llm.internal.LlamaEngine
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
}

private const val STANDARD_FEEDS = "news/standard_feeds.json"

/** Wraps the database so Hilt can hand out a missing one. */
class DatabaseHolder(
    val db: HainDatabase?,
)
