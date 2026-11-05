package app.hain.feature.news.api

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** Background and text colours of the reading view (T-076). [APP] follows the app's light or dark look. */
enum class ReaderTheme { APP, LIGHT, DARK, SEPIA }

/**
 * The news settings (T-075, T-076, T-079), on their own page in the news screen:
 * reading view (text size, serif or sans, line spacing, colours, pictures in the article), pictures in
 * the list and the daily briefing note (off by default).
 */
data class NewsPrefs(
    val textScale: Float = 1f,
    val serif: Boolean = true,
    val lineSpacing: Float = LINE_NORMAL,
    val theme: ReaderTheme = ReaderTheme.APP,
    val articleImages: Boolean = true,
    val listImages: Boolean = true,
    val briefingNotification: Boolean = false,
) {
    companion object {
        const val LINE_TIGHT = 1.35f
        const val LINE_NORMAL = 1.55f
        const val LINE_WIDE = 1.8f
        val TEXT_SCALES = listOf(0.85f, 1f, 1.15f, 1.3f, 1.5f)
        val LINE_SPACINGS = listOf(LINE_TIGHT, LINE_NORMAL, LINE_WIDE)
    }
}

/** Where the news settings are kept. */
interface NewsPrefsStore {
    val prefs: StateFlow<NewsPrefs>

    fun update(change: (NewsPrefs) -> NewsPrefs)
}

/** In memory only (tests, screenshots). */
class MemoryNewsPrefs(
    initial: NewsPrefs = NewsPrefs(),
) : NewsPrefsStore {
    private val state = MutableStateFlow(initial)
    override val prefs: StateFlow<NewsPrefs> = state.asStateFlow()

    override fun update(change: (NewsPrefs) -> NewsPrefs) {
        state.update(change)
    }
}

/**
 * The news settings in their own small SharedPreferences file (`hain_news`), so they do not touch
 * the app's main settings store.
 */
class SharedNewsPrefs(
    context: Context,
) : NewsPrefsStore {
    private val file = context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)
    private val state = MutableStateFlow(read())
    override val prefs: StateFlow<NewsPrefs> = state.asStateFlow()

    override fun update(change: (NewsPrefs) -> NewsPrefs) {
        val next = change(state.value)
        state.value = next
        file
            .edit()
            .putFloat(TEXT, next.textScale)
            .putBoolean(SERIF, next.serif)
            .putFloat(LINES, next.lineSpacing)
            .putString(THEME, next.theme.name)
            .putBoolean(ARTICLE_IMAGES, next.articleImages)
            .putBoolean(LIST_IMAGES, next.listImages)
            .putBoolean(BRIEFING_NOTE, next.briefingNotification)
            .apply()
    }

    private fun read(): NewsPrefs {
        val d = NewsPrefs()
        return NewsPrefs(
            textScale = file.getFloat(TEXT, d.textScale),
            serif = file.getBoolean(SERIF, d.serif),
            lineSpacing = file.getFloat(LINES, d.lineSpacing),
            theme = runCatching { ReaderTheme.valueOf(file.getString(THEME, null) ?: "") }.getOrDefault(d.theme),
            articleImages = file.getBoolean(ARTICLE_IMAGES, d.articleImages),
            listImages = file.getBoolean(LIST_IMAGES, d.listImages),
            briefingNotification = file.getBoolean(BRIEFING_NOTE, d.briefingNotification),
        )
    }

    companion object {
        const val FILE = "hain_news"
        private const val TEXT = "reader_text_scale"
        private const val SERIF = "reader_serif"
        private const val LINES = "reader_line_spacing"
        private const val THEME = "reader_theme"
        private const val ARTICLE_IMAGES = "reader_images"
        private const val LIST_IMAGES = "list_images"
        private const val BRIEFING_NOTE = "briefing_notification"

        /** Whether the daily briefing note is on, for the background job that has no screen. */
        fun briefingNotificationOn(context: Context): Boolean =
            context.applicationContext
                .getSharedPreferences(FILE, Context.MODE_PRIVATE)
                .getBoolean(BRIEFING_NOTE, false)
    }
}
