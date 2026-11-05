package app.hain.feature.news.api

import app.hain.tools.internal.news.NewsItem
import app.hain.tools.internal.news.NewsRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import java.io.File
import java.time.Instant
import java.time.ZoneId
import kotlin.math.max

/** One point of the daily briefing: a short sentence, the headline it is about and who reported it. */
data class BriefingPoint(
    val text: String,
    val title: String,
    val sources: List<String>,
    val link: String?,
)

/**
 * "Dein Briefing heute" (T-079): the most important stories of the day in 5–7 points with their
 * sources, made on the phone once a day ([day] = local date) and kept. [byBrain] = the sentences come
 * from the local brain; otherwise they are the stories' own first sentences.
 */
data class Briefing(
    val day: String,
    val createdAt: Long,
    val byBrain: Boolean,
    val points: List<BriefingPoint>,
)

/** A story several headlines are about: the newest [lead] headline and all of them. */
data class BriefingStory(
    val lead: NewsItem,
    val items: List<NewsItem>,
) {
    val sources: List<String> get() = items.map { it.source }.distinct()
}

/**
 * Picks the day's top stories without any server (T-079): headlines of the last 24 hours that share
 * enough words are one story; a story reported by more sources counts more, newer ones a little more.
 */
object BriefingPicker {
    const val MAX_POINTS = 7
    const val MIN_POINTS = 5
    private const val DAY_MS = 24 * 60 * 60 * 1000L
    private const val MIN_WORD = 4
    private const val SAME_STORY = 0.28
    private const val SHARED_WORDS = 3
    private const val SOURCE_WEIGHT = 3.0
    private const val FRESH_HOURS = 24.0
    private const val FRESH_WEIGHT = 2.0
    private const val HOUR_MS = 60 * 60 * 1000.0
    private const val MAX_LEADS_PER_SOURCE = 2
    private const val PICTURE_BONUS = 0.5
    private const val TEASER_WORDS_CHARS = 160

    private val STOP =
        setOf(
            "aber",
            "alle",
            "auch",
            "auf",
            "aus",
            "beim",
            "dass",
            "dem",
            "den",
            "der",
            "des",
            "die",
            "doch",
            "eine",
            "einem",
            "einen",
            "einer",
            "eines",
            "für",
            "gegen",
            "hat",
            "haben",
            "nach",
            "nicht",
            "noch",
            "über",
            "und",
            "unter",
            "vom",
            "von",
            "vor",
            "wie",
            "wird",
            "werden",
            "zum",
            "zur",
            "soll",
            "sind",
            "mehr",
            "neue",
            "neuer",
            "neues",
            "jetzt",
            "heute",
            "this",
            "that",
            "with",
            "from",
            "have",
            "will",
            "after",
            "over",
            "into",
            "about",
            "what",
            "when",
            "they",
            "their",
            "says",
            "said",
            "more",
            "than",
            "were",
            "been",
            "news",
            "live",
            "video",
            "kommentar",
            "interview",
        )

    fun stories(
        items: List<NewsItem>,
        now: Long,
        max: Int = MAX_POINTS,
    ): List<BriefingStory> {
        val recent = items.filter { now - it.time <= DAY_MS }
        val pool = if (recent.size >= MIN_POINTS) recent else items.sortedByDescending { it.time }.take(max * 2 * 2)
        val clusters = mutableListOf<MutableList<Pair<NewsItem, Set<String>>>>()
        pool.sortedByDescending { it.time }.forEach { item ->
            val words = words(item)
            if (words.isEmpty()) return@forEach
            val home =
                clusters.firstOrNull { cluster ->
                    cluster.any { (_, other) -> similar(words, other) }
                }
            if (home != null) home += item to words else clusters += mutableListOf(item to words)
        }
        val scored =
            clusters
                .map { c -> BriefingStory(c.first().first, c.map { it.first }) }
                .sortedByDescending { score(it, now) }
        val perSource = mutableMapOf<String, Int>()
        return scored
            .filter { story ->
                val n = perSource.getOrDefault(story.lead.feedId, 0)
                (n < MAX_LEADS_PER_SOURCE).also { if (it) perSource[story.lead.feedId] = n + 1 }
            }.take(max)
    }

    internal fun words(item: NewsItem): Set<String> =
        NewsRepository
            .normalizeTitle(item.title + " " + item.summary.take(TEASER_WORDS_CHARS))
            .split(' ')
            .filter { it.length >= MIN_WORD && it !in STOP && !it.all(Char::isDigit) }
            .toSet()

