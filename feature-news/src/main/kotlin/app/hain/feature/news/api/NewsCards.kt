@file:Suppress("TooManyFunctions") // the parts of the news list: chips, briefing, top story, cards

package app.hain.feature.news.api

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.hain.cards.api.PaywallBadge
import app.hain.feature.news.R
import app.hain.tools.internal.news.NewsItem
import app.hain.tools.internal.news.NewsMode
import app.hain.ui.HainIcons
import app.hain.ui.components.HainChip
import app.hain.ui.components.HainIconButton
import app.hain.ui.theme.HainLevel
import app.hain.ui.theme.LocalHainColors
import app.hain.ui.theme.LocalHainShapes
import app.hain.ui.theme.LocalHainSpacing
import app.hain.ui.theme.LocalHainTypography
import app.hain.ui.theme.hainSurface

/**
 * The mode choice (NEWS.md §1, ADR-14) as one line of chips that never wraps: on a narrow phone or
 * with big text the line scrolls sideways instead of breaking "Standard + eigene" in two (Oskar's
 * screenshot 04.11.). With own feeds in use a fourth chip opens "Meine Feeds".
 */
@Composable
internal fun ModeChips(
    mode: NewsMode,
    ownFeeds: Int,
    onMode: (NewsMode) -> Unit,
    onOpenFeeds: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val labels =
        listOf(
            NewsMode.STANDARD to stringResource(R.string.news_mode_standard),
            NewsMode.STANDARD_PLUS_OWN to stringResource(R.string.news_mode_plus_own),
            NewsMode.OWN_ONLY to stringResource(R.string.news_mode_own),
        )
    Row(
        modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(LocalHainSpacing.current.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        labels.forEach { (m, label) ->
            HainChip(
                text = label,
                selected = m == mode,
                onClick = { onMode(m) },
                modifier = Modifier.semantics { selected = m == mode },
            )
        }
        if (mode.usesOwn) {
            HainChip(text = stringResource(R.string.news_own_feeds_count, ownFeeds), onClick = onOpenFeeds)
        }
    }
}

/** "Dein Briefing heute" (T-079): numbered points with their sources; tap opens the story. */
@Suppress("LongMethod")
@Composable
internal fun BriefingCard(
    state: BriefingState,
    onRemake: () -> Unit,
    onToggle: () -> Unit,
    onOpenLink: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalHainColors.current
    val type = LocalHainTypography.current
    val spacing = LocalHainSpacing.current
    val briefing = state.briefing
    // T-265: before the first briefing the card offers to make one now
    if (briefing == null && !state.making && !state.available) return
    Column(
        modifier
            .fillMaxWidth()
            .hainSurface(HainLevel.RAISED, LocalHainShapes.current.card, fill = colors.surface)
            .padding(spacing.md)
            .animateContentSize(),
        verticalArrangement = Arrangement.spacedBy(spacing.sm),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(
                painterResource(R.drawable.ic_news_sparkles),
                contentDescription = null,
                colorFilter = ColorFilter.tint(colors.accent),
                modifier = Modifier.size(18.dp),
            )
            Column(Modifier.weight(1f).padding(start = spacing.sm)) {
                BasicText(
                    stringResource(R.string.news_briefing_title),
                    style = type.button.copy(color = colors.text),
                    modifier = Modifier.semantics { heading() },
                )
                BasicText(
                    stringResource(R.string.news_briefing_local),
                    style = type.label.copy(color = colors.textMuted),
                )
            }
            HainIconButton(
                HainIcons.retry,
                stringResource(R.string.news_briefing_remake),
                onRemake,
                enabled = !state.making,
                tint = colors.textMuted,
                iconSize = 18.dp,
            )
        }
        if (state.making) BriefingProgress(state)
        if (briefing == null) {
            if (!state.making) TextAction(stringResource(R.string.news_briefing_make_now), onRemake)
            return@Column
        }
        val shown = if (state.expanded) briefing.points else briefing.points.take(COLLAPSED_POINTS)
        shown.forEachIndexed { i, point ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(LocalHainShapes.current.row)
                    .clickable(enabled = point.link != null, role = Role.Button) { onOpenLink(point.link) }
                    .padding(vertical = 6.dp),
            ) {
                BasicText(
                    "${i + 1}",
                    style = type.button.copy(color = colors.accent),
                    modifier = Modifier.width(22.dp),
                )
                Column(Modifier.weight(1f)) {
                    BasicText(point.text, style = type.bodySmall.copy(color = colors.text))
                    BasicText(
                        point.sources.joinToString(" · "),
                        style = type.label.copy(color = colors.textMuted),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
        if (briefing.points.size > COLLAPSED_POINTS) {
            TextAction(
                stringResource(if (state.expanded) R.string.news_briefing_less else R.string.news_briefing_more),
                onToggle,
            )
        }
    }
}

/** T-265: the step the briefing is at ("Feeds laden 3/19", "Artikel lesen", "Zusammenfassen"), with a bar. */
@Composable
private fun BriefingProgress(state: BriefingState) {
    val colors = LocalHainColors.current
    val text =
        when (state.step) {
            BriefingStep.FEEDS -> stringResource(R.string.news_briefing_step_feeds, state.done, state.total)
            BriefingStep.READING -> stringResource(R.string.news_briefing_step_reading)
            BriefingStep.SUMMING_UP -> stringResource(R.string.news_briefing_step_summing)
            null -> stringResource(R.string.news_briefing_making)
        }
    val share =
        when (state.step) {
            BriefingStep.FEEDS -> if (state.total > 0) FEEDS_SHARE * state.done / state.total else 0f
            BriefingStep.READING -> FEEDS_SHARE
            BriefingStep.SUMMING_UP -> READ_SHARE
            null -> 0f
        }
    BasicText(text, style = LocalHainTypography.current.label.copy(color = colors.textMuted))
    Box(
        Modifier
            .fillMaxWidth()
            .height(BAR.dp)
            .clip(RoundedCornerShape(BAR.dp))
            .background(colors.border),
    ) {
        Box(Modifier.fillMaxWidth(share).height(BAR.dp).background(colors.accent))
    }
}

private const val FEEDS_SHARE = 0.5f
private const val READ_SHARE = 0.65f
private const val BAR = 3

/** The big top story (T-075): the picture across the whole width, then source, headline and teaser. */
@Composable
internal fun HeroCard(
    item: NewsItem,
    time: String?,
    actions: NewsActions,
    modifier: Modifier = Modifier,
) {
    val colors = LocalHainColors.current
    val type = LocalHainTypography.current
    val spacing = LocalHainSpacing.current
    val shape = LocalHainShapes.current.card
    Column(
        modifier
            .fillMaxWidth()
            .hainSurface(HainLevel.RAISED, shape, fill = colors.bg)
            .clip(shape)
            .combinedClickable(
                onClickLabel = stringResource(R.string.news_top_story),
                onLongClickLabel = stringResource(R.string.news_share),
                onLongClick = { actions.onShare(item) },
                onClick = { actions.onOpen(item) },
            ),
    ) {
        NewsPicture(item.imageUrl, Modifier.fillMaxWidth().aspectRatio(HERO_RATIO))
        Column(Modifier.padding(spacing.md), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Meta(item, time)
            BasicText(
                item.title,
                style = type.title.copy(color = colors.text),
                maxLines = 4,
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
        }
    }
}

/**
 * One headline (T-075): source, time, the "Paywall" mark only for an article found cut off (T-077),
 * the headline and a teaser, the picture on the right. Tap opens the reading view, long press
 * shares the link. A card without a picture asks the article page for one once ([NewsActions.onWantPicture]).
 */
@Suppress("LongParameterList")
@Composable
internal fun NewsCard(
    item: NewsItem,
    time: String?,
    pictures: Boolean,
    selected: Boolean,
    actions: NewsActions,
    modifier: Modifier = Modifier,
) {
    val colors = LocalHainColors.current
    val type = LocalHainTypography.current
    val spacing = LocalHainSpacing.current
    if (pictures && item.imageUrl == null) {
        LaunchedEffect(item.guid) { actions.onWantPicture(item) }
    }
    Row(
        modifier
            .fillMaxWidth()
            .clip(LocalHainShapes.current.row)
            .background(if (selected) colors.accentSoft else colors.bg)
            .combinedClickable(
                onClickLabel = item.title,
                onLongClickLabel = stringResource(R.string.news_share),
                onLongClick = { actions.onShare(item) },
                onClick = { actions.onOpen(item) },
            ).padding(horizontal = spacing.sm, vertical = spacing.md),
        horizontalArrangement = Arrangement.spacedBy(spacing.md),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Meta(item, time)
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
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (pictures && item.imageUrl != null) {
            NewsPicture(
                item.imageUrl,
                Modifier.padding(top = 2.dp).size(THUMB.dp).clip(LocalHainShapes.current.chip),
                maxWidth = NewsImages.THUMB_WIDTH,
            )
        }
    }
}

@Composable
private fun Meta(
    item: NewsItem,
    time: String?,
) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        val meta = listOfNotNull(item.source, time).joinToString(" · ")
        BasicText(
            meta,
            style = LocalHainTypography.current.label.copy(color = LocalHainColors.current.textMuted),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false),
        )
        if (item.paywall) PaywallBadge()
    }
}

/** Thin line between cards. */
@Composable
internal fun Hairline(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().height(1.dp).background(LocalHainColors.current.border))
}

/**
 * Quiet green text button ("Alle Punkte zeigen", "Auf Website öffnen", "Zusammenfassen"). T-238: the text stays
 * small, the touch target is 48 dp high and at least 48 dp wide.
 */
@Composable
internal fun TextAction(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier
            .clip(LocalHainShapes.current.row)
            .clickable(role = Role.Button, onClick = onClick)
            .defaultMinSize(minWidth = MIN_TOUCH.dp, minHeight = MIN_TOUCH.dp)
            .padding(horizontal = 6.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        BasicText(text, style = LocalHainTypography.current.label.copy(color = LocalHainColors.current.accent))
    }
}

private const val COLLAPSED_POINTS = 3
private const val HERO_RATIO = 16f / 9f
private const val THUMB = 84
private const val MIN_TOUCH = 48
