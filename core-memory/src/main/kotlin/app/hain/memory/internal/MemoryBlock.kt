package app.hain.memory.internal

import app.hain.memory.api.MemoryFact
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** The memory lines for the prompt (MEMORY.md §3.4), in the prompt's language. */
object MemoryBlock {
    fun of(
        facts: List<MemoryFact>,
        lang: String,
        zone: ZoneId = ZoneId.systemDefault(),
    ): String {
        val head = if (lang == "de") HEAD_DE else HEAD_EN
        val lines =
            facts.joinToString("\n") { fact ->
                "- ${fact.text} (${DAY.format(Instant.ofEpochMilli(fact.createdAt).atZone(zone))})"
            }
        return "$head\n$lines"
    }

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

    // the facts are often the user's own words ("ich keine Pilze mag"), so "ich" means the user
    private const val HEAD_DE = "Das hat der Nutzer dir über sich gesagt (nur nutzen, wenn es zur Frage passt):"
    private const val HEAD_EN = "What the user told you about themselves (use it only when it fits the question):"
}
