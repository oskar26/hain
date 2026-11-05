package app.hain.feature.chat.api

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.hain.feature.chat.R
import app.hain.feature.chat.internal.SourceRef
import app.hain.tools.api.FaviconRules
import app.hain.tools.api.Favicons
import app.hain.ui.HainIcons
import app.hain.ui.motion.HainMotion
import app.hain.ui.motion.LocalReduceMotion
import app.hain.ui.motion.hainTween
import app.hain.ui.theme.HainLevel
import app.hain.ui.theme.LocalHainColors
import app.hain.ui.theme.LocalHainSpacing
import app.hain.ui.theme.LocalHainTypography
import app.hain.ui.theme.hainSurface

/**
 * T-274: the pages an answer is based on, as one quiet line under it: the site icons on top of each
 * other and "3 Quellen". A tap opens the list (number, icon, title, site); a tap on a row opens the page.
 * The numbers match the green chips in the text.
 */
@Composable
internal fun SourceChips(
    sources: List<SourceRef>,
    modifier: Modifier = Modifier,
    favicons: Favicons? = null,
    initiallyOpen: Boolean = false,
) {
    if (sources.isEmpty()) return
    val colors = LocalHainColors.current
    val type = LocalHainTypography.current
    val spacing = LocalHainSpacing.current
    val reduce = LocalReduceMotion.current
    var open by rememberSaveable(sources) { mutableStateOf(initiallyOpen) }
    val opened = stringResource(R.string.chat_sources_open)
    val closed = stringResource(R.string.chat_sources_closed)
    Column(modifier.fillMaxWidth().padding(top = spacing.xs)) {
        Row(
            Modifier
                .clip(RoundedCornerShape(ROW_CORNER.dp))
                .clickable(role = Role.Button) { open = !open }
                .semantics { stateDescription = if (open) opened else closed }
                .heightIn(min = ROW_MIN.dp)
                .padding(end = spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StackedIcons(sources, favicons)
            Spacer(Modifier.width(spacing.sm))
            BasicText(
                pluralStringResource(R.plurals.chat_tool_sources, sources.size, sources.size),
                style = type.label.copy(color = colors.textMuted),
            )
            Spacer(Modifier.width(2.dp))
            Image(
                HainIcons.chevronRight,
                contentDescription = null,
                colorFilter = ColorFilter.tint(colors.textMuted),
                modifier = Modifier.size(CHEVRON.dp).rotate(if (open) -QUARTER else QUARTER),
            )
        }
        AnimatedVisibility(
            visible = open,
            enter =
                fadeIn(hainTween(HainMotion.FAST_MS, reduce)) +
                    expandVertically(hainTween(HainMotion.FAST_MS, reduce)),
            exit =
                fadeOut(hainTween(HainMotion.FAST_MS, reduce)) +
                    shrinkVertically(hainTween(HainMotion.FAST_MS, reduce)),
        ) {
            Column(
                Modifier
                    .padding(top = spacing.xs)
                    .fillMaxWidth()
                    .hainSurface(HainLevel.FLAT, RoundedCornerShape(LIST_CORNER.dp))
                    .padding(vertical = spacing.xs),
            ) {
                sources.forEachIndexed { index, source -> SourceRow(index + 1, source, favicons) }
            }
        }
    }
}

/** Up to four site icons, each a little over the one before (like the sources line in ChatGPT). */
@Composable
private fun StackedIcons(
    sources: List<SourceRef>,
    favicons: Favicons?,
) {
    val colors = LocalHainColors.current
    val shown = sources.distinctBy { FaviconRules.host(it.url) ?: it.url }.take(MAX_ICONS)
    Box(Modifier.width((STACK_ICON + (shown.size - 1) * STACK_STEP).dp)) {
        shown.forEachIndexed { i, source ->
            Box(
                Modifier
                    .offset(x = (i * STACK_STEP).dp)
                    .size(STACK_ICON.dp)
                    .clip(CircleShape)
                    .background(colors.bg)
                    .border(RING.dp, colors.bg, CircleShape),
                contentAlignment = Alignment.Center,
            ) { SourceIcon(source.url, favicons, Modifier.clip(CircleShape)) }
        }
    }
}

@Composable
private fun SourceRow(
    number: Int,
    source: SourceRef,
    favicons: Favicons?,
) {
    val context = LocalContext.current
    val colors = LocalHainColors.current
    val type = LocalHainTypography.current
    val spacing = LocalHainSpacing.current
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button) { openSource(context, source.url) }
            .heightIn(min = ROW_MIN.dp)
            .padding(horizontal = spacing.md, vertical = spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        NumberChip(number)
        Spacer(Modifier.width(spacing.sm))
        SourceIcon(source.url, favicons)
        Spacer(Modifier.width(spacing.sm))
        Column(Modifier.weight(1f)) {
            BasicText(
                source.title,
                style = type.bodySmall.copy(color = colors.text),
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
            )
            FaviconRules.host(source.url)?.removePrefix("www.")?.let { host ->
                BasicText(
                    host,
                    style = type.label.copy(color = colors.textMuted),
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/** The same green number as the chip in the text. */
@Composable
private fun NumberChip(number: Int) {
    val colors = LocalHainColors.current
    Box(
        Modifier
            .heightIn(min = NUMBER_BOX.dp)
            .widthIn(min = NUMBER_BOX.dp)
            .clip(CircleShape)
            .background(colors.accentSoft)
            .padding(horizontal = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(
            number.toString(),
            style =
                LocalHainTypography.current.label.copy(
                    color = colors.accent,
                    fontSize = NUMBER_SP.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                ),
        )
    }
}

private const val ROW_MIN = 32
private const val ROW_CORNER = 10
private const val LIST_CORNER = 16
private const val CHEVRON = 14
private const val QUARTER = 90f
private const val MAX_ICONS = 4
private const val STACK_ICON = 18
private const val STACK_STEP = 11
private const val RING = 1.5f
private const val NUMBER_BOX = 18
private const val NUMBER_SP = 10
