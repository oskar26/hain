@file:Suppress("TooManyFunctions") // hub, category page, rows, labels

package app.hain.feature.settings.api

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.hain.feature.settings.R
import app.hain.models.api.BrainChoice
import app.hain.models.api.DeviceCheck
import app.hain.models.api.DeviceProfile
import app.hain.models.api.DownloadState
import app.hain.models.api.FailReason
import app.hain.models.api.Fit
import app.hain.models.api.ModelInfo
import app.hain.models.api.ModelRepository
import app.hain.ui.HainIcons
import app.hain.ui.components.Hain3D
import app.hain.ui.components.Hain3DIcon
import app.hain.ui.components.HainIconButton
import app.hain.ui.components.ProgressBar
import app.hain.ui.components.TopBar
import app.hain.ui.layout.HainColumns
import app.hain.ui.layout.HainGrid
import app.hain.ui.layout.currentHainWindowSize
import app.hain.ui.motion.HainMotion
import app.hain.ui.motion.LocalReduceMotion
import app.hain.ui.motion.hainTween
import app.hain.ui.theme.GlowHue
import app.hain.ui.theme.HainLevel
import app.hain.ui.theme.LocalHainColors
import app.hain.ui.theme.LocalHainShapes
import app.hain.ui.theme.LocalHainSpacing
import app.hain.ui.theme.LocalHainTypography
import app.hain.ui.theme.glassTile
import app.hain.ui.theme.hainSurface
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

private const val MB = 1_000_000L
private const val GB = 1_000_000_000L

/**
 * T-234 (Oskar 04.11. 22:18): "Modelle & Pakete" instead of one long pack list. The first page
 * shows five groups; each opens its own page with size, status, download and delete.
 */
enum class PackCategory(
    val title: Int,
    val hint: Int,
    val icon: Hain3D,
    val hue: GlowHue,
) {
    /** Offline Wikipedia (T-237). */
    KNOWLEDGE(R.string.packs_cat_knowledge, R.string.packs_cat_knowledge_hint, Hain3D.BOOK, GlowHue.SKY),

    /** The language models. */
    BRAINS(R.string.packs_cat_brains, R.string.packs_cat_brains_hint, Hain3D.HEAD, GlowHue.MINT),

    /** Text recognition, search by meaning, images. */
    MEDIA(R.string.packs_cat_media, R.string.packs_cat_media_hint, Hain3D.DOCUMENT, GlowHue.PEACH),

    /** Dictation (Moonshine) and reading aloud. */
    VOICE(R.string.packs_cat_voice, R.string.packs_cat_voice_hint, Hain3D.BUBBLE, GlowHue.LILAC),

    /** Everything else. */
    EXTRAS(R.string.packs_cat_extras, R.string.packs_cat_extras_hint, Hain3D.GIFT, GlowHue.SUN),
    ;

    companion object {
        /** The group of a catalogue pack by its kind. */
        fun of(pack: ModelInfo): PackCategory =
            when (pack.kind) {
                ModelInfo.KIND_KNOWLEDGE -> KNOWLEDGE
                ModelInfo.KIND_BRAIN -> BRAINS
                ModelInfo.KIND_EMBED, KIND_OCR, KIND_IMAGE -> MEDIA
                KIND_STT, KIND_TTS -> VOICE
                else -> EXTRAS
            }

        private const val KIND_OCR = "ocr"
        private const val KIND_IMAGE = "image"
        private const val KIND_STT = "stt"
        private const val KIND_TTS = "tts"
    }
}

/**
 * A pack that is not in the model catalogue but downloaded by another part of the app, such as the
 * text recognition pack (Häppchen 9). [progress] is 0..1 while it loads, `null` otherwise.
 */
class ExtraPack(
    val kind: Kind,
    val bytes: Long,
    val installed: Flow<Boolean>,
    val progress: Flow<Float?> = flowOf(null),
    val onGet: () -> Unit,
    val onRemove: () -> Unit,
) {
    enum class Kind(
        val category: PackCategory,
        val title: Int,
        val description: Int,
    ) {
        OCR(PackCategory.MEDIA, R.string.packs_ocr_title, R.string.packs_ocr_desc),
    }
}

