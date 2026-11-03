package app.hain.feature.settings.api

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import app.hain.ui.theme.LocalHainColors
import app.hain.ui.theme.LocalHainShapes
import app.hain.ui.theme.LocalHainSpacing
import app.hain.ui.theme.LocalHainTypography

@Composable
internal fun Group(label: Int) {
    BasicText(
        stringResource(label),
        style = LocalHainTypography.current.label.copy(color = LocalHainColors.current.textMuted),
        modifier = Modifier.padding(top = LocalHainSpacing.current.md),
    )
}

@Composable
internal fun <T> Choice(
    label: Int,
    selected: T,
    options: List<Pair<T, Int>>,
    onSelect: (T) -> Unit,
) {
    val colors = LocalHainColors.current
    val type = LocalHainTypography.current
    Column(verticalArrangement = Arrangement.spacedBy(LocalHainSpacing.current.sm)) {
        BasicText(stringResource(label), style = type.bodySmall.copy(color = colors.text))
        Row(horizontalArrangement = Arrangement.spacedBy(LocalHainSpacing.current.sm)) {
            options.forEach { (value, name) ->
                val on = value == selected
                Box(
                    Modifier
                        .background(if (on) colors.accent else colors.surfaceHigh, LocalHainShapes.current.button)
                        .clickable { onSelect(value) }
                        .padding(horizontal = LocalHainSpacing.current.md, vertical = LocalHainSpacing.current.sm),
                ) {
                    BasicText(
                        stringResource(name),
                        style = type.label.copy(color = if (on) colors.onAccent else colors.text),
                    )
                }
            }
        }
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
    Row(
        Modifier.fillMaxWidth().clickable { onChange(!checked) },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            BasicText(stringResource(label), style = type.bodySmall.copy(color = colors.text))
            BasicText(stringResource(hint), style = type.label.copy(color = colors.textMuted))
        }
        Box(
            Modifier
                .background(if (checked) colors.accent else colors.surfaceHigh, RoundedCornerShape(percent = 50))
                .padding(horizontal = LocalHainSpacing.current.md, vertical = LocalHainSpacing.current.xs),
        ) {
            BasicText(
                if (checked) "●" else "○",
                style = type.label.copy(color = if (checked) colors.onAccent else colors.textMuted),
            )
        }
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
        BasicText(stringResource(label), style = type.bodySmall.copy(color = colors.text))
        Box(
            Modifier
                .fillMaxWidth()
                .background(colors.surfaceHigh, LocalHainShapes.current.button)
                .padding(LocalHainSpacing.current.md),
        ) {
            if (text.isEmpty()) BasicText(stringResource(hint), style = type.bodySmall.copy(color = colors.textMuted))
            BasicTextField(
                value = text,
                onValueChange = {
                    text = it
                    onChange(it)
                },
                textStyle = type.bodySmall.copy(color = colors.text),
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
    Column(Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        BasicText(stringResource(label), style = type.bodySmall.copy(color = colors.text))
        BasicText(stringResource(hint), style = type.label.copy(color = colors.textMuted))
    }
}
