@file:Suppress("TooManyFunctions", "MatchingDeclarationName") // the news screen: pages, list, layout

package app.hain.feature.news.api

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.hain.cards.api.NewsTime
import app.hain.feature.news.R
import app.hain.tools.internal.news.NewsItem
import app.hain.tools.internal.news.NewsMode
import app.hain.ui.HainIcons
import app.hain.ui.LocalExternalLaunch
import app.hain.ui.components.EmptyState
import app.hain.ui.components.HainIconButton
import app.hain.ui.components.TopBar
import app.hain.ui.effects.hainGlass
import app.hain.ui.effects.hainGlassSource
import app.hain.ui.effects.rememberHainGlassState
import app.hain.ui.openExternally
import app.hain.ui.theme.LocalHainColors
import app.hain.ui.theme.LocalHainSpacing
import app.hain.ui.theme.LocalHainTypography
import java.time.ZoneId
import java.util.Locale

/** What the news screen can do; the screen itself keeps no state but the open page. */
@Suppress("LongParameterList") // one callback per action
class NewsActions(
    val onBack: () -> Unit = {},
    val onRefresh: () -> Unit = {},
    val onMode: (NewsMode) -> Unit = {},
    val onOpen: (NewsItem) -> Unit = {},
    val onShare: (NewsItem) -> Unit = {},
    val onOpenWebsite: (NewsItem) -> Unit = {},
    val onWantPicture: (NewsItem) -> Unit = {},
    val onCloseArticle: () -> Unit = {},
    val onAsk: (String) -> Unit = {},
    val onCloseAsk: () -> Unit = {},
    val onContinueInChat: () -> Unit = {},
    val onRemakeBriefing: () -> Unit = {},
    val onToggleBriefing: () -> Unit = {},
    val onOpenLink: (String?) -> Unit = {},
    val onPrefs: ((NewsPrefs) -> NewsPrefs) -> Unit = {},
    val onBriefingNote: (Boolean) -> Unit = {},
    val feeds: OwnFeedActions = OwnFeedActions(),
)

/** Pages of the news screen besides the list. */
enum class NewsPage { LIST, FEEDS, SETTINGS }

/**
 * News as an RSS reader (T-073, T-075–T-079): frosted top bar, mode chips, "Dein Briefing
 * heute", a big top story and cards with pictures, pull to refresh. A tap opens the reading view
 * in the app; Pim answers questions about the article. Phones show one column, from 840 dp the list
 * sits left and the article right. [onContinueInChat] gets the text for a new chat.
 */
@Suppress("LongParameterList", "LongMethod") // the app's hooks; the actions are wired in one place
@Composable
fun NewsScreen(
    viewModel: NewsViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    images: NewsImages? = null,
    onContinueInChat: (String) -> Unit = {},
    onBriefingSchedule: (Boolean) -> Unit = {},
    startPage: NewsPage = NewsPage.LIST,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val uri = LocalUriHandler.current
    val context = LocalContext.current
    val external = LocalExternalLaunch.current
    val shareTitle = stringResource(R.string.news_share)
    LaunchedEffect(Unit) { viewModel.start() }
    val permission =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            viewModel.updatePrefs { it.copy(briefingNotification = granted) }
            onBriefingSchedule(granted)
        }
    val actions =
        remember(viewModel) {
            NewsActions(
                onBack = onBack,
                onRefresh = viewModel::refresh,
                onMode = viewModel::setMode,
                onOpen = viewModel::open,
                onShare = { item ->
                    item.link?.let { link ->
                        val send =
                            Intent(Intent.ACTION_SEND)
                                .setType("text/plain")
                                .putExtra(Intent.EXTRA_SUBJECT, item.title)
                                .putExtra(Intent.EXTRA_TEXT, link)
                        openExternally(external) { context.startActivity(Intent.createChooser(send, shareTitle)) }
                    }
                },
                onOpenWebsite = { item -> item.link?.let { link -> openExternally(external) { uri.openUri(link) } } },
                onWantPicture = viewModel::wantPicture,
                onCloseArticle = viewModel::close,
                onAsk = viewModel::ask,
                onCloseAsk = viewModel::closeAsk,
                onContinueInChat = { viewModel.chatHandOff()?.let(onContinueInChat) },
                onRemakeBriefing = viewModel::remakeBriefing,
                onToggleBriefing = viewModel::toggleBriefing,
                onOpenLink = viewModel::openLink,
                onPrefs = viewModel::updatePrefs,
                onBriefingNote = { on ->
                    val needsAsk =
                        on &&
                            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
                            PackageManager.PERMISSION_GRANTED
                    if (needsAsk) {
                        permission.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        viewModel.updatePrefs { it.copy(briefingNotification = on) }
                        onBriefingSchedule(on)
                    }
                },
                feeds =
                    OwnFeedActions(
                        onAdd = { url -> viewModel.addFeed(url) },
                        onRename = viewModel::renameFeed,
                        onDelete = viewModel::deleteFeed,
                        onClearError = viewModel::clearAddError,
                    ),
            )
        }
    CompositionLocalProvider(LocalNewsImages provides images) {
        NewsBody(state = state, actions = actions, modifier = modifier, startPage = startPage)
    }
}

