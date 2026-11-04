package app.hain.docs.internal.text

import java.nio.ByteBuffer
import java.nio.charset.CharacterCodingException
import java.nio.charset.Charset
import java.nio.charset.CodingErrorAction

/**
 * Plain text and Markdown files (T-120). The bytes are read as UTF-8; when that fails (old Windows
 * files) as Windows-1252. Long files are split into parts of about [PART_CHARS] characters at empty
 * lines, so answers can still point to a place ("Teil 3").
 */
object TextFiles {
    const val PART_CHARS = 3000

    /** File endings Hain reads as text. */
    val EXTENSIONS = setOf("txt", "md", "markdown", "text", "csv")

    @Suppress("SwallowedException") // not UTF-8: read it as Windows-1252 instead
    fun decode(bytes: ByteArray): String {
        val body = if (bytes.startsWith(BOM)) bytes.copyOfRange(BOM.size, bytes.size) else bytes
        val text =
            try {
                Charsets.UTF_8
                    .newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(body))
                    .toString()
            } catch (e: CharacterCodingException) {
                String(body, Charset.forName("windows-1252"))
            }
        return text.replace("\r\n", "\n").replace('\r', '\n').trim()
    }

    /** [text] in parts of at most about [max] characters, cut at empty lines, then at line ends. */
    fun parts(
        text: String,
        max: Int = PART_CHARS,
    ): List<String> {
        if (text.isBlank()) return emptyList()
        val out = mutableListOf<String>()
        val current = StringBuilder()

        fun add(
            piece: String,
            gap: String,
        ) {
            if (current.isNotEmpty() && current.length + piece.length + gap.length > max) {
                out += current.toString().trim()
                current.clear()
            }
            current.append(piece).append(gap)
        }
        text.split(PARAGRAPH).forEach { paragraph ->
            if (paragraph.length > max) paragraph.lines().forEach { add(it, "\n") } else add(paragraph, "\n\n")
        }
        if (current.isNotBlank()) out += current.toString().trim()
        return out.filter { it.isNotBlank() }
    }

    fun isText(
        name: String,
        mime: String?,
    ): Boolean {
        val ext = name.substringAfterLast('.', "").lowercase()
        return ext in EXTENSIONS || mime?.startsWith("text/") == true
    }

    private fun ByteArray.startsWith(prefix: ByteArray): Boolean {
        if (size < prefix.size) return false
        return prefix.indices.all { this[it] == prefix[it] }
    }

    private val BOM = byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte())
    private val PARAGRAPH = Regex("\n\\s*\n")
}
