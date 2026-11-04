package app.hain.feature.settings.api

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.hain.common.api.DevSettings
import app.hain.common.api.ThinkingOverride
import app.hain.feature.settings.R
import app.hain.models.api.DevModels
import app.hain.models.api.DownloadState
import app.hain.models.api.Fit
import app.hain.models.api.GgufFile
import app.hain.models.api.HfListing
import app.hain.models.api.HuggingFace
import app.hain.ui.theme.HainLevel
import app.hain.ui.theme.LocalHainColors
import app.hain.ui.theme.LocalHainShapes
import app.hain.ui.theme.LocalHainSpacing
import app.hain.ui.theme.LocalHainTypography
import app.hain.ui.theme.hainSurface
import kotlinx.coroutines.launch
import java.util.Locale

/** Context sizes offered in developer mode (T-211). */
@Suppress("MagicNumber") // the five sizes themselves
internal val CONTEXT_CHOICES = listOf(512, 1024, 2048, 4096, 8192)

/** Parses a number field: dot or comma, `null` when it is not a number. */
internal fun parseNumber(text: String): Float? = text.trim().replace(',', '.').toFloatOrNull()

/**
 * Settings → Entwickler (T-211): own model from Hugging Face, sampling, thinking, own system prompt
 * and the speed display. Every change is saved at once and used from the next message on.
 */
@Suppress("LongMethod") // one column of developer settings
@Composable
internal fun DeveloperPage(
    dev: DevSettings,
    actions: SettingsActions,
    devModels: DevModels?,
) {
    val set = actions.onDev
    Info(R.string.dev_intro)
    ModelSection(dev, set, devModels)
    Group(R.string.dev_sampling)
    NumberSetting(R.string.dev_temperature, R.string.dev_temperature_hint, dev.temperature) {
        set(dev.copy(temperature = it))
    }
    NumberSetting(R.string.dev_top_k, R.string.dev_top_k_hint, dev.topK.toFloat(), whole = true) {
        set(dev.copy(topK = it.toInt()))
    }
    NumberSetting(R.string.dev_top_p, R.string.dev_top_p_hint, dev.topP) { set(dev.copy(topP = it)) }
    NumberSetting(R.string.dev_min_p, R.string.dev_min_p_hint, dev.minP) { set(dev.copy(minP = it)) }
    NumberSetting(R.string.dev_repeat, R.string.dev_repeat_hint, dev.repeatPenalty) {
        set(dev.copy(repeatPenalty = it))
    }
    Choice(
        R.string.dev_context,
        dev.contextSize,
        CONTEXT_CHOICES.map { it to 0 },
        { set(dev.copy(contextSize = it)) },
        labels = CONTEXT_CHOICES.map { it.toString() },
    )
    if (dev.contextSize > CONTEXT_WARN) Warn(R.string.dev_context_warning)
    NumberSetting(R.string.dev_max_tokens, R.string.dev_max_tokens_hint, dev.maxTokens.toFloat(), whole = true) {
        set(dev.copy(maxTokens = it.toInt()))
    }
    NumberSetting(R.string.dev_seed, R.string.dev_seed_hint, dev.seed.toFloat(), whole = true) {
        set(dev.copy(seed = it.toInt()))
    }
    Group(R.string.dev_thinking)
    Choice(
        R.string.dev_thinking_mode,
        dev.thinking,
        listOf(
            ThinkingOverride.AUTO to R.string.dev_thinking_auto,
            ThinkingOverride.ON to R.string.dev_thinking_on,
            ThinkingOverride.OFF to R.string.dev_thinking_off,
        ),
        { set(dev.copy(thinking = it)) },
    )
    NumberSetting(
        R.string.dev_thinking_budget,
        R.string.dev_thinking_budget_hint,
        dev.thinkingBudget.toFloat(),
        whole = true,
    ) { set(dev.copy(thinkingBudget = it.toInt())) }
    Toggle(R.string.dev_raw_thinking, R.string.dev_raw_thinking_hint, dev.showRawThinking) {
        set(dev.copy(showRawThinking = it))
    }
    TextSetting(R.string.dev_system_prompt, R.string.dev_system_prompt_hint, dev.systemPrompt) {
        set(dev.copy(systemPrompt = it))
    }
    Toggle(R.string.dev_stats, R.string.dev_stats_hint, dev.showStats) { set(dev.copy(showStats = it)) }
    Link(R.string.dev_reset, R.string.dev_reset_hint) { set(dev.resetTuning()) }
    Link(R.string.about_dev_off, R.string.about_dev_off_hint) { set(dev.copy(enabled = false)) }
}