/** The news screen for a given [state] (also used by the screenshots and the UI test). */
@Suppress("LongParameterList")
@Composable
fun NewsBody(
    state: NewsUiState,
    actions: NewsActions,
    modifier: Modifier = Modifier,
    startOnFeeds: Boolean = false,
    startPage: NewsPage = if (startOnFeeds) NewsPage.FEEDS else NewsPage.LIST,
    now: Long = System.currentTimeMillis(),
    startWithAsk: Boolean = false,
) {
    var page by rememberSaveable { mutableStateOf(startPage) }
    // the list keeps its place while an article is open on a phone
    val listState = rememberLazyListState()
    BoxWithConstraints(modifier.fillMaxSize().background(LocalHainColors.current.bg)) {
        val wide = maxWidth >= WIDE_FROM.dp
        val open = state.open
        // T-266: opened right on the settings (from Settings → Nachrichten), back leaves the news again
        val leave: () -> Unit = {
            if (startPage == NewsPage.SETTINGS) {
                actions.onBack()
            } else {
                page = NewsPage.LIST
            }
        }
        BackHandler(enabled = page != NewsPage.LIST) { leave() }
        BackHandler(enabled = page == NewsPage.LIST && open == null) { actions.onBack() }
        when {
            page != NewsPage.LIST -> SubPage(page, state, actions) { leave() }
            wide -> {
                // T-076: list on the left, the article on the right (list-detail from 840 dp)
                val listWidth = (maxWidth * LIST_SHARE).coerceIn(LIST_MIN.dp, LIST_MAX.dp)
                Row(Modifier.fillMaxSize()) {
                    NewsList(state, actions, now, listState, { page = it }, Modifier.width(listWidth).fillMaxHeight())
                    Box(Modifier.width(1.dp).fillMaxHeight().background(LocalHainColors.current.border))
                    Box(Modifier.weight(1f).fillMaxHeight()) {
                        if (open != null) {
                            ReaderPane(open, state.prefs, state.ask, actions, now, startWithAsk = startWithAsk)
                        } else {
                            ReaderPlaceholder()
                        }
                    }
                }
            }
            open != null -> ReaderPane(open, state.prefs, state.ask, actions, now, startWithAsk = startWithAsk)
            else -> NewsList(state, actions, now, listState, { page = it }, Modifier.fillMaxSize())
        }
    }
}

@Composable
private fun SubPage(
    page: NewsPage,
    state: NewsUiState,
    actions: NewsActions,
    onDone: () -> Unit,
) {
    var current by rememberSaveable { mutableStateOf(page) }
    BackHandler(enabled = current == NewsPage.FEEDS && page == NewsPage.SETTINGS) { current = NewsPage.SETTINGS }
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        val title = if (current == NewsPage.FEEDS) R.string.news_own_feeds else R.string.news_settings_title
        TopBar(
            title = stringResource(title),
            leading = {
                HainIconButton(HainIcons.back, stringResource(R.string.news_back), {
                    if (current == NewsPage.FEEDS && page == NewsPage.SETTINGS) {
                        current = NewsPage.SETTINGS
                    } else {
                        onDone()
                    }
                })
            },
        )
        Hairline()
        when (current) {
            NewsPage.FEEDS -> OwnFeedsPage(state, actions.feeds, Modifier.weight(1f))
            else ->
                NewsSettingsPage(
                    prefs = state.prefs,
                    onPrefs = actions.onPrefs,
                    onBriefingNote = actions.onBriefingNote,
                    onOpenFeeds = { current = NewsPage.FEEDS },
                    modifier = Modifier.weight(1f),
                )
        }
    }
}

