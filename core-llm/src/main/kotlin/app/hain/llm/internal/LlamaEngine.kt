package app.hain.llm.internal

import android.content.Context
import app.hain.llm.api.EngineState
import app.hain.llm.api.GenerationEvent
import app.hain.llm.api.LlmEngine
import app.hain.llm.api.SamplingParams
import app.hain.llm.api.StopReason
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

/** [LlmEngine] on top of llama.cpp via [NativeLlm] (T-032). All native calls run on one thread. */
@OptIn(ExperimentalCoroutinesApi::class)
class LlamaEngine(
    private val context: Context,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO.limitedParallelism(1),
) : LlmEngine {
    private val mutableState = MutableStateFlow<EngineState>(EngineState.Unloaded)
    override val state: StateFlow<EngineState> = mutableState.asStateFlow()

    @Volatile private var handle = 0L

    @Volatile private var cancelRequested = false
    private var initialized = false

    override val isAvailable: Boolean get() = NativeLlm.loadError == null

    override suspend fun load(
        path: String,
        contextSize: Int,
    ): Boolean =
        withContext(dispatcher) {
            val current = mutableState.value
            if (current is EngineState.Ready && current.path == path && handle != 0L) return@withContext true
            NativeLlm.loadError?.let {
                mutableState.value = EngineState.Failed(it.message ?: "native library missing")
                return@withContext false
            }
            if (!initialized) {
                NativeLlm.nativeInit(context.applicationInfo.nativeLibraryDir)
                initialized = true
            }
            freeHandle()
            mutableState.value = EngineState.Loading(path)
            val newHandle = NativeLlm.nativeLoad(path, contextSize, threadCount(), 0)
            handle = newHandle
            mutableState.value = if (newHandle != 0L) EngineState.Ready(path) else EngineState.Failed("load failed")
            newHandle != 0L
        }

    override fun generate(
        prompt: String,
        params: SamplingParams,
    ): Flow<GenerationEvent> =
        callbackFlow {
            val h = handle
            if (h == 0L) {
                trySend(GenerationEvent.Done(StopReason.ERROR))
                close()
                return@callbackFlow
            }
            cancelRequested = false
            val code =
                NativeLlm.nativeGenerate(h, prompt.encodeToByteArray(), params.toArray(), null) { bytes ->
                    trySend(GenerationEvent.Token(bytes.decodeToString()))
                    !cancelRequested && !isClosedForSend
                }
            trySend(GenerationEvent.Done(code.toStopReason()))
            close()
            awaitClose { cancelRequested = true }
        }.flowOn(dispatcher)

    override fun cancel() {
        cancelRequested = true
        val h = handle
        if (h != 0L) NativeLlm.nativeCancel(h)
    }

    override suspend fun unload() =
        withContext(dispatcher) {
            freeHandle()
            mutableState.value = EngineState.Unloaded
        }

    private fun freeHandle() {
        if (handle != 0L) {
            NativeLlm.nativeFree(handle)
            handle = 0L
        }
    }

    private fun threadCount(): Int = (Runtime.getRuntime().availableProcessors() - 2).coerceIn(2, MAX_THREADS)

    private fun Int.toStopReason(): StopReason =
        when (this) {
            NativeLlm.STOP_EOS -> StopReason.END
            NativeLlm.STOP_MAX_TOKENS -> StopReason.MAX_TOKENS
            NativeLlm.STOP_CANCELLED -> StopReason.CANCELLED
            NativeLlm.STOP_CONTEXT_FULL -> StopReason.CONTEXT_FULL
            else -> StopReason.ERROR
        }

    private companion object {
        const val MAX_THREADS = 6
    }
}
