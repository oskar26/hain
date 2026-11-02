package app.hain.feature.settings.api

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import app.hain.common.api.AnswerLanguage
import app.hain.common.api.AnswerMode
import app.hain.common.api.HainSettings
import app.hain.common.api.ThemeChoice
import app.hain.feature.settings.R
import app.hain.ui.HainIcons
import app.hain.ui.components.HainIconButton
import app.hain.ui.components.TopBar
import app.hain.ui.theme.LocalHainColors
import app.hain.ui.theme.LocalHainShapes
import app.hain.ui.theme.LocalHainSpacing
import app.hain.ui.theme.LocalHainTypography

/** Settings, level 1 (SETTINGS.md): look, answers, about you. Plain words, no jargon. */
@Suppress("LongMethod") // one screen, one column of setting rows
@Composable
fun SettingsScreen(
    settings: HainSettings,
    actions: SettingsActions,
    modifier: Modifier = Modifier,
) {
    val spacing = LocalHainSpacing.current
    BackHandler(onBack = actions.onBack)
    Column(
        modifier
            .fillMaxSize()
            .background(LocalHainColors.current.bg)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        TopBar(
            title = stringResource(R.string.settings_title),
            leading = { HainIconButton(HainIcons.close, stringResource(R.string.settings_back), actions.onBack) },
        )
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = spacing.lg),
            verticalArrangement = Arrangement.spacedBy(spacing.md),
        ) {
            Group(R.string.settings_group_look)
            Choice(
                R.string.settings_theme,
                settings.theme,
                listOf(
                    ThemeChoice.SYSTEM to R.string.settings_theme_system,
                    ThemeChoice.LIGHT to R.string.settings_theme_light,
                    ThemeChoice.DARK to R.string.settings_theme_dark,
                ),
                actions.onTheme,
            )
            Toggle(
                R.string.settings_readable,
                R.string.settings_readable_hint,
                settings.readableFont,
                actions.onReadableFont,
            )
            Group(R.string.settings_group_answers)
            Choice(
                R.string.settings_answer_language,
                settings.answerLanguage,
                listOf(
                    AnswerLanguage.AUTO to R.string.settings_lang_auto,
                    AnswerLanguage.DE to R.string.settings_lang_de,
                    AnswerLanguage.EN to R.string.settings_lang_en,
                ),
                actions.onAnswerLanguage,
            )
            Choice(
                R.string.settings_answer_mode,
                settings.answerMode,
                listOf(
                    AnswerMode.AUTO to R.string.settings_mode_auto,
                    AnswerMode.FAST to R.string.settings_mode_fast,
                    AnswerMode.THOROUGH to R.string.settings_mode_thorough,
                ),
                actions.onAnswerMode,
            )
            Group(R.string.settings_group_packs)
            Link(R.string.settings_packs, R.string.settings_packs_hint, actions.onOpenPacks)
            Group(R.string.settings_group_you)
            TextSetting(
                R.string.settings_about_me,
                R.string.settings_about_me_hint,
                settings.aboutMe,
                actions.onAboutMe,
            )
            TextSetting(R.string.settings_style, R.string.settings_style_hint, settings.style, actions.onStyle)
            BasicText(
                stringResource(R.string.settings_reset),
                style = LocalHainTypography.current.bodySmall.copy(color = LocalHainColors.current.accent),
                modifier = Modifier.clickable(onClick = actions.onReset).padding(vertical = spacing.md),
            )
            Spacer(Modifier.height(spacing.xl))
        }
    }
}

@Composable
private fun Group(label: Int) {
    BasicText(
        stringResource(label),
        style = LocalHainTypography.current.label.copy(color = LocalHainColors.current.textMuted),
        modifier = Modifier.padding(top = LocalHainSpacing.current.md),
    )
}

@Composable
private fun <T> Choice(
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
private fun Toggle(
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
private fun TextSetting(
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
private fun Link(
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
