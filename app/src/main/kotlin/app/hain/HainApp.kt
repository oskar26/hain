package app.hain

import android.app.Application
import android.util.Log
import app.hain.common.api.BrainChoiceStore
import app.hain.di.DatabaseHolder
import app.hain.feature.news.api.BriefingDaily
import app.hain.feature.news.api.BriefingMaker
import app.hain.feature.news.api.FileBriefingStore
import app.hain.llm.api.LlmEngine
import app.hain.memory.api.MemoryExtraction
import app.hain.memory.api.MemoryStore
import app.hain.models.api.BrainChoice
import app.hain.models.api.ModelRepository
import app.hain.tools.internal.news.NewsRepository
import dagger.Lazy
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.flow.first
import java.io.File
import javax.inject.Inject

@HiltAndroidApp
class HainApp : Application() {
    // T-290: all lazy, so nothing heavy (the encrypted database, the brain engine) is built on the main
    // thread while the app starts; the database is opened right away on a background thread instead
    @Inject lateinit var memory: Lazy<MemoryStore>

    @Inject lateinit var llm: Lazy<LlmEngine>

    @Inject lateinit var models: Lazy<ModelRepository>

    @Inject lateinit var brainChoice: Lazy<BrainChoiceStore>

    @Inject lateinit var database: Lazy<DatabaseHolder>

    // Lazy: the news repository is only built when the morning note runs, not at every app start
    @Inject lateinit var news: Lazy<NewsRepository>

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
        Thread(::prepareInBackground, "hain-start").start()
    }

    /** T-290: opens the database early (the chat list needs it next) and wires the memory extraction. */
    private fun prepareInBackground() {
        // T-291: a broken database or Keystore must not take the app down; chats then stay in memory
        runCatching { setUpExtraction() }.onFailure { Log.e("Hain", "Background start failed", it) }
    }

    private fun setUpExtraction() {
        // T-114: the background extraction needs the chats (the database) and the brain in use.
        // T-267: the picked brain (as in the chat), not the default one: with LFM3-Omni as the new
        // default, phones that still run LFM3 1.7B or the Großes Gehirn would never extract otherwise.
        val db = database.get().db ?: return
        MemoryExtraction.current =
            MemoryExtraction(
                context = this,
                store = memory.get(),
                llm = llm.get(),
                brainPath = {
                    val repository = models.get()
                    val picked = runCatching { brainChoice.get().selectedId.first() }.getOrNull()
                    val brain = BrainChoice.active(repository.catalog, picked, repository::isInstalled)
                    repository.mainFile(brain).absolutePath.takeIf { repository.isInstalled(brain) }
                },
                chats = db.chats(),
                messages = db.messages(),
            )
    }
}
