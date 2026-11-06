package app.hain.feature.news.api

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.hain.tools.internal.news.AddFeedResult
import app.hain.tools.internal.news.NewsFeed
import app.hain.tools.internal.news.NewsItem
import app.hain.tools.internal.news.NewsMode
import app.hain.tools.internal.news.NewsRepository
import app.hain.tools.internal.news.NewsSnapshot
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus

/** Why adding an own feed did not work (shown under the address field). */
enum class AddFeedError { NOT_A_FEED, DUPLICATE, FULL, OFFLINE, FAILED }

/** The article open in the reading view (T-076): its headline, and what loading it gave (`null` = loading). */
data class OpenArticle(
    val item: NewsItem,
    val result: ReaderResult? = null,
)

/** A question to Pim about the open article (T-078): running, or Pim's answer. */
data class AskState(
    val question: String,
    val result: AskResult? = null,
)

/** The daily briefing on top of the list (T-079). */
data class BriefingState(
    val briefing: Briefing? = null,
    val making: Boolean = false,
    val expanded: Boolean = false,
    /** T-265: there is a briefing maker, so the card can offer "Jetzt erstellen" before the first one. */
    val available: Boolean = false,
    /** T-265: the step while [making], and for [BriefingStep.FEEDS] how many feeds are done of how many. */
    val step: BriefingStep? = null,
    val done: Int = 0,
    val total: Int = 0,
)

/** Everything the news screen shows (T-073, T-075–T-079). */
data class NewsUiState(
    val mode: NewsMode = NewsMode.STANDARD,
    val items: List<NewsItem> = emptyList(),
    val ownFeeds: List<NewsFeed> = emptyList(),
    val broken: Set<String> = emptySet(),
    val refreshing: Boolean = false,
    val offline: Boolean = false,
    val adding: Boolean = false,
    val addError: AddFeedError? = null,
    val prefs: NewsPrefs = NewsPrefs(),
    val briefing: BriefingState = BriefingState(),
    val open: OpenArticle? = null,
    val ask: AskState? = null,
)

/**
 * News screen state: the mode, own feeds (add, rename, delete), refresh, pictures for cards without
 * one, the reading view, questions to Pim and the daily briefing. [scope] replaces `viewModelScope`
 * in tests. Without [reader], [asker] or [briefings] that part is simply not there.
 */
