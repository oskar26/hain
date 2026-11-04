package app.hain.feature.chat.export

import app.hain.feature.chat.internal.Chat
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * All chats as one ZIP of Markdown files (T-054, Settings → Privacy). One file per chat, named
 * "<date>-<title>.md", the newest first; private and empty chats are left out.
 */
object ChatZip {
    /** Writes the ZIP to [out] (left open for the caller) and returns how many chats went in. */
    fun write(
        chats: List<Chat>,
        out: OutputStream,
        labelsOf: (Chat) -> ExportLabels,
        dateOf: (Chat) -> String,
    ): Int {
        val wanted = chats.filter { !it.isPrivate && ChatMarkdown.exportable(it.messages).isNotEmpty() }
        val zip = ZipOutputStream(out)
        val used = mutableSetOf<String>()
        wanted.sortedByDescending { it.updatedAt }.forEach { chat ->
            val name = unique("${dateOf(chat)}-${ExportFileName.slug(chat.title)}", used) + ".md"
            zip.putNextEntry(ZipEntry(name).apply { time = chat.updatedAt })
            zip.write(ChatMarkdown.export(chat, labelsOf(chat)).toByteArray(Charsets.UTF_8))
            zip.closeEntry()
        }
        zip.finish()
        return wanted.size
    }

    private fun unique(
        base: String,
        used: MutableSet<String>,
    ): String {
        var name = base
        var n = 2
        while (!used.add(name)) name = "$base-${n++}"
        return name
    }
}
