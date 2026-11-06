package app.hain.tools.api

import java.util.Locale

/**
 * T-293 (Oskar 06.11. 17:04, screenshot "Bundespräsidentenwahl"): asked "Was hat es mit der Neuwahl
 * des Bundespräsidenten auf sich? Wer kann eigentlich wählen?", LFM3 wrote only a ```card checklist of
 * its own questions ("Wer kann wählen?", "Wie läuft die Wahl ab?") and no answer at all. A card is meant
 * to come after a sentence and only for lists, plans and comparisons. This decides when an answer is
 * "only a card nobody asked for", so the engine asks once more for a real answer. Pure, tested.
 */
object CardGuard {
    /** Shortest text outside the card that still counts as an answer. */
    private const val MIN_PROSE = 40

    /** Whether [message] asks for something a card is made for (list, to-do, plan, table, chart …). */
    fun wanted(message: String): Boolean = WANTED.containsMatchIn(message.lowercase(Locale.GERMAN))

    /**
     * Whether [answer] is a card in place of an answer: (almost) no text outside the ```card block, or a
     * checklist whose points are questions. `false` when there is no card.
     */
    fun replacesAnswer(answer: String): Boolean {
        val blocks = CARD.findAll(answer).map { it.value }.toList()
        if (blocks.isEmpty()) return false
        val prose = CARD.replace(answer, " ").trim()
        val points = blocks.flatMap { b -> b.lines().map { it.trim() }.filter { it.startsWith("- ") } }
        val asking = points.count { it.removeSuffix("[x]").trim().endsWith("?") }
        val questions = points.isNotEmpty() && asking * 2 >= points.size
        return prose.length < MIN_PROSE || questions
    }

    /** One line so the next try answers in sentences (and may look it up). */
    fun hint(lang: String): String =
        if (lang == "de") {
            "Keine Karte und keine Liste mit Fragen. Beantworte die Frage des Nutzers direkt in 2 bis 5 ganzen " +
                "Sätzen. Geht es um Aktuelles wie Politik oder Wahlen, rufe zuerst web_search auf."
        } else {
            "No card and no list of questions. Answer the user's question directly in 2 to 5 full sentences. " +
                "If it is about current events like politics or elections, call web_search first."
        }

    private val CARD = Regex("```card[\\s\\S]*?(?:```|$)")

    private val WANTED =
        Regex(
            "liste|checkliste|to-?do|einkauf|packliste|abhak|plan\\b|planen|wochenplan|stundenplan|lernplan|" +
                "tabelle|vergleich|vergleiche|gegenüberstell|übersicht|diagramm|grafik|fortschritt|schritte|" +
                "\\blist\\b|checklist|shopping|table|compare|comparison|chart|graph|schedule|progress|steps",
        )
}
