package app.hain.di

import android.content.Context
import app.hain.feature.chat.internal.ChatStore
import app.hain.llm.api.LlmEngine
import app.hain.llm.internal.LlamaEngine
import app.hain.models.api.ModelRepository
import app.hain.models.internal.DefaultModelRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
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
    fun chatStore(): ChatStore = ChatStore()
}
