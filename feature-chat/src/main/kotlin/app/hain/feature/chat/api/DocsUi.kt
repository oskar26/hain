@file:Suppress("TooManyFunctions") // one small composable per part of the file UI

package app.hain.feature.chat.api

import android.graphics.BitmapFactory
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.hain.docs.api.DocAttachment
import app.hain.docs.api.DocInfo
import app.hain.docs.api.DocKind
import app.hain.docs.api.ImportError
import app.hain.docs.api.ImportStep
import app.hain.feature.chat.R
import app.hain.ui.HainIcons
import app.hain.ui.components.EmptyState
import app.hain.ui.components.HainButton
import app.hain.ui.components.HainButtonStyle
import app.hain.ui.components.HainCard
import app.hain.ui.components.HainIconButton
import app.hain.ui.components.HainPopover
import app.hain.ui.components.HainPopupItem
import app.hain.ui.components.TopBar
import app.hain.ui.motion.rememberHainHaptics
import app.hain.ui.theme.LocalHainColors
import app.hain.ui.theme.LocalHainShapes
import app.hain.ui.theme.LocalHainSpacing
import app.hain.ui.theme.LocalHainTypography

/** Häppchen 9: the paper clip in the message box with "Datei oder Foto" and "Meine Dateien". */
@Composable
internal fun AttachButton(
    onFile: () -> Unit,
    onLibrary: () -> Unit,
    enabled: Boolean = true,
) {
    var open by rememberSaveable { mutableStateOf(false) }
    val haptics = rememberHainHaptics()
    Box {
        HainIconButton(
            painterResource(R.drawable.ic_paperclip),
            stringResource(R.string.docs_attach),
            {
                haptics.tick()
                open = !open
            },
            tint = LocalHainColors.current.icon,
            enabled = enabled,
        )
        HainPopover(expanded = open, onDismiss = { open = false }) {
            HainPopupItem(HainIcons.image, stringResource(R.string.docs_pick_file), {
                open = false
                onFile()
            })
            HainPopupItem(HainIcons.document, stringResource(R.string.docs_my_files), {
                open = false
                onLibrary()
            })
        }
    }
}

/** "Rechnung.pdf · 3 Seiten" */
@Composable
internal fun chipLabel(doc: DocAttachment): String =
    if (doc.pages <= 1) {
        stringResource(R.string.docs_chip_one, doc.name)
    } else {
        stringResource(R.string.docs_chip_many, doc.name, doc.pages)
    }

/** File chips; with [onRemove] each chip is a button that takes the file off again. */
@Composable
internal fun AttachmentChips(
    docs: List<DocAttachment>,
    modifier: Modifier = Modifier,
    onRemove: ((DocAttachment) -> Unit)? = null,
) {
    if (docs.isEmpty()) return
    val spacing = LocalHainSpacing.current
    Row(
        modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        docs.forEach { doc -> FileChip(doc, onRemove) }
    }
}

