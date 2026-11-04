package app.hain.feature.chat.internal

import app.hain.common.api.DevSettings
import app.hain.llm.api.ChatMessage
import app.hain.llm.api.DevTuning
import app.hain.llm.api.GenerationEvent
import app.hain.llm.api.GenerationStats
import app.hain.llm.api.LlmEngine
import app.hain.llm.api.ParsedOutput
import app.hain.llm.api.PromptBuilder
import app.hain.llm.api.Role
import app.hain.llm.api.SamplingParams
import app.hain.llm.api.StopReason
import app.hain.llm.api.ToolCall
import app.hain.llm.api.ToolSchema
import app.hain.llm.api.UiDelta
import app.hain.llm.internal.AnswerMode
import app.hain.llm.internal.GgufPromptBuilder
import app.hain.llm.internal.LfmPromptBuilder
import app.hain.llm.internal.StreamSplitter
import app.hain.llm.internal.ThinkingPolicy
import app.hain.tools.api.HainTools
import app.hain.tools.api.InfoCard
import app.hain.tools.api.NewsHeadline
import app.hain.tools.api.ResearchBudget
import app.hain.tools.api.ResearchNotes
import app.hain.tools.api.ToolCallFixer
import app.hain.tools.api.ToolContext
import app.hain.tools.api.ToolResult
import app.hain.tools.api.ToolRouter
import app.hain.tools.internal.search.PageReader
import app.hain.tools.internal.search.ReadPage
import app.hain.tools.internal.search.SearchTool
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.transformWhile
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import java.time.ZonedDateTime

/** What the UI sees while one answer is produced. Text fields hold everything so far. */
sealed interface TurnUpdate {
    data class Streaming(
        val reasoning: String,
        val text: String,
        val thinking: Boolean,
        val steps: List<ToolStep> = emptyList(),
        /** Running research clock (Gründlich, T-069); `null` = no time display. */
        val research: ResearchClock? = null,
    ) : TurnUpdate

    data class Finished(
        val output: ParsedOutput,
        val reason: StopReason,
        val thinkingMillis: Long?,
        val steps: List<ToolStep> = emptyList(),
        val sources: List<SourceRef> = emptyList(),
        val trace: List<ToolTrace> = emptyList(),
        val actions: List<ActionCard> = emptyList(),
        /** Headlines of the `news` tool for the NewsCard (T-072). */
        val news: List<NewsHeadline> = emptyList(),
        /** Speed of the last generation (developer mode, T-211). */
        val stats: GenerationStats? = null,
        /** Weather and Wikipedia cards for under the answer (3b-2). */
        val cards: List<InfoCard> = emptyList(),
    ) : TurnUpdate
}

/** When the research of an answer started and how long it may take, for a quiet time display. */
data class ResearchClock(
    val startedAt: Long,
    val budgetMs: Long,
)

/**
 * One user turn: system prompt + history → prompt → stream → [StreamSplitter] for the live view →
 * strict parse at the end. With [tools] the brain may call tools (T-062, LLM.md §8) within the
 * [ResearchBudget] of the answer mode (T-069: rounds, searches, pages, time), then one last round
 * without tools so it has to answer. Tools of one round run at the same time; pages are read as
 * excerpts and older results shrink to [ResearchNotes]. A message that is only a sum ("12*7") is
 * answered by the calculator at once, without the brain.
 *
 * Speed (2d-2): the system prompt is filled with the time of [promptAnchor], which only moves on
 * every [ANCHOR_MS], so the prompt start stays the same and the brain does not re-read the chat.
 * The prompt is sent with checkpoints (end of the fixed system text, of the system prompt, of the
 * history) where the next turn picks up.
 *
 * Language (T-080 fix): [turnHint] gives a short system line ("Antworte auf Türkisch.") that is put
 * after the history, right before the answer. It is never stored, so the cached start and the
 * checkpoint at the end of the history stay valid.
 */
