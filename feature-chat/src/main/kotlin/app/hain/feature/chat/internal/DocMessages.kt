package app.hain.feature.chat.internal

/**
 * Häppchen 9: puts the attached documents into the user messages that brought them, before the
 * user's words, each document once (where it was first attached). The chat as stored keeps only
 * the chips; the text comes from "Meine Dateien" for every answer.
 */
internal object DocMessages {
    fun withDocuments(
        messages: List<UiMessage>,
        blocks: Map<Long, String>,
    ): List<UiMessage> {
        if (blocks.isEmpty()) return messages
        val seen = mutableSetOf<Long>()
        return messages.map { m ->
            if (!m.isUser || m.attachments.isEmpty()) return@map m
            val mine = m.attachments.filter { seen.add(it.id) }.mapNotNull { blocks[it.id] }
            if (mine.isEmpty()) m else m.copy(text = mine.joinToString("\n\n") + "\n\n" + m.text)
        }
    }
}
