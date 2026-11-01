package app.hain.feature.chat.internal

import app.hain.llm.api.ChatMessage
import app.hain.llm.api.GenerationEvent
import app.hain.llm.api.LlmEngine
import app.hain.llm.api.ParsedOutput
import app.hain.llm.api.PromptBuilder
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
 */
class ChatEngine(
    private val llm: LlmEngine,
    private val systemPrompt: () -> String,
    private val promptBuilder: PromptBuilder = LfmPromptBuilder(),
    private val clock: () -> Long = System::currentTimeMillis,
) {
    fun respond(
        history: List<ChatMessage>,
        mode: AnswerMode = AnswerMode.AUTO,
    ): Flow<TurnUpdate> =
        flow {
            val lastUser = history.lastOrNull()?.content.orEmpty()
            val thinking = ThinkingPolicy.decide(lastUser, mode)
            val prompt =
                promptBuilder.build(
                    system = systemPrompt(),
                    tools = emptyList(),
                    history = history,
                    addGenerationPrompt = true,
                    enableThinking = thinking,
                )
            val params = SamplingParams.CHAT.copy(maxTokens = if (thinking) MAX_TOKENS_THINKING else MAX_TOKENS)
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

            llm.generate(prompt, params).collect { event ->
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
            emit(TurnUpdate.Finished(parsed, reason, thoughtFor))
        }

    private companion object {
        const val MAX_TOKENS = 1024
        const val MAX_TOKENS_THINKING = 4096
    }
}
