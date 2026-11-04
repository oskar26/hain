package app.hain.docs.api

/**
 * How attached documents reach the brain (DOCS_OCR.md "Using documents in chat", T-123):
 * small documents (together at most [INLINE_TOKENS]) go into the user message whole, page by page
 * with `[S. n]`; larger ones are only named, and the tool `search_document` finds their parts.
 */
object DocPrompt {
    const val INLINE_TOKENS = 3000

    /** What a chat's documents give the prompt: a block per document id, and the ids to search. */
    data class ForChat(
        val blocks: Map<Long, String> = emptyMap(),
        val search: List<Long> = emptyList(),
    )

    /** Which documents go in whole ([inline]) and which are searched ([search]). */
    data class Plan(
        val inline: List<DocAttachment>,
        val search: List<DocAttachment>,
    )

    /** Oldest first: each document goes in whole while all whole ones together stay small. */
    fun plan(docs: List<DocAttachment>): Plan {
        val inline = mutableListOf<DocAttachment>()
        val search = mutableListOf<DocAttachment>()
        var used = 0
        docs.distinctBy { it.id }.forEach { d ->
            if (used + d.tokens <= INLINE_TOKENS) {
                inline += d
                used += d.tokens
            } else {
                search += d
            }
        }
        return Plan(inline, search)
    }

    /** A whole document: `<document name="…">[S. 1] …</document>`; one-page files without page marks. */
    fun inline(
        name: String,
        pages: List<DocPage>,
    ): String {
        val filled = pages.filter { it.text.isNotBlank() }
        val body =
            if (pages.size <= 1) {
                filled.joinToString("\n\n") { it.text.trim() }
            } else {
                filled.joinToString("\n\n") { "[S. ${it.number}]\n${it.text.trim()}" }
            }
        return "<document name=\"${attr(name)}\">\n$body\n</document>"
    }

    /** A long document: only its name and how to read it. */
    fun searchNote(doc: DocAttachment): String =
        "<document name=\"${attr(doc.name)}\" pages=\"${doc.pages}\">This document is long. " +
            "Use the tool search_document to find the parts you need and cite pages like [S. 2].</document>"

    private fun attr(s: String): String = s.replace("\"", "'").replace("<", "‹").replace(">", "›")
}
