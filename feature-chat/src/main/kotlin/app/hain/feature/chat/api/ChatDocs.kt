package app.hain.feature.chat.api

import android.net.Uri
import app.hain.docs.api.DocAttachment
import app.hain.docs.api.DocInfo
import app.hain.docs.api.DocLibrary
import app.hain.docs.api.ImportError
import app.hain.docs.api.ImportOutcome
import app.hain.docs.api.ImportStep
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Häppchen 9: attaching files in the chat. Reads a picked file into "Meine Dateien" with a progress
 * line, asks "Texterkennung laden?" before the pack is downloaded, and hands the finished document
 * to the message box. One file at a time.
 */
class ChatDocs(
    private val library: DocLibrary,
    private val scope: CoroutineScope,
) {
    /** A file with picture pages, waiting for the user's answer about the text recognition. */
    data class Ask(
        val uri: Uri,
        val scanPages: Int,
        val textPages: Int,
    )

    data class State(
        val step: ImportStep? = null,
        val ask: Ask? = null,
        /** 0…1 while the text recognition pack loads. */
        val download: Float? = null,
        val error: ImportError? = null,
        val packFailed: Boolean = false,
    ) {
        val busy: Boolean get() = step != null || download != null
    }

    private val mutable = MutableStateFlow(State())
    val state: StateFlow<State> = mutable.asStateFlow()

    val documents: Flow<List<DocInfo>> get() = library.documents
    val ocrInstalled: StateFlow<Boolean> get() = library.ocrInstalled

    /** Size of the text recognition pack in MB, for "Texterkennung laden? (14 MB)". */
    val packMegabytes: String get() = "%.0f".format(library.pack.totalBytes / BYTES_PER_MB)

    private var onDone: (DocAttachment) -> Unit = {}
    private var job: Job? = null

    /** Reads [uri]; [done] gets the document for the message box. */
    fun open(
        uri: Uri,
        done: (DocAttachment) -> Unit,
    ) {
        if (mutable.value.busy) return
        onDone = done
        run(uri, withoutOcr = false)
    }

    /** Answer to "Texterkennung laden?": load the pack, read only the text pages, or cancel. */
    fun answer(choice: OcrChoice) {
        val ask = mutable.value.ask ?: return
        mutable.update { it.copy(ask = null) }
        when (choice) {
            OcrChoice.LOAD ->
                job =
                    scope.launch {
                        mutable.update { it.copy(download = 0f, packFailed = false) }
                        val ok =
                            library.downloadOcr { done, total ->
                                mutable.update { it.copy(download = if (total > 0) done.toFloat() / total else 0f) }
                            }
                        mutable.update { it.copy(download = null, packFailed = !ok) }
                        if (ok) run(ask.uri, withoutOcr = false)
                    }
            OcrChoice.WITHOUT -> run(ask.uri, withoutOcr = true)
            OcrChoice.CANCEL -> Unit
        }
    }

    fun dismissError() = mutable.update { it.copy(error = null, packFailed = false) }

    fun delete(id: Long) {
        scope.launch { library.delete(id) }
    }

    fun removePack() = library.removeOcr()

    /** Loads the pack from "Meine Dateien" without a file waiting. */
    fun loadPack() {
        if (mutable.value.busy) return
        job =
            scope.launch {
                mutable.update { it.copy(download = 0f, packFailed = false) }
                val ok =
                    library.downloadOcr { done, total ->
                        mutable.update { it.copy(download = if (total > 0) done.toFloat() / total else 0f) }
                    }
                mutable.update { it.copy(download = null, packFailed = !ok) }
            }
    }

    private fun run(
        uri: Uri,
        withoutOcr: Boolean,
    ) {
        job =
            scope.launch {
                mutable.update { it.copy(step = ImportStep.Reading(1, 1), error = null) }
                val outcome =
                    try {
                        library.import(uri, withoutOcr) { step -> mutable.update { it.copy(step = step) } }
                    } finally {
                        mutable.update { it.copy(step = null) }
                    }
                when (outcome) {
                    is ImportOutcome.Done -> onDone(DocAttachment.of(outcome.doc))
                    is ImportOutcome.NeedsOcr ->
                        mutable.update { it.copy(ask = Ask(uri, outcome.scanPages, outcome.textPages)) }
                    is ImportOutcome.Failed -> mutable.update { it.copy(error = outcome.error) }
                }
            }
    }

    private companion object {
        const val BYTES_PER_MB = 1_000_000.0
    }
}

/** What the user answered to "Texterkennung laden?". */
enum class OcrChoice { LOAD, WITHOUT, CANCEL }
