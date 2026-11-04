package app.hain.docs.api

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.provider.OpenableColumns
import app.hain.common.api.net.NetworkGate
import app.hain.data.KeyProvider
import app.hain.docs.fix.GermanOcrFixer
import app.hain.docs.fix.WordList
import app.hain.docs.internal.chunk.Chunker
import app.hain.docs.internal.db.DocChunkEntity
import app.hain.docs.internal.db.DocsDatabase
import app.hain.docs.internal.db.DocumentEntity
import app.hain.docs.internal.ephemeral.EphemeralDocs
import app.hain.docs.internal.pdf.LockedPdfException
import app.hain.docs.internal.pdf.PdfPages
import app.hain.docs.internal.pdf.PdfTextExtractor
import app.hain.docs.internal.search.SearchChunk
import app.hain.docs.internal.text.TextFiles
import app.hain.docs.ocr.OcrPack
import app.hain.docs.ocr.OrtOcrEngine
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import kotlin.math.max

/** What came of reading a file. */
sealed interface ImportOutcome {
    data class Done(
        val doc: DocInfo,
    ) : ImportOutcome

    /** Picture pages need the text recognition, which is not on the phone yet: ask first. */
    data class NeedsOcr(
        val scanPages: Int,
        val textPages: Int,
    ) : ImportOutcome

    data class Failed(
        val error: ImportError,
    ) : ImportOutcome
}

/**
 * "Meine Dateien" (T-120–T-123): reads PDFs, photos and text files into the encrypted
 * `hain_docs.db`, with the text recognition pack for scans and photos, and gives the chat what it
 * needs: whole small documents for the prompt, the best parts of long ones for `search_document`.
 * The file itself is never kept; a copy in the cache is deleted right after reading.
 * Files read for a private chat (`ephemeral`) stay only in memory and never go into the database.
 */
