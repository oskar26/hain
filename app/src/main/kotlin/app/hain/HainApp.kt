package app.hain

import android.app.Application
import app.hain.di.DatabaseHolder
import app.hain.llm.api.LlmEngine
import app.hain.memory.api.MemoryExtraction
import app.hain.memory.api.MemoryStore
import app.hain.models.api.ModelRepository
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class HainApp : Application() {
    @Inject lateinit var memory: MemoryStore

    @Inject lateinit var llm: LlmEngine

    @Inject lateinit var models: ModelRepository

    @Inject lateinit var database: DatabaseHolder

    override fun onCreate() {
        super.onCreate()
        // T-114: the background extraction needs the chats (the database) and the default brain
        val db = database.db ?: return
        val brain = models.defaultBrain
        MemoryExtraction.current =
            MemoryExtraction(
                context = this,
                store = memory,
                llm = llm,
                brainPath = { models.mainFile(brain).absolutePath.takeIf { models.isInstalled(brain) } },
                chats = db.chats(),
                messages = db.messages(),
            )
    }
}
