package app.hain

import app.hain.common.api.net.NetworkGate
import app.hain.feature.news.api.ArticleAsk
import app.hain.feature.news.api.ArticleReader
import app.hain.feature.news.api.BrainCall
import app.hain.feature.news.api.BriefingMaker
import app.hain.feature.news.api.BriefingStore
import app.hain.feature.news.api.NewsPrefsStore
import app.hain.feature.news.api.NewsViewModel
import app.hain.llm.api.ChatMessage
import app.hain.llm.api.EngineState
import app.hain.llm.api.GenerationEvent
import app.hain.llm.api.LlmEngine
import app.hain.llm.api.Role
import app.hain.llm.api.SamplingParams
import app.hain.llm.api.StopReason
import app.hain.tools.internal.news.NewsRepository

/**
 * One short call to the local brain for the news (T-078 questions about an article, T-079 briefing),
 * thinking off. Loads the brain when it is not loaded yet, unless [onlyWhenLoaded] (the briefing when
 * the news open: no 1 GB load just for that); `null` when there is no brain file ([modelPath] =
 * `null`) or it cannot load or answer.
 */
class NewsBrain(
    private val llm: LlmEngine,
    private val modelPath: () -> String?,
    private val contextSize: () -> Int,
    private val onlyWhenLoaded: Boolean = false,
) : BrainCall {
    @Suppress("ReturnCount")
    override suspend fun complete(
        system: String,
        user: String,
    ): String? {
        val path = modelPath()
        val loaded = llm.state.value is EngineState.Ready
        if (onlyWhenLoaded && !loaded) return null
        val ready = path != null && (loaded || llm.load(path, contextSize()))
        if (!ready) return null
        val prompt =
            // T-241: the loaded brain's own format (LFM or Qwen)
            llm.format.promptBuilder().build(
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
                is GenerationEvent.Reading -> Unit
            }
        }
        return out.toString().takeIf { reason == StopReason.END || reason == StopReason.MAX_TOKENS }
    }

    private companion object {
        /** Short answers; low temperature so it sticks to the article. */
        val PARAMS = SamplingParams(temperature = 0.1f, maxTokens = 500)
    }
}

/** What the news screen needs from the app (T-073, T-075–T-079). */
class NewsParts(
    val repository: NewsRepository,
    val gate: NetworkGate,
    val llm: LlmEngine,
    val prefs: NewsPrefsStore,
    val briefings: BriefingStore,
)

/**
 * The news screen's view model: feeds from [NewsParts.repository], the reading view through the
 * gate, questions to Pim (article cut to the brain's context) and the daily briefing, in [lang].
 */
fun newsScreenModel(
    parts: NewsParts,
    modelPath: () -> String?,
    contextSize: () -> Int,
    lang: () -> String,
): NewsViewModel {
    val brain = NewsBrain(parts.llm, modelPath, contextSize)
    val quick = NewsBrain(parts.llm, modelPath, contextSize, onlyWhenLoaded = true)
    return NewsViewModel(
        repository = parts.repository,
        reader = ArticleReader(parts.gate),
        asker = ArticleAsk(brain, contextSize),
        briefings = BriefingMaker(parts.briefings, brain = brain, quickBrain = quick),
        prefsStore = parts.prefs,
        lang = lang,
    )
}
