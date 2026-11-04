package app.hain.feature.chat.export

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts.CreateDocument
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import app.hain.feature.chat.R
import app.hain.feature.chat.internal.Chat
import app.hain.ui.LocalExternalLaunch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.OutputStream
import java.text.DateFormat
import java.time.Instant
import java.time.ZoneId
import java.util.Date

/** The two file kinds a chat can be exported as (T-054). */
enum class ExportFormat(
    val extension: String,
    val mime: String,
) {
    MARKDOWN("md", "text/markdown"),
    PDF("pdf", "application/pdf"),
}

/** Save (file picker) or share (share sheet) one chat; see [rememberChatExport]. */
class ChatExportActions internal constructor(
    val save: (Chat, ExportFormat) -> Unit,
    val share: (Chat, ExportFormat) -> Unit,
)

/**
 * Export of a single chat (T-054): "Speichern" opens the system file picker (Storage Access
 * Framework, you choose the folder), "Teilen" writes the file into the cache and opens the share
 * sheet. Private chats are never exported. [find] looks the chat up again after the file picker.
 */
@Composable
fun rememberChatExport(find: (Long) -> Chat?): ChatExportActions {
    val context = LocalContext.current
    val external = LocalExternalLaunch.current
    val scope = rememberCoroutineScope()
    // survives the activity being recreated while the file picker is open
    var pendingId by rememberSaveable { mutableStateOf<Long?>(null) }
    var pendingFormat by rememberSaveable { mutableStateOf(ExportFormat.MARKDOWN) }

    fun onPicked(uri: Uri?) {
        val chat = pendingId?.let(find)
        pendingId = null
        if (uri == null || chat == null) return
        scope.saveTo(context, uri, chat, pendingFormat)
    }
    val saveMarkdown = rememberLauncherForActivityResult(CreateDocument(ExportFormat.MARKDOWN.mime), ::onPicked)
    val savePdf = rememberLauncherForActivityResult(CreateDocument(ExportFormat.PDF.mime), ::onPicked)
    return ChatExportActions(
        save = { chat, format ->
            if (!chat.isPrivate) {
                pendingId = chat.id
                pendingFormat = format
                external()
                val name = ExportFileName.of(chat, fileDate(chat.updatedAt), format.extension)
                runCatching {
                    if (format == ExportFormat.PDF) savePdf.launch(name) else saveMarkdown.launch(name)
                }.onFailure { toast(context, R.string.chat_export_failed) }
            }
        },
        share = { chat, format ->
            if (!chat.isPrivate) {
                scope.launch {
                    val file =
                        runCatching { withContext(Dispatchers.IO) { cacheFile(context, chat, format) } }.getOrNull()
                    if (file == null) {
                        toast(context, R.string.chat_export_failed)
                        return@launch
                    }
                    val uri = FileProvider.getUriForFile(context, context.packageName + AUTHORITY, file)
                    val send =
                        Intent(Intent.ACTION_SEND)
                            .setType(format.mime)
                            .putExtra(Intent.EXTRA_STREAM, uri)
                            .putExtra(Intent.EXTRA_SUBJECT, chat.title)
                            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    external()
                    val title = context.getString(R.string.chat_export_share_title)
                    context.startActivity(Intent.createChooser(send, title))
                }
            }
        },
    )
}

/** Labels in the app language; the date line names the day the chat started. */
fun exportLabels(
    context: Context,
    chat: Chat,
): ExportLabels {
    val date = DateFormat.getDateInstance(DateFormat.LONG).format(Date(chat.createdAt))
    return ExportLabels(
        you = context.getString(R.string.chat_export_you),
        hain = context.getString(R.string.chat_title),
        exportedLine = context.getString(R.string.chat_export_line, date),
        sources = context.getString(R.string.chat_export_sources),
        pageOf = context.getString(R.string.chat_export_page),
        untitled = context.getString(R.string.chat_export_untitled),
        stopped = context.getString(R.string.chat_stopped),
    )
}

/** Writes one chat in [format] to [out]. */
fun writeChat(
    context: Context,
    chat: Chat,
    format: ExportFormat,
    out: OutputStream,
) {
    val labels = exportLabels(context, chat)
    when (format) {
        ExportFormat.MARKDOWN -> out.write(ChatMarkdown.export(chat, labels).toByteArray(Charsets.UTF_8))
        ExportFormat.PDF -> ChatPdf(context).write(chat, labels, out)
    }
}

/** "2026-11-04" in the phone's time zone, for file names. */
fun fileDate(millis: Long): String =
    Instant
        .ofEpochMilli(millis)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
        .toString()

private fun CoroutineScope.saveTo(
    context: Context,
    uri: Uri,
    chat: Chat,
    format: ExportFormat,
) = launch {
    val ok =
        withContext(Dispatchers.IO) {
            runCatching {
                context.contentResolver.openOutputStream(uri)?.use { writeChat(context, chat, format, it) } != null
            }.getOrDefault(false)
        }
    toast(context, if (ok) R.string.chat_export_saved else R.string.chat_export_failed)
}

/** Exports older than a few minutes are cleared first, so the cache never grows. */
private fun cacheFile(
    context: Context,
    chat: Chat,
    format: ExportFormat,
): File {
    val dir = File(context.cacheDir, EXPORT_DIR).apply { mkdirs() }
    val old = System.currentTimeMillis() - KEEP_MS
    dir.listFiles()?.filter { it.lastModified() < old }?.forEach { it.delete() }
    val file = File(dir, ExportFileName.of(chat, fileDate(chat.updatedAt), format.extension))
    file.outputStream().use { writeChat(context, chat, format, it) }
    return file
}

private fun toast(
    context: Context,
    text: Int,
) = Toast.makeText(context, text, Toast.LENGTH_SHORT).show()

private const val EXPORT_DIR = "exports"
private const val KEEP_MS = 10 * 60 * 1000L
private const val AUTHORITY = ".exports"