@Composable
private fun FileChip(
    doc: DocAttachment,
    onRemove: ((DocAttachment) -> Unit)?,
) {
    val colors = LocalHainColors.current
    val type = LocalHainTypography.current
    val spacing = LocalHainSpacing.current
    val label = chipLabel(doc)
    val removeLabel = stringResource(R.string.docs_remove_attachment, doc.name)
    val tap =
        if (onRemove == null) {
            Modifier
        } else {
            Modifier
                .semantics(mergeDescendants = true) {
                    role = Role.Button
                    contentDescription = removeLabel
                }.clickable { onRemove(doc) }
        }
    Box(Modifier.heightIn(min = TOUCH.dp).then(tap), contentAlignment = Alignment.Center) {
        Row(
            Modifier
                .clip(LocalHainShapes.current.chip)
                .background(colors.accentSoft)
                .padding(horizontal = spacing.md, vertical = CHIP_V.dp)
                .widthIn(max = CHIP_MAX.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                if (doc.kind == DocKind.IMAGE) HainIcons.image else HainIcons.document,
                null,
                colorFilter = ColorFilter.tint(colors.accent),
                modifier = Modifier.size(ICON.dp),
            )
            Spacer(Modifier.width(spacing.xs))
            BasicText(
                label,
                style = type.label.copy(color = colors.text),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
            if (onRemove != null) {
                Spacer(Modifier.width(spacing.xs))
                Image(
                    HainIcons.close,
                    null,
                    colorFilter = ColorFilter.tint(colors.textMuted),
                    modifier = Modifier.size(ICON.dp),
                )
            }
        }
    }
}

/**
 * Everything about files above the message box: the "Texterkennung laden?" card, the reading
 * progress, a problem, and the chips of the files waiting for the next message.
 */
@Suppress("LongParameterList")
@Composable
internal fun DocsBar(
    state: ChatDocs.State,
    attachments: List<DocAttachment>,
    packMegabytes: String,
    onAnswer: (OcrChoice) -> Unit,
    onDismiss: () -> Unit,
    onRemove: (DocAttachment) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = LocalHainSpacing.current
    Column(
        modifier.fillMaxWidth().padding(horizontal = spacing.md),
        verticalArrangement = Arrangement.spacedBy(spacing.sm),
    ) {
        state.ask?.let { ask -> OcrAskCard(ask, packMegabytes, onAnswer) }
        progressText(state)?.let { (text, fraction) -> ProgressLine(text, fraction) }
        errorText(state)?.let { ErrorRow(it, onDismiss) }
        AttachmentChips(attachments, onRemove = onRemove)
    }
}

@Composable
private fun OcrAskCard(
    ask: ChatDocs.Ask,
    packMegabytes: String,
    onAnswer: (OcrChoice) -> Unit,
) {
    val spacing = LocalHainSpacing.current
    val colors = LocalHainColors.current
    HainCard(title = stringResource(R.string.docs_ocr_title)) {
        BasicText(
            stringResource(R.string.docs_ocr_body, packMegabytes),
            style = LocalHainTypography.current.body.copy(color = colors.textMuted),
        )
        Spacer(Modifier.height(spacing.md))
        Row(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
            HainButton(stringResource(R.string.docs_ocr_load), { onAnswer(OcrChoice.LOAD) })
            if (ask.textPages > 0) {
                HainButton(
                    stringResource(R.string.docs_ocr_without),
                    { onAnswer(OcrChoice.WITHOUT) },
                    style = HainButtonStyle.SECONDARY,
                )
            }
            HainButton(
                stringResource(R.string.docs_ocr_cancel),
                { onAnswer(OcrChoice.CANCEL) },
                style = HainButtonStyle.GHOST,
            )
        }
    }
}

@Composable
private fun progressText(state: ChatDocs.State): Pair<String, Float?>? {
    state.download?.let { f ->
        return stringResource(R.string.docs_ocr_loading, (f * PERCENT).toInt()) to f
    }
    return when (val step = state.step) {
        null -> null
        is ImportStep.Reading ->
            stringResource(R.string.docs_reading, step.page, step.total) to step.page.toFloat() / step.total
        is ImportStep.Recognizing ->
            stringResource(R.string.docs_recognizing, step.page, step.total) to
                (step.page - 1).toFloat() / step.total
        ImportStep.Saving -> stringResource(R.string.docs_saving) to null
    }
}

@Composable
private fun errorText(state: ChatDocs.State): String? {
    if (state.packFailed) return stringResource(R.string.docs_ocr_failed)
    return when (state.error) {
        null -> null
        ImportError.UNREADABLE -> stringResource(R.string.docs_error_unreadable)
        ImportError.LOCKED -> stringResource(R.string.docs_error_locked)
        ImportError.EMPTY -> stringResource(R.string.docs_error_empty)
        ImportError.TOO_BIG -> stringResource(R.string.docs_error_too_big)
        ImportError.UNSUPPORTED -> stringResource(R.string.docs_error_unsupported)
        ImportError.NO_OCR -> stringResource(R.string.docs_error_no_ocr)
    }
}

/** A quiet line with a thin green bar; no bar when the length is unknown. */
@Composable
private fun ProgressLine(
    text: String,
    fraction: Float?,
) {
    val colors = LocalHainColors.current
    val spacing = LocalHainSpacing.current
    Column(Modifier.fillMaxWidth().semantics(mergeDescendants = true) {}) {
        BasicText(text, style = LocalHainTypography.current.label.copy(color = colors.textMuted))
        Spacer(Modifier.height(spacing.xs))
        Box(
            Modifier
                .fillMaxWidth()
                .height(BAR.dp)
                .clip(LocalHainShapes.current.chip)
                .background(colors.accentSoft),
        ) {
            Box(
                Modifier
                    .fillMaxWidth((fraction ?: 1f).coerceIn(0f, 1f))
                    .height(BAR.dp)
                    .background(colors.accent),
            )
        }
    }
}

@Composable
private fun ErrorRow(
    text: String,
    onDismiss: () -> Unit,
) {
    val colors = LocalHainColors.current
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        BasicText(
            text,
            style = LocalHainTypography.current.label.copy(color = colors.danger),
            modifier = Modifier.weight(1f),
        )
        HainIconButton(HainIcons.close, stringResource(R.string.docs_dismiss), onDismiss, tint = colors.textMuted)
    }
}

/**
 * "Meine Dateien": every document Hain has read, newest first, to attach again or delete, and the
 * text recognition pack (load or "Pack entfernen").
 */
