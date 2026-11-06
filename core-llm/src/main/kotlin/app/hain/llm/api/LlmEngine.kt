package app.hain.llm.api

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/** Sampling settings for one generation (LLM.md §6). */
data class SamplingParams(
    val temperature: Float = 0.1f,
    val topK: Int = 50,
    val topP: Float = 1.0f,
    val minP: Float = 0.0f,
    val repeatPenalty: Float = 1.1f,
    val repeatLastN: Int = 64,
    val maxTokens: Int = 1024,
    val seed: Int = -1,
    /** GBNF grammar the answer must follow (rule `root`), e.g. JSON for the memory (T-114); `null` = free text. */
    val grammar: String? = null,
) {
    fun toArray(): FloatArray =
        floatArrayOf(
            temperature,
            topK.toFloat(),
            topP,
            minP,
            repeatPenalty,
            repeatLastN.toFloat(),
            maxTokens.toFloat(),
            if (seed < 0) DEFAULT_SEED else seed.toFloat(),
        )

    companion object {
        /** LLAMA_DEFAULT_SEED (0xFFFFFFFF) as float → random seed in native code. */
        private const val DEFAULT_SEED = 4.2949673E9f

        val CHAT = SamplingParams()
        val CREATIVE = SamplingParams(temperature = 0.7f, topP = 0.95f, minP = 0.05f)
    }
}

/** Why a generation ended. */
enum class StopReason { END, MAX_TOKENS, CANCELLED, CONTEXT_FULL, ERROR }

/** One piece of a running generation. */
sealed interface GenerationEvent {
    data class Token(
        val text: String,
    ) : GenerationEvent

    /** T-294: [done] of [total] prompt tokens read so far (before the first token; only when there is reading). */
    data class Reading(
        val done: Int,
        val total: Int,
    ) : GenerationEvent {
        val fraction: Float get() = if (total > 0) (done.toFloat() / total).coerceIn(0f, 1f) else 1f
    }

    data class Done(
        val reason: StopReason,
        /** Speed of this generation (T-211, developer mode); `null` when the engine does not know. */
        val stats: GenerationStats? = null,
    ) : GenerationEvent
}

/** How fast one generation was (T-211): read prompt, first token, tokens per second. */
data class GenerationStats(
    val promptTokens: Int,
    val reusedTokens: Int,
    val prefillMs: Long,
    val genTokens: Int,
    val genMs: Long,
    /** From the request to the first streamed token, in ms. */
    val firstTokenMs: Long,
) {
    val tokensPerSecond: Float get() = if (genMs > 0) genTokens * MS_PER_S / genMs else 0f

    private companion object {
        const val MS_PER_S = 1000f
    }
}

sealed interface EngineState {
    data object Unloaded : EngineState

    /** [progress] runs 0→1 while the file is read; below 0 when unknown. */
    data class Loading(
        val path: String,
        val progress: Float = -1f,
    ) : EngineState

    data class Ready(
        val path: String,
    ) : EngineState

    data class Failed(
        val message: String,
    ) : EngineState
}

/**
 * Context window the brain is loaded with (T-088). 8192, not the 2048 of the 01.11. benchmark, on
 * purpose: chats are not shortened yet and a "Gründlich" answer with pages and thinking needs up to
 * ~3000 tokens, so 2048 would end long chats with "context full". Host measurement 03.11.
 * (LFM3-1.7B-QAD-Q4_0, same 978-token prompt): time to the first token and t/s were equal for
 * 2048 / 4096 / 8192 within noise (14.5 / 14.6 / 14.2 s cold on 2 host cores, 0.83 / 0.87 / 0.88 s
 * warm); only memory grows (peak 2245 / 2294 / 2390 MB). The slowness came from re-reading the prompt.
 */
const val CONTEXT_SIZE = 8192

/** Runs the local brain (T-032). One model loaded at a time; one generation at a time. */
interface LlmEngine {
    val state: StateFlow<EngineState>

    /** True when the native library is part of this build. */
    val isAvailable: Boolean

    suspend fun load(
        path: String,
        contextSize: Int = CONTEXT_SIZE,
    ): Boolean

    /**
     * Streams the answer to a fully templated [prompt]. Cancelling the flow stops generation.
     * [checkpoints] are character offsets into [prompt] (prefixes the next turn starts with, e.g.
     * the end of the system prompt or of the history); the engine keeps its memory there so the
     * next turn does not read the whole chat again.
     */
    fun generate(
        prompt: String,
        params: SamplingParams = SamplingParams.CHAT,
        checkpoints: List<Int> = emptyList(),
    ): Flow<GenerationEvent>

    /** Reads [prompt] (e.g. the system prompt) into memory ahead of time. `false` if not possible. */
    suspend fun prefill(
        prompt: String,
        checkpoints: List<Int> = emptyList(),
    ): Boolean = false

    /** Stops the running generation (or prefill) as soon as possible, even inside a long prompt. */
    fun cancel()

    /** T-241: chat format of the loaded brain (template, sampling, thinking); [BrainFormat.LFM] when none is loaded. */
    val format: BrainFormat get() = BrainFormat.LFM

    /** Chat template stored in the loaded GGUF (`tokenizer.chat_template`), or `null` (T-211). */
    fun chatTemplate(): String? = null

    /**
     * Formats [messages] (role → text, roles "system" / "user" / "assistant") with the loaded model's
     * own chat template via llama.cpp (T-211, own models in developer mode). `null` when the model
     * has no template llama.cpp knows.
     */
    fun applyTemplate(
        messages: List<Pair<String, String>>,
        addAssistant: Boolean,
    ): String? = null

    suspend fun unload()
}
