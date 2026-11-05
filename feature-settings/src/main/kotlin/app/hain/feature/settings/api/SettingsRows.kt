package app.hain.feature.settings.api

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import app.hain.ui.components.HainSwitch
import app.hain.ui.components.SegmentedControl
import app.hain.ui.motion.rememberHainHaptics
import app.hain.ui.theme.HainLevel
import app.hain.ui.theme.LocalHainColors
import app.hain.ui.theme.LocalHainShapes
import app.hain.ui.theme.LocalHainSpacing
import app.hain.ui.theme.LocalHainTypography
import app.hain.ui.theme.hainSurface

@Composable
internal fun Group(label: Int) {
    // T-266 (Lichtung): a hairline above each group and more air, so a page reads in calm blocks
    val spacing = LocalHainSpacing.current
    Column(Modifier.fillMaxWidth().padding(top = spacing.sm), verticalArrangement = Arrangement.spacedBy(spacing.md)) {
        Box(Modifier.fillMaxWidth().height(HAIRLINE.dp).background(LocalHainColors.current.border))
        BasicText(
            stringResource(label),
            style = LocalHainTypography.current.label.copy(color = LocalHainColors.current.accent),
        )
    }
}

private const val HAIRLINE = 0.5f

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
            BasicText(stringResource(hint), style = type.bodySmall.copy(color = colors.textMuted))
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
        BasicText(stringResource(hint), style = type.bodySmall.copy(color = colors.textMuted))
    }
}
