package app.hain.tools.internal.search

import app.hain.common.api.net.GateRequest
import app.hain.common.api.net.NetworkGate
import app.hain.common.api.net.OfflineException
import app.hain.common.api.net.Purpose
import app.hain.llm.api.ToolSchema
import app.hain.tools.api.Args
import app.hain.tools.api.Tool
import app.hain.tools.api.ToolContext
import app.hain.tools.api.ToolResult
import app.hain.tools.api.ToolRouter
import app.hain.tools.api.ToolSource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import java.io.IOException

/** One page that was read: where, its title and its text, already cut. */
data class ReadPage(
    val url: String,
    val title: String,
    val text: String,
)

/** Loads web pages through the [NetworkGate] and turns them into short text (T-067). */
class PageReader(
    private val gate: NetworkGate,
    private val timeoutMs: Long = PAGE_TIMEOUT_MS,
) {
    /**
     * One page; throws on network errors, `null` when it holds no readable text. With a [focus]
     * (the question), only the paragraphs that fit it best are kept (T-069, [Excerpt]); without
     * one, the start of the page.
     */
    suspend fun read(
        url: String,
        maxChars: Int,
        focus: String? = null,
    ): ReadPage? {
        require(url.startsWith("https://") || url.startsWith("http://")) { "Not a web address." }
        val response = gate.fetch(GateRequest(url, Purpose.PAGE_READ))
        if (!response.ok) throw IOException("HTTP ${response.code}")
        val raw = HtmlText.extract(response.body.take(MAX_HTML), if (focus.isNullOrBlank()) maxChars else MAX_TEXT)
        if (raw.text.length < MIN_TEXT) return null
        val text = if (focus.isNullOrBlank()) raw.text else Excerpt.select(raw.text, focus, maxChars)
        return ReadPage(url, raw.title.ifBlank { hostOf(url) }, text)
    }

    /** Several pages at the same time, each with its own time limit; pages that fail are left out. */
    @Suppress("TooGenericExceptionCaught", "SwallowedException")
    suspend fun readAll(
        urls: List<String>,
        maxChars: Int,
        focus: String? = null,
    ): List<ReadPage> =
        coroutineScope {
            urls
                .map { url ->
                    async {
                        try {
                            withTimeoutOrNull(timeoutMs) { read(url, maxChars, focus) }
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: OfflineException) {
                            throw e
                        } catch (e: Exception) {
                            null
                        }
                    }
                }.awaitAll()
                .filterNotNull()
        }

    companion object {
        const val PAGE_TIMEOUT_MS = 8_000L
        private const val MAX_HTML = 600_000
        private const val MIN_TEXT = 80

        /** Text looked at before the best paragraphs are picked. */
        private const val MAX_TEXT = 40_000

        fun hostOf(url: String): String = url.substringAfter("://").substringBefore('/').removePrefix("www.")

        /**
         * Pages as the brain reads them: `{"pages":[{"n","title","url","text"}]}`. [numbers] are the
         * source numbers the answer cites (the chips under it); by default 1, 2, 3 …
         */
        fun forModel(
            pages: List<ReadPage>,
            numbers: List<Int> = pages.indices.map { it + 1 },
        ): String =
            JsonObject(
                mapOf(
                    "pages" to
                        JsonArray(
                            pages.mapIndexed { i, p ->
                                JsonObject(
                                    mapOf(
                                        "n" to JsonPrimitive(numbers.getOrElse(i) { i + 1 }),
                                        "title" to JsonPrimitive(p.title),
                                        "url" to JsonPrimitive(p.url),
                                        "text" to JsonPrimitive(p.text),
                                    ),
                                )
                            },
                        ),
                ),
            ).toString()
    }
}

/** `read_page`: the brain reads one address, e.g. a link the user sent (T-067). */
class ReadPageTool(
    private val reader: PageReader,
) : Tool {
    override val online = true
    override val schema =
        ToolSchema(
            name = NAME,
            description = "Read the text of one web page, e.g. a link from the user.",
            parameters =
                buildJsonObject {
                    put("type", "object")
                    putJsonObject("properties") {
                        putJsonObject("url") {
                            put("type", "string")
                            put("description", "The full address, starting with https://")
                        }
                    }
                    put("required", JsonArray(listOf(JsonPrimitive("url"))))
                },
        )

    override suspend fun run(
        args: Map<String, Any?>,
        ctx: ToolContext,
    ): ToolResult {
        val url = Args.required(args, "url")
        val page =
            reader.read(url, MAX_CHARS)
                ?: return ToolRouter.failure("empty", "The page has no text Hain can read.")
        return ToolResult(
            forModel = PageReader.forModel(listOf(page)),
            label = PageReader.hostOf(url),
            sources = listOf(ToolSource(page.title, page.url)),
        )
    }

    companion object {
        const val NAME = "read_page"
        private const val MAX_CHARS = 3000
    }
}