    private fun similar(
        a: Set<String>,
        b: Set<String>,
    ): Boolean {
        val shared = a.intersect(b).size
        val union = a.union(b).size
        return shared >= SHARED_WORDS || (union > 0 && shared.toDouble() / union >= SAME_STORY)
    }

    private fun score(
        story: BriefingStory,
        now: Long,
    ): Double {
        val ageHours = (now - story.items.maxOf { it.time }) / HOUR_MS
        val fresh = max(0.0, 1 - ageHours / FRESH_HOURS) * FRESH_WEIGHT
        return story.sources.size * SOURCE_WEIGHT + fresh + if (story.lead.imageUrl != null) PICTURE_BONUS else 0.0
    }

    /** The story's own first sentence (at most [maxChars]), or its headline. */
    fun firstSentence(
        item: NewsItem,
        maxChars: Int = 200,
    ): String {
        val teaser = item.summary.trim()
        if (teaser.isEmpty()) return item.title
        val end =
            Regex("""[.!?](\s|$)""")
                .find(teaser)
                ?.range
                ?.first
                ?.plus(1) ?: teaser.length
        val sentence = teaser.take(end)
        return if (sentence.length <= maxChars) sentence else sentence.take(maxChars).substringBeforeLast(' ') + "…"
    }
}

/** Where today's briefing is kept. */
interface BriefingStore {
    suspend fun load(): Briefing?

    suspend fun save(briefing: Briefing)
}

class MemoryBriefingStore(
    private var saved: Briefing? = null,
) : BriefingStore {
    override suspend fun load(): Briefing? = saved

    override suspend fun save(briefing: Briefing) {
        saved = briefing
    }
}

/** One small JSON file next to the news (`files/news/briefing.json`). */
class FileBriefingStore(
    private val file: File,
) : BriefingStore {
    override suspend fun load(): Briefing? =
        withContext(Dispatchers.IO) {
            if (!file.isFile) null else runCatching { BriefingJson.decode(file.readText()) }.getOrNull()
        }

    override suspend fun save(briefing: Briefing) {
        withContext(Dispatchers.IO) {
            runCatching {
                file.parentFile?.mkdirs()
                file.writeText(BriefingJson.encode(briefing))
            }
        }
    }
}

object BriefingJson {
    fun encode(b: Briefing): String =
        buildJsonObject {
            put("day", b.day)
            put("createdAt", b.createdAt)
            put("byBrain", b.byBrain)
            putJsonArray("points") {
                b.points.forEach { p ->
                    add(
                        buildJsonObject {
                            put("text", p.text)
                            put("title", p.title)
                            putJsonArray("sources") { p.sources.forEach { add(JsonPrimitive(it)) } }
                            p.link?.let { put("link", it) }
                        },
                    )
                }
            }
        }.toString()

    fun decode(text: String): Briefing? {
        val o = Json.parseToJsonElement(text).jsonObject
        val day = o.str("day") ?: return null
        val points =
            (o["points"] as? JsonArray).orEmpty().mapNotNull { el ->
                val p = el as? JsonObject ?: return@mapNotNull null
                BriefingPoint(
                    text = p.str("text") ?: return@mapNotNull null,
                    title = p.str("title").orEmpty(),
                    sources = (p["sources"] as? JsonArray).orEmpty().mapNotNull { (it as? JsonPrimitive)?.content },
                    link = p.str("link"),
                )
            }
        return Briefing(
            day = day,
            createdAt = (o["createdAt"] as? JsonPrimitive)?.longOrNull ?: 0,
            byBrain = (o["byBrain"] as? JsonPrimitive)?.booleanOrNull == true,
            points = points,
        )
    }

    private fun JsonObject.str(key: String): String? = (this[key] as? JsonPrimitive)?.takeIf { it.isString }?.content
}

/** T-265: what the briefing is doing right now, shown on its card with a counter for the feeds. */
enum class BriefingStep { FEEDS, READING, SUMMING_UP }

/**
 * Today's briefing (T-079): made at most once a day and kept in [store]; "Neu erzeugen" makes it
 * again. With a brain each point is one short sentence written on the phone; without one (or when it
 * fails) the points are the stories' own first sentences. [quickBrain] is used when the screen opens
 * and should only answer when the brain is already loaded (no 1 GB load just for the briefing);
 * [brain] is used for "Neu erzeugen". A briefing made without the brain is made again with it once
 * the brain is there.
 */
