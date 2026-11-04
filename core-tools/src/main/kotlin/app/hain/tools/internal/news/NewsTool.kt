package app.hain.tools.internal.news

import app.hain.llm.api.ToolSchema
import app.hain.tools.api.Args
import app.hain.tools.api.NewsHeadline
import app.hain.tools.api.Tool
import app.hain.tools.api.ToolContext
import app.hain.tools.api.ToolResult
import app.hain.tools.api.ToolRouter
import app.hain.tools.api.ToolSource
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * `news` (T-072, NEWS.md §4): recent headlines from the feeds of the user's news mode. Finds the
 * best matches for the topic in title and teaser (refreshing the feeds first when they are older
 * than the refresh interval) and returns up to 8 lines `[n] source – title – teaser – date`; the
 * headlines also go to the NewsCard under the answer. No hits → `not_found`, and the router tries
 * a web search instead.
 */
class NewsTool(
    private val repository: NewsRepository,
) : Tool {
    override val online = true
    override val schema =
        ToolSchema(
            name = NAME,
            description = "Get recent headlines. Use for current events.",
            parameters =
                buildJsonObject {
                    put("type", "object")
                    putJsonObject("properties") {
                        putJsonObject("query") {
                            put("type", "string")
                            put("description", "topic, optional")
                        }
                        putJsonObject("lang") {
                            put("type", "string")
                            put("enum", JsonArray(listOf("de", "en", "any").map(::JsonPrimitive)))
                        }
                    }
                    put("required", JsonArray(emptyList()))
                },
        )

    override suspend fun run(
        args: Map<String, Any?>,
        ctx: ToolContext,
    ): ToolResult {
        val query = Args.string(args, "query") ?: Args.string(args, "topic")
        val lang = Args.string(args, "lang")?.lowercase(Locale.ROOT)?.takeIf { it == "de" || it == "en" }
        val items = repository.search(query, lang)
        if (items.isEmpty()) {
            val about = query?.let { " about '$it'" }.orEmpty()
            return ToolRouter.failure("not_found", "No recent headlines$about in the news feeds.")
        }
        return ToolResult(
            forModel = forModel(items, ctx.now.zone),
            label = query,
            sources = items.mapNotNull { item -> item.link?.let { ToolSource("${item.source}: ${item.title}", it) } },
            news = items.map { NewsHeadline(it.source, it.title, it.link, it.published, it.paywall) },
        )
    }

    companion object {
        const val NAME = "news"
        const val TEASER_CHARS = 200
        private val DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm", Locale.ROOT)

        /** `[1] tagesschau – Title – teaser – 2026-11-04 17:30`, one line per headline. */
        fun forModel(
            items: List<NewsItem>,
            zone: ZoneId,
        ): String =
            items
                .mapIndexed { i, item ->
                    val teaser = item.summary.take(TEASER_CHARS).trim()
                    val date = DATE.format(Instant.ofEpochMilli(item.time).atZone(zone))
                    listOf("[${i + 1}] ${item.source}", item.title, teaser, date)
                        .filter { it.isNotBlank() }
                        .joinToString(" – ")
                }.joinToString("\n")
    }
}
