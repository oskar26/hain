package app.hain

import app.hain.common.api.net.NetworkGate
import app.hain.feature.news.api.BrainCall
import app.hain.feature.news.api.NewsViewModel
import app.hain.feature.news.api.SummarizeArticle
import app.hain.llm.api.ChatMessage
import app.hain.llm.api.EngineState
import app.hain.llm.api.GenerationEvent
import app.hain.llm.api.LlmEngine
import app.hain.llm.api.Role
import app.hain.llm.api.SamplingParams
import app.hain.llm.api.StopReason
import app.hain.llm.internal.LfmPromptBuilder
import app.hain.tools.internal.news.NewsRepository

/**
 * T-074: one short call to the local brain for an article summary, thinking off. Loads the brain
 * when it is not loaded yet; `null` when there is no brain file ([modelPath] = `null`) or it cannot
 * load or answer.
 */
class NewsBrain(
    private val llm: LlmEngine,
    private val modelPath: () -> String?,
    private val contextSize: () -> Int,
) : BrainCall {
    override suspend fun complete(
        system: String,
        user: String,
    ): String? {
        val path = modelPath()
        val ready = path != null && (llm.state.value is EngineState.Ready || llm.load(path, contextSize()))
        if (!ready) return null
        val prompt =
            LfmPromptBuilder().build(
                system = system,
                tools = emptyList(),
                history = listOf(ChatMessage(Role.USER, user)),
                addGenerationPrompt = true,
                enableThinking = false,
            )
        val out = StringBuilder()
        var reason = StopReason.ERROR
        llm.generate(prompt, PARAMS).collect { event ->
            when (event) {
                is GenerationEvent.Token -> out.append(event.text)
                is GenerationEvent.Done -> reason = event.reason
            }
        }
        return out.toString().takeIf { reason == StopReason.END || reason == StopReason.MAX_TOKENS }
    }

    private companion object {
        /** Five short points; low temperature so it sticks to the article. */
        val PARAMS = SamplingParams(temperature = 0.1f, maxTokens = 400)
    }
}

/** What the news screen needs from the app (T-073). */
class NewsParts(
    val repository: NewsRepository,
    val gate: NetworkGate,
    val llm: LlmEngine,
)

/** The news screen's view model: feeds from [NewsParts.repository], summaries by the brain in [lang]. */
fun newsScreenModel(
    parts: NewsParts,
    modelPath: () -> String?,
    contextSize: () -> Int,
    lang: () -> String,
): NewsViewModel {
    val brain = NewsBrain(parts.llm, modelPath, contextSize)
    return NewsViewModel(parts.repository, SummarizeArticle(parts.gate, brain), lang)
}