/** Models & packs: groups first, then one page per group (SETTINGS.md, MODELS.md). */
@Suppress("LongParameterList") // the extras and the start page have defaults
@Composable
fun PacksScreen(
    models: ModelRepository,
    ramGb: Int,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    extras: List<ExtraPack> = emptyList(),
    startCategory: PackCategory? = null,
) {
    val spacing = LocalHainSpacing.current
    val reduce = LocalReduceMotion.current
    var category by rememberSaveable { mutableStateOf(startCategory) }
    BackHandler { if (category != null) category = null else onBack() }
    val device = remember(ramGb) { DeviceProfile(ramGb, models.freeSpace()) }
    // T-287: what is on the phone, again after every download, stop and delete (was read once)
    val installedFlow = remember(models) { models.installedIds }
    val installed by installedFlow.collectAsStateWithLifecycle(emptySet())
    // T-287: Android 13+ asks once for notifications, so the download shows its progress when Hain is closed
    val download = rememberDownloadStarter(models)
    // T-251: two columns of tiles on wide windows (tablet landscape, open foldable)
    val columns = HainColumns.count(currentHainWindowSize())
    Column(
        modifier
            .fillMaxSize()
            .background(LocalHainColors.current.bg)
            // T-289: status/navigation bars, camera cut-out and keyboard
            .safeDrawingPadding(),
    ) {
        val open = category
        TopBar(
            title = stringResource(open?.title ?: R.string.packs_title),
            leading = {
                HainIconButton(
                    if (open == null) HainIcons.close else HainIcons.back,
                    stringResource(R.string.settings_back),
                    { if (open != null) category = null else onBack() },
                )
            },
        )
        AnimatedContent(
            targetState = open,
            transitionSpec = {
                fadeIn(hainTween(HainMotion.BASE_MS, reduce)) togetherWith
                    fadeOut(hainTween(HainMotion.FAST_MS, reduce))
            },
            label = "packs-page",
            modifier = Modifier.weight(1f),
        ) { page ->
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                Column(
                    Modifier
                        .widthIn(max = HainColumns.pageMaxDp(columns).dp)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = spacing.lg),
                    verticalArrangement = Arrangement.spacedBy(spacing.md),
                ) {
                    if (page == null) {
                        PacksHub(models, device, installed, extras, columns) { category = it }
                    } else {
                        CategoryPage(page, models, PackContext(device, installed, download), extras, columns)
                    }
                    Spacer(Modifier.height(spacing.xl))
                }
            }
        }
    }
}

@Suppress("LongParameterList") // the page's data and the open callback
@Composable
private fun PacksHub(
    models: ModelRepository,
    device: DeviceProfile,
    installedIds: Set<String>,
    extras: List<ExtraPack>,
    columns: Int,
    open: (PackCategory) -> Unit,
) {
    BasicText(
        stringResource(R.string.packs_intro),
        style = LocalHainTypography.current.bodySmall.copy(color = LocalHainColors.current.textMuted),
    )
    HainGrid(PackCategory.entries, columns, LocalHainSpacing.current.md) { cat, cell ->
        val packs = offeredPacks(models, cat, device, installedIds)
        val installed = packs.count { it.id in installedIds }
        val count = packs.size + extras.count { it.kind.category == cat }
        CategoryTile(cat, installed, count, cell) { open(cat) }
    }
}

