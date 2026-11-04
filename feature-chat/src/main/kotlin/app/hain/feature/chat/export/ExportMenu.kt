package app.hain.feature.chat.export

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import app.hain.feature.chat.R
import app.hain.feature.chat.internal.Chat
import app.hain.ui.HainIcons
import app.hain.ui.components.HainIconButton
import app.hain.ui.components.HainPopupItem
import app.hain.ui.components.HainPopupMenu
import app.hain.ui.theme.LocalHainColors

/** The four ways out of a chat (T-054): PDF or Markdown, each to share or to save as a file. */
@Composable
internal fun ExportItems(onPick: (format: ExportFormat, share: Boolean) -> Unit) {
    HainPopupItem(
        HainIcons.share,
        stringResource(R.string.chat_export_pdf_share),
        { onPick(ExportFormat.PDF, true) },
    )
    HainPopupItem(
        HainIcons.download,
        stringResource(R.string.chat_export_pdf_save),
        { onPick(ExportFormat.PDF, false) },
    )
    HainPopupItem(
        HainIcons.share,
        stringResource(R.string.chat_export_md_share),
        { onPick(ExportFormat.MARKDOWN, true) },
    )
    HainPopupItem(
        HainIcons.download,
        stringResource(R.string.chat_export_md_save),
        { onPick(ExportFormat.MARKDOWN, false) },
    )
}

/** Share button in the top bar of an open chat; opens [ExportItems] as a glass menu right under it. */
@Composable
internal fun ExportButton(
    chat: Chat,
    export: ChatExportActions,
) {
    var open by remember { mutableStateOf(false) }
    val density = LocalDensity.current
    Box {
        HainIconButton(
            HainIcons.share,
            stringResource(R.string.chat_export),
            { open = true },
            tint = LocalHainColors.current.icon,
        )
        HainPopupMenu(
            expanded = open,
            onDismiss = { open = false },
            alignment = Alignment.TopEnd,
            offset = with(density) { IntOffset(MENU_SHADOW_ROOM.dp.roundToPx(), MENU_OFFSET.dp.roundToPx()) },
        ) {
            ExportItems { format, share ->
                open = false
                if (share) export.share(chat, format) else export.save(chat, format)
            }
        }
    }
}

private const val MENU_OFFSET = 32
private const val MENU_SHADOW_ROOM = 12
