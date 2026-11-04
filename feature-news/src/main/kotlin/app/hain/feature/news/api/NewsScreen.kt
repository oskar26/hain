@file:Suppress("TooManyFunctions", "MatchingDeclarationName") // the news screen: list, rows, summary

package app.hain.feature.news.api

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.hain.cards.api.NewsTime
import app.hain.cards.api.PaywallBadge
import app.hain.feature.news.R
import app.hain.tools.internal.news.NewsItem
import app.hain.tools.internal.news.NewsMode
import app.hain.ui.HainIcons
import app.hain.ui.LocalExternalLaunch
import app.hain.ui.components.EmptyState
import app.hain.ui.components.HainIconButton
import app.hain.ui.components.SegmentedControl
import app.hain.ui.components.TopBar
import app.hain.ui.openExternally
import app.hain.ui.theme.LocalHainColors
import app.hain.ui.theme.LocalHainShapes
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
    val onSummarize: (NewsItem) -> Unit = {},
    val onHideSummary: (String) -> Unit = {},
    val feeds: OwnFeedActions = OwnFeedActions(),
)

/**
 * News (T-073, NEWS.md §1 and §6): a three-way mode switch, the headlines of today, yesterday and
 * before, refresh in the top bar, own feeds on their own page. A tap opens the article, a long press
 * shares its link, "Zusammenfassen" asks Pim for five points (T-074).
 */
@Composable
fun NewsScreen(
    viewModel: NewsViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val uri = LocalUriHandler.current
    val context = LocalContext.current
    val external = LocalExternalLaunch.current
    val shareTitle = stringResource(R.string.news_share)
    LaunchedEffect(Unit) { viewModel.start() }
    NewsBody(
        state = state,
        actions =
            NewsActions(
                onBack = onBack,
                onRefresh = viewModel::refresh,
                onMode = viewModel::setMode,
                onOpen = { item -> item.link?.let { link -> openExternally(external) { uri.openUri(link) } } },
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
                onSummarize = viewModel::summarize,
                onHideSummary = viewModel::hideSummary,
                feeds =
                    OwnFeedActions(
                        onAdd = { url -> viewModel.addFeed(url) },
                        onRename = viewModel::renameFeed,
                        onDelete = viewModel::deleteFeed,
                        onClearError = viewModel::clearAddError,
                    ),
            ),
        modifier = modifier,
    )
}

/** The news screen for a given [state] (also used by the screenshots and the UI test). */
@Composable
fun NewsBody(
    state: NewsUiState,
    actions: NewsActions,
    modifier: Modifier = Modifier,
    startOnFeeds: Boolean = false,
    now: Long = System.currentTimeMillis(),
) {
    var feedsOpen by rememberSaveable { mutableStateOf(startOnFeeds) }
    BackHandler { if (feedsOpen) feedsOpen = false else actions.onBack() }
    Column(
        modifier
            .fillMaxSize()
            .background(LocalHainColors.current.bg)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        if (feedsOpen) {
            TopBar(
                title = stringResource(R.string.news_own_feeds),
                leading = { HainIconButton(HainIcons.back, stringResource(R.string.news_back), { feedsOpen = false }) },
            )
            OwnFeedsPage(state, actions.feeds, Modifier.weight(1f))
        } else {
            TopBar(
                title = stringResource(R.string.news_title),
                leading = { HainIconButton(HainIcons.back, stringResource(R.string.news_back), actions.onBack) },
                trailing = {
                    HainIconButton(
                        HainIcons.retry,
                        stringResource(R.string.news_refresh),
                        actions.onRefresh,
                        enabled = !state.refreshing,
                    )
                },
            )
            NewsList(state, actions, now, Modifier.weight(1f)) { feedsOpen = true }
        }
    }
}