/** Own model: which one runs, a field for a Hugging Face repo or link, and its .gguf files. */
@Composable
private fun ModelSection(
    dev: DevSettings,
    set: (DevSettings) -> Unit,
    devModels: DevModels?,
) {
    val colors = LocalHainColors.current
    val type = LocalHainTypography.current
    val scope = rememberCoroutineScope()
    Group(R.string.dev_model)
    BasicText(
        stringResource(
            R.string.dev_model_active,
            dev.customModelName.ifBlank { stringResource(R.string.dev_model_default) },
        ),
        style = type.body.copy(color = colors.text),
    )
    if (dev.customModelPath.isNotBlank()) {
        Link(R.string.dev_model_back, R.string.dev_model_back_hint) {
            set(dev.copy(customModelPath = "", customModelName = ""))
        }
    }
    var input by rememberSaveable { mutableStateOf("") }
    var listing by remember { mutableStateOf<HfListing?>(null) }
    var busy by remember { mutableStateOf(false) }
    TextSetting(R.string.dev_model_repo, R.string.dev_model_repo_hint, input) { input = it }
    BasicText(
        stringResource(if (busy) R.string.dev_model_searching else R.string.dev_model_search),
        style = type.button.copy(color = if (devModels != null) colors.accent else colors.textMuted),
        modifier =
            Modifier
                .clickable(role = Role.Button, enabled = devModels != null && !busy && input.isNotBlank()) {
                    val models = devModels ?: return@clickable
                    busy = true
                    scope.launch {
                        listing = models.list(input)
                        busy = false
                    }
                }.padding(vertical = LocalHainSpacing.current.sm),
    )
    when (val l = listing) {
        is HfListing.Failed -> Warn(failText(l.reason))
        is HfListing.Files -> l.files.forEach { file -> if (devModels != null) FileRow(file, dev, set, devModels) }
        null -> Unit
    }
    Warn(R.string.dev_model_warning)
}

private fun failText(reason: String): Int =
    when (reason) {
        "offline" -> R.string.dev_model_err_offline
        "not_found" -> R.string.dev_model_err_not_found
        "no_gguf" -> R.string.dev_model_err_no_gguf
        "invalid" -> R.string.dev_model_err_invalid
        else -> R.string.dev_model_err_network
    }

