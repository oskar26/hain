@file:Suppress("MatchingDeclarationName") // the page and its actions

package app.hain.feature.news.api

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import app.hain.feature.news.R
import app.hain.tools.internal.news.NewsFeed
import app.hain.ui.HainIcons
import app.hain.ui.components.HainButton
import app.hain.ui.components.HainButtonStyle
import app.hain.ui.components.HainCard
import app.hain.ui.components.HainIconButton
import app.hain.ui.theme.HainLevel
import app.hain.ui.theme.LocalHainColors
import app.hain.ui.theme.LocalHainShapes
import app.hain.ui.theme.LocalHainSpacing
import app.hain.ui.theme.LocalHainTypography
import app.hain.ui.theme.hainSurface

/** What the own-feeds page can do (T-073): add by address, rename, delete. Nothing else. */
class OwnFeedActions(
    val onAdd: (String) -> Unit = {},
    val onRename: (String, String) -> Unit = { _, _ -> },
    val onDelete: (String) -> Unit = {},
    val onClearError: () -> Unit = {},
)

/**
 * The user's own feeds (NEWS.md §1): add one by its RSS/Atom address (checked before it is kept),
 * rename or delete it; a feed that stopped answering says so. The standard feeds are never listed
 * here and have no switches: the mode is the only choice (ADR-14).
 */
@Composable
internal fun OwnFeedsPage(
    state: NewsUiState,
    actions: OwnFeedActions,
    modifier: Modifier = Modifier,
) {
    val spacing = LocalHainSpacing.current
    val colors = LocalHainColors.current
    val type = LocalHainTypography.current
    var address by rememberSaveable { mutableStateOf("") }
    // a feed that was added empties the field; on an error the address stays to fix it
    LaunchedEffect(state.ownFeeds.size) { address = "" }
    Column(
        modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = spacing.lg),
        verticalArrangement = Arrangement.spacedBy(spacing.md),
    ) {
        BasicText(stringResource(R.string.news_feeds_hint), style = type.label.copy(color = colors.textMuted))
        TextInput(address, stringResource(R.string.news_feed_add_hint)) {
            address = it
            actions.onClearError()
        }
        state.addError?.let { error ->
            BasicText(stringResource(errorText(error)), style = type.bodySmall.copy(color = colors.danger))
        }
        HainButton(
            text = stringResource(if (state.adding) R.string.news_feed_adding else R.string.news_feed_add),
            onClick = { actions.onAdd(address) },
            modifier = Modifier.fillMaxWidth(),
            style = HainButtonStyle.SECONDARY,
            enabled = address.isNotBlank() && !state.adding,
        )
        HainCard {
            if (state.ownFeeds.isEmpty()) {
                BasicText(
                    stringResource(R.string.news_feeds_empty),
                    style = type.bodySmall.copy(color = colors.textMuted),
                )
            }
            state.ownFeeds.forEach { feed -> FeedRow(feed, feed.id in state.broken, actions) }
        }
    }
}

@Composable
private fun FeedRow(
    feed: NewsFeed,
    broken: Boolean,
    actions: OwnFeedActions,
) {
    val colors = LocalHainColors.current
    val type = LocalHainTypography.current
    var editing by rememberSaveable(feed.id) { mutableStateOf(false) }
    var name by rememberSaveable(feed.id) { mutableStateOf(feed.name) }
    if (editing) {
        Column(verticalArrangement = Arrangement.spacedBy(LocalHainSpacing.current.xs)) {
            TextInput(name, feed.name) { name = it }
            TextAction(stringResource(R.string.news_feed_save), onClick = {
                actions.onRename(feed.id, name)
                editing = false
            })
        }
        return
    }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            BasicText(feed.name, style = type.body.copy(color = colors.text), maxLines = 1)
            BasicText(
                if (broken) stringResource(R.string.news_feed_broken) else feed.url,
                style = type.label.copy(color = if (broken) colors.danger else colors.textMuted),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        HainIconButton(HainIcons.edit, stringResource(R.string.news_feed_rename), { editing = true })
        HainIconButton(HainIcons.delete, stringResource(R.string.news_feed_delete), { actions.onDelete(feed.id) })
    }
}

@Composable
private fun TextInput(
    value: String,
    hint: String,
    onChange: (String) -> Unit,
) {
    val colors = LocalHainColors.current
    val type = LocalHainTypography.current
    Box(
        Modifier
            .fillMaxWidth()
            .hainSurface(HainLevel.FLAT, LocalHainShapes.current.card)
            .padding(LocalHainSpacing.current.md),
    ) {
        if (value.isEmpty()) BasicText(hint, style = type.body.copy(color = colors.textMuted))
        BasicTextField(
            value = value,
            onValueChange = onChange,
            singleLine = true,
            textStyle = type.body.copy(color = colors.text),
            cursorBrush = SolidColor(colors.accent),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

private fun errorText(error: AddFeedError): Int =
    when (error) {
        AddFeedError.NOT_A_FEED -> R.string.news_feed_error_not_a_feed
        AddFeedError.DUPLICATE -> R.string.news_feed_error_duplicate
        AddFeedError.FULL -> R.string.news_feed_error_full
        AddFeedError.OFFLINE -> R.string.news_feed_error_offline
        AddFeedError.FAILED -> R.string.news_feed_error_failed
    }
