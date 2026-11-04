package app.hain.llm.internal

import android.content.Context
import android.util.Log
import app.hain.llm.api.TextEmbedder
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * [TextEmbedder] on llama.cpp's embedding mode (T-110). Its own thread and its own small context
 * ([CONTEXT] tokens, LFM2.5-Embedding reads up to 512), so it never waits for the brain. Measured on
 * the host (2 cores, LFM2.5-Embedding-350M Q4_0, 04.11.): ~45 ms per short fact, 1024 values.
 * The model (~220 MB, mmap) is freed after [idleMs] without use.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class LlamaEmbedder(
    private val context: Context,
    private val idleMs: Long = IDLE_MS,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO.limitedParallelism(1),
) : TextEmbedder {
    private val scope = CoroutineScope(SupervisorJob() + dispatcher)

    // only touched on [dispatcher]
    private var handle = 0L
    private var loadedPath: String? = null
    private var idle: Job? = null

    override val isAvailable: Boolean get() = NativeLlm.loadError == null

    override suspend fun embed(
        modelPath: String,
        text: String,
    ): FloatArray? =
        withContext(dispatcher) {
            if (text.isBlank() || !isAvailable) return@withContext null
            idle?.cancel()
            if (handle == 0L || loadedPath != modelPath) {
                free()
                NativeLlm.nativeInit(context.applicationInfo.nativeLibraryDir)
                handle = NativeLlm.nativeEmbedLoad(modelPath, CONTEXT, THREADS)
                loadedPath = if (handle != 0L) modelPath else null
                if (handle == 0L) Log.w(TAG, "embedder could not load $modelPath")
            }
            val vector = if (handle != 0L) NativeLlm.nativeEmbed(handle, text.encodeToByteArray()) else null
            idle =
                scope.launch {
                    delay(idleMs)
                    free()
                }
            vector
        }

    override suspend fun unload() = withContext(dispatcher) { free() }

    private fun free() {
        if (handle != 0L) {
            NativeLlm.nativeEmbedFree(handle)
            handle = 0L
            loadedPath = null
        }
    }

    private companion object {
        const val TAG = "HainLlm"
        const val CONTEXT = 512
        const val THREADS = 2
        const val IDLE_MS = 60_000L
    }
}