/** A group on the first page: icon, name, short line, "2 von 4 auf dem Handy", chevron. */
@Composable
private fun CategoryTile(
    cat: PackCategory,
    installed: Int,
    count: Int,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val colors = LocalHainColors.current
    val type = LocalHainTypography.current
    val spacing = LocalHainSpacing.current
    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = TILE_MIN.dp)
            .glassTile(cat.hue, lightAt = Offset(x = 0.1f, y = 0.5f))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = spacing.md, vertical = spacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.md),
    ) {
        Hain3DIcon(cat.icon, size = 32.dp)
        Column(Modifier.weight(1f)) {
            BasicText(stringResource(cat.title), style = type.button.copy(color = colors.text))
            BasicText(stringResource(cat.hint), style = type.bodySmall.copy(color = colors.textMuted))
            if (count > 0) {
                BasicText(
                    stringResource(R.string.packs_cat_count, installed, count),
                    style = type.label.copy(color = if (installed > 0) colors.accent else colors.textMuted),
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
        Image(HainIcons.chevronRight, null, Modifier.size(20.dp), colorFilter = ColorFilter.tint(colors.icon))
    }
}

/** One group's page: a line on what it is for, then every pack of the group. */
@Composable
private fun CategoryPage(
    cat: PackCategory,
    models: ModelRepository,
    context: PackContext,
    extras: List<ExtraPack>,
    columns: Int,
) {
    val colors = LocalHainColors.current
    val type = LocalHainTypography.current
    BasicText(stringResource(categoryIntro(cat)), style = type.bodySmall.copy(color = colors.textMuted))
    val packs = offeredPacks(models, cat, context.device, context.installed)
    val extra = extras.filter { it.kind.category == cat }
    // T-251: catalogue packs and the extra packs share one grid
    val tiles: List<Any> = packs + extra
    HainGrid(tiles, columns, LocalHainSpacing.current.md) { tile, cell ->
        when (tile) {
            is ModelInfo -> PackRow(tile, models, context, cell)
            is ExtraPack -> ExtraPackRow(tile, cell)
        }
    }
    // reading aloud uses the phone's own voices: nothing to download
    if (cat == PackCategory.VOICE) InfoTile(R.string.packs_tts_title, R.string.packs_tts_desc)
    if (cat == PackCategory.KNOWLEDGE) {
        BasicText(stringResource(R.string.packs_wiki_licence), style = type.label.copy(color = colors.textMuted))
    }
    if (packs.isEmpty() && extra.isEmpty() && cat != PackCategory.VOICE) {
        InfoTile(R.string.packs_empty_title, R.string.packs_empty_desc)
    }
}

private fun categoryIntro(cat: PackCategory): Int =
    when (cat) {
        PackCategory.KNOWLEDGE -> R.string.packs_knowledge_intro
        PackCategory.BRAINS -> R.string.packs_brains_intro
        PackCategory.MEDIA -> R.string.packs_media_intro
        PackCategory.VOICE -> R.string.packs_voice_intro
        PackCategory.EXTRAS -> R.string.packs_extras_intro
    }

@Composable
private fun InfoTile(
    title: Int,
    body: Int,
) {
    val colors = LocalHainColors.current
    val type = LocalHainTypography.current
    Column(
        Modifier
            .fillMaxWidth()
            .hainSurface(HainLevel.FLAT, LocalHainShapes.current.card)
            .padding(LocalHainSpacing.current.lg),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        BasicText(stringResource(title), style = type.button.copy(color = colors.text))
        BasicText(stringResource(body), style = type.bodySmall.copy(color = colors.textMuted))
    }
}

/** What every pack row needs from the page (T-287). */
private class PackContext(
    val device: DeviceProfile,
    val installed: Set<String>,
    val download: (ModelInfo) -> Unit,
)

@Composable
private fun PackRow(
    pack: ModelInfo,
    models: ModelRepository,
    context: PackContext,
    modifier: Modifier = Modifier,
) {
    val flow = remember(pack.id) { models.downloadState(pack) }
    val state by flow.collectAsStateWithLifecycle(DownloadState.Idle)
    val fit = DeviceCheck.fit(pack, context.device)
    PackTile(
        icon = PackCategory.of(pack).icon,
        hue = PackCategory.of(pack).hue,
        title = packTitle(pack),
        size = pack.totalSize,
        description = packDescription(pack)?.let { stringResource(it) },
        warning = fitText(fit)?.takeIf { state !is DownloadState.Installed }?.let { stringResource(it) },
        modifier = modifier,
        progress = PackStatus.fraction(state),
    ) {
        PackActions(pack, state, fit, models, context.download)
    }
}

/** T-287: status line and buttons of one pack for every download state. */
@Suppress("LongMethod") // one branch per state
@Composable
private fun RowScope.PackActions(
    pack: ModelInfo,
    state: DownloadState,
    fit: Fit,
    models: ModelRepository,
    download: (ModelInfo) -> Unit,
) {
    val colors = LocalHainColors.current
    val type = LocalHainTypography.current
    val status = type.label.copy(color = colors.text)
    val muted = type.label.copy(color = colors.textMuted)
    val statusModifier = Modifier.weight(1f, fill = false)
    when (state) {
        DownloadState.Installed -> {
            BasicText(stringResource(R.string.packs_installed), statusModifier, type.label.copy(color = colors.accent))
            if (!pack.isDefault) ActionButton(R.string.packs_delete) { models.delete(pack) }
        }
        is DownloadState.Running -> {
            val line =
                stringResource(
                    R.string.packs_loading_bytes,
                    PackStatus.percent(state.fraction),
                    sizeText(state.downloaded),
                    sizeText(state.total),
                )
            BasicText(line, statusModifier, status)
            ActionButton(R.string.packs_cancel) { models.cancelDownload(pack) }
        }
        is DownloadState.Paused -> {
            val line = stringResource(R.string.packs_paused, sizeText(state.downloaded), sizeText(state.total))
            BasicText(line, statusModifier, muted)
            ActionButton(R.string.packs_resume) { download(pack) }
            ActionButton(R.string.packs_discard) { models.delete(pack) }
        }
        DownloadState.Queued -> {
            BasicText(stringResource(R.string.packs_queued), statusModifier, muted)
            ActionButton(R.string.packs_cancel) { models.cancelDownload(pack) }
        }
        DownloadState.Verifying -> BasicText(stringResource(R.string.packs_verifying), statusModifier, muted)
        is DownloadState.Failed -> {
            BasicText(stringResource(failText(state)), statusModifier, type.label.copy(color = colors.danger))
            if (fit != Fit.TOO_SMALL) ActionButton(R.string.packs_retry) { download(pack) }
        }
        DownloadState.Idle ->
            if (fit != Fit.TOO_SMALL && fit != Fit.NO_SPACE) ActionButton(R.string.packs_download) { download(pack) }
    }
}

private fun failText(state: DownloadState.Failed): Int =
    when (state.reason) {
        FailReason.NETWORK -> R.string.packs_failed_network
        FailReason.STORAGE -> R.string.packs_failed_storage
        FailReason.CHECKSUM -> R.string.packs_failed_checksum
        FailReason.UNKNOWN -> R.string.packs_failed
    }

/**
 * T-287: starts a download; on Android 13+ asks once for notifications first (the progress then shows
 * in the notification shade while Hain is closed). A "no" still downloads, only without the note.
 */
@Composable
private fun rememberDownloadStarter(models: ModelRepository): (ModelInfo) -> Unit {
    val context = LocalContext.current
    var waiting by remember { mutableStateOf<ModelInfo?>(null) }
    val ask =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
            waiting?.let(models::startDownload)
            waiting = null
        }
    return { pack ->
        val needsAsk =
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED
        if (needsAsk) {
            waiting = pack
            ask.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            models.startDownload(pack)
        }
    }
}

@Composable
private fun ExtraPackRow(
    pack: ExtraPack,
    modifier: Modifier = Modifier,
) {
    val colors = LocalHainColors.current
    val type = LocalHainTypography.current
    val installed by pack.installed.collectAsStateWithLifecycle(false)
    val progress by pack.progress.collectAsStateWithLifecycle(null)
    PackTile(
        icon = pack.kind.category.icon,
        hue = pack.kind.category.hue,
        title = stringResource(pack.kind.title),
        size = pack.bytes,
        description = stringResource(pack.kind.description),
        modifier = modifier,
    ) {
        val loading = progress
        when {
            installed -> {
                BasicText(stringResource(R.string.packs_installed), style = type.label.copy(color = colors.accent))
                ActionButton(R.string.packs_delete, pack.onRemove)
            }
            loading != null ->
                BasicText(
                    stringResource(R.string.packs_loading, (loading * PERCENT).toInt()),
                    style = type.label.copy(color = colors.text),
                )
            else -> ActionButton(R.string.packs_download, pack.onGet)
        }
    }
}

/** One pack: icon, name and size, what it is for, a fit warning, then status and buttons. */
@Suppress("LongParameterList") // the pieces of one row
@Composable
private fun PackTile(
    icon: Hain3D,
    hue: GlowHue,
    title: String,
    size: Long,
    description: String? = null,
    warning: String? = null,
    modifier: Modifier = Modifier,
    progress: Float? = null,
    actions: @Composable RowScope.() -> Unit,
) {
    val colors = LocalHainColors.current
    val type = LocalHainTypography.current
    val spacing = LocalHainSpacing.current
    Column(
        modifier
            .fillMaxWidth()
            .glassTile(hue, lightAt = Offset(x = 0.12f, y = 0.2f))
            .padding(spacing.lg),
        verticalArrangement = Arrangement.spacedBy(spacing.sm),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(spacing.md)) {
            Hain3DIcon(icon, size = 32.dp)
            Column(Modifier.weight(1f)) {
                BasicText(title, style = type.button.copy(color = colors.text))
                BasicText(sizeText(size), style = type.label.copy(color = colors.textMuted))
            }
        }
        description?.let { BasicText(it, style = type.bodySmall.copy(color = colors.textMuted)) }
        warning?.let { BasicText(it, style = type.label.copy(color = colors.danger)) }
        progress?.let { ProgressBar(fraction = it, modifier = Modifier.fillMaxWidth()) }
        Row(
            Modifier.fillMaxWidth().heightIn(min = 48.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.md),
            content = actions,
        )
    }
}

