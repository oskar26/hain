package app.hain.feature.memory.api

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.hain.feature.memory.R
import app.hain.memory.api.MemoryFact
import app.hain.memory.api.MemoryStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import java.io.IOException

/**
 * Settings → Gedächtnis (T-115, MEMORY.md §5): see, edit and delete what Pim keeps, accept or drop
 * suggestions, "Über dich", memory on/off, export/import as a JSON file the user picks (Android's
 * file picker, nothing goes online) and delete everything.
 */
@Suppress("LongMethod") // the page's state, its file pickers and its actions in one place
@Composable
fun MemoryScreen(
    store: MemoryStore,
    aboutMe: String,
    onAboutMe: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val facts by store.facts.collectAsStateWithLifecycle()
    val enabled by store.enabled.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var notice by remember { mutableStateOf<Notice?>(null) }
    val deleted = stringResource(R.string.memory_deleted)
    val exported = stringResource(R.string.memory_exported)
    val failed = stringResource(R.string.memory_import_failed)
    var imported by remember { mutableStateOf<Int?>(null) }
    val importedText = imported?.let { pluralStringResource(R.plurals.memory_imported, it, it) }
    LaunchedEffect(importedText) { if (importedText != null) notice = Notice(importedText) }
    LaunchedEffect(notice) {
        if (notice != null) {
            delay(NOTICE_MS)
            notice = null
        }
    }

    val exporter =
        rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
            if (uri == null) return@rememberLauncherForActivityResult
            scope.launch {
                val ok =
                    withContext(Dispatchers.IO) {
                        runCatching {
                            val bytes = store.exportJson().toByteArray()
                            context.contentResolver.openOutputStream(uri)?.use { it.write(bytes) }
                        }.isSuccess
                    }
                notice = Notice(if (ok) exported else failed)
            }
        }
    val importer =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri == null) return@rememberLauncherForActivityResult
            scope.launch {
                val text =
                    withContext(Dispatchers.IO) {
                        runCatching {
                            context.contentResolver.openInputStream(uri)?.use { it.readBytes().decodeToString() }
                        }.getOrNull()
                    }
                val count = text?.let { importOrNull(store, it) }
                if (count == null) notice = Notice(failed) else imported = count
            }
        }

    val current = notice
    MemoryPage(
        state = MemoryPageState(facts, enabled, aboutMe, current?.text, current?.undo),
        actions =
            MemoryActions(
                onBack = onBack,
                onEnabled = store::setEnabled,
                onEdit = { id, text -> scope.launch { store.edit(id, text) } },
                onDelete = { fact ->
                    scope.launch { store.delete(fact.id) }
                    notice = Notice(deleted, fact)
                },
                onUndo = {
                    current?.undo?.let { fact -> scope.launch { store.restore(fact) } }
                    notice = null
                },
                onAccept = { id -> scope.launch { store.accept(id) } },
                onDeleteAll = { scope.launch { store.deleteAll() } },
                onAboutMe = onAboutMe,
                onExport = { exporter.launch(EXPORT_NAME) },
                onImport = { importer.launch(arrayOf("application/json", "text/plain", "*/*")) },
            ),
        modifier = modifier,
    )
}

/** Number of new facts, or `null` when the file is not a Hain memory file. */
@Suppress("SwallowedException") // a wrong file is a normal answer, shown as a short line
private suspend fun importOrNull(
    store: MemoryStore,
    json: String,
): Int? =
    try {
        store.importJson(json)
    } catch (e: IllegalArgumentException) {
        null
    } catch (e: SerializationException) {
        null
    } catch (e: IOException) {
        null
    }

private data class Notice(
    val text: String,
    val undo: MemoryFact? = null,
)

private const val NOTICE_MS = 4_000L
private const val EXPORT_NAME = "hain-gedaechtnis.json"
