package app.hain.feature.news.api

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.hain.tools.internal.news.AddFeedResult
import app.hain.tools.internal.news.NewsFeed
import app.hain.tools.internal.news.NewsItem
import app.hain.tools.internal.news.NewsMode
import app.hain.tools.internal.news.NewsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Why adding an own feed did not work (shown under the address field). */
enum class AddFeedError { NOT_A_FEED, DUPLICATE, FULL, OFFLINE, FAILED }

/** A summary on the news screen: running, or what came back (T-074; kept only while the screen is open). */
sealed interface SummaryState {
    data object Loading : SummaryState

    data class Ready(
        val result: SummaryResult,
    ) : SummaryState
}

/** Everything the news screen shows (T-073). */
data class NewsUiState(
    val mode: NewsMode = NewsMode.STANDARD,
    val items: List<NewsItem> = emptyList(),
    val ownFeeds: List<NewsFeed> = emptyList(),
    val broken: Set<String> = emptySet(),
    val refreshing: Boolean = false,
    val offline: Boolean = false,
    val adding: Boolean = false,
    val addError: AddFeedError? = null,
    val summaries: Map<String, SummaryState> = emptyMap(),
)

/**
 * News screen state (T-073/T-074): the mode switch, own feeds (add, rename, delete), refresh and the
 * on-demand summary. [scope] replaces `viewModelScope` in tests.
 */
@Suppress("TooManyFunctions") // one function per screen action
class NewsViewModel(
    private val repository: NewsRepository,
    private val summarizer: SummarizeArticle? = null,
    private val lang: () -> String = { "de" },
    scope: CoroutineScope? = null,
) : ViewModel() {
    private data class Local(
        val refreshing: Boolean = false,
        val offline: Boolean = false,
        val adding: Boolean = false,
        val addError: AddFeedError? = null,
        val summaries: Map<String, SummaryState> = emptyMap(),
    )

    private val work: CoroutineScope = scope ?: viewModelScope
    private val local = MutableStateFlow(Local())
    private val mutableState = MutableStateFlow(NewsUiState())
    val state: StateFlow<NewsUiState> = mutableState.asStateFlow()

    init {
        work.launch {
            combine(repository.state, local) { s, l ->
                NewsUiState(
                    mode = s.mode,
                    items = repository.items(s),
                    ownFeeds = s.ownFeeds,
                    broken = repository.brokenOwnFeeds(s),
                    refreshing = l.refreshing,
                    offline = l.offline,
                    adding = l.adding,
                    addError = l.addError,
                    summaries = l.summaries,
                )
            }.collect { mutableState.value = it }
        }
    }

    /** Opening the screen: read the cache, then fetch what is older than the refresh interval. */
    fun start() {
        work.launch {
            repository.load()
            if (repository.stale()) runRefresh(force = false)
        }
    }

    /** The refresh button. */
    fun refresh() {
        work.launch { runRefresh(force = true) }
    }

    fun setMode(mode: NewsMode) {
        work.launch {
            repository.setMode(mode)
            runRefresh(force = false)
        }
    }

    fun addFeed(
        url: String,
        name: String? = null,
    ) {
        if (local.value.adding) return
        local.update { it.copy(adding = true, addError = null) }
        work.launch {
            val error =
                when (repository.addFeed(url, name)) {
                    is AddFeedResult.Added -> null
                    AddFeedResult.NotAFeed -> AddFeedError.NOT_A_FEED
                    AddFeedResult.Duplicate -> AddFeedError.DUPLICATE
                    AddFeedResult.Full -> AddFeedError.FULL
                    AddFeedResult.Offline -> AddFeedError.OFFLINE
                    AddFeedResult.Failed -> AddFeedError.FAILED
                }
            local.update { it.copy(adding = false, addError = error) }
        }
    }

    fun clearAddError() {
        local.update { it.copy(addError = null) }
    }

    fun renameFeed(
        id: String,
        name: String,
    ) {
        work.launch { repository.renameFeed(id, name) }
    }

    fun deleteFeed(id: String) {
        work.launch { repository.deleteFeed(id) }
    }

    /** "Zusammenfassen" on [item]; a second tap while it runs does nothing. */
    fun summarize(item: NewsItem) {
        val summarizer = summarizer ?: return
        if (local.value.summaries[item.guid] == SummaryState.Loading) return
        setSummary(item.guid, SummaryState.Loading)
        work.launch {
            val result = summarizer.summarize(item.link, item.title, lang())
            setSummary(item.guid, SummaryState.Ready(result))
        }
    }

    /** Folds a summary away again (it is gone for good: summaries are never stored). */
    fun hideSummary(guid: String) {
        local.update { it.copy(summaries = it.summaries - guid) }
    }

    private fun setSummary(
        guid: String,
        summary: SummaryState,
    ) {
        local.update { it.copy(summaries = it.summaries + (guid to summary)) }
    }

    private suspend fun runRefresh(force: Boolean) {
        local.update { it.copy(refreshing = true) }
        val result = repository.refresh(force)
        local.update { it.copy(refreshing = false, offline = result.offline) }
    }
}