/** A plain name for a pack; Wikipedia packs get theirs in the app's language. */
@Composable
private fun packTitle(pack: ModelInfo): String =
    when (pack.id) {
        "wikipedia-de-top-mini" -> stringResource(R.string.packs_wiki_de_top)
        "wikipedia-de-all-mini" -> stringResource(R.string.packs_wiki_de_all)
        "wikipedia-en-top-mini" -> stringResource(R.string.packs_wiki_en_top)
        "wikipedia-en-top1m-mini" -> stringResource(R.string.packs_wiki_en_1m)
        else -> pack.name
    }

private fun packDescription(pack: ModelInfo): Int? =
    when {
        pack.kind == ModelInfo.KIND_EMBED -> R.string.packs_embed_desc
        pack.kind == ModelInfo.KIND_KNOWLEDGE && pack.id.contains("top") -> R.string.packs_wiki_top_desc
        pack.kind == ModelInfo.KIND_KNOWLEDGE -> R.string.packs_wiki_big_desc
        // T-261: one or two honest sentences per pack: what it brings and when it is worth it
        pack.kind == ModelInfo.KIND_BRAIN && pack.isStrong -> R.string.packs_brain_strong_desc
        pack.kind == ModelInfo.KIND_BRAIN && pack.isOptional -> R.string.packs_brain_optional_desc
        pack.kind == ModelInfo.KIND_BRAIN -> R.string.packs_brain_desc
        pack.kind == "stt" -> R.string.packs_stt_desc
        else -> null
    }

