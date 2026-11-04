package app.hain.memory.internal

import app.hain.memory.api.MemoryFact
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** The memory lines for the prompt (MEMORY.md §3.4), in the prompt's language. */
object MemoryBlock {
    /** [notes] are the user's own topic notes (label, note) for the topics of [facts] (7b, "Themen"). */
    fun of(
        facts: List<MemoryFact>,
        lang: String,
        zone: ZoneId = ZoneId.systemDefault(),
        notes: List<Pair<String, String>> = emptyList(),
    ): String {
        val head = if (lang == "de") HEAD_DE else HEAD_EN
        val lines =
            facts.joinToString("\n") { fact ->
                "- ${fact.text} (${DAY.format(Instant.ofEpochMilli(fact.createdAt).atZone(zone))})"
            }
        val noteLines =
            notes.joinToString("") { (label, note) -> "\n$label: " + note.replace(SPACES, " ").take(NOTE_CHARS) }
        return "$head\n$lines$noteLines"
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

    private val DAY = DateTimeFormatter.ISO_LOCAL_DATE
    private val SPACES = Regex("\\s+")

    /** A topic note in the prompt is cut (the memory has ≤ 600 tokens in all, MEMORY.md §1). */
    private const val NOTE_CHARS = 300

    // the facts are often the user's own words ("ich keine Pilze mag"), so "ich" means the user
    private const val HEAD_DE = "Das hat der Nutzer dir über sich gesagt (nur nutzen, wenn es zur Frage passt):"
    private const val HEAD_EN = "What the user told you about themselves (use it only when it fits the question):"
}
