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

    data class Done(
        val reason: StopReason,
    ) : GenerationEvent
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

/** Runs the local brain (T-032). One model loaded at a time; one generation at a time. */
interface LlmEngine {
    val state: StateFlow<EngineState>

    /** True when the native library is part of this build. */
    val isAvailable: Boolean

    suspend fun load(
        path: String,
        contextSize: Int = 8192,
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

    suspend fun unload()
}
