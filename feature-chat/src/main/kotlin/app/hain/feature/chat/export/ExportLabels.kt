package app.hain.feature.chat.export

/**
 * The words an export needs, already in the app language (T-054). Filled from string resources on
 * the phone and written out by hand in tests.
 */
data class ExportLabels(
    /** Name of your own messages, "Du" / "You". */
    val you: String = "You",
    /** Name of the answers, "Hain". */
    val hain: String = "Hain",
    /** Line under the title, e.g. "Exportiert aus Hain · 4. November 2026". */
    val exportedLine: String = "",
    /** Heading over the pages an answer is based on. */
    val sources: String = "Sources",
    /** Footer of a PDF page with the page number and the page count, e.g. "Seite %1$d von %2$d". */
    val pageOf: String = "%1\$d / %2\$d",
    /** Shown instead of an empty title. */
    val untitled: String = "Chat",
    /** Mark of an answer that was stopped. */
    val stopped: String = "",
)
