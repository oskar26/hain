package app.hain

import android.app.Application
import app.hain.di.DatabaseHolder
import app.hain.feature.news.api.BriefingDaily
import app.hain.feature.news.api.BriefingMaker
import app.hain.feature.news.api.FileBriefingStore
import app.hain.llm.api.LlmEngine
import app.hain.memory.api.MemoryExtraction
import app.hain.memory.api.MemoryStore
import app.hain.models.api.ModelRepository
import app.hain.tools.internal.news.NewsRepository
import dagger.hilt.android.HiltAndroidApp
import java.io.File
import javax.inject.Inject

@HiltAndroidApp
class HainApp : Application() {
    @Inject lateinit var memory: MemoryStore

    @Inject lateinit var llm: LlmEngine

    @Inject lateinit var models: ModelRepository

    @Inject lateinit var database: DatabaseHolder

    // Lazy: the news repository is only built when the morning note runs, not at every app start
    @Inject lateinit var news: dagger.Lazy<NewsRepository>

    override fun onCreate() {
        super.onCreate()
        // T-079: the optional morning briefing note (off by default) runs without the screen
        BriefingDaily.current =
            BriefingDaily(
                context = this,
                repository = { news.get() },
                maker = BriefingMaker(FileBriefingStore(File(filesDir, "news/briefing.json"))),
                lang = { if (resources.configuration.locales[0]?.language == "de") "de" else "en" },
            )
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
