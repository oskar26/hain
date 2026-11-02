package app.hain.di

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import app.hain.common.api.SettingsRepository
import app.hain.data.HainDatabase
import app.hain.data.KeystoreKeyProvider
import app.hain.feature.chat.internal.ChatStore
import app.hain.feature.chat.internal.RoomChatPersistence
import app.hain.llm.api.LlmEngine
import app.hain.llm.internal.LlamaEngine
import app.hain.models.api.ModelRepository
import app.hain.models.internal.DefaultModelRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import java.io.File
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

    /** Chats survive app restarts in the encrypted database; if it cannot open, chats stay in memory. */
    @Provides
    @Singleton
    fun chatStore(
        @ApplicationContext context: Context,
    ): ChatStore {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val persistence =
            runCatching { RoomChatPersistence(HainDatabase.open(context, KeystoreKeyProvider(context))) }
                .onFailure { Log.e("Hain", "Chat database unavailable, chats stay in memory", it) }
                .getOrNull()
        return ChatStore(persistence = persistence, scope = scope)
    }
}