class BriefingMaker(
    private val store: BriefingStore,
    private val brain: BrainCall? = null,
    private val quickBrain: BrainCall? = brain,
    private val clock: () -> Long = System::currentTimeMillis,
    private val zone: () -> ZoneId = ZoneId::systemDefault,
) {
    private val lock = Mutex()

    fun dayOf(time: Long): String =
        Instant
            .ofEpochMilli(time)
            .atZone(zone())
            .toLocalDate()
            .toString()

    /** The kept briefing if it is from today, else `null`. */
    suspend fun cached(): Briefing? = store.load()?.takeIf { it.day == dayOf(clock()) }

    /**
     * Today's briefing from [items]: the kept one unless [force] (or it was made without the brain
     * and [useBrain] allows it now). `null` when there are too few headlines.
     */
    @Suppress("CyclomaticComplexMethod") // kept, upgrade, forced and too-few cases in one locked step
    suspend fun today(
        items: List<NewsItem>,
        lang: String,
        force: Boolean = false,
        useBrain: Boolean = true,
        onStep: (BriefingStep) -> Unit = {},
    ): Briefing? =
        lock.withLock {
            val kept = cached()
            val upgrade = kept != null && !kept.byBrain && useBrain && quickBrain != null
            if (kept != null && !force && !upgrade) return@withLock kept
            val now = clock()
            onStep(BriefingStep.READING)
            val stories = BriefingPicker.stories(items, now)
            if (stories.size < MIN_STORIES) return@withLock kept
            val writer =
                if (!useBrain) {
                    null
                } else if (force) {
                    brain
                } else {
                    quickBrain
                }
            if (writer != null) onStep(BriefingStep.SUMMING_UP)
            val sentences = writer?.let { brainSentences(it, stories, lang) }
            // an upgrade without an answer keeps today's briefing as it is
            if (upgrade && !force && sentences == null) return@withLock kept
            val points =
                stories.mapIndexed { i, s ->
                    BriefingPoint(
                        text = sentences?.getOrNull(i) ?: BriefingPicker.firstSentence(s.lead),
                        title = s.lead.title,
                        sources = s.sources,
                        link = s.lead.link,
                    )
                }
            val made = Briefing(dayOf(now), now, sentences != null, points)
            store.save(made)
            made
        }

    @Suppress("TooGenericExceptionCaught", "SwallowedException")
    private suspend fun brainSentences(
        brain: BrainCall,
        stories: List<BriefingStory>,
        lang: String,
    ): List<String>? {
        val list =
            stories
                .mapIndexed { i, s -> "${i + 1}. ${s.lead.title} — ${BriefingPicker.firstSentence(s.lead, TEASER)}" }
                .joinToString("\n")
        val answer =
            try {
                brain.complete(prompt(lang), list)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                null
            }
        val lines = numbered(answer ?: return null, stories.size)
        return lines.takeIf { it.count { l -> l != null } >= stories.size - 1 }?.mapIndexed { i, l ->
            l ?: BriefingPicker.firstSentence(stories[i].lead)
        }
    }

    companion object {
        const val MIN_STORIES = 3
        private const val TEASER = 220
        private val NUMBERED = Regex("""^\s*(\d{1,2})[.)]\s*(.+)$""")

        fun prompt(lang: String): String =
            if (lang == "de") {
                "Unten stehen die wichtigsten Meldungen des Tages, nummeriert. Schreib zu jeder Nummer genau " +
                    "einen kurzen, sachlichen Satz auf Deutsch in einfacher Sprache, der sagt, was passiert ist. " +
                    "Format: \"1. Satz\". Keine Einleitung, keine eigene Meinung, nichts erfinden."
            } else {
                "Below are the day's most important news items, numbered. For each number write exactly one " +
                    "short, neutral sentence in plain English that says what happened. Format: \"1. Sentence\". " +
                    "No intro, no opinion, do not make anything up."
            }

        /** Line `n.` of the brain's answer for n = 1..[count] (thinking dropped); `null` where missing. */
        fun numbered(
            raw: String,
            count: Int,
        ): List<String?> {
            val found = mutableMapOf<Int, String>()
            raw.substringAfter("</think>").lines().forEach { line ->
                val m = NUMBERED.find(line) ?: return@forEach
                val n = m.groupValues[1].toInt()
                val text = m.groupValues[2].replace("**", "").trim()
                if (n in 1..count && n !in found && text.isNotEmpty()) found[n] = text
            }
            return (1..count).map { found[it] }
        }
    }
}