@Suppress("TooManyFunctions", "LongParameterList", "ReturnCount") // one function per screen action
class NewsViewModel(
    private val repository: NewsRepository,
    private val reader: ArticleReader? = null,
    private val asker: ArticleAsk? = null,
    private val briefings: BriefingMaker? = null,
    private val prefsStore: NewsPrefsStore = MemoryNewsPrefs(),
    private val lang: () -> String = { "de" },
    scope: CoroutineScope? = null,
) : ViewModel() {
    private data class Local(
        val refreshing: Boolean = false,
        val offline: Boolean = false,
        val adding: Boolean = false,
        val addError: AddFeedError? = null,
        val briefing: BriefingState = BriefingState(),
        val open: OpenArticle? = null,
        val ask: AskState? = null,
    )

    private val local = MutableStateFlow(Local(briefing = BriefingState(available = briefings != null)))

    // T-291: a feed, an article page or the brain that throws (a parser bug, a full disk) ends that one job:
    // spinners stop, an open article or question shows "failed", and the app stays open
    private val failed =
        CoroutineExceptionHandler { _, _ ->
            local.update { l ->
                l.copy(
                    refreshing = false,
                    adding = false,
                    briefing = l.briefing.copy(making = false, step = null),
                    open = l.open?.let { if (it.result == null) it.copy(result = ReaderResult.Failed) else it },
                    ask = l.ask?.let { if (it.result == null) it.copy(result = AskResult.Failed) else it },
                )
            }
        }
    private val work: CoroutineScope = (scope ?: viewModelScope) + failed
    private val mutableState = MutableStateFlow(NewsUiState())
    private var askJob: Job? = null
    private var openJob: Job? = null
    val state: StateFlow<NewsUiState> = mutableState.asStateFlow()

    init {
        work.launch {
            combine(repository.state, local, prefsStore.prefs) { s, l, p ->
                NewsUiState(
                    mode = s.mode,
                    // T-291: the list keys are feed + guid; a feed that repeats an entry would crash the list
                    items = NewsRepository.mixed(repository.items(s)).distinctBy { it.feedId to it.guid },
                    ownFeeds = s.ownFeeds,
                    broken = repository.brokenOwnFeeds(s),
                    refreshing = l.refreshing,
                    offline = l.offline,
                    adding = l.adding,
                    addError = l.addError,
                    prefs = p,
                    briefing = l.briefing,
                    open = l.open?.let { o -> fresh(o, s) },
                    ask = l.ask,
                )
            }.collect { mutableState.value = it }
        }
    }

    /** The open article with its headline as it is now (the paywall mark can change after reading). */
    private fun fresh(
        open: OpenArticle,
        s: NewsSnapshot,
    ): OpenArticle {
        val now = s.items.firstOrNull { it.guid == open.item.guid && it.feedId == open.item.feedId } ?: return open
        val locked = now.link != null && now.link in s.locked
        return open.copy(item = open.item.copy(paywall = locked, imageUrl = open.item.imageUrl ?: now.imageUrl))
    }

    /** Opening the screen: read the cache and today's briefing, then fetch what is older than the refresh interval. */
    fun start() {
        work.launch {
            repository.load()
            briefings?.cached()?.let { b -> local.update { it.copy(briefing = it.briefing.copy(briefing = b)) } }
            if (repository.stale()) runRefresh(force = false)
            makeBriefing(force = false)
        }
    }

    /** Pull to refresh and the refresh button. */
    fun refresh() {
        work.launch { runRefresh(force = true) }
    }

    fun setMode(mode: NewsMode) {
        work.launch {
            repository.setMode(mode)
            runRefresh(force = false)
        }
    }

    /** T-075: a card without a picture asks the article page for one (once, through the gate). */
    fun wantPicture(item: NewsItem) {
        val link = item.link ?: return
        if (item.imageUrl != null || !prefsStore.prefs.value.listImages) return
        work.launch { repository.lookUpPageImage(link) }
    }

    // ── reading view (T-076) ───────────────────────────────────────────────────

    /** Opens [item] in the reading view and loads the article. */
    fun open(item: NewsItem) {
        openJob?.cancel()
        askJob?.cancel()
        local.update { it.copy(open = OpenArticle(item), ask = null) }
        val reader = reader ?: return
        openJob =
            work.launch {
                val link = item.link
                val result = reader.read(link)
                if (result is ReaderResult.Ready && link != null) {
                    // T-077: the mark only when the text really is cut off; the page picture for the card
                    repository.markRead(link, result.article.locked, result.article.leadImage)
                }
                local.update { l ->
                    if (l.open?.item?.guid == item.guid) l.copy(open = OpenArticle(item, result)) else l
                }
            }
    }

    /** Back from the reading view. */
    fun close() {
        openJob?.cancel()
        askJob?.cancel()
        local.update { it.copy(open = null, ask = null) }
    }

    /** Opens the link of a briefing point in the reading view. */
    fun openLink(link: String?) {
        val item = state.value.items.firstOrNull { it.link == link } ?: return
        open(item)
    }

    // ── Pim about the article (T-078) ───────────────────────────────────────────

    fun ask(question: String) {
        val article = (local.value.open?.result as? ReaderResult.Ready)?.article ?: return
        val asker = asker ?: return
        val running = local.value.ask?.let { it.result == null } == true
        val q = question.trim()
        if (q.isEmpty() || running) return
        local.update { it.copy(ask = AskState(q)) }
        askJob =
            work.launch {
                val result = asker.ask(article, q, lang())
                local.update { l -> if (l.ask?.question == q) l.copy(ask = AskState(q, result)) else l }
            }
    }

    fun closeAsk() {
        askJob?.cancel()
        local.update { it.copy(ask = null) }
    }

    /** The text "Im Chat weiterführen" puts into a new chat, or `null` without an answer. */
    fun chatHandOff(): String? {
        val l = local.value
        val article = (l.open?.result as? ReaderResult.Ready)?.article ?: return null
        val ask = l.ask ?: return null
        val answer = (ask.result as? AskResult.Answer)?.text ?: return null
        return ArticleAsk.chatHandOff(article, ask.question, answer, lang())
    }

    // ── briefing (T-079) ──────────────────────────────────────────────────────

    /** "Neu erzeugen" / "Jetzt erstellen" (T-265): fresh feeds first, each step shown on the card. */
    fun remakeBriefing() {
        if (briefings == null || local.value.briefing.making) return
        work.launch {
            setStep(BriefingStep.FEEDS)
            local.update { it.copy(refreshing = true) }
            val result =
                repository.refresh(force = true) { done, total ->
                    local.update { l -> l.copy(briefing = l.briefing.copy(done = done, total = total)) }
                }
            local.update { it.copy(refreshing = false, offline = result.offline) }
            makeBriefing(force = true, already = true)
        }
    }

    private fun setStep(step: BriefingStep) {
        local.update { l -> l.copy(briefing = l.briefing.copy(making = true, step = step)) }
    }

    fun toggleBriefing() {
        local.update { it.copy(briefing = it.briefing.copy(expanded = !it.briefing.expanded)) }
    }

    /** [already]: [remakeBriefing] set "making" itself (its feed step came first). */
    private suspend fun makeBriefing(
        force: Boolean,
        already: Boolean = false,
    ) {
        val maker = briefings ?: return
        if (local.value.briefing.making && !already) return
        val items = repository.items()
        if (items.isEmpty()) {
            local.update { l -> l.copy(briefing = l.briefing.copy(making = false, step = null)) }
            return
        }
        local.update { it.copy(briefing = it.briefing.copy(making = true)) }
        val made = runCatching { maker.today(items, lang(), force = force, onStep = ::setStep) }.getOrNull()
        local.update { l ->
            l.copy(briefing = l.briefing.copy(briefing = made ?: l.briefing.briefing, making = false, step = null))
        }
    }

    // ── settings and own feeds ──────────────────────────────────────────────────

    fun updatePrefs(change: (NewsPrefs) -> NewsPrefs) {
        prefsStore.update(change)
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

    private suspend fun runRefresh(force: Boolean) {
        local.update { it.copy(refreshing = true) }
        val result = repository.refresh(force)
        local.update { it.copy(refreshing = false, offline = result.offline) }
        if (result.fetched > 0 && local.value.briefing.briefing == null) makeBriefing(force = false)
    }
}
