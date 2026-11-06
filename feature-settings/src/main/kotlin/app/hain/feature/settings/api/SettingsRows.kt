package app.hain.feature.settings.api

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.hain.ui.components.HainSwitch
import app.hain.ui.components.SegmentedControl
import app.hain.ui.motion.rememberHainHaptics
import app.hain.ui.motion.staggeredEnter
import app.hain.ui.theme.GlowHue
import app.hain.ui.theme.HainLevel
import app.hain.ui.theme.LocalHainColors
import app.hain.ui.theme.LocalHainShapes
import app.hain.ui.theme.LocalHainSpacing
import app.hain.ui.theme.LocalHainTypography
import app.hain.ui.theme.glassTile
import app.hain.ui.theme.hainSurface

@Composable
internal fun Group(label: Int) {
    // T-266 (Lichtung): a hairline above each group and more air, so a page reads in calm blocks.
    // T-292: more air above (a new block starts), the heading marked as one for TalkBack
    val spacing = LocalHainSpacing.current
    Column(Modifier.fillMaxWidth().padding(top = spacing.md), verticalArrangement = Arrangement.spacedBy(spacing.md)) {
        Box(Modifier.fillMaxWidth().height(HAIRLINE.dp).background(LocalHainColors.current.border))
        SectionTitle(label)
    }
}

/** T-292: the small green heading over a block of settings. */
@Composable
internal fun SectionTitle(
    label: Int,
    modifier: Modifier = Modifier,
) {
    BasicText(
        stringResource(label),
        style = LocalHainTypography.current.label.copy(color = LocalHainColors.current.accent),
        modifier = modifier.semantics { heading() },
    )
}

/**
 * T-292 (Copilot-like list, Lichtung): one block of a settings page – a heading in the margin, then
 * its rows on their own calm card with plenty of air. [index] staggers the entrance (T-295).
 */
@Composable
internal fun SettingsSection(
    title: Int?,
    hue: GlowHue,
    index: Int,
    content: @Composable ColumnScope.() -> Unit,
) {
    val spacing = LocalHainSpacing.current
    Column(
        Modifier.fillMaxWidth().staggeredEnter(index),
        verticalArrangement = Arrangement.spacedBy(spacing.sm),
    ) {
        if (title != null) SectionTitle(title, Modifier.padding(start = spacing.sm))
        Column(
            Modifier
                .fillMaxWidth()
                .glassTile(hue, elevation = SECTION_ELEVATION.dp, lightAt = Offset(x = 0.5f, y = 0f))
                .padding(horizontal = spacing.lg, vertical = spacing.md),
            verticalArrangement = Arrangement.spacedBy(spacing.lg),
            content = content,
        )
    }
}

private const val HAIRLINE = 0.5f
private const val SECTION_ELEVATION = 4

/** T-292: a hint under a row is at most this many lines; the rest ends in "…". */
internal const val HINT_LINES = 2

@Composable
internal fun <T> Choice(
    label: Int,
    selected: T,
    options: List<Pair<T, Int>>,
    onSelect: (T) -> Unit,
    /** Plain labels (e.g. numbers) instead of the string resources in [options]. */
    labels: List<String>? = null,
) {
    val colors = LocalHainColors.current
    val type = LocalHainTypography.current
    Column(verticalArrangement = Arrangement.spacedBy(LocalHainSpacing.current.sm)) {
        BasicText(stringResource(label), style = type.body.copy(color = colors.text))
        // one shared either-or control for the whole app (T-010)
        SegmentedControl(
            options = labels ?: options.map { stringResource(it.second) },
            selected = options.indexOfFirst { it.first == selected }.coerceAtLeast(0),
            onSelect = { onSelect(options[it].first) },
        )
    }
}

@Composable
internal fun Toggle(
    label: Int,
    hint: Int,
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
            BasicText(
                stringResource(hint),
                style = type.bodySmall.copy(color = colors.textMuted),
                maxLines = HINT_LINES,
                overflow = TextOverflow.Ellipsis,
            )
        }
        HainSwitch(checked)
    }
}

@Composable
internal fun TextSetting(
    label: Int,
    hint: Int,
    value: String,
    onChange: (String) -> Unit,
) {
    val colors = LocalHainColors.current
    val type = LocalHainTypography.current
    var text by rememberSaveable { mutableStateOf(value) }
    Column(verticalArrangement = Arrangement.spacedBy(LocalHainSpacing.current.sm)) {
        BasicText(stringResource(label), style = type.body.copy(color = colors.text))
        Box(
            Modifier
                .fillMaxWidth()
                .hainSurface(HainLevel.FLAT, LocalHainShapes.current.card)
                .padding(LocalHainSpacing.current.md),
        ) {
            if (text.isEmpty()) BasicText(stringResource(hint), style = type.body.copy(color = colors.textMuted))
            BasicTextField(
                value = text,
                onValueChange = {
                    text = it
                    onChange(it)
                },
                textStyle = type.body.copy(color = colors.text),
                cursorBrush = SolidColor(colors.accent),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
internal fun Link(
    label: Int,
    hint: Int,
    onClick: () -> Unit,
) {
    val colors = LocalHainColors.current
    val type = LocalHainTypography.current
    Column(
        Modifier
            .fillMaxWidth()
            .clip(LocalHainShapes.current.row)
            .clickable(onClick = onClick)
            .padding(vertical = LocalHainSpacing.current.sm),
    ) {
        BasicText(stringResource(label), style = type.body.copy(color = colors.text))
        BasicText(
            stringResource(hint),
            style = type.bodySmall.copy(color = colors.textMuted),
            maxLines = HINT_LINES,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
