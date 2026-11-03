package app.hain.feature.chat.internal

import app.hain.llm.api.EngineState
import app.hain.llm.api.GenerationEvent
import app.hain.llm.api.LlmEngine
import app.hain.llm.api.SamplingParams
import app.hain.llm.api.StopReason
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow

/** Scripted brain for tests: each generate() takes the next reply; `null` chunks = never ends. */
internal class FakeLlm(
    vararg replies: List<String>?,
    private val end: StopReason = StopReason.END,
) : LlmEngine {
    private val queue = ArrayDeque(replies.toList())
    val prompts = mutableListOf<String>()
    val checkpoints = mutableListOf<List<Int>>()
    val prefills = mutableListOf<String>()
    var lastParams: SamplingParams? = null
    var cancels = 0
    var loads = 0
    override val state: StateFlow<EngineState> = MutableStateFlow(EngineState.Ready("x"))
    override val isAvailable = true

    val lastPrompt: String get() = prompts.lastOrNull().orEmpty()

    override suspend fun load(
        path: String,
        contextSize: Int,
    ): Boolean {
        loads++
        return true
    }

    override suspend fun prefill(
        prompt: String,
        checkpoints: List<Int>,
    ): Boolean {
        prefills += prompt
        return true
    }

    override fun generate(
        prompt: String,
        params: SamplingParams,
        checkpoints: List<Int>,
    ): Flow<GenerationEvent> {
        prompts += prompt
        this.checkpoints += checkpoints
        lastParams = params
        val chunks = if (queue.isEmpty()) listOf("ok") else queue.removeFirst()
        return flow {
            if (chunks == null) {
                emit(GenerationEvent.Token("Ich denke"))
                awaitCancellation()
            }
            chunks.forEach { emit(GenerationEvent.Token(it)) }
            emit(GenerationEvent.Done(end))
        }
    }

    override fun cancel() {
        cancels++
    }

    override suspend fun unload() = Unit
}
