package app.hain.feature.chat.internal

import app.hain.llm.api.ChatMessage
import app.hain.llm.api.GenerationEvent
import app.hain.llm.api.LlmEngine
import app.hain.llm.api.ParsedOutput
import app.hain.llm.api.PromptBuilder
import app.hain.llm.api.Role
import app.hain.llm.api.SamplingParams
import app.hain.llm.api.StopReason
import app.hain.llm.api.UiDelta
import app.hain.llm.internal.AnswerMode
import app.hain.llm.internal.LfmPromptBuilder
import app.hain.llm.internal.StreamSplitter
import app.hain.llm.internal.ThinkingPolicy
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/** What the UI sees while one answer is produced. Text fields hold everything so far. */
sealed interface TurnUpdate {
    data class Streaming(
        val reasoning: String,
        val text: String,
        val thinking: Boolean,
    ) : TurnUpdate

    data class Finished(
        val output: ParsedOutput,
        val reason: StopReason,
        val thinkingMillis: Long?,
    ) : TurnUpdate
}

/**
 * One user turn without tools (T-050, MVP slice): system prompt + history → prompt → stream →
 * [StreamSplitter] for the live view → strict parse at the end. The tool loop comes with T-062.
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
class ChatEngine(
    private val llm: LlmEngine,
    private val systemPrompt: (atMillis: Long) -> String,
    private val promptBuilder: PromptBuilder = LfmPromptBuilder(),
    private val clock: () -> Long = System::currentTimeMillis,
    private val answerMode: () -> AnswerMode = { AnswerMode.AUTO },
    private val turnHint: (userMessages: List<String>) -> String? = { null },
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
        val prompt = promptBuilder.build(system, emptyList(), emptyList(), false, false)
        return llm.prefill(prompt, listOfNotNull(fixedSystemEnd(prompt, system)))
    }

    fun respond(
        history: List<ChatMessage>,
        mode: AnswerMode = answerMode(),
    ): Flow<TurnUpdate> =
        flow {
            val lastUser = history.lastOrNull()?.content.orEmpty()
            val thinking = ThinkingPolicy.decide(lastUser, mode)
            val system = systemPrompt(promptAnchor())
            val prompt =
                promptBuilder.build(
                    system = system,
                    tools = emptyList(),
                    history = withLanguageHint(history, lastUser),
                    addGenerationPrompt = true,
                    enableThinking = thinking,
                )
            val params = SamplingParams.CHAT.copy(maxTokens = if (thinking) MAX_TOKENS_THINKING else MAX_TOKENS)
            val checkpoints = checkpointsFor(prompt, system, history)
            val splitter = StreamSplitter(thinkingEnabled = thinking)
            val reasoning = StringBuilder()
            val text = StringBuilder()
            val start = clock()
            var thinkingEnd: Long? = null
            var reason = StopReason.ERROR

            fun apply(deltas: List<UiDelta>) {
                deltas.forEach { d ->
                    when (d) {
                        is UiDelta.Reasoning -> reasoning.append(d.text)
                        is UiDelta.Text -> {
                            if (thinking && thinkingEnd == null) thinkingEnd = clock()
                            text.append(d.text)
                        }
                        is UiDelta.ToolCallStarted -> Unit
                    }
                }
            }

            llm.generate(prompt, params, checkpoints).collect { event ->
                when (event) {
                    is GenerationEvent.Token -> {
                        apply(splitter.feed(event.text))
                        emit(TurnUpdate.Streaming(reasoning.toString(), text.toString(), thinking))
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
                    parsed.copy(reasoning = reasoning.toString().ifBlank { null }, text = text.toString())
                } else {
                    parsed
                }
            emit(TurnUpdate.Finished(output, reason, thoughtFor))
        }

    /**
     * Short chat title (2d-2): asks the brain right after the history, thinking off, so the chat is
     * still in memory and the next turn continues from the checkpoint at the end of [history].
     * Returns `null` when nothing usable came back.
     */
    suspend fun title(history: List<ChatMessage>): String? {
        val system = systemPrompt(promptAnchor())
        val ask = history + ChatMessage(Role.USER, TITLE_REQUEST)
        val prompt = promptBuilder.build(system, emptyList(), ask, true, false)
        val historyEnd = promptBuilder.build(system, emptyList(), history, false, false)
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
    ): List<Int> {
        val build = { h: List<ChatMessage> -> promptBuilder.build(system, emptyList(), h, false, false) }
        val beforeLastUser = history.dropLastWhile { it.role != Role.USER }.dropLast(1)
        return listOfNotNull(
            fixedSystemEnd(prompt, system),
            build(emptyList()).length,
            build(beforeLastUser).takeIf { beforeLastUser.isNotEmpty() && prompt.startsWith(it) }?.length,
            build(history).takeIf { prompt.startsWith(it) }?.length,
        ).filter { it in 1 until prompt.length }.distinct().sorted()
    }

    companion object {
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