private const val PERCENT = 100
private const val TILE_MIN = 72

private fun fitText(fit: Fit): Int? =
    when (fit) {
        Fit.GOOD -> null
        Fit.TIGHT -> R.string.packs_fit_tight
        Fit.NO_SPACE -> R.string.packs_fit_space
        Fit.TOO_SMALL -> R.string.packs_fit_small
    }

private fun sizeText(bytes: Long): String {
    if (bytes < GB) return "${bytes / MB} MB"
    return "%.1f GB".format(bytes.toDouble() / GB)
}

@Composable
private fun ActionButton(
    label: Int,
    onClick: () -> Unit,
) {
    val colors = LocalHainColors.current
    Box(
        Modifier
            .heightIn(min = 48.dp) // T-254: touch target ≥ 48 dp
            .hainSurface(HainLevel.FLAT, LocalHainShapes.current.button)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = LocalHainSpacing.current.md, vertical = LocalHainSpacing.current.sm),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(stringResource(label), style = LocalHainTypography.current.label.copy(color = colors.text))
    }
}

/** T-240: catalogue packs of [cat]; the Großes Gehirn only on phones where it can run (or when on the phone). */
private fun offeredPacks(
    models: ModelRepository,
    cat: PackCategory,
    device: DeviceProfile,
    installed: Set<String>,
): List<ModelInfo> {
    val inCategory = models.catalog.filter { PackCategory.of(it) == cat }
    return inCategory.filter { BrainChoice.offered(it, device, it.id in installed) }
}

/** T-287: progress bar and percent of a pack's download state. Pure, tested. */
internal object PackStatus {
    /** 0..1 while something loads or is paused, `null` when no bar should show. */
    fun fraction(state: DownloadState): Float? =
        when (state) {
            is DownloadState.Running -> state.fraction.coerceIn(0f, 1f)
            is DownloadState.Paused -> state.fraction
            DownloadState.Verifying -> 1f
            else -> null
        }

    fun percent(fraction: Float): Int = (fraction.coerceIn(0f, 1f) * PERCENT).toInt()
}