@Suppress("TooManyFunctions")
class DocLibrary(
    context: Context,
    private val keys: KeyProvider,
    gate: NetworkGate,
) {
    private val app = context.applicationContext
    private val db by lazy { DocsDatabase.open(app, keys).docs() }
    val pack = OcrPack(File(app.filesDir, OcrPack.DIR), gate)
    private val packReady = MutableStateFlow(pack.installed())
    private val privateDocs = EphemeralDocs()

    /** `true` when the text recognition pack is on the phone. */
    val ocrInstalled: StateFlow<Boolean> = packReady.asStateFlow()

    val documents: Flow<List<DocInfo>> by lazy { db.all().map { list -> list.map { it.toInfo() } } }

    /** The search tool for the chat's tool router. */
    val searchTool: SearchDocumentTool by lazy { SearchDocumentTool { ids -> searchChunks(ids) } }

    /**
     * Reads [uri]. Without the pack, picture pages give [ImportOutcome.NeedsOcr] unless [withoutOcr]
     * (then a PDF keeps only its text pages). [onStep] reports progress. With [ephemeral] (a private
     * chat) the text is only kept in memory until [forgetEphemeral], never in "Meine Dateien".
     */
    @Suppress("TooGenericExceptionCaught", "SwallowedException")
    suspend fun import(
        uri: Uri,
        withoutOcr: Boolean = false,
        ephemeral: Boolean = false,
        onStep: (ImportStep) -> Unit = {},
    ): ImportOutcome =
        withContext(Dispatchers.IO) {
            val (name, size) = nameAndSize(uri)
            if (size > MAX_BYTES) return@withContext ImportOutcome.Failed(ImportError.TOO_BIG)
            val mime = app.contentResolver.getType(uri)
            val file = File(app.cacheDir, "import_" + System.nanoTime())
            try {
                app.contentResolver.openInputStream(uri)?.use { input ->
                    file.outputStream().use { input.copyTo(it) }
                } ?: return@withContext ImportOutcome.Failed(ImportError.UNREADABLE)
                if (file.length() > MAX_BYTES) return@withContext ImportOutcome.Failed(ImportError.TOO_BIG)
                val target = Target(name, mime, ephemeral)
                when (kindOf(name, mime, file)) {
                    DocKind.PDF -> readPdf(file, target, withoutOcr, onStep)
                    DocKind.IMAGE -> readImage(file, target, onStep)
                    DocKind.TEXT -> readText(file, target, onStep)
                    null -> ImportOutcome.Failed(ImportError.UNSUPPORTED)
                }
            } catch (e: LockedPdfException) {
                ImportOutcome.Failed(ImportError.LOCKED)
            } catch (e: IOException) {
                ImportOutcome.Failed(ImportError.UNREADABLE)
            } catch (e: OutOfMemoryError) {
                ImportOutcome.Failed(ImportError.TOO_BIG)
            } catch (e: RuntimeException) {
                ImportOutcome.Failed(ImportError.UNREADABLE)
            } finally {
                file.delete()
            }
        }

    /** Downloads the text recognition pack (after the user said yes). `false` when it failed. */
    suspend fun downloadOcr(onProgress: (done: Long, total: Long) -> Unit = { _, _ -> }): Boolean =
        withContext(Dispatchers.IO) {
            val ok = runCatching { pack.download(onProgress) }.isSuccess
            packReady.value = pack.installed()
            ok && packReady.value
        }

    /** "Pack entfernen": the documents and their text stay. */
    fun removeOcr() {
        pack.remove()
        packReady.value = pack.installed()
    }

    suspend fun delete(id: Long) {
        if (EphemeralDocs.isEphemeral(id)) {
            privateDocs.remove(id)
        } else {
            withContext(Dispatchers.IO) { db.delete(id) }
        }
    }

    /** Forgets every file of a private chat (the chat ended). */
    fun forgetEphemeral() = privateDocs.clear()

    suspend fun info(ids: List<Long>): List<DocInfo> =
        withContext(Dispatchers.IO) {
            val (temp, saved) = ids.partition { EphemeralDocs.isEphemeral(it) }
            val fromDb = if (saved.isEmpty()) emptyList() else db.byIds(saved).map { it.toInfo() }
            fromDb + privateDocs.info(temp)
        }

    /** The pages of a document again, from its parts. */
    suspend fun pages(id: Long): List<DocPage> =
        withContext(Dispatchers.IO) {
            if (EphemeralDocs.isEphemeral(id)) return@withContext privateDocs.pages(id)
            db
                .chunks(listOf(id))
                .groupBy { it.page }
                .toSortedMap()
                .map { (page, parts) ->
                    DocPage(page, parts.sortedBy { it.index }.joinToString("\n\n") { it.text }, parts.any { it.ocr })
                }
        }

    /**
     * The document blocks for the prompt of a chat whose messages carry [attachments]: whole small
     * documents, and notes for long ones (those are read with `search_document`). Deleted files drop out.
     */
    suspend fun forPrompt(attachments: List<DocAttachment>): DocPrompt.ForChat {
        val existing = info(attachments.map { it.id }.distinct()).associateBy { it.id }
        val plan = DocPrompt.plan(attachments.filter { it.id in existing })
        val blocks =
            plan.inline.associate { it.id to DocPrompt.inline(it.name, pages(it.id)) } +
                plan.search.associate { it.id to DocPrompt.searchNote(it) }
        return DocPrompt.ForChat(blocks, plan.search.map { it.id })
    }

    private suspend fun searchChunks(ids: List<Long>): List<SearchChunk> =
        withContext(Dispatchers.IO) {
            val (temp, saved) = ids.partition { EphemeralDocs.isEphemeral(it) }
            val names = if (saved.isEmpty()) emptyMap() else db.byIds(saved).associate { it.id to it.name }
            val fromDb =
                if (saved.isEmpty()) {
                    emptyList()
                } else {
                    db.chunks(saved).map { SearchChunk(names[it.documentId].orEmpty(), it.page, it.text) }
                }
            fromDb + privateDocs.chunks(temp)
        }

    @Suppress("LongMethod", "ReturnCount")
    private suspend fun readPdf(
        file: File,
        target: Target,
        withoutOcr: Boolean,
        onStep: (ImportStep) -> Unit,
    ): ImportOutcome {
        PDFBoxResourceLoader.init(app)
        val texts = PdfTextExtractor.pages(file) { page, total -> onStep(ImportStep.Reading(page, total)) }
        if (texts.isEmpty()) return ImportOutcome.Failed(ImportError.EMPTY)
        val scans = texts.indices.filter { PdfTextExtractor.needsOcr(texts[it]) }
        if (scans.isNotEmpty() && !packReady.value && !withoutOcr) {
            return ImportOutcome.NeedsOcr(scans.size, texts.size - scans.size)
        }
        val pages = texts.mapIndexed { i, t -> DocPage(i + 1, if (i in scans) "" else t) }.toMutableList()
        var ocrPages = 0
        val thumbnail = PdfPages(file).use { pdf -> runCatching { pdf.thumbnail(0) }.getOrNull() }
        if (scans.isNotEmpty() && packReady.value) {
            val fixer = fixer()
            OrtOcrEngine(pack).use { engine ->
                PdfPages(file).use { pdf ->
                    scans.forEachIndexed { n, i ->
                        onStep(ImportStep.Recognizing(n + 1, scans.size))
                        val bitmap = pdf.render(i)
                        val result =
                            try {
                                engine.read(bitmap)
                            } finally {
                                bitmap.recycle()
                            }
                        if (result.text.isNotBlank()) {
                            val text = fixer.fix(result.text)
                            pages[i] = DocPage(i + 1, text, ocr = true, confidence = result.confidence)
                            ocrPages++
                        }
                    }
                }
            }
        }
        val missing = pages.count { it.text.isBlank() }
        if (missing == pages.size) {
            return ImportOutcome.Failed(if (packReady.value) ImportError.EMPTY else ImportError.NO_OCR)
        }
        return save(target, DocKind.PDF, pages, ocrPages, missing, thumbnail, onStep)
    }

    @Suppress("ReturnCount")
    private suspend fun readImage(
        file: File,
        target: Target,
        onStep: (ImportStep) -> Unit,
    ): ImportOutcome {
        if (!packReady.value) return ImportOutcome.NeedsOcr(scanPages = 1, textPages = 0)
        val bitmap = decodeUpright(file) ?: return ImportOutcome.Failed(ImportError.UNREADABLE)
        onStep(ImportStep.Recognizing(1, 1))
        val thumbnail = thumbnailOf(bitmap)
        val result =
            try {
                OrtOcrEngine(pack).use { it.read(bitmap) }
            } finally {
                bitmap.recycle()
            }
        if (result.text.isBlank()) return ImportOutcome.Failed(ImportError.EMPTY)
        val page = DocPage(1, fixer().fix(result.text), ocr = true, confidence = result.confidence)
        return save(target, DocKind.IMAGE, listOf(page), 1, 0, thumbnail, onStep)
    }

    private suspend fun readText(
        file: File,
        target: Target,
        onStep: (ImportStep) -> Unit,
    ): ImportOutcome {
        onStep(ImportStep.Reading(1, 1))
        val parts = TextFiles.parts(TextFiles.decode(file.readBytes()))
        if (parts.isEmpty()) return ImportOutcome.Failed(ImportError.EMPTY)
        val pages = parts.mapIndexed { i, t -> DocPage(i + 1, t) }
        return save(target, DocKind.TEXT, pages, 0, 0, null, onStep)
    }

    @Suppress("LongParameterList")
    private suspend fun save(
        target: Target,
        kind: DocKind,
        pages: List<DocPage>,
        ocrPages: Int,
        missing: Int,
        thumbnail: ByteArray?,
        onStep: (ImportStep) -> Unit,
    ): ImportOutcome {
        onStep(ImportStep.Saving)
        val chars = pages.sumOf { it.text.length }
        val entity =
            DocumentEntity(
                name = target.name,
                kind = kind.name.lowercase(),
                mime = target.mime,
                pages = pages.size,
                chars = chars,
                addedAt = System.currentTimeMillis(),
                ocrPages = ocrPages,
                missingPages = missing,
                thumbnail = thumbnail,
            )
        if (target.ephemeral) return ImportOutcome.Done(privateDocs.add(entity.toInfo(), pages))
        val rows = Chunker.chunk(pages).map { DocChunkEntity(0, 0, it.page, it.index, it.text, it.ocr, it.confidence) }
        val id = db.add(entity, rows)
        return ImportOutcome.Done(entity.copy(id = id).toInfo())
    }

    /** Where a read file goes: its name and type, and whether it is only kept for a private chat. */
    private data class Target(
        val name: String,
        val mime: String?,
        val ephemeral: Boolean,
    )

    private fun fixer(): GermanOcrFixer = GermanOcrFixer(app.assets.open(WordList.ASSET).use { WordList.read(it) })

    private fun nameAndSize(uri: Uri): Pair<String, Long> {
        var name = uri.lastPathSegment?.substringAfterLast('/') ?: "Datei"
        var size = -1L
        runCatching {
            app.contentResolver.query(uri, null, null, null, null)?.use { c ->
                if (c.moveToFirst()) {
                    val n = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val s = c.getColumnIndex(OpenableColumns.SIZE)
                    if (n >= 0) c.getString(n)?.let { name = it }
                    if (s >= 0 && !c.isNull(s)) size = c.getLong(s)
                }
            }
        }
        return name to size
    }

    private fun kindOf(
        name: String,
        mime: String?,
        file: File,
    ): DocKind? {
        val ext = name.substringAfterLast('.', "").lowercase()
        val head = ByteArray(PDF_MAGIC.size).also { b -> file.inputStream().use { it.read(b) } }
        return when {
            head.contentEquals(PDF_MAGIC) || mime == "application/pdf" || ext == "pdf" -> DocKind.PDF
            mime?.startsWith("image/") == true || ext in IMAGE_EXT -> DocKind.IMAGE
            TextFiles.isText(name, mime) -> DocKind.TEXT
            else -> null
        }
    }

    /** The photo at most [PdfPages.MAX_SIDE] on the long side, turned as the camera saved it (EXIF). */
    @Suppress("ReturnCount")
    private fun decodeUpright(file: File): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.path, bounds)
        var sample = 1
        while (max(bounds.outWidth, bounds.outHeight) / (sample * 2) >= PdfPages.MAX_SIDE) sample *= 2
        val bitmap =
            BitmapFactory.decodeFile(
                file.path,
                BitmapFactory.Options().apply {
                    inSampleSize = sample
                    inPreferredConfig = Bitmap.Config.ARGB_8888
                },
            ) ?: return null
        val degrees = exifDegrees(file)
        val longest = max(bitmap.width, bitmap.height)
        val scale = if (longest > PdfPages.MAX_SIDE) PdfPages.MAX_SIDE.toFloat() / longest else 1f
        if (degrees == 0f && scale == 1f) return bitmap
        val m =
            Matrix().apply {
                postScale(scale, scale)
                postRotate(degrees)
            }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, m, true).also {
            if (it !== bitmap) bitmap.recycle()
        }
    }

    private fun exifDegrees(file: File): Float {
        val orientation =
            runCatching { ExifInterface(file.path).getAttributeInt(ExifInterface.TAG_ORIENTATION, 1) }.getOrDefault(1)
        return when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> QUARTER
            ExifInterface.ORIENTATION_ROTATE_180 -> HALF
            ExifInterface.ORIENTATION_ROTATE_270 -> THREE_QUARTERS
            else -> 0f
        }
    }

    private fun thumbnailOf(bitmap: Bitmap): ByteArray {
        val longest = max(bitmap.width, bitmap.height)
        val s = THUMB_SIDE.toFloat() / longest
        val small =
            Bitmap.createScaledBitmap(
                bitmap,
                (bitmap.width * s).toInt().coerceAtLeast(1),
                (bitmap.height * s).toInt().coerceAtLeast(1),
                true,
            )
        return PdfPages.png(small).also { if (small !== bitmap) small.recycle() }
    }

    private fun DocumentEntity.toInfo() =
        DocInfo(
            id = id,
            name = name,
            kind = DocKind.entries.firstOrNull { it.name.equals(kind, true) } ?: DocKind.PDF,
            pages = pages,
            chars = chars,
            addedAt = addedAt,
            ocrPages = ocrPages,
            missingPages = missingPages,
            thumbnail = thumbnail,
        )

    private companion object {
        const val MAX_BYTES = 50L * 1024 * 1024
        const val THUMB_SIDE = 160
        const val QUARTER = 90f
        const val HALF = 180f
        const val THREE_QUARTERS = 270f
        val IMAGE_EXT = setOf("jpg", "jpeg", "png", "webp", "heic", "heif", "bmp", "gif")
        val PDF_MAGIC = "%PDF".toByteArray()
    }
}
