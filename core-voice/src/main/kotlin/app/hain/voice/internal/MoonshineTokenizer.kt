package app.hain.voice.internal

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.ByteArrayOutputStream

/**
 * Turns Moonshine token ids back into text (T-140). The model ships a SentencePiece-style BPE
 * `tokenizer.json`: `▁` stands for a space, `<0xNN>` pieces are raw UTF-8 bytes (byte fallback),
 * the first space is dropped. Only decoding is needed, so this is a plain id → piece table.
 */
class MoonshineTokenizer(
    private val pieces: Array<String?>,
    private val special: Set<Int> = DEFAULT_SPECIAL,
) {
    val size: Int get() = pieces.size

    @Suppress("LoopWithTooManyJumpStatements") // skip special and unknown ids
    fun decode(ids: List<Int>): String {
        val bytes = ByteArrayOutputStream()
        for (id in ids) {
            if (id in special) continue
            val piece = pieces.getOrNull(id) ?: continue
            val byte = byteOf(piece)
            if (byte != null) {
                bytes.write(byte)
            } else {
                bytes.write(piece.replace(SPACE_MARK, ' ').toByteArray(Charsets.UTF_8))
            }
        }
        return bytes.toString(Charsets.UTF_8.name()).removePrefix(" ")
    }

    companion object {
        private const val SPACE_MARK = '▁'
        private const val HEX = 16
        private val DEFAULT_SPECIAL = setOf(0, 1, 2)
        private val BYTE_PIECE = Regex("^<0x([0-9A-Fa-f]{2})>$")

        private fun byteOf(piece: String): Int? =
            if (piece.length == BYTE_LEN && piece.startsWith("<0x")) {
                BYTE_PIECE
                    .find(piece)
                    ?.groupValues
                    ?.get(1)
                    ?.toInt(HEX)
            } else {
                null
            }

        private const val BYTE_LEN = 6

        /** Reads the `model.vocab` map and the special `added_tokens` of a Hugging Face tokenizer.json. */
        fun fromJson(json: String): MoonshineTokenizer {
            val root = Json.parseToJsonElement(json).jsonObject
            val vocab =
                root
                    .getValue("model")
                    .jsonObject
                    .getValue("vocab")
                    .jsonObject
            var max = -1
            val entries = vocab.entries.map { (piece, id) -> id.jsonPrimitive.int to piece }
            entries.forEach { max = maxOf(max, it.first) }
            val special = mutableSetOf<Int>()
            root["added_tokens"]?.jsonArray?.forEach { token ->
                val o = token.jsonObject
                val id = o.getValue("id").jsonPrimitive.int
                if (o["special"]?.jsonPrimitive?.content == "true") special += id
                max = maxOf(max, id)
            }
            val pieces = arrayOfNulls<String>(max + 1)
            entries.forEach { (id, piece) -> pieces[id] = piece }
            return MoonshineTokenizer(pieces, special.ifEmpty { DEFAULT_SPECIAL })
        }
    }
}
