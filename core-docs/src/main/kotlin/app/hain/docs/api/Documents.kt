package app.hain.docs.api

/** What kind of file a document came from (DOCS_OCR.md). */
enum class DocKind { PDF, TEXT, IMAGE }

/**
 * One document in "Meine Dateien" (T-123). Only the text is kept (encrypted), never the file.
 *
 * @property pages pages of a PDF; 1 for a photo; for a text file the number of its parts.
 * @property ocrPages pages whose text came from the text recognition (T-121).
 * @property missingPages pages that are pictures and could not be read (no text recognition).
 * @property thumbnail small PNG of the first page, or `null`.
 */
data class DocInfo(
    val id: Long,
    val name: String,
    val kind: DocKind,
    val pages: Int,
    val chars: Int,
    val addedAt: Long,
    val ocrPages: Int = 0,
    val missingPages: Int = 0,
    val thumbnail: ByteArray? = null,
) {
    /** Rough token count (≈ 4 characters per token in German and English). */
    val tokens: Int get() = (chars + CHARS_PER_TOKEN - 1) / CHARS_PER_TOKEN

    override fun equals(other: Any?): Boolean =
        other is DocInfo &&
            id == other.id &&
            name == other.name &&
            kind == other.kind &&
            pages == other.pages &&
            chars == other.chars &&
            addedAt == other.addedAt &&
            ocrPages == other.ocrPages &&
            missingPages == other.missingPages &&
            thumbnail.contentEqualsNullable(other.thumbnail)

    override fun hashCode(): Int = id.hashCode() * HASH + name.hashCode()

    companion object {
        const val CHARS_PER_TOKEN = 4
        private const val HASH = 31
    }
}

private fun ByteArray?.contentEqualsNullable(other: ByteArray?): Boolean =
    if (this == null || other == null) this === other else contentEquals(other)

/** The text of one page; [ocr] = read by the text recognition, [confidence] its mean certainty. */
data class DocPage(
    val number: Int,
    val text: String,
    val ocr: Boolean = false,
    val confidence: Float? = null,
)

/** Where an import stands; shown as a small progress line above the message box. */
sealed interface ImportStep {
    /** Reading the text layer of page [page] of [total]. */
    data class Reading(
        val page: Int,
        val total: Int,
    ) : ImportStep

    /** Recognizing the text of picture page [page] of [total]. */
    data class Recognizing(
        val page: Int,
        val total: Int,
    ) : ImportStep

    /** Saving the parts of the document. */
    data object Saving : ImportStep
}

/** Why a file could not be read. */
enum class ImportError { UNREADABLE, LOCKED, EMPTY, TOO_BIG, UNSUPPORTED, NO_OCR }
