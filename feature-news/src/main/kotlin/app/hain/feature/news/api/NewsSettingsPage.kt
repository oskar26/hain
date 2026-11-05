package app.hain.feature.news.api

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.hain.feature.news.R
import app.hain.ui.HainIcons
import app.hain.ui.components.HainSwitch
import app.hain.ui.motion.rememberHainHaptics
import app.hain.ui.theme.HainLevel
import app.hain.ui.theme.LocalHainColors
import app.hain.ui.theme.LocalHainShapes
import app.hain.ui.theme.LocalHainSpacing
import app.hain.ui.theme.LocalHainTypography
import app.hain.ui.theme.hainSurface

/**
 * The news settings page (T-076, T-079), reached from the news page's top bar (the app's settings
 * can link here too): reading view (text size, serif or sans, line spacing, colours, with a sample
 * paragraph), pictures in the article and in the list, the morning briefing note (off by default)
 * and the own feeds.
 */
@Suppress("LongMethod")
@Composable
fun NewsSettingsPage(
    prefs: NewsPrefs,
    onPrefs: ((NewsPrefs) -> NewsPrefs) -> Unit,
    onBriefingNote: (Boolean) -> Unit,
    onOpenFeeds: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = LocalHainSpacing.current
    val colors = LocalHainColors.current
    val rc = readerColors(prefs.theme)
    Column(
        modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .wrapContentWidth(Alignment.CenterHorizontally)
            .widthIn(max = MAX_READ_WIDTH.dp)
            .fillMaxWidth()
            .padding(horizontal = spacing.lg, vertical = spacing.md),
        verticalArrangement = Arrangement.spacedBy(spacing.md),
    ) {
        Section(R.string.news_prefs_reader) {
            BasicText(
                stringResource(R.string.news_prefs_sample),
                style = readerBody(prefs, rc.text),
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .hainSurface(HainLevel.FLAT, LocalHainShapes.current.chip, fill = rc.bg)
                        .padding(spacing.md),
            )
            ReaderControls(prefs, onPrefs)
            SwitchRow(R.string.news_prefs_article_images, null, prefs.articleImages) { on ->
                onPrefs { it.copy(articleImages = on) }
            }
        }
        Section(R.string.news_title) {
            SwitchRow(R.string.news_prefs_list_images, R.string.news_prefs_images_hint, prefs.listImages) { on ->
                onPrefs { it.copy(listImages = on) }
            }
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(LocalHainShapes.current.row)
                    .clickable(role = Role.Button, onClick = onOpenFeeds)
                    .padding(vertical = spacing.md),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BasicText(
                    stringResource(R.string.news_prefs_feeds),
                    style = LocalHainTypography.current.body.copy(color = colors.text),
                    modifier = Modifier.weight(1f),
                )
                androidx.compose.foundation.Image(
                    HainIcons.chevronRight,
                    contentDescription = null,
                    colorFilter =
                        androidx.compose.ui.graphics.ColorFilter
                            .tint(colors.textMuted),
                )
            }
        }
        Section(R.string.news_prefs_briefing) {
            SwitchRow(
                R.string.news_prefs_briefing_note,
                R.string.news_prefs_briefing_hint,
                prefs.briefingNotification,
                onBriefingNote,
            )
        }
    }
}

@Composable
private fun Section(
    title: Int,
    content: @Composable () -> Unit,
) {
    val spacing = LocalHainSpacing.current
    Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
        BasicText(
            stringResource(title),
            style = LocalHainTypography.current.label.copy(color = LocalHainColors.current.textMuted),
            modifier = Modifier.padding(top = spacing.sm).semantics { heading() },
        )
        Column(
            Modifier
                .fillMaxWidth()
                .hainSurface(HainLevel.RAISED, LocalHainShapes.current.card, fill = LocalHainColors.current.surface)
                .padding(spacing.md),
            verticalArrangement = Arrangement.spacedBy(spacing.sm),
        ) { content() }
    }
}

@Composable
private fun SwitchRow(
    label: Int,
    hint: Int?,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
) {
    val colors = LocalHainColors.current
    val type = LocalHainTypography.current
    val haptics = rememberHainHaptics()
    Row(
        Modifier
            .fillMaxWidth()
            .clip(LocalHainShapes.current.row)
            .toggleable(value = checked, role = Role.Switch) {
                haptics.toggle()
                onChange(it)
            }.padding(vertical = LocalHainSpacing.current.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f).padding(end = LocalHainSpacing.current.md)) {
            BasicText(stringResource(label), style = type.body.copy(color = colors.text))
            hint?.let { BasicText(stringResource(it), style = type.bodySmall.copy(color = colors.textMuted)) }
        }
        HainSwitch(checked)
    }
}