@Suppress("LongMethod")
@Composable
fun MyFilesScreen(
    docs: ChatDocs,
    onAttach: (DocAttachment) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler(onBack = onBack)
    val colors = LocalHainColors.current
    val spacing = LocalHainSpacing.current
    val files by docs.documents.collectAsStateWithLifecycle(emptyList())
    val installed by docs.ocrInstalled.collectAsStateWithLifecycle()
    val state by docs.state.collectAsStateWithLifecycle()
    val haptics = rememberHainHaptics()
    Column(
        modifier
            .fillMaxSize()
            .background(colors.bg)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        TopBar(
            title = stringResource(R.string.docs_my_files),
            leading = { HainIconButton(HainIcons.back, stringResource(R.string.docs_back), onBack) },
        )
        LazyColumn(
            Modifier.weight(1f).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(spacing.xs),
        ) {
            if (files.isEmpty()) {
                item {
                    EmptyState(
                        title = stringResource(R.string.docs_library_empty),
                        body = stringResource(R.string.docs_library_empty_body),
                        modifier = Modifier.padding(spacing.xl),
                    )
                }
            }
            items(files, key = { it.id }) { file ->
                FileRow(
                    file,
                    onAttach = {
                        haptics.select()
                        onAttach(DocAttachment.of(file))
                    },
                    onDelete = {
                        haptics.tick()
                        docs.delete(file.id)
                    },
                )
            }
            item {
                PackCard(
                    installed = installed,
                    megabytes = docs.packMegabytes,
                    state = state,
                    onLoad = docs::loadPack,
                    onRemove = docs::removePack,
                    modifier = Modifier.padding(spacing.md),
                )
            }
        }
    }
}

@Composable
private fun FileRow(
    file: DocInfo,
    onAttach: () -> Unit,
    onDelete: () -> Unit,
) {
    val colors = LocalHainColors.current
    val type = LocalHainTypography.current
    val spacing = LocalHainSpacing.current
    val attachLabel = stringResource(R.string.docs_attach_this, file.name)
    val chip = chipLabel(DocAttachment.of(file))
    val detail =
        when {
            file.missingPages > 0 -> stringResource(R.string.docs_missing_pages, file.missingPages)
            file.ocrPages > 0 -> stringResource(R.string.docs_ocr_badge)
            else -> null
        }
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = TOUCH.dp)
            .semantics(mergeDescendants = true) {
                role = Role.Button
                contentDescription = attachLabel
            }.clickable(onClick = onAttach)
            .padding(horizontal = spacing.md, vertical = spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Thumbnail(file)
        Spacer(Modifier.width(spacing.md))
        Column(Modifier.weight(1f)) {
            BasicText(chip, style = type.body.copy(color = colors.text), maxLines = 1, overflow = TextOverflow.Ellipsis)
            detail?.let { BasicText(it, style = type.label.copy(color = colors.textMuted), maxLines = 1) }
        }
        HainIconButton(
            HainIcons.delete,
            stringResource(R.string.docs_delete, file.name),
            onDelete,
            tint = colors.textMuted,
        )
    }
}

@Composable
private fun Thumbnail(file: DocInfo) {
    val colors = LocalHainColors.current
    val bitmap: ImageBitmap? =
        remember(file.id) {
            file.thumbnail?.let { bytes ->
                runCatching { BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap() }.getOrNull()
            }
        }
    Box(
        Modifier
            .size(THUMB.dp)
            .clip(LocalHainShapes.current.chip)
            .background(colors.surfaceHigh),
        contentAlignment = Alignment.Center,
    ) {
        if (bitmap != null) {
            Image(bitmap, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        } else {
            Image(
                if (file.kind == DocKind.IMAGE) HainIcons.image else HainIcons.document,
                null,
                colorFilter = ColorFilter.tint(colors.icon),
                modifier = Modifier.size(ICON_BIG.dp),
            )
        }
    }
}

@Suppress("LongParameterList")
@Composable
private fun PackCard(
    installed: Boolean,
    megabytes: String,
    state: ChatDocs.State,
    onLoad: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalHainColors.current
    val type = LocalHainTypography.current
    val spacing = LocalHainSpacing.current
    HainCard(modifier, title = stringResource(R.string.docs_pack_title)) {
        BasicText(
            stringResource(if (installed) R.string.docs_pack_installed else R.string.docs_pack_missing, megabytes),
            style = type.body.copy(color = colors.text),
        )
        Spacer(Modifier.height(spacing.xs))
        BasicText(stringResource(R.string.docs_pack_note), style = type.label.copy(color = colors.textMuted))
        Spacer(Modifier.height(spacing.md))
        val progress = progressText(state)
        when {
            state.download != null && progress != null -> ProgressLine(progress.first, progress.second)
            installed ->
                HainButton(stringResource(R.string.docs_pack_remove), onRemove, style = HainButtonStyle.SECONDARY)
            else -> HainButton(stringResource(R.string.docs_ocr_load), onLoad, enabled = !state.busy)
        }
        if (state.packFailed) {
            Spacer(Modifier.height(spacing.xs))
            BasicText(stringResource(R.string.docs_ocr_failed), style = type.label.copy(color = colors.danger))
        }
    }
}

private const val TOUCH = 48
private const val CHIP_V = 6
private const val CHIP_MAX = 260
private const val ICON = 14
private const val ICON_BIG = 22
private const val THUMB = 44
private const val BAR = 3
private const val PERCENT = 100