/** One .gguf file: size, fit hint, download with progress, then "Nutzen" / "Entfernen". */
@Suppress("LongMethod", "CyclomaticComplexMethod") // one card with its download states
@Composable
private fun FileRow(
    file: GgufFile,
    dev: DevSettings,
    set: (DevSettings) -> Unit,
    models: DevModels,
) {
    val colors = LocalHainColors.current
    val type = LocalHainTypography.current
    val spacing = LocalHainSpacing.current
    val scope = rememberCoroutineScope()
    val flow = remember(file) { models.state(file) }
    val state by flow.collectAsStateWithLifecycle(DownloadState.Idle)
    val path = remember(file) { models.path(file) }
    Column(
        Modifier
            .fillMaxWidth()
            .hainSurface(HainLevel.FLAT, LocalHainShapes.current.card)
            .padding(spacing.md),
        verticalArrangement = Arrangement.spacedBy(spacing.xs),
    ) {
        BasicText(file.name, style = type.body.copy(color = colors.text))
        val size = if (file.size > 0) sizeText(file.size) else stringResource(R.string.dev_model_size_unknown)
        val ram = HuggingFace.minRamGb(file.size)
        val line = if (ram > 0) size + " · " + stringResource(R.string.dev_model_ram, ram, models.ramGb) else size
        BasicText(line, style = type.label.copy(color = colors.textMuted))
        val fit = models.fit(file)
        if (fit != Fit.GOOD) Warn(fitText(fit))
        if (file.sha256.isBlank()) {
            BasicText(
                stringResource(R.string.dev_model_no_sha),
                style = type.label.copy(color = colors.textMuted),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(spacing.md), verticalAlignment = Alignment.CenterVertically) {
            when (val s = state) {
                is DownloadState.Installed ->
                    if (dev.customModelPath == path) {
                        BasicText(
                            stringResource(R.string.dev_model_in_use),
                            style = type.label.copy(color = colors.accent),
                        )
                    } else {
                        Action(
                            R.string.dev_model_use,
                        ) { set(dev.copy(customModelPath = path, customModelName = file.label)) }
                        Action(R.string.dev_model_delete) { models.delete(file) }
                    }
                is DownloadState.Running -> {
                    BasicText(
                        stringResource(R.string.dev_model_loading, (s.fraction * PERCENT).toInt()),
                        style = type.label.copy(color = colors.text),
                    )
                    Action(R.string.dev_model_cancel) { models.cancel(file) }
                }
                DownloadState.Queued, DownloadState.Verifying ->
                    BasicText(
                        stringResource(R.string.dev_model_wait),
                        style = type.label.copy(color = colors.textMuted),
                    )
                is DownloadState.Failed -> {
                    BasicText(stringResource(R.string.dev_model_failed), style = type.label.copy(color = colors.danger))
                    Action(R.string.dev_model_download) { scope.launch { models.download(file) } }
                }
                DownloadState.Idle ->
                    if (fit != Fit.TOO_SMALL && fit != Fit.NO_SPACE) {
                        Action(R.string.dev_model_download) { scope.launch { models.download(file) } }
                    }
            }
        }
    }
}

private fun fitText(fit: Fit): Int =
    when (fit) {
        Fit.TIGHT, Fit.GOOD -> R.string.packs_fit_tight
        Fit.NO_SPACE -> R.string.packs_fit_space
        Fit.TOO_SMALL -> R.string.packs_fit_small
    }

private fun sizeText(bytes: Long): String =
    if (bytes >= GB) {
        String.format(Locale.ROOT, "%.2f GB", bytes / GB.toDouble())
    } else {
        String.format(Locale.ROOT, "%.0f MB", bytes / MB.toDouble())
    }

@Composable
private fun Action(
    label: Int,
    onClick: () -> Unit,
) {
    BasicText(
        stringResource(label),
        style = LocalHainTypography.current.button.copy(color = LocalHainColors.current.accent),
        modifier =
            Modifier
                .clip(LocalHainShapes.current.row)
                .clickable(role = Role.Button, onClick = onClick)
                .padding(vertical = LocalHainSpacing.current.xs),
    )
}

@Composable
private fun Warn(text: Int) {
    BasicText(
        stringResource(text),
        style = LocalHainTypography.current.label.copy(color = LocalHainColors.current.danger),
    )
}

/**
 * A number with its label and hint; the small field on the right takes dot or comma. A value is
 * saved as soon as it is a number ([whole] = only whole numbers).
 */
@Composable
private fun NumberSetting(
    label: Int,
    hint: Int,
    value: Float,
    whole: Boolean = false,
    onChange: (Float) -> Unit,
) {
    val colors = LocalHainColors.current
    val type = LocalHainTypography.current
    val shown = if (whole) value.toInt().toString() else value.toString()
    var text by remember(label) { mutableStateOf(shown) }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f).padding(end = LocalHainSpacing.current.md)) {
            BasicText(stringResource(label), style = type.body.copy(color = colors.text))
            BasicText(stringResource(hint), style = type.bodySmall.copy(color = colors.textMuted))
        }
        Box(
            Modifier
                .width(FIELD_WIDTH.dp)
                .hainSurface(HainLevel.FLAT, LocalHainShapes.current.row)
                .padding(horizontal = LocalHainSpacing.current.sm, vertical = LocalHainSpacing.current.sm),
        ) {
            BasicTextField(
                value = text,
                onValueChange = { typed ->
                    text = typed
                    val n = parseNumber(typed)
                    if (n != null && (!whole || n == n.toInt().toFloat())) onChange(n)
                },
                singleLine = true,
                textStyle = type.body.copy(color = colors.text),
                cursorBrush = SolidColor(colors.accent),
                keyboardOptions =
                    KeyboardOptions(keyboardType = if (whole) KeyboardType.Number else KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

private const val PERCENT = 100
private const val CONTEXT_WARN = 4096
private const val FIELD_WIDTH = 96
private const val GB = 1_000_000_000L
private const val MB = 1_000_000L