@Composable
private fun NewsList(
    state: NewsUiState,
    actions: NewsActions,
    now: Long,
    modifier: Modifier = Modifier,
    onOpenFeeds: () -> Unit,
) {
    val spacing = LocalHainSpacing.current
    val colors = LocalHainColors.current
    val type = LocalHainTypography.current
    val zone = ZoneId.systemDefault()
    val locale = LocalConfiguration.current.locales[0] ?: Locale.getDefault()
    val groups = NewsGroups.group(state.items, now, zone)
    LazyColumn(
        modifier.fillMaxWidth(),
        contentPadding = PaddingValues(start = spacing.lg, end = spacing.lg, bottom = spacing.xl),
        verticalArrangement = Arrangement.spacedBy(spacing.sm),
    ) {
        item(key = "modes") {
            Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
                SegmentedControl(
                    options =
                        listOf(
                            stringResource(R.string.news_mode_standard),
                            stringResource(R.string.news_mode_plus_own),
                            stringResource(R.string.news_mode_own),
                        ),
                    selected = NewsMode.entries.indexOf(state.mode),
                    onSelect = { actions.onMode(NewsMode.entries[it]) },
                )
                if (state.mode.usesOwn) {
                    BasicText(
                        stringResource(R.string.news_own_feeds_count, state.ownFeeds.size),
                        style = type.bodySmall.copy(color = colors.accent),
                        modifier =
                            Modifier
                                .clickable(role = Role.Button, onClick = onOpenFeeds)
                                .padding(vertical = spacing.xs),
                    )
                } else {
                    BasicText(stringResource(R.string.news_mix_hint), style = type.label.copy(color = colors.textMuted))
                }
                when {
                    state.offline -> Note(stringResource(R.string.news_offline))
                    state.refreshing -> Note(stringResource(R.string.news_loading))
                }
            }
        }
        if (groups.isEmpty() && !state.refreshing) {
            item(key = "empty") { EmptyNews(state, onOpenFeeds) }
        }
        groups.forEach { (day, dayItems) ->
            item(key = "day-${day.name}") { DayHeader(day) }
            items(dayItems, key = { "${it.feedId}/${it.guid}" }) { item ->
                NewsRow(
                    item = item,
                    time = NewsTime.label(item.published, now, zone, locale),
                    summary = state.summaries[item.guid],
                    actions = actions,
                )
            }
        }
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
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
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
        modifier = Modifier.padding(top = LocalHainSpacing.current.md).semantics { heading() },
    )
}

/** One headline: source, time and paywall badge, the title, a teaser of up to 3 lines, "Zusammenfassen". */
@Composable
private fun NewsRow(
    item: NewsItem,
    time: String?,
    summary: SummaryState?,
    actions: NewsActions,
) {
    val colors = LocalHainColors.current
    val type = LocalHainTypography.current
    val spacing = LocalHainSpacing.current
    val shareLabel = stringResource(R.string.news_share)
    Column(
        Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClickLabel = item.link,
                onLongClickLabel = shareLabel,
                onLongClick = { actions.onShare(item) },
                onClick = { actions.onOpen(item) },
            ).padding(vertical = spacing.sm),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            val meta = listOfNotNull(item.source, time).joinToString(" · ")
            BasicText(meta, style = type.label.copy(color = colors.textMuted), maxLines = 1)
            if (item.paywall) PaywallBadge()
        }
        BasicText(
            item.title,
            style = type.body.copy(color = colors.text),
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
        )
        if (item.summary.isNotBlank()) {
            BasicText(
                item.summary,
                style = type.bodySmall.copy(color = colors.textMuted),
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
        }
        SummaryPart(item, summary, actions)
    }
}

@Composable
private fun SummaryPart(
    item: NewsItem,
    summary: SummaryState?,
    actions: NewsActions,
) {
    val colors = LocalHainColors.current
    val type = LocalHainTypography.current
    val spacing = LocalHainSpacing.current
    when (summary) {
        null -> TextAction(stringResource(R.string.news_summarize)) { actions.onSummarize(item) }
        SummaryState.Loading -> Note(stringResource(R.string.news_summarizing))
        is SummaryState.Ready -> {
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(colors.surface, LocalHainShapes.current.card)
                    .padding(spacing.md),
                verticalArrangement = Arrangement.spacedBy(spacing.xs),
            ) {
                when (val result = summary.result) {
                    is SummaryResult.Bullets ->
                        result.points.forEach { point ->
                            BasicText("•  $point", style = type.bodySmall.copy(color = colors.text))
                        }
                    SummaryResult.NotReadable -> {
                        BasicText(
                            stringResource(R.string.news_not_readable),
                            style = type.bodySmall.copy(color = colors.text),
                        )
                        if (item.link != null) {
                            TextAction(stringResource(R.string.news_open_link)) { actions.onOpen(item) }
                        }
                    }
                    SummaryResult.Offline -> Note(stringResource(R.string.news_summary_offline))
                    SummaryResult.NoBrain -> Note(stringResource(R.string.news_summary_no_brain))
                    SummaryResult.Failed -> Note(stringResource(R.string.news_summary_failed))
                }
                TextAction(stringResource(R.string.news_summary_hide)) { actions.onHideSummary(item.guid) }
            }
        }
    }
}

/** Quiet green text button ("Zusammenfassen", "Im Browser öffnen"). */
@Composable
internal fun TextAction(
    text: String,
    onClick: () -> Unit,
) {
    BasicText(
        text,
        style = LocalHainTypography.current.label.copy(color = LocalHainColors.current.accent),
        modifier = Modifier.clickable(role = Role.Button, onClick = onClick).padding(vertical = 6.dp),
    )
}