@Suppress("LongParameterList", "TooManyFunctions") // test hooks; one function per step of a turn
class ChatEngine(
    private val llm: LlmEngine,
    private val systemPrompt: (atMillis: Long) -> String,
    private val promptBuilder: PromptBuilder = LfmPromptBuilder(),
    private val clock: () -> Long = System::currentTimeMillis,
    private val answerMode: () -> AnswerMode = { AnswerMode.AUTO },
    private val turnHint: (userMessages: List<String>) -> String? = { null },
    private val tools: ToolRouter? = null,
    private val toolContext: () -> ToolContext = { ToolContext("en", ZonedDateTime.now(), offline = false) },
    /** Developer settings (T-211), read at the start of every message. */
    private val tuning: () -> DevSettings = { DevSettings() },
    /** `true` while an own GGUF from developer mode is the brain: its template, no tools (T-211). */
    private val ownModel: () -> Boolean = { false },
    /**
     * 7b: the memory's line for this turn (facts that fit, "Gemerkt"), after [turnHint]. It may embed
     * the question with the memory pack, so it runs off the main thread and may suspend.
     */
    private val memoryHint: (suspend (userMessages: List<String>) -> String?)? = null,
) {
    private val ggufBuilder = GgufPromptBuilder(llm::applyTemplate)

    /** Hain's LFM prompt, or the own model's template in developer mode. */
    private fun builder(): PromptBuilder = if (ownModel()) ggufBuilder else promptBuilder

    private var anchor = 0L

    /** Time the system prompt is filled with; moves forward at most every [ANCHOR_MS]. */
    internal fun promptAnchor(): Long {
        val now = clock()
        if (anchor == 0L || now - anchor >= ANCHOR_MS || now < anchor) anchor = now
        return anchor
    }

    /** Reads the system prompt into the brain's memory, so the first answer starts sooner. */
    suspend fun warmUp(): Boolean {
        val system = DevTuning.systemPrompt(tuning(), systemPrompt(promptAnchor()))
        val prompt = builder().build(system, schemas(), emptyList(), false, false)
        return llm.prefill(prompt, listOfNotNull(fixedSystemEnd(prompt, system)))
    }

    /**
     * Stops the research of the running answer (T-069): no more tools, Pim answers right away with
     * what it collected. Only while it is still looking things up; `false` when there is nothing to
     * stop that way (no tool yet, or the answer is already being written), then the caller stops
     * the whole answer instead.
     */
    fun stopResearch(): Boolean {
        val turn = active
        if (turn == null || !turn.canStop) return false
        turn.stopRequested = true
        turn.stopSignal.value = true
        llm.cancel()
        return true
    }

    @Volatile private var active: Turn? = null

    @Suppress("CyclomaticComplexMethod", "LongMethod") // the research loop: one place for its rules
    fun respond(
        history: List<ChatMessage>,
        mode: AnswerMode = answerMode(),
    ): Flow<TurnUpdate> =
        flow {
            val lastUser = history.lastOrNull()?.content.orEmpty()
            val budget = ResearchBudget.of(mode)
            val ctx =
                toolContext().copy(
                    pagesToRead = budget.pagesPerSearch,
                    searchApproved = SearchConsent.approved(history),
                )
            val quick = quickAnswer(history, ctx)
            if (quick != null) {
                emit(quick)
                return@flow
            }
            val dev = tuning()
            val thinking = DevTuning.thinking(dev, ThinkingPolicy.decide(lastUser, mode))
            val system = DevTuning.systemPrompt(dev, systemPrompt(promptAnchor()))
            val schemas = if (ownModel()) emptyList() else tools?.schemas(ctx.offline, ctx.webSearch).orEmpty()
            val researchHint = if (schemas.isNotEmpty() && !ctx.offline) researchHint(mode, ctx.lang) else null
            val online = schemas.any { tools?.isOnline(it.name) == true }
            val followUp =
                (if (online) FollowUp.link(history, ctx.lang) else null)
                    ?: if (schemas.isNotEmpty()) FollowUp.hint(history, ctx.lang) else null
            val hints = listOfNotNull(researchHint, followUp).joinToString(" ").ifBlank { null }
            val base = withLanguageHint(history, lastUser, hints)
            val turn = Turn(budget, lastUser, dev)
            active = turn
            try {
                var round = 0
                while (true) {
                    val final =
                        round >= budget.maxRounds || turn.stopRequested || budget.timeUp(turn.startedAt, clock())
                    turn.finalRound = final
                    turn.textStarted = false
                    val roundTools = if (final) emptyList() else schemas
                    val closing = if (final && turn.researching) listOf(closingLine(ctx.lang)) else emptyList()
                    val messages = base + turn.promptMessages(round) + closing
                    val result = generateRound(system, roundTools, messages, history, thinking, turn)
                    turn.thinkingMs += result.thinkingMs ?: 0L
                    // stopped or out of time while the brain was still choosing tools: answer with what is there
                    if (!final && result.reason == StopReason.CANCELLED && turn.cut) {
                        result.output.reasoning?.let { turn.reasoning.append(it).append("\n\n") }
                        round = budget.maxRounds
                        continue
                    }
                    val router = tools
                    // 4d: real names and parameters, dates relative to now, the same call only once
                    val calls =
                        router
                            ?.let { r -> ToolCallFixer.dedupe(result.output.toolCalls.map { r.fix(it, ctx).call }) }
                            .orEmpty()
                            .take(budget.callsPerRound)
                    val answered = calls.isEmpty() || roundTools.isEmpty() || result.reason != StopReason.END
                    if (router == null || answered) {
                        val earlier = turn.reasoning.toString().trim()
                        val reasoning =
                            listOfNotNull(earlier.ifBlank { null }, result.output.reasoning?.ifBlank { null })
                                .joinToString("\n\n")
                                .ifBlank { null }
                        // T-220: no [1] without a source, no tool talk, no "Fakt:" label
                        emit(
                            TurnUpdate.Finished(
                                result.output.copy(reasoning = reasoning, text = cleaned(result.output.text, turn)),
                                result.reason,
                                if (thinking) turn.thinkingMs else null,
                                turn.steps.toList(),
                                turn.sources.toList(),
                                turn.trace.toList(),
                                turn.actions.toList(),
                                news = turn.news.toList(),
                                stats = result.stats,
                                cards = turn.cards.toList(),
                            ),
                        )
                        return@flow
                    }
                    result.output.reasoning?.let { turn.reasoning.append(it).append("\n\n") }
                    turn.add(ChatMessage(Role.ASSISTANT, result.output.text, calls, result.output.reasoning), round)
                    runCalls(router, calls, ctx, turn, thinking, round)
                    round++
                }
            } finally {
                if (active === turn) active = null
            }
        }

    /** What one user turn collected so far across tool rounds. */
    private class Turn(
        val budget: ResearchBudget,
        val question: String,
        val dev: DevSettings = DevSettings(),
    ) {
        /** One message of the research; [compact] replaces it once a newer round exists (T-069). */
        class Entry(
            val message: ChatMessage,
            val round: Int,
            val compact: String? = null,
        )

        val entries = mutableListOf<Entry>()
        val steps = mutableListOf<ToolStep>()
        val sources = mutableListOf<SourceRef>()
        val trace = mutableListOf<ToolTrace>()
        val actions = mutableListOf<ActionCard>()
        val news = mutableListOf<NewsHeadline>()
        val cards = mutableListOf<InfoCard>()
        val notes = ResearchNotes()
        val readUrls = mutableSetOf<String>()
        val readSites = mutableSetOf<String>()

        /** Calls already run in this turn ([ToolCallFixer.key]); a repeat gets a hint instead (4d). */
        val ranKeys = mutableSetOf<String>()
        val reasoning = StringBuilder()
        var thinkingMs = 0L
        var searches = 0
        var startedAt: Long? = null

        @Volatile var researching = false

        @Volatile var finalRound = false

        @Volatile var textStarted = false

        @Volatile var stopRequested = false

        /** The research time ran out while the brain was still choosing tools. */
        @Volatile var timeCut = false
        val stopSignal = MutableStateFlow(false)

        val cut: Boolean get() = stopRequested || timeCut

        /** Stop can still end only the research: tools ran, no answer text yet, not stopped before. */
        val canStop: Boolean get() = researching && !finalRound && !answering

        private val answering: Boolean get() = textStarted || stopRequested

        fun add(
            message: ChatMessage,
            round: Int,
            compact: String? = null,
        ) {
            entries += Entry(message, round, compact)
        }

        /** Messages for round [round]: results of the last round in full, older ones as notes. */
        fun promptMessages(round: Int): List<ChatMessage> =
            entries.map { e ->
                if (e.compact != null && e.round < round - 1) e.message.copy(content = e.compact) else e.message
            }

        /** Number of the source [url] (its chip), adding it when new. */
        fun sourceNumber(
            title: String,
            url: String,
        ): Int {
            val i = sources.indexOfFirst { it.url == url }
            if (i >= 0) return i + 1
            sources += SourceRef(title, url)
            return sources.size
        }
    }

    private data class RoundResult(
        val output: ParsedOutput,
        val reason: StopReason,
        val thinkingMs: Long?,
        val stats: GenerationStats? = null,
    )

    /** One tool call of a round, its timeline line and whether the search budget refused it. */
    private class Planned(
        val call: ToolCall,
        val step: ToolStep,
        val refused: Boolean,
        val repeat: Boolean = false,
    )

    /**
     * Runs the tool calls of one round (T-069): all at the same time, then the pages of every search
     * at the same time, one line each in the timeline. Stop or the end of the research time cuts it
     * short; what was found until then is kept.
     */
    @Suppress("LongParameterList", "LongMethod", "CyclomaticComplexMethod") // one round needs the whole turn
    private suspend fun FlowCollector<TurnUpdate>.runCalls(
        router: ToolRouter,
        calls: List<ToolCall>,
        ctx: ToolContext,
        turn: Turn,
        thinking: Boolean,
        round: Int,
    ) {
        if (turn.startedAt == null) turn.startedAt = clock()
        turn.researching = true
        val planned =
            calls.map { call ->
                val repeat = !turn.ranKeys.add(ToolCallFixer.key(call))
                val search = call.name == SEARCH_TOOL && !repeat
                val refused = search && turn.searches >= turn.budget.maxSearches
                if (search && !refused) turn.searches++
                val step = ToolStep(call.name, detailOf(call), StepState.RUNNING, router.isOnline(call.name))
                Planned(call, step, refused, repeat)
            }
        val firstStep = turn.steps.size
        planned.forEach { turn.steps += it.step }
        emitProgress(turn, thinking)
        val results =
            guarded(turn) {
                coroutineScope {
                    planned
                        .map { p ->
                            async {
                                when {
                                    p.repeat -> ToolRouter.failure("repeat", REPEAT_HINT)
                                    p.refused -> ToolRouter.failure("limit", LIMIT_HINT)
                                    else -> router.run(p.call, ctx)
                                }
                            }
                        }.awaitAll()
                }
            } ?: planned.map { ToolRouter.failure(if (turn.stopRequested) "stopped" else "time_up", CUT_HINT) }
        results.forEachIndexed { i, result ->
            // a Wikipedia call that became a web search used one of the searches
            if (result.via == SEARCH_TOOL && planned[i].call.name != SEARCH_TOOL) turn.searches++
            turn.steps[firstStep + i] =
                planned[i].step.copy(
                    detail = result.label ?: planned[i].step.detail,
                    state = if (result.error == null) StepState.DONE else StepState.FAILED,
                    error = result.error,
                    via = result.via,
                )
            // a phone action is only prepared here; the card under the answer asks before anything happens
            result.action?.let { turn.actions += ActionCard(it) }
            // T-072: headlines for the NewsCard, each article once
            result.news.forEach { h -> if (turn.news.none { it.title == h.title && it.url == h.url }) turn.news += h }
            // 3b-2: weather and Wikipedia as a card under the answer, one per place or article
            result.card?.let { card -> if (turn.cards.none { it == card }) turn.cards += card }
        }
        val pages = readPages(router, planned.map { it.call }, results, ctx, turn, thinking)
        results.forEachIndexed { i, result ->
            val call = planned[i].call
            val read = pages[i]
            val numbered = read.map { turn.sourceNumber(it.title, it.url) }
            if (read.isEmpty()) result.sources.forEach { turn.sourceNumber(it.title, it.url) }
            numbered.zip(read).forEach { (n, page) -> turn.notes.add(n, page.text, focusOf(call, turn)) }
            val content = if (read.isEmpty()) result.forModel else withPages(result, read, numbered)
            val compact = if (read.isEmpty()) compactOf(content) else notesOf(result, numbered, turn)
            turn.add(ChatMessage(Role.TOOL, content, toolCallId = call.id), round, compact)
            turn.trace += ToolTrace(call.name, call.arguments.mapValues { it.value?.toString().orEmpty() }, compact)
        }
        if (turn.budget.liveNotes && turn.notes.facts > 0) {
            turn.steps.removeAll { it.name == NOTES_STEP }
            turn.steps += ToolStep(NOTES_STEP, "${turn.notes.facts}/${turn.notes.sources}", StepState.DONE, false)
        }
        emitProgress(turn, thinking)
    }

    /**
     * After the searches of a round: reads their best pages, each from a site not read yet, within
     * the page budget, all at once; one timeline line ("Lese 4 Seiten …"). One list per result.
     */
    @Suppress("LongParameterList")
    private suspend fun FlowCollector<TurnUpdate>.readPages(
        router: ToolRouter,
        calls: List<ToolCall>,
        results: List<ToolResult>,
        ctx: ToolContext,
        turn: Turn,
        thinking: Boolean,
    ): List<List<ReadPage>> {
        var left = turn.budget.maxPages - turn.readUrls.size
        val picks =
            results.map { result ->
                if (result.error != null || left <= 0) return@map emptyList()
                val urls =
                    result.readUrls
                        .filter { it !in turn.readUrls && SearchTool.siteOf(it) !in turn.readSites }
                        .distinctBy { SearchTool.siteOf(it) }
                        .take(minOf(turn.budget.pagesPerSearch, left))
                left -= urls.size
                urls.forEach {
                    turn.readUrls += it
                    turn.readSites += SearchTool.siteOf(it)
                }
                urls
            }
        val total = picks.sumOf { it.size }
        if (total == 0) return results.map { emptyList() }
        val step = ToolStep(READ_STEP, total.toString(), StepState.RUNNING, online = true)
        turn.steps += step
        val at = turn.steps.lastIndex
        emitProgress(turn, thinking)
        val read =
            guarded(turn) {
                coroutineScope {
                    picks
                        .mapIndexed { i, urls ->
                            async {
                                val left = turn.budget.remaining(turn.startedAt, clock())
                                val limit = minOf(ToolRouter.READ_TIMEOUT_MS, left)
                                router.readExcerpts(urls, ctx, turn.budget.charsPerPage, focusOf(calls[i], turn), limit)
                            }
                        }.awaitAll()
                }
            } ?: picks.map { emptyList() }
        val count = read.sumOf { it.size }
        turn.steps[at] =
            if (count == 0) {
                step.copy(state = StepState.FAILED, error = "failed")
            } else {
                step.copy(detail = "$count", state = StepState.DONE)
            }
        return read
    }

    /**
     * Runs [block] unless the research time is up or Stop was pressed; Stop or the end of the time
     * cancels it midway. `null` = cut short.
     */
    @Suppress("SwallowedException")
    private suspend fun <T> guarded(
        turn: Turn,
        block: suspend () -> T,
    ): T? {
        val left = turn.budget.remaining(turn.startedAt, clock())
        if (left <= 0L || turn.stopRequested) return null
        return coroutineScope {
            val work = async { block() }
            val watch =
                launch {
                    turn.stopSignal.first { it }
                    work.cancel()
                }
            try {
                withTimeoutOrNull(left) { work.await() }
            } catch (e: CancellationException) {
                currentCoroutineContext().ensureActive()
                null
            } finally {
                watch.cancel()
                work.cancel()
            }
        }
    }

    private suspend fun FlowCollector<TurnUpdate>.emitProgress(
        turn: Turn,
        thinking: Boolean,
    ) {
        emit(TurnUpdate.Streaming(turn.reasoning.toString().trim(), "", thinking, turn.steps.toList(), clockOf(turn)))
    }

    /** The research clock for the live time display (Gründlich only, while still researching). */
    @Suppress("ReturnCount")
    private fun clockOf(turn: Turn): ResearchClock? {
        val start = turn.startedAt ?: return null
        if (!turn.budget.liveNotes || turn.finalRound) return null
        return ResearchClock(start, turn.budget.timeMs)
    }

    @Suppress("LongParameterList", "CyclomaticComplexMethod") // one generation needs all of these
    private suspend fun FlowCollector<TurnUpdate>.generateRound(
        system: String,
        roundTools: List<ToolSchema>,
        messages: List<ChatMessage>,
        history: List<ChatMessage>,
        thinking: Boolean,
        turn: Turn,
    ): RoundResult {
        val prompt =
            builder().build(
                system = system,
                tools = roundTools,
                history = messages,
                addGenerationPrompt = true,
                enableThinking = thinking,
            )
        val params = DevTuning.sampling(turn.dev, thinking, MAX_TOKENS, MAX_TOKENS_THINKING)
        val checkpoints = checkpointsFor(prompt, system, history, roundTools)
        val splitter = StreamSplitter(thinkingEnabled = thinking)
        val reasoning = StringBuilder()
        val text = StringBuilder()
        val start = clock()
        var thinkingEnd: Long? = null
        var reason = StopReason.ERROR
        var stats: GenerationStats? = null
        var pending: ToolStep? = null
        val earlier = turn.reasoning.toString()

        fun apply(deltas: List<UiDelta>) {
            deltas.forEach { d ->
                when (d) {
                    is UiDelta.Reasoning -> reasoning.append(d.text)
                    is UiDelta.Text -> {
                        if (thinking && thinkingEnd == null) thinkingEnd = clock()
                        text.append(d.text)
                    }
                    is UiDelta.ToolCallStarted -> {
                        if (thinking && thinkingEnd == null) thinkingEnd = clock()
                        val name = d.name ?: ""
                        pending = ToolStep(name, null, StepState.RUNNING, tools?.isOnline(name) == true)
                    }
                }
            }
        }

        // T-069: while research runs, Stop or the end of the research time ends a tool round early
        val mayCut = roundTools.isNotEmpty() && turn.researching
        llm
            .generate(prompt, params, checkpoints)
            .transformWhile { event ->
                emit(event)
                val outOfTime = mayCut && turn.budget.timeUp(turn.startedAt, clock())
                if (outOfTime && text.isEmpty()) turn.timeCut = true
                !(mayCut && text.isEmpty() && turn.cut)
            }.collect { event ->
                when (event) {
                    is GenerationEvent.Token -> {
                        apply(splitter.feed(event.text))
                        if (text.isNotEmpty()) turn.textStarted = true
                        val steps = turn.steps + listOfNotNull(pending)
                        val shown = (earlier + reasoning).trim()
                        emit(TurnUpdate.Streaming(shown, cleaned(text, turn), thinking, steps, clockOf(turn)))
                    }
                    is GenerationEvent.Done -> {
                        reason = event.reason
                        stats = event.stats
                    }
                }
            }
        if (mayCut && turn.cut && reason != StopReason.END) reason = StopReason.CANCELLED
        val (rest, parsed) = splitter.finish()
        apply(rest)
        val thoughtFor = if (thinking) (thinkingEnd ?: clock()) - start else null
        // A stopped answer keeps what was shown live: thinking stays thinking, never becomes the answer.
        val output =
            if (reason == StopReason.CANCELLED) {
                parsed.copy(
                    reasoning = reasoning.toString().ifBlank { null },
                    text = text.toString(),
                    toolCalls = emptyList(),
                )
            } else {
                parsed
            }
        return RoundResult(output, reason, thoughtFor, stats)
    }

    /**
     * The live text without fake citations (T-220): only markers for sources of this turn. With a
     * prepared phone action, "eingestellt" becomes "vorbereitet" (4b-2): nothing is done before the tap.
     */
    private fun cleaned(
        text: CharSequence,
        turn: Turn,
    ): String {
        val clean = CitationCleaner.clean(text.toString(), turn.sources.size)
        return if (turn.actions.isEmpty()) clean else ActionWording.clean(clean)
    }

    /** "12*7" → answered by the calculator right away (TOOLS.md §5). */
    @Suppress("ReturnCount")
    private fun quickAnswer(
        history: List<ChatMessage>,
        ctx: ToolContext,
    ): TurnUpdate.Finished? {
        if (tools == null) return null
        val last = history.lastOrNull()?.takeIf { it.role == Role.USER } ?: return null
        HainTools.clockAnswer(last.content, ctx.now, ctx.lang)?.let { clock ->
            return TurnUpdate.Finished(ParsedOutput(null, clock, emptyList()), StopReason.END, null, emptyList())
        }
        val answer = HainTools.quickAnswer(last.content, ctx.lang) ?: return null
        return TurnUpdate.Finished(
            ParsedOutput(null, answer, emptyList()),
            StopReason.END,
            null,
            listOf(ToolStep("calculate", answer, StepState.DONE, online = false)),
        )
    }

    private fun schemas(): List<ToolSchema> {
        if (ownModel()) return emptyList()
        val ctx = toolContext()
        return tools?.schemas(ctx.offline, ctx.webSearch).orEmpty()
    }

    /**
     * Short chat title (2d-2): asks the brain right after the history, thinking off, so the chat is
     * still in memory and the next turn continues from the checkpoint at the end of [history].
     * Returns `null` when nothing usable came back.
     */
    suspend fun title(history: List<ChatMessage>): String? {
        val system = DevTuning.systemPrompt(tuning(), systemPrompt(promptAnchor()))
        val ask = history + ChatMessage(Role.USER, TITLE_REQUEST)
        val tools = schemas()
        val prompt = builder().build(system, tools, ask, true, false)
        val historyEnd = builder().build(system, tools, history, false, false)
        val checkpoints =
            listOfNotNull(fixedSystemEnd(prompt, system), historyEnd.length.takeIf { prompt.startsWith(historyEnd) })
        val out = StringBuilder()
        var reason = StopReason.ERROR
        llm.generate(prompt, TITLE_PARAMS, checkpoints).collect { event ->
            when (event) {
                is GenerationEvent.Token -> out.append(event.text)
                is GenerationEvent.Done -> reason = event.reason
            }
        }
        if (reason != StopReason.END && reason != StopReason.MAX_TOKENS) return null
        return cleanTitle(out.toString())
    }

    /**
     * History plus the one-line language hint and, for a research, how to research (not stored,
     * after the cached part of the prompt).
     */
    private suspend fun withLanguageHint(
        history: List<ChatMessage>,
        lastUser: String,
        research: String? = null,
    ): List<ChatMessage> {
        val userMessages = history.filter { it.role == Role.USER }.map { it.content }.ifEmpty { listOf(lastUser) }
        val memory = memoryHint?.let { hint -> withContext(Dispatchers.Default) { hint(userMessages) } }
        val lines =
            listOfNotNull(turnHint(userMessages), memory)
                .filter { it.isNotBlank() }
                .joinToString("\n")
        val hint =
            listOfNotNull(lines.ifBlank { null }, research)
                .joinToString(" ")
                .ifBlank { null } ?: return history
        return history + ChatMessage(Role.SYSTEM, hint)
    }

    private fun checkpointsFor(
        prompt: String,
        system: String,
        history: List<ChatMessage>,
        tools: List<ToolSchema>,
    ): List<Int> {
        val build = { h: List<ChatMessage> -> builder().build(system, tools, h, false, false) }
        val beforeLastUser = history.dropLastWhile { it.role != Role.USER }.dropLast(1)
        return listOfNotNull(
            fixedSystemEnd(prompt, system),
            build(emptyList()).length,
            build(beforeLastUser).takeIf { beforeLastUser.isNotEmpty() && prompt.startsWith(it) }?.length,
            build(history).takeIf { prompt.startsWith(it) }?.length,
        ).filter { it in 1 until prompt.length }.distinct().sorted()
    }

    companion object {
        const val SEARCH_TOOL = "web_search"

        /** Timeline line for the pages read after a search; its detail is the number of pages. */
        const val READ_STEP = "read_pages"

        /** Timeline line for the research notes (Gründlich); its detail is "facts/sources". */
        const val NOTES_STEP = "notes"

        private const val LIMIT_HINT = "No more searches for this answer. Answer with what you found."
        private const val REPEAT_HINT =
            "You already ran this exact call above. Answer from that result, or try other words once."
        private const val CUT_HINT = "The research was ended. Answer with what you found so far."
        private const val OTHER_HITS = 3
        private const val COMPACT_CHARS = 400

        /** The words a page excerpt is picked for: the search words and the user's question. */
        private fun focusOf(
            call: ToolCall,
            turn: Turn,
        ): String = listOfNotNull(detailOf(call), turn.question).joinToString(" ")

        /**
         * A search result with the pages read from it, for the brain: the pages' best parts under
         * their source numbers, plus a few hits that were not read.
         */
        internal fun withPages(
            result: ToolResult,
            pages: List<ReadPage>,
            numbers: List<Int>,
        ): String {
            val root = runCatching { Json.parseToJsonElement(result.forModel).jsonObject }.getOrNull()
            val inner = (root?.get("result") as? JsonObject) ?: root
            val readUrls = pages.map { it.url }.toSet()
            val json =
                buildJsonObject {
                    (inner?.get("query") as? JsonPrimitive)?.let { put("query", it) }
                    ((root?.get("note") ?: inner?.get("note")) as? JsonPrimitive)?.let { put("note", it) }
                    val read = Json.parseToJsonElement(PageReader.forModel(pages, numbers)).jsonObject
                    put("pages", read.getValue("pages"))
                    val other = result.hits.filter { it.url !in readUrls }.take(OTHER_HITS)
                    if (other.isNotEmpty()) {
                        putJsonArray("other") {
                            other.forEach { h ->
                                addJsonObject {
                                    put("title", h.title)
                                    put("snippet", h.snippet)
                                }
                            }
                        }
                    }
                }
            return json.toString()
        }

        /** What an older search result shrinks to: its search words and its notes ("[2] …"). */
        private fun notesOf(
            result: ToolResult,
            numbers: List<Int>,
            turn: Turn,
        ): String {
            val lines = numbers.flatMap { turn.notes.lines(it) }.distinct()
            return buildJsonObject {
                result.label?.let { put("query", it) }
                putJsonArray("notes") { lines.forEach { add(JsonPrimitive(it)) } }
            }.toString()
        }

        private fun compactOf(content: String): String = PromptHistory.cut(content, COMPACT_CHARS)

        /** How to research in this mode, after the history; `null` = the system prompt is enough. */
        internal fun researchHint(
            mode: AnswerMode,
            lang: String,
        ): String? =
            when {
                mode != AnswerMode.THOROUGH -> null
                lang == "de" -> THOROUGH_DE
                else -> THOROUGH_EN
            }

        /** Last line before the answer once tools are over. */
        internal fun finalHint(lang: String): String = if (lang == "de") FINAL_DE else FINAL_EN

        private fun closingLine(lang: String) = ChatMessage(Role.SYSTEM, finalHint(lang))

        private const val THOROUGH_DE =
            "Gründliche Recherche: Du hast bis zu einer Minute. Suche mit mehreren Anfragen, " +
                "auch umformuliert und auf Englisch, und nutze verschiedene Seiten. Du darfst bis zu " +
                "3 Werkzeuge auf einmal aufrufen. Stütze Fakten auf mindestens 2 Quellen und nenne Widersprüche."
        private const val THOROUGH_EN =
            "Thorough research: you have up to one minute. Search with several queries, also reworded " +
                "and in German, and use different pages. You may call up to 3 tools at once. " +
                "Back facts with at least 2 sources and point out contradictions."
        private const val FINAL_DE =
            "Die Recherche ist beendet. Antworte jetzt nur aus den Ergebnissen oben, nenne die Quellen " +
                "als [1], [2] und sag, wenn Quellen sich widersprechen."
        private const val FINAL_EN =
            "The research is over. Answer now only from the results above, cite the sources as [1], [2] " +
                "and say when sources contradict each other."

        private val DETAIL_KEYS =
            listOf("place", "topic", "expression", "query", "url", "time", "title", "destination")

        /** A few words for the tool line while it runs: the place, topic or sum the brain asked for. */
        internal fun detailOf(call: ToolCall): String? =
            DETAIL_KEYS.firstNotNullOfOrNull { call.arguments[it]?.toString()?.takeIf { v -> v.isNotBlank() } }

        private const val MAX_TOKENS = 1024
        private const val MAX_TOKENS_THINKING = 4096
        private const val MAX_TITLE_WORDS = 6
        private const val MAX_TITLE_CHARS = 48

        /** The system prompt's time moves on at most every 30 minutes (one re-read per half hour). */
        const val ANCHOR_MS = 30 * 60 * 1000L

        /** Headings (EN / DE prompt) after which the per-day values (time, place) follow. */
        private val CONTEXT_HEADINGS = listOf("# Context", "# Kontext")
        private const val TITLE_REQUEST =
            "Give this chat a short title: at most 6 words, in the language of the chat. " +
                "Reply with the title only, no quotes, no full stop."
        private val TITLE_PARAMS = SamplingParams.CHAT.copy(maxTokens = 24, temperature = 0.1f)
        private val TITLE_TRIM = Regex("^[\\s\"'„“”«»*#:-]+|[\\s\"'„“”«»*.!:-]+$")

        /** End of the part of the system prompt that never changes (before the time and place). */
        internal fun fixedSystemEnd(
            prompt: String,
            system: String,
        ): Int? {
            val start = prompt.indexOf(system)
            val heading = CONTEXT_HEADINGS.map { system.indexOf(it) }.filter { it > 0 }.minOrNull() ?: -1
            return if (start < 0 || heading <= 0) null else start + heading
        }

        /** First line of the model's answer, without quotes or a label, at most 6 words. */
        internal fun cleanTitle(raw: String): String? {
            val line =
                raw
                    .substringAfter("</think>")
                    .lineSequence()
                    .map { it.trim() }
                    .firstOrNull { it.isNotEmpty() }
                    ?.removePrefix("Title:")
                    ?.removePrefix("Titel:")
                    ?.replace(TITLE_TRIM, "")
                    ?.trim()
                    .orEmpty()
            if (line.isEmpty()) return null
            val words = line.split(Regex("\\s+")).take(MAX_TITLE_WORDS).joinToString(" ")
            return words
                .take(MAX_TITLE_CHARS)
                .trim()
                .replaceFirstChar { it.uppercase() }
                .ifEmpty { null }
        }
    }
}
