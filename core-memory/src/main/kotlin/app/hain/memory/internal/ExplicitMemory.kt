package app.hain.memory.internal

import app.hain.memory.api.MemoryFact

/**
 * "Merk dir, dass …" / "Vergiss, dass …" (MEMORY.md §2.1): found by plain rules in German and
 * English, without the brain, only at the start of a message. "Merk dir das", questions ("kannst
 * du dir merken, was …") and "vergiss alles" are left to the brain and the memory page.
 */
object ExplicitMemory {
    sealed interface Command {
        val text: String

        data class Remember(
            override val text: String,
        ) : Command

        data class Forget(
            override val text: String,
        ) : Command
    }

    private const val OPTIONS = "(?iu)"
    private const val LEAD = "^\\s*(?:(?:hey|hi|hallo)\\s+)?(?:pim\\s*[,:]?\\s*)?(?:bitte\\s+|please\\s+)?"
    private const val SEP = "\\s*[,:]?\\s*"

    /** "Vergiss nicht, dass …" / "Don't forget …" means keep it: checked before [forget]. */
    private val dontForget =
        listOf(
            "vergiss(?:\\s+es)?(?:\\s+bitte)?\\s+nicht$SEP(?:dass\\s+)?(.+)",
            "(?:don['’]t|do\\s+not|dont)\\s+forget\\b(?:\\s+that)?$SEP(.+)",
        ).map { Regex(OPTIONS + LEAD + it) }

    private val remember =
        listOf(
            "merk(?:e|')?\\s+dir(?:\\s+bitte)?$SEP(?:dass\\s+)?(.+)",
            "(?:kannst|könntest|kann)\\s+du\\s+dir\\s+(?:bitte\\s+)?merken$SEP(?:dass\\s+)?(.+)",
            "notier(?:e)?\\s+dir$SEP(?:dass\\s+)?(.+)",
            "(?:can|could)\\s+you\\s+(?:please\\s+)?remember\\b(?:\\s+that)?$SEP(.+)",
            "remember\\b(?:\\s+that)?$SEP(.+)",
            "(?:keep|bear)\\s+in\\s+mind(?:\\s+that)?$SEP(.+)",
        ).map { Regex(OPTIONS + LEAD + it) }

    private val forget =
        listOf(
            "vergiss\\b(?:\\s+bitte)?$SEP(?:dass\\s+)?(.+)",
            "(?:can|could)\\s+you\\s+(?:please\\s+)?forget\\b(?:\\s+that)?$SEP(.+)",
            "forget\\b(?:\\s+that)?$SEP(.+)",
        ).map { Regex(OPTIONS + LEAD + it) }

    /** Words that point back ("das", "it"): no fact of their own. */
    private val notAFact =
        setOf(
            "das",
            "dies",
            "es",
            "alles",
            "das alles",
            "all das",
            "this",
            "that",
            "it",
            "all",
            "everything",
            "all that",
            "about it",
            "about that",
            "me",
            "mich",
            "mir",
        )
    private val questionStart =
        Regex(
            "(?iu)^(?:was|wer|wie|wann|wo|warum|welche[rsnm]?|ob|what|who|how|when|where|why|which|if|whether)\\b",
        )
    private val tail = Regex("(?iu)[\\s,]*(?:bitte|please|danke|thanks)?[\\s.!?…\"'“”„»«]*$")
    private val quotes = Regex("^[\\s\"'“”„»«]+|[\\s\"'“”„»«]+$")
    private val spaces = Regex("\\s+")

    /** The command in [message], or `null` when it is a normal message. */
    @Suppress("ReturnCount")
    fun parse(message: String): Command? {
        val line = message.trim()
        if (line.isEmpty()) return null
        (dontForget + remember).firstNotNullOfOrNull { it.find(line) }?.let { match ->
            return clean(match.groupValues[1])?.let { Command.Remember(it) }
        }
        forget.firstNotNullOfOrNull { it.find(line) }?.let { match ->
            return clean(match.groupValues[1])?.let { Command.Forget(it) }
        }
        return null
    }

    /** The fact text: without "bitte", end marks and quotes; `null` when nothing is left to keep. */
    internal fun clean(raw: String): String? {
        val text =
            raw
                .replace(tail, "")
                .replace(quotes, "")
                .replace(spaces, " ")
                .trim()
        if (text.length < MIN_CHARS || text.lowercase() in notAFact || questionStart.containsMatchIn(text)) return null
        return cut(text)
    }

    /** At most [MemoryFact.MAX_CHARS], cut at a word. */
    fun cut(text: String): String {
        val trimmed = text.replace(spaces, " ").trim()
        if (trimmed.length <= MemoryFact.MAX_CHARS) return trimmed
        val head = trimmed.take(MemoryFact.MAX_CHARS - 1)
        val space = head.lastIndexOf(' ')
        return (if (space > MemoryFact.MAX_CHARS / 2) head.take(space) else head).trimEnd() + "…"
    }

    private const val MIN_CHARS = 3
}
