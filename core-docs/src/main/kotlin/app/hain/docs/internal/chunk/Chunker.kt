package app.hain.docs.internal.chunk

import app.hain.docs.api.DocPage

/** One part of a document as it is stored and searched (`doc_chunk`). */
data class Chunk(
    val page: Int,
    val index: Int,
    val text: String,
    val ocr: Boolean = false,
    val confidence: Float? = null,
)

/**
 * Cuts pages into parts of about 400 tokens (DOCS_OCR.md: ≈ [MAX_CHARS] characters), never across
 * a page, so every part knows its page for "S. 3". It cuts at empty lines first, then at sentence
 * ends, then at spaces. Parts do not overlap: the parts of a page joined by an empty line give the
 * page again, so a small document can go into the prompt whole without keeping its text twice.
 */
object Chunker {
    const val MAX_CHARS = 1600
    private const val MIN_TAIL = 200

    fun chunk(
        pages: List<DocPage>,
        maxChars: Int = MAX_CHARS,
    ): List<Chunk> {
        var index = 0
        return pages.flatMap { page ->
            pieces(page.text, maxChars).map { text ->
                Chunk(page.number, index++, text, page.ocr, page.confidence)
            }
        }
    }

    /** [text] in pieces of at most [maxChars]; empty text gives no piece. */
    @Suppress("ReturnCount")
    fun pieces(
        text: String,
        maxChars: Int = MAX_CHARS,
    ): List<String> {
        val clean = text.trim()
        if (clean.isEmpty()) return emptyList()
        if (clean.length <= maxChars) return listOf(clean)
        val units = clean.split(PARAGRAPH).flatMap { splitLong(it.trim(), maxChars) }.filter { it.isNotEmpty() }
        val out = mutableListOf<String>()
        val current = StringBuilder()
        units.forEach { unit ->
            if (current.isNotEmpty() && current.length + unit.length + 2 > maxChars) {
                out += current.toString().trim()
                current.clear()
            }
            if (current.isNotEmpty()) current.append("\n\n")
            current.append(unit)
        }
        val last = current.toString().trim()
        if (last.isNotEmpty()) {
            // a tiny rest joins the part before it when there is room
            if (out.isNotEmpty() && last.length < MIN_TAIL && out.last().length + last.length + 2 <= maxChars) {
                out[out.lastIndex] = out.last() + "\n\n" + last
            } else {
                out += last
            }
        }
        return out
    }

    /** A paragraph longer than [max] in sentence groups; a very long sentence at spaces. */
    private fun splitLong(
        paragraph: String,
        max: Int,
    ): List<String> {
        if (paragraph.length <= max) return listOf(paragraph)
        val out = mutableListOf<String>()
        val current = StringBuilder()
        paragraph.split(SENTENCE).forEach { sentence ->
            hardCut(sentence, max).forEach { s ->
                if (current.isNotEmpty() && current.length + s.length + 1 > max) {
                    out += current.toString().trim()
                    current.clear()
                }
                if (current.isNotEmpty()) current.append(' ')
                current.append(s)
            }
        }
        if (current.isNotBlank()) out += current.toString().trim()
        return out
    }

    private fun hardCut(
        sentence: String,
        max: Int,
    ): List<String> {
        if (sentence.length <= max) return listOf(sentence)
        val out = mutableListOf<String>()
        var rest = sentence
        while (rest.length > max) {
            val cut = rest.lastIndexOf(' ', max).takeIf { it > max / 2 } ?: max
            out += rest.substring(0, cut).trim()
            rest = rest.substring(cut).trim()
        }
        if (rest.isNotEmpty()) out += rest
        return out
    }

    private val PARAGRAPH = Regex("\n\\s*\n")
    private val SENTENCE = Regex("(?<=[.!?…])\\s+")
}
