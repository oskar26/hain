package app.hain.feature.chat.export

import app.hain.feature.chat.internal.Chat
import app.hain.feature.chat.internal.UiMessage

/**
 * One chat as a Markdown file (T-054): title, a line with the date, then every message under its
 * role. Answers keep their Markdown, their sources follow as links. The thinking of an answer and
 * empty or failed answers are left out; private chats are never exported.
 */
object ChatMarkdown {
    fun export(
        chat: Chat,
        labels: ExportLabels,
    ): String =
        buildString {
            append("# ").append(chat.title.ifBlank { labels.untitled }.oneLine()).append("\n\n")
            if (labels.exportedLine.isNotBlank()) append('*').append(labels.exportedLine).append("*\n\n")
            append("---\n")
            exportable(chat.messages).forEach { message ->
                append('\n')
                append("**").append(if (message.isUser) labels.you else labels.hain).append("**\n\n")
                append(message.text.trim().replace("\r\n", "\n")).append('\n')
                if (message.stopped && labels.stopped.isNotBlank()) append("\n*").append(labels.stopped).append("*\n")
                if (message.sources.isNotEmpty()) {
                    append('\n').append(labels.sources).append(":\n\n")
                    message.sources.forEach { source ->
                        append("- [").append(source.title.oneLine().escapeBrackets()).append("](")
                        append(source.url.replace(" ", "%20").replace(")", "%29")).append(")\n")
                    }
                }
            }
        }

    /** The messages worth exporting: finished and not empty. */
    internal fun exportable(messages: List<UiMessage>): List<UiMessage> =
        messages.filter { !it.streaming && !it.failed && it.text.isNotBlank() }

    private fun String.oneLine(): String = trim().replace(Regex("\\s+"), " ")

    private fun String.escapeBrackets(): String = replace("[", "\\[").replace("]", "\\]")
}

/** File names for exports: "hain-<title>-<date>", only safe letters, at most 60 characters of title. */
object ExportFileName {
    fun of(
        chat: Chat,
        date: String,
        extension: String,
    ): String = "hain-${slug(chat.title)}-$date.$extension"

    fun slug(title: String): String {
        val umlauts =
            title
                .lowercase()
                .replace("ä", "ae")
                .replace("ö", "oe")
                .replace("ü", "ue")
                .replace("ß", "ss")
        val clean =
            umlauts
                .replace(Regex("[^a-z0-9]+"), "-")
                .trim('-')
                .take(MAX_SLUG)
                .trimEnd('-')
        return clean.ifEmpty { "chat" }
    }

    private const val MAX_SLUG = 60
}
