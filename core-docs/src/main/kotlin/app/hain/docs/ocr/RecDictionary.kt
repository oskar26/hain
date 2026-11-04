package app.hain.docs.ocr

/**
 * The characters of the PP-OCRv5 Latin recognizer (T-121). They come with the model in
 * `inference.yml` (`PostProcess.character_dict`), so the app never ships its own copy that could
 * drift from the model. Class 0 is the CTC blank, then the characters, then a space at the end
 * (`use_space_char`), exactly as PaddleOCR's CTCLabelDecode does.
 */
object RecDictionary {
    private const val KEY = "character_dict:"

    /** The `character_dict` list of a PaddleOCR `inference.yml` (only the small YAML subset it uses). */
    fun parseYaml(yaml: String): List<String> {
        val lines = yaml.replace("\r\n", "\n").lines()
        val start = lines.indexOfFirst { it.trim() == KEY }
        require(start >= 0) { "no character_dict in model file" }
        val out = mutableListOf<String>()
        for (line in lines.drop(start + 1)) {
            val item = line.trimStart()
            if (!item.startsWith("- ") && item != "-") break
            out += value(item.removePrefix("-").removePrefix(" "))
        }
        require(out.isNotEmpty()) { "empty character_dict" }
        return out
    }

    /** CTC classes: blank, the characters, space. */
    fun classes(dict: List<String>): List<String> = listOf("") + dict + listOf(" ")

    /** One YAML scalar: 'single' (with '' for '), "double" (with \ escapes) or plain. */
    internal fun value(raw: String): String {
        val s = raw.trimEnd()
        return when {
            s.length >= 2 && s.startsWith('\'') && s.endsWith('\'') -> s.substring(1, s.length - 1).replace("''", "'")
            s.length >= 2 && s.startsWith('"') && s.endsWith('"') -> unescape(s.substring(1, s.length - 1))
            else -> s
        }
    }

    private fun unescape(s: String): String {
        val out = StringBuilder()
        var i = 0
        while (i < s.length) {
            val c = s[i]
            if (c == '\\' && i + 1 < s.length) {
                val n = s[i + 1]
                when (n) {
                    'n' -> out.append('\n')
                    't' -> out.append('\t')
                    'u' -> {
                        out.append(s.substring(i + 2, (i + HEX_END).coerceAtMost(s.length)).toInt(HEX).toChar())
                        i += HEX_END - 2
                    }
                    else -> out.append(n)
                }
                i += 2
            } else {
                out.append(c)
                i++
            }
        }
        return out.toString()
    }

    private const val HEX = 16
    private const val HEX_END = 6
}
