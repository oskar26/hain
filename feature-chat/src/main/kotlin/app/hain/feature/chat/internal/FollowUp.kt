package app.hain.feature.chat.internal

import app.hain.llm.api.ChatMessage
import app.hain.llm.api.Role

/**
 * Short follow-ups ("und in München?", "und wann ist sie gestorben?", "mach lieber 15 draus") after
 * an answer that used a tool (4d, T-184). Host eval: the small brain answered them from the last
 * answer or said "I would have to look it up" instead of calling the tool again. A one-line hint
 * names the tool; thanks and longer messages get none.
 */
internal object FollowUp {
    private const val MAX_WORDS = 7
    private val STARTS =
        listOf(
            "und ",
            "and ",
            "what about",
            "how about",
            "mach ",
            "make it",
            "lieber",
            "instead",
            "auch ",
            "also ",
            "dann ",
        )
    private val THANKS = Regex("""\b(danke|dank|thanks|thank you|thx|super|cool|ok|okay|perfekt|great)\b""")
    private val SPACES = Regex("\\s+")
    private val LINK = Regex("""https?://[^\s<>"]+""")

    /**
     * A link in the newest user message: "lies ihn mit read_page" (host eval: "lies mal
     * https://… und fass zusammen" was answered with "I cannot find that link").
     */
    fun link(
        history: List<ChatMessage>,
        lang: String,
    ): String? {
        val text = history.lastOrNull { it.role == Role.USER }?.content
        val url = text?.let { LINK.find(it)?.value?.trimEnd('.', ',', ')', '!', '?') }
        return when {
            url == null -> null
            lang == "de" -> "Die Nachricht enthält einen Link: lies $url mit read_page."
            else -> "The message has a link: read $url with read_page."
        }
    }

    /** The hint for the newest user message in [history], or `null`. */
    fun hint(
        history: List<ChatMessage>,
        lang: String,
    ): String? {
        val userIndex = history.indexOfLast { it.role == Role.USER }
        val tool = if (userIndex > 0 && isFollowUp(history[userIndex].content)) toolBefore(history, userIndex) else null
        return when {
            tool == null -> null
            lang == "de" -> "Das ist eine Folgefrage: rufe $tool neu auf, mit den Werten aus dieser Nachricht."
            else -> "This is a follow-up: call $tool again with the values from this message."
        }
    }

    private fun isFollowUp(content: String): Boolean {
        val text = content.trim().lowercase()
        val short = text.split(SPACES).size <= MAX_WORDS
        val asks = text.endsWith("?") || STARTS.any { text.startsWith(it) }
        return short && asks && !THANKS.containsMatchIn(text)
    }

    /** The first tool of the assistant's last answer before [userIndex] (same exchange only). */
    private fun toolBefore(
        history: List<ChatMessage>,
        userIndex: Int,
    ): String? {
        val before = history.subList(0, userIndex)
        val previousUser = before.indexOfLast { it.role == Role.USER }
        return before
            .drop(previousUser + 1)
            .lastOrNull { it.role == Role.ASSISTANT && it.toolCalls.isNotEmpty() }
            ?.toolCalls
            ?.first()
            ?.name
    }
}
