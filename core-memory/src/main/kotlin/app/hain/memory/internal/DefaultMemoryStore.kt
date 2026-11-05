package app.hain.memory.internal

import app.hain.memory.api.EmbedKind
import app.hain.memory.api.Embedder
import app.hain.memory.api.MemoryCategory
import app.hain.memory.api.MemoryContext
import app.hain.memory.api.MemoryFact
import app.hain.memory.api.MemoryStore
import app.hain.memory.api.MemoryTopic
import app.hain.memory.api.Nearest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull

/** On/off switch of the memory, kept outside the database (SharedPreferences on the phone). */
interface MemorySwitch {
    fun get(): Boolean

    fun set(on: Boolean)
}

/** A switch that only lives in memory (tests, previews). */
class PlainSwitch(
    private var on: Boolean = true,
) : MemorySwitch {
    override fun get() = on

    override fun set(on: Boolean) {
        this.on = on
    }
}

/**
 * The memory (T-112, T-113): facts from [source], mirrored in [facts] so the prompt can read them
 * on every turn without waiting for the database. With an [embedder] (T-110, optional pack) every
 * fact gets a vector in [vectors] in the background and recall is hybrid (MEMORY.md §3).
 */
@Suppress("TooManyFunctions", "LongParameterList") // one function per action of the memory page; test hooks
class DefaultMemoryStore(
    private val source: FactSource,
    private val scope: CoroutineScope,
    private val switch: MemorySwitch = PlainSwitch(),
    private val embedder: Embedder? = null,
    vectors: VectorSource = InMemoryVectorSource(),
    private val clock: () -> Long = System::currentTimeMillis,
) : MemoryStore {
    override val facts: StateFlow<List<MemoryFact>> =
        source.observe().stateIn(scope, SharingStarted.Eagerly, emptyList())

    override val topics: StateFlow<List<MemoryTopic>> =
        source.observeTopics().stateIn(scope, SharingStarted.Eagerly, emptyList())

    private val index: VectorIndex? = embedder?.let { VectorIndex(vectors, it) }

    init {
        // new and edited facts get their vector in the background (one at a time, outside the lock)
        if (index != null) scope.launch { facts.collectLatest { index.sync(it) } }
    }

    private val on = MutableStateFlow(switch.get())
    override val enabled: StateFlow<Boolean> = on

    /** Facts written but maybe not yet back from [source] (so a quick second "merk dir" is no duplicate). */
    private val written = mutableListOf<MemoryFact>()
    private val lock = Mutex()

    override fun setEnabled(on: Boolean) {
        switch.set(on)
        this.on.value = on
    }

    override fun profile(): List<MemoryFact> {
        if (!on.value) return emptyList()
        // newest first; one fact per slot (an accepted update puts the old one out of use anyway)
        return facts.value
            .filter { !it.suggested && !it.sensitive }
            .mapNotNull { f -> SelfFacts.slot(f.text)?.let { it to f } }
            .distinctBy { it.first }
            .map { it.second }
            .take(PROFILE_MAX)
    }

    override fun homePlace(): String? =
        SelfFacts.homePlace(profile().filter { SelfFacts.slot(it.text) == SelfFacts.Slot.HOME }.map { it.text })

    override suspend fun remember(
        text: String,
        category: MemoryCategory,
        sensitive: Boolean,
        suggested: Boolean,
        originChatId: Long?,
        replaces: Long?,
    ): MemoryFact? {
        if (!on.value) return null
        val fact =
            MemoryFact(
                id = 0,
                text = ExplicitMemory.cut(text),
                category = category,
                createdAt = clock(),
                suggested = suggested,
                sensitive = sensitive || category == MemoryCategory.HEALTH,
                originChatId = originChatId,
                replaces = replaces,
            )
        return add(fact)?.first
    }

    /** Inserts [fact] unless the same text is kept already: (the fact, whether it is new); `null` when empty. */
    private suspend fun add(fact: MemoryFact): Pair<MemoryFact, Boolean>? {
        if (fact.text.isBlank()) return null
        return lock.withLock {
            val key = normal(fact.text)
            val known = (facts.value + written).firstOrNull { normal(it.text) == key }
            if (known != null) return@withLock known to false
            val saved = fact.copy(id = source.insert(fact))
            written += saved
            if (written.size > RECENT) written.removeAt(0)
            saved to true
        }
    }

    override suspend fun edit(
        id: Long,
        text: String,
    ) {
        val clean = ExplicitMemory.cut(text)
        val fact = (facts.value + written).firstOrNull { it.id == id } ?: return
        if (clean.isBlank()) {
            delete(id)
        } else {
            lock.withLock { written.replaceAll { if (it.id == id) it.copy(text = clean) else it } }
            source.update(fact.copy(text = clean))
        }
    }

    override suspend fun delete(id: Long) {
        lock.withLock { written.removeAll { it.id == id } }
        source.delete(id)
        index?.forget(id)
    }

    override suspend fun restore(fact: MemoryFact) {
        add(fact.copy(id = 0))
    }

    override suspend fun accept(id: Long) {
        val fact = (facts.value + written).firstOrNull { it.id == id } ?: return
        lock.withLock { written.replaceAll { if (it.id == id) it.copy(suggested = false, replaces = null) else it } }
        source.update(fact.copy(suggested = false, replaces = null))
        // an update or contradiction found in the background: the old fact goes out of use, not away
        val old = fact.replaces?.let { r -> (facts.value + written).firstOrNull { it.id == r && it.id != id } }
        if (old != null) {
            lock.withLock { written.removeAll { it.id == old.id } }
            source.supersede(old.id, id)
            index?.forget(old.id)
        }
    }

    override suspend fun deleteAll() {
        lock.withLock { written.clear() }
        source.deleteAll()
        index?.clear()
    }

    override suspend fun setTopicNote(
        title: String,
        note: String,
    ) {
        val clean = note.trim().take(MemoryTopic.MAX_NOTE)
        source.setTopic(MemoryTopic(title.trim(), clean, clock()))
    }

    override fun smartSearch(): Boolean = embedder?.ready() == true

    @Suppress("ReturnCount")
    override suspend fun recall(
        query: String,
        lang: String,
        now: Long,
    ): MemoryContext? {
        if (!on.value) return null
        val all = facts.value
        val usable = all.filter { !it.suggested }
        if (usable.isEmpty()) return null
        val cosines = queryCosines(query, usable)
        val chosen =
            Ranking
                .select(all, query, now, cosine = cosines?.let { m -> { f -> m[f.id]?.let(Vectors::relevance) } })
                .map { it.fact }
        if (chosen.isEmpty()) return null
        return MemoryContext(MemoryBlock.of(chosen, lang, notes = notesFor(chosen, lang)), chosen)
    }

    /** Cosine of every fact with a vector to [query]; `null` without the embedder (keywords only). */
    @Suppress("ReturnCount")
    private suspend fun queryCosines(
        query: String,
        usable: List<MemoryFact>,
    ): Map<Long, Double>? {
        val idx = index ?: return null
        val emb = embedder ?: return null
        if (!emb.ready() || query.isBlank()) return null
        val vector = withTimeoutOrNull(QUERY_TIMEOUT_MS) { emb.embed(query, EmbedKind.QUERY) } ?: return null
        val cosines = idx.cosines(usable, vector)
        // the pack came after the facts: fill in the missing vectors for the next questions
        if (cosines.size < usable.size) scope.launch { idx.sync(facts.value) }
        return cosines
    }

    @Suppress("ReturnCount")
    override suspend fun nearest(text: String): Nearest? {
        val idx = index ?: return null
        val emb = embedder ?: return null
        if (!emb.ready()) return null
        val all = facts.value
        if (all.isEmpty()) return null
        val vector = emb.embed(text, EmbedKind.DOCUMENT) ?: return null
        idx.sync(all)
        val best = idx.cosines(all, vector).maxByOrNull { it.value } ?: return null
        return all.firstOrNull { it.id == best.key }?.let { Nearest(it, best.value) }
    }

    /** The user's notes on the topics of [chosen] (topic title = category key). */
    private fun notesFor(
        chosen: List<MemoryFact>,
        lang: String,
    ): List<Pair<String, String>> {
        val keys = chosen.map { it.category.key }.toSet()
        return topics.value.filter { it.title in keys }.map { MemoryBlock.topicLabel(it.title, lang) to it.note }
    }

    @Suppress("ReturnCount")
    override suspend fun forget(query: String): List<MemoryFact> {
        if (!on.value) return emptyList()
        val all = facts.value
        val scores = Ranking.keywordScores(all, query)
        val best = scores.maxOrNull() ?: return emptyList()
        if (best <= 0.0) return emptyList()
        // the best match and any about as good (two facts about the same thing), never more than a few
        val gone = all.filterIndexed { i, _ -> scores[i] >= best - FORGET_MARGIN }.take(MAX_FORGET)
        gone.forEach { delete(it.id) }
        return gone
    }

    @Suppress("ReturnCount")
    override fun context(
        query: String,
        lang: String,
        now: Long,
    ): MemoryContext? {
        if (!on.value) return null
        val chosen = Ranking.select(facts.value, query, now).map { it.fact }
        if (chosen.isEmpty()) return null
        return MemoryContext(MemoryBlock.of(chosen, lang, notes = notesFor(chosen, lang)), chosen)
    }

    override fun exportJson(): String {
        val rows =
            facts.value.map { f ->
                JsonObject(
                    mapOf(
                        "text" to JsonPrimitive(f.text),
                        "category" to JsonPrimitive(f.category.key),
                        "status" to JsonPrimitive(if (f.suggested) SUGGESTED else ACTIVE),
                        "sensitive" to JsonPrimitive(f.sensitive),
                        "importance" to JsonPrimitive(f.importance),
                        "created_at" to JsonPrimitive(f.createdAt),
                    ),
                )
            }
        val root =
            mapOf("app" to JsonPrimitive(APP), "version" to JsonPrimitive(EXPORT_VERSION), "facts" to JsonArray(rows))
        return JsonObject(root).toString()
    }

    override suspend fun importJson(json: String): Int {
        val root = Json.parseToJsonElement(json).jsonObject
        require(root["app"]?.jsonPrimitive?.content == APP) { "not a Hain memory file" }
        val rows = requireNotNull(root["facts"]?.jsonArray) { "no facts" }
        var added = 0
        for (row in rows) {
            val o = row.jsonObject
            val text = ExplicitMemory.cut(o["text"]?.jsonPrimitive?.content.orEmpty())
            val category = MemoryCategory.fromKey(o["category"]?.jsonPrimitive?.content)
            val fact =
                MemoryFact(
                    id = 0,
                    text = text,
                    category = category,
                    createdAt = o["created_at"]?.jsonPrimitive?.longOrNull ?: clock(),
                    suggested = o["status"]?.jsonPrimitive?.content == SUGGESTED,
                    sensitive =
                        o["sensitive"]?.jsonPrimitive?.booleanOrNull == true || category == MemoryCategory.HEALTH,
                    importance =
                        o["importance"]?.jsonPrimitive?.doubleOrNull?.coerceIn(0.0, 1.0)
                            ?: MemoryFact.DEFAULT_IMPORTANCE,
                )
            if (add(fact)?.second == true) added++
        }
        return added
    }

    private fun normal(text: String): String = text.lowercase().replace(NOT_WORD, " ").trim()

    private companion object {
        val NOT_WORD = Regex("[^\\p{L}\\p{N}]+")
        const val RECENT = 32
        const val PROFILE_MAX = 6
        const val FORGET_MARGIN = 0.05
        const val MAX_FORGET = 3
        const val EXPORT_VERSION = 1
        const val APP = "hain"
        const val ACTIVE = "active"
        const val SUGGESTED = "suggested"

        /** The question's vector must not hold up the answer: the first one may load the pack. */
        const val QUERY_TIMEOUT_MS = 2_500L
    }
}