@Suppress("LongMethod", "LongParameterList")
@Composable
private fun NewsList(
    state: NewsUiState,
    actions: NewsActions,
    now: Long,
    listState: LazyListState,
    onPage: (NewsPage) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = LocalHainSpacing.current
    val zone = ZoneId.systemDefault()
    val locale = LocalConfiguration.current.locales[0] ?: Locale.getDefault()
    val glass = rememberHainGlassState()
    val top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + BAR.dp
    val bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + spacing.xl
    val pictures = state.prefs.listImages
    val hero = if (pictures) state.items.firstOrNull { it.imageUrl != null && it.time >= now - HERO_MAX_AGE } else null
    val rest = state.items.filterNot { it === hero }
    val groups = NewsGroups.group(rest, now, zone)
    val selected = state.open?.item?.guid
    Box(modifier) {
        PullRefresh(
            refreshing = state.refreshing,
            onRefresh = actions.onRefresh,
            indicatorTop = top,
            modifier = Modifier.fillMaxSize(),
        ) {
            LazyColumn(
                Modifier.fillMaxSize().hainGlassSource(glass),
                state = listState,
                contentPadding = PaddingValues(start = spacing.md, end = spacing.md, top = top, bottom = bottom),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                item(key = "modes") {
                    Column(
                        Modifier.padding(vertical = spacing.sm),
                        verticalArrangement = Arrangement.spacedBy(spacing.sm),
                    ) {
                        ModeChips(state.mode, state.ownFeeds.size, actions.onMode, { onPage(NewsPage.FEEDS) })
                        when {
                            state.offline -> Note(stringResource(R.string.news_offline))
                            state.refreshing && state.items.isEmpty() -> Note(stringResource(R.string.news_loading))
                        }
                    }
                }
                item(key = "briefing") {
                    BriefingCard(
                        state.briefing,
                        actions.onRemakeBriefing,
                        actions.onToggleBriefing,
                        actions.onOpenLink,
                        Modifier.padding(bottom = spacing.md),
                    )
                }
                if (hero != null) {
                    item(key = "hero/${hero.feedId}/${hero.guid}") {
                        HeroCard(
                            hero,
                            NewsTime.label(hero.published, now, zone, locale),
                            actions,
                            Modifier.padding(bottom = spacing.sm),
                        )
                    }
                }
                if (state.items.isEmpty() && !state.refreshing) {
                    item(key = "empty") { EmptyNews(state) { onPage(NewsPage.FEEDS) } }
                }
                groups.forEach { (day, dayItems) ->
                    item(key = "day-${day.name}") { DayHeader(day) }
                    items(dayItems, key = { "${it.feedId}/${it.guid}" }) { item ->
                        Column {
                            NewsCard(
                                item = item,
                                time = NewsTime.label(item.published, now, zone, locale),
                                pictures = pictures,
                                selected = item.guid == selected,
                                actions = actions,
                            )
                            Hairline(Modifier.padding(horizontal = spacing.sm))
                        }
                    }
                }
            }
        }
        ListTopBar(
            refreshing = state.refreshing,
            actions = actions,
            onSettings = { onPage(NewsPage.SETTINGS) },
            modifier = Modifier.align(Alignment.TopCenter).hainGlass(glass).statusBarsPadding(),
        )
    }
}

/** Frosted glass top bar (Lichtung): back, "Nachrichten", settings and refresh. */
@Composable
private fun ListTopBar(
    refreshing: Boolean,
    actions: NewsActions,
    onSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxWidth()) {
        TopBar(
            title = stringResource(R.string.news_title),
            leading = { HainIconButton(HainIcons.back, stringResource(R.string.news_back), actions.onBack) },
            trailing = {
                Row {
                    HainIconButton(HainIcons.settings, stringResource(R.string.news_settings), onSettings)
                    HainIconButton(
                        HainIcons.retry,
                        stringResource(R.string.news_refresh),
                        actions.onRefresh,
                        enabled = !refreshing,
                    )
                }
            },
        )
        Hairline()
    }
}

@Composable
private fun Note(text: String) {
    BasicText(text, style = LocalHainTypography.current.label.copy(color = LocalHainColors.current.textMuted))
}

@Composable
private fun EmptyNews(
    state: NewsUiState,
    onOpenFeeds: () -> Unit,
) {
    val ownOnlyEmpty = state.mode == NewsMode.OWN_ONLY && state.ownFeeds.isEmpty()
    Box(Modifier.fillMaxWidth().padding(top = 48.dp), contentAlignment = Alignment.Center) {
        EmptyState(
            title = stringResource(if (ownOnlyEmpty) R.string.news_empty_own else R.string.news_empty),
            body = stringResource(if (ownOnlyEmpty) R.string.news_empty_own_hint else R.string.news_empty_hint),
            modifier = if (ownOnlyEmpty) Modifier.clickable(role = Role.Button, onClick = onOpenFeeds) else Modifier,
        )
    }
}

@Composable
private fun DayHeader(day: NewsDay) {
    val label =
        when (day) {
            NewsDay.TODAY -> R.string.news_today
            NewsDay.YESTERDAY -> R.string.news_yesterday
            NewsDay.OLDER -> R.string.news_older
        }
    BasicText(
        stringResource(label),
        style = LocalHainTypography.current.label.copy(color = LocalHainColors.current.textMuted),
        modifier = Modifier.padding(top = LocalHainSpacing.current.md, start = 8.dp).semantics { heading() },
    )
}

private const val BAR = 56
private const val WIDE_FROM = 840
private const val LIST_SHARE = 0.42f
private const val LIST_MIN = 340
private const val LIST_MAX = 460
private const val HERO_MAX_AGE = 36L * 60 * 60 * 1000
