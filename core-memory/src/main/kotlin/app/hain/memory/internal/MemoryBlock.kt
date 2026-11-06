package app.hain.memory.internal

import app.hain.memory.api.MemoryFact

/** The memory lines for the prompt (MEMORY.md §3.4), in the prompt's language. */
object MemoryBlock {
    /**
     * T-279 (host test, LFM3 m2 "Gib mir 3 Vokabeln" with "lernt Spanisch"): a head, one full sentence per
     * fact ("- Der Nutzer lernt Spanisch."), the topic notes, then one line to use it for language, place and
     * examples. [notes] are the user's own topic notes (label, note) for the topics of [facts] (7b, "Themen").
     */
    fun of(
        facts: List<MemoryFact>,
        lang: String,
        notes: List<Pair<String, String>> = emptyList(),
    ): String {
        val de = lang == "de"
        val lines = facts.joinToString("\n") { "- " + sentence(it.text, de) }
        val noteLines =
            notes.joinToString("") { (label, note) -> "\n$label: " + note.replace(SPACES, " ").take(NOTE_CHARS) }
        return "${if (de) HEAD_DE else HEAD_EN}\n$lines$noteLines\n${if (de) TAIL_DE else TAIL_EN}"
    }

    /**
     * A fact as a sentence: "lernt Spanisch" → "Der Nutzer lernt Spanisch.", while the user's own words
     * ("ich mag keine Pilze") and nouns ("Lieblingsfarbe: Blau") only get a capital and a full stop.
     */
    fun sentence(
        text: String,
        de: Boolean,
    ): String {
        val t = text.trim().trimEnd('.', ' ')
        if (t.isEmpty()) return t
        val first = t.substringBefore(' ').lowercase()
        val verb = t[0].isLowerCase() && first !in OWN_WORDS
        val body = if (verb) (if (de) "Der Nutzer " else "The user ") + t else t.replaceFirstChar { it.uppercase() }
        return if (body.last() in END) body else "$body."
    }

    /** Prompt name of a topic (its title is the category key, e.g. `person`). */
    fun topicLabel(
        title: String,
        lang: String,
    ): String {
        val de = lang == "de"
        val name = TOPIC_NAMES[title]?.let { if (de) it.first else it.second } ?: title
        return if (de) "Notiz zu $name" else "Note on $name"
    }

    /** Topic title (category key) → German and English name in the prompt. */
    private val TOPIC_NAMES =
        mapOf(
            "preference" to ("Vorlieben" to "Likes"),
            "person" to ("Menschen" to "People"),
            "place" to ("Orte" to "Places"),
            "work" to ("Schule & Arbeit" to "School & work"),
            "health" to ("Gesundheit" to "Health"),
            "other" to ("Sonstiges" to "Other"),
        )

    /** "Gemerkt" line right before the answer, so Pim confirms in its own words. */
    fun remembered(
        text: String,
        lang: String,
    ): String =
        if (lang == "de") {
            "Du hast dir gerade gemerkt: „$text“. Bestätige das kurz in einem Satz."
        } else {
            "You just saved to memory: \"$text\". Confirm it briefly in one sentence."
        }

    /** What "vergiss …" removed, or that there was nothing like it. */
    fun forgotten(
        texts: List<String>,
        lang: String,
    ): String =
        when {
            texts.isEmpty() && lang == "de" ->
                "Dazu war nichts gespeichert. Sag das kurz; alles Gespeicherte steht unter Einstellungen → Gedächtnis."
            texts.isEmpty() ->
                "Nothing like that was saved. Say so briefly; everything saved is under Settings → Memory."
            lang == "de" -> "Du hast vergessen: " + texts.joinToString("; ") { "„$it“" } + ". Bestätige das kurz."
            else -> "You forgot: " + texts.joinToString("; ") { "\"$it\"" } + ". Confirm it briefly."
        }

    /** One line so the brain knows when to call `remember`. */
    fun toolRule(lang: String): String =
        if (lang == "de") {
            "Bittet der Nutzer dich, dir etwas zu merken, rufe remember."
        } else {
            "If the user asks you to remember something, call remember."
        }

    /** First words that are the user's own ("ich", "mein") or no verb: these facts get no "Der Nutzer". */
    private val OWN_WORDS =
        setOf("ich", "mein", "meine", "meinen", "meiner", "meinem", "mir", "mich", "i", "i'm", "my", "me", "mine")

    private val END = setOf('.', '!', '?', '…')
    private val SPACES = Regex("\\s+")

    /** A topic note in the prompt is cut (the memory has ≤ 600 tokens in all, MEMORY.md §1). */
    private const val NOTE_CHARS = 300

    private const val HEAD_DE = "Das weißt du über den Nutzer:"
    private const val HEAD_EN = "What you know about the user:"
    private const val TAIL_DE =
        "Beachte das bei deiner Antwort, auch bei der Wahl der Sprache, des Orts und der Beispiele."
    private const val TAIL_EN =
        "Keep it in mind for your answer, also for the language, the place and the examples."
}
