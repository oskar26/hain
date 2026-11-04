package app.hain.feature.chat.internal

import app.hain.llm.api.ChatMessage
import app.hain.llm.api.Role
import app.hain.tools.api.ToolRouter

/**
 * Setting "Websuche: fragen" (3b-2): Pim asked "Darf ich im Web suchen?" in his last answer (its web
 * search came back with `ask_first`) and the user's new message says yes. Only then may the web search
 * run in this turn. A new question without a yes asks again.
 */
object SearchConsent {
    private val YES =
        Regex(
            """(?iu)^\W*(ja|jo|jep|klar|gerne?|ok(ay)?|okey|passt|mach( das| ruhig)?|bitte|such|yes|yeah|yep|sure|""" +
                """go( ahead)?|please|do it)(?!\p{L})""",
        )

    fun approved(history: List<ChatMessage>): Boolean {
        val last = history.indexOfLast { it.role == Role.USER }
        if (last < 0 || !YES.containsMatchIn(history[last].content.trim())) return false
        val before = history.subList(0, last)
        val previousUser = before.indexOfLast { it.role == Role.USER }
        return before
            .drop(previousUser + 1)
            .any { it.role == Role.TOOL && it.content.contains(ToolRouter.ASK_FIRST) }
    }
}
