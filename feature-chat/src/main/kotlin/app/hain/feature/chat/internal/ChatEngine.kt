package app.hain.feature.chat.internal

import app.hain.llm.api.ChatMessage
import app.hain.llm.api.GenerationEvent
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
import app.hain.llm.internal.LfmPromptBuilder
import app.hain.llm.internal.StreamSplitter
import app.hain.llm.internal.ThinkingPolicy
import app.hain.tools.api.HainTools
import app.hain.tools.api.ToolContext
import app.hain.tools.api.ToolResult
import app.hain.tools.api.ToolRouter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.flow
import java.time.ZonedDateTime

/** What the UI sees while one answer is produced. Text fields hold everything so far. */
sealed interface TurnUpdate {
    data class Streaming(
        val reasoning: String,
        val text: String,
        val thinking: Boolean,
        val steps: List<ToolStep> = emptyList(),
    ) : TurnUpdate

    data class Finished(
        val output: ParsedOutput,
        val reason: StopReason,
        val thinkingMillis: Long?,
        val steps: List<ToolStep> = emptyList(),
        val sources: List<SourceRef> = emptyList(),
        val trace: List<ToolTrace> = emptyList(),
    ) : TurnUpdate
}

/**
 * One user turn: system prompt + history → prompt → stream → [StreamSplitter] for the live view →
 * strict parse at the end. With [tools] the brain may call tools (T-062, LLM.md §8): at most
 * [MAX_TOOL_ROUNDS] rounds, then one last round without tools so it has to answer. A message that is
 * only a sum ("12*7") is answered by the calculator at once, without the brain.
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
) {
    private var anchor = 0L

    /** Time the system prompt is filled with; moves forward at most every [ANCHOR_MS]. */
    internal fun promptAnchor(): Long {
        val now = clock()
        if (anchor == 0L || now - anchor >= ANCHOR_MS || now < anchor) anchor = now
        return anchor
    }

    /** Reads the system prompt into the brain's memory, so the first answer starts sooner. */
    suspend fun warmUp(): Boolean {
        val system = systemPrompt(promptAnchor())
        val prompt = promptBuilder.build(system, schemas(), emptyList(), false, false)
        return llm.prefill(prompt, listOfNotNull(fixedSystemEnd(prompt, system)))
    }

    fun respond(
        history: List<ChatMessage>,
        mode: AnswerMode = answerMode(),
    ): Flow<TurnUpdate> =
        flow {
            val lastUser = history.lastOrNull()?.content.orEmpty()
            val fast = mode == AnswerMode.FAST
            val ctx = toolContext().let { if (fast) it.copy(pagesToRead = ToolContext.FAST_PAGES) else it }
            val quick = quickAnswer(history, ctx)
            if (quick != null) {
                emit(quick)
                return@flow
            }
            val thinking = ThinkingPolicy.decide(lastUser, mode)
            val system = systemPrompt(promptAnchor())
            val schemas = tools?.schemas(ctx.offline).orEmpty()
            val base = withLanguageHint(history, lastUser)
            val turn = Turn()
            var round = 0
            while (true) {
                val roundTools = if (round < MAX_TOOL_ROUNDS) schemas else emptyList()
                val result = generateRound(system, roundTools, base + turn.messages, history, thinking, turn)
                turn.thinkingMs += result.thinkingMs ?: 0L
                val calls = result.output.toolCalls.take(MAX_CALLS_PER_ROUND)
                val router = tools
                val answered = calls.isEmpty() || roundTools.isEmpty() || result.reason != StopReason.END
                if (router == null || answered) {
                    val earlier = turn.reasoning.toString().trim()
                    val reasoning =
                        listOfNotNull(earlier.ifBlank { null }, result.output.reasoning?.ifBlank { null })
                            .joinToString("\n\n")
                            .ifBlank { null }
                    emit(
                        TurnUpdate.Finished(
                            result.output.copy(reasoning = reasoning),
                            result.reason,
                            if (thinking) turn.thinkingMs else null,
                            turn.steps.toList(),
                            turn.sources.toList(),
                            turn.trace.toList(),
                        ),
                    )
                    return@flow
                }
                result.output.reasoning?.let { turn.reasoning.append(it).append("\n\n") }
                turn.messages += ChatMessage(Role.ASSISTANT, result.output.text, calls, result.output.reasoning)
                calls.forEach { call -> runTool(router, call, ctx, turn, thinking, mode == AnswerMode.FAST) }
                round++
            }
        }

    /** What one user turn collected so far across tool rounds. */
    private class Turn {
        val messages = mutableListOf<ChatMessage>()
        val steps = mutableListOf<ToolStep>()
        val sources = mutableListOf<SourceRef>()
        val trace = mutableListOf<ToolTrace>()
        val reasoning = StringBuilder()
        var thinkingMs = 0L
        var searches = 0
    }

    private data class RoundResult(
        val output: ParsedOutput,
        val reason: StopReason,
        val thinkingMs: Long?,
    )

    @Suppress("LongParameterList") // one tool call needs the whole turn
    private suspend fun FlowCollector<TurnUpdate>.runTool(
        router: ToolRouter,
        call: ToolCall,
        ctx: ToolContext,
        turn: Turn,
        thinking: Boolean,
        fast: Boolean,
    ) {
        val step = ToolStep(call.name, detailOf(call), StepState.RUNNING, router.isOnline(call.name))
        turn.steps += step
        emit(TurnUpdate.Streaming(turn.reasoning.toString().trim(), "", thinking, turn.steps.toList()))
        val isSearch = call.name == SEARCH_TOOL
        val result =
            if (isSearch && turn.searches >= maxSearches(fast)) {
                ToolRouter.failure("limit", "No more searches for this answer. Answer with what you found.")
            } else {
                router.run(call, ctx)
            }
        if (isSearch) turn.searches++
        turn.steps[turn.steps.lastIndex] =
            step.copy(
                detail = result.label ?: step.detail,
                state = if (result.error == null) StepState.DONE else StepState.FAILED,
                error = result.error,
            )
        val pages = readPages(router, result, ctx, turn, thinking)
        (pages?.sources ?: result.sources).forEach { s ->
            if (turn.sources.none { it.url == s.url }) turn.sources += SourceRef(s.title, s.url)
        }
        val content = if (pages == null) result.forModel else combine(result.forModel, pages.forModel)
        turn.messages += ChatMessage(Role.TOOL, content, toolCallId = call.id)
        turn.trace += ToolTrace(call.name, call.arguments.mapValues { it.value?.toString().orEmpty() }, content)
        emit(TurnUpdate.Streaming(turn.reasoning.toString().trim(), "", thinking, turn.steps.toList()))
    }

    /** After a web search: read the best pages at once, shown as its own line ("Lese 3 Seiten"). */
    private suspend fun FlowCollector<TurnUpdate>.readPages(
        router: ToolRouter,
        result: ToolResult,
        ctx: ToolContext,
        turn: Turn,
        thinking: Boolean,
    ): ToolResult? {
        if (result.error != null || result.readUrls.isEmpty()) return null
        val step = ToolStep(READ_STEP, result.readUrls.size.toString(), StepState.RUNNING, online = true)
        turn.steps += step
        emit(TurnUpdate.Streaming(turn.reasoning.toString().trim(), "", thinking, turn.steps.toList()))
        val pages = router.readPages(result.readUrls, ctx)
        turn.steps[turn.steps.lastIndex] =
            if (pages == null) {
                step.copy(state = StepState.FAILED, error = "failed")
            } else {
                step.copy(detail = pages.label, state = StepState.DONE)
            }
        return pages
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
            promptBuilder.build(
                system = system,
                tools = roundTools,
                history = messages,
                addGenerationPrompt = true,
                enableThinking = thinking,
            )
        val params = SamplingParams.CHAT.copy(maxTokens = if (thinking) MAX_TOKENS_THINKING else MAX_TOKENS)
        val checkpoints = checkpointsFor(prompt, system, history, roundTools)
        val splitter = StreamSplitter(thinkingEnabled = thinking)
        val reasoning = StringBuilder()
        val text = StringBuilder()
        val start = clock()
        var thinkingEnd: Long? = null
        var reason = StopReason.ERROR
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

        llm.generate(prompt, params, checkpoints).collect { event ->
            when (event) {
                is GenerationEvent.Token -> {
                    apply(splitter.feed(event.text))
                    val steps = turn.steps + listOfNotNull(pending)
                    emit(TurnUpdate.Streaming((earlier + reasoning).trim(), text.toString(), thinking, steps))
                }
                is GenerationEvent.Done -> reason = event.reason
            }
        }
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
        return RoundResult(output, reason, thoughtFor)
    }

    /** "12*7" → answered by the calculator right away (TOOLS.md §5). */
    @Suppress("ReturnCount")
    private fun quickAnswer(
        history: List<ChatMessage>,
        ctx: ToolContext,
    ): TurnUpdate.Finished? {
        if (tools == null) return null
        val last = history.lastOrNull()?.takeIf { it.role == Role.USER } ?: return null
        val answer = HainTools.quickAnswer(last.content, ctx.lang) ?: return null
        return TurnUpdate.Finished(
            ParsedOutput(null, answer, emptyList()),
            StopReason.END,
            null,
            listOf(ToolStep("calculate", answer, StepState.DONE, online = false)),
        )
    }

    private fun schemas(): List<ToolSchema> = tools?.schemas(toolContext().offline).orEmpty()

    /**
     * Short chat title (2d-2): asks the brain right after the history, thinking off, so the chat is
     * still in memory and the next turn continues from the checkpoint at the end of [history].
     * Returns `null` when nothing usable came back.
     */
    suspend fun title(history: List<ChatMessage>): String? {
        val system = systemPrompt(promptAnchor())
        val ask = history + ChatMessage(Role.USER, TITLE_REQUEST)
        val tools = schemas()
        val prompt = promptBuilder.build(system, tools, ask, true, false)
        val historyEnd = promptBuilder.build(system, tools, history, false, false)
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

    /** History plus the one-line language hint (not stored, after the cached part of the prompt). */
    private fun withLanguageHint(
        history: List<ChatMessage>,
        lastUser: String,
    ): List<ChatMessage> {
        val userMessages = history.filter { it.role == Role.USER }.map { it.content }.ifEmpty { listOf(lastUser) }
        val hint = turnHint(userMessages)?.takeIf { it.isNotBlank() } ?: return history
        return history + ChatMessage(Role.SYSTEM, hint)
    }

    private fun checkpointsFor(
        prompt: String,
        system: String,
        history: List<ChatMessage>,
        tools: List<ToolSchema>,
    ): List<Int> {
        val build = { h: List<ChatMessage> -> promptBuilder.build(system, tools, h, false, false) }
        val beforeLastUser = history.dropLastWhile { it.role != Role.USER }.dropLast(1)
        return listOfNotNull(
            fixedSystemEnd(prompt, system),
            build(emptyList()).length,
            build(beforeLastUser).takeIf { beforeLastUser.isNotEmpty() && prompt.startsWith(it) }?.length,
            build(history).takeIf { prompt.startsWith(it) }?.length,
        ).filter { it in 1 until prompt.length }.distinct().sorted()
    }

    companion object {
        /**
         * Tool rounds per user turn before the brain must answer (LLM.md §8). Four, so a research
         * can search, read, search again and still look something up (Werkzeuge II).
         */
        const val MAX_TOOL_ROUNDS = 4

        /** Web searches per answer; the "Schnell" mode allows one (with one page read). */
        private const val MAX_SEARCHES = 2
        const val SEARCH_TOOL = "web_search"

        /** Timeline line for the pages read after a search; its detail is the number of pages. */
        const val READ_STEP = "read_pages"

        private fun maxSearches(fast: Boolean) = if (fast) 1 else MAX_SEARCHES

        /** Search hits and the pages read from them as one tool answer. */
        internal fun combine(
            search: String,
            pages: String,
        ): String = "{\"search\":$search,\"read\":$pages}"

        private const val MAX_CALLS_PER_ROUND = 3
        private val DETAIL_KEYS = listOf("place", "topic", "expression", "query", "url")

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
