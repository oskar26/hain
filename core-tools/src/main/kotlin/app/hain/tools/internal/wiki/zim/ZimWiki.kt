package app.hain.tools.internal.wiki.zim

import app.hain.tools.api.OfflineWiki
import app.hain.tools.internal.wiki.WikiSummary
import java.io.File
import java.net.URLEncoder
import java.util.Locale

/**
 * T-237: Wikipedia without internet from the installed knowledge packs (Kiwix "mini" ZIM files: the
 * intro of every article). [packs] gives the pack file per language ("de", "en"), bigger packs
 * first; archives stay open until the list changes.
 *
 * Finding an article: the title as typed, then with a capital first letter, then Title Case
 * ("kölner dom" → "Kölner Dom"); redirects are followed ("Koeln" → "Köln"). Disambiguation pages
 * are recognised by their text ("steht für:", "may refer to:").
 */
class ZimWiki(
    private val packs: () -> Map<String, List<File>>,
    private val codec: ClusterCodec,
) : OfflineWiki {
    private val open = mutableMapOf<File, ZimArchive>()

    override fun languages(): Set<String> = packs().filterValues { files -> files.any { it.isFile } }.keys

    override fun summary(
        topic: String,
        lang: String,
    ): WikiSummary? =
        packs()[lang]
            .orEmpty()
            .asSequence()
            .filter { it.isFile }
            .mapNotNull { file -> archive(file)?.let { summaryIn(it, topic, lang) } }
            .firstOrNull()

    private fun summaryIn(
        archive: ZimArchive,
        topic: String,
        lang: String,
    ): WikiSummary? {
        val hit = find(archive, topic) ?: return null
        val html = String(archive.content(hit), Charsets.UTF_8)
        val extract = ZimText.intro(html)
        return extract.takeIf { it.isNotBlank() }?.let {
            WikiSummary(
                title = hit.shownTitle,
                extract = it,
                url = articleUrl(lang, hit.shownTitle),
                disambiguation = ZimText.isDisambiguation(html, it),
            )
        }
    }

    @Synchronized
    private fun archive(file: File): ZimArchive? =
        open[file] ?: runCatching { ZimArchive(file, codec) }.getOrNull()?.also { open[file] = it }

    /** The article for [topic] in [archive], following redirects; `null` when there is none. */
    internal fun find(
        archive: ZimArchive,
        topic: String,
    ): ZimArchive.Entry? =
        candidates(topic)
            .asSequence()
            .flatMap { url -> NAMESPACES.asSequence().map { ns -> archive.search(ns, url) } }
            .filter { it >= 0 }
            .mapNotNull { archive.resolve(it) }
            .firstOrNull { it.mime?.startsWith("text/html") == true }

    companion object {
        /** Content namespace: 'C' since ZIM 6.1, 'A' in older files. */
        private val NAMESPACES = listOf('C', 'A')

        /** URL forms to try for a topic, most exact first. */
        fun candidates(topic: String): List<String> {
            val base =
                topic
                    .trim()
                    .trim('"', '\'', '„', '“', '?', '!', '.')
                    .replace(Regex("\\s+"), " ")
            if (base.isEmpty()) return emptyList()
            val first = base.replaceFirstChar { it.titlecase(Locale.ROOT) }
            val titleCase =
                base.split(' ').joinToString(" ") { w ->
                    if (w.length > 2 || w == base) w.replaceFirstChar { it.titlecase(Locale.ROOT) } else w
                }
            return listOf(base, first, titleCase, base.lowercase(Locale.ROOT).replaceFirstChar { it.titlecase() })
                .map { it.replace(' ', '_') }
                .distinct()
        }

        fun articleUrl(
            lang: String,
            title: String,
        ): String =
            "https://$lang.wikipedia.org/wiki/" +
                URLEncoder.encode(title.replace(' ', '_'), "UTF-8").replace("+", "%20").replace("%2F", "/")
    }
}
