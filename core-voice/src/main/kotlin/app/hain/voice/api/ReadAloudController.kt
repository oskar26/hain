package app.hain.voice.api

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** What read aloud is doing; [key] names the answer being read (one at a time). */
sealed interface ReadAloudState {
    data object Idle : ReadAloudState

    data class Speaking(
        val key: String,
        val sentence: Int,
        val total: Int,
    ) : ReadAloudState

    /** The phone has no voice for this language (the system voice is missing or broken). */
    data class Failed(
        val key: String,
    ) : ReadAloudState
}

/**
 * Read aloud (T-141): cleans the Markdown, splits it into sentences and hands them to [synth] one
 * by one, always [AHEAD] sentences ahead, so the engine can prepare the next sentence while the
 * current one plays and the first words come quickly. Tapping the same answer again stops.
 */
class ReadAloudController(
    private val scope: CoroutineScope,
    private val synth: SpeechSynth,
) {
    private val mutableState = MutableStateFlow<ReadAloudState>(ReadAloudState.Idle)
    val state: StateFlow<ReadAloudState> = mutableState.asStateFlow()

    private var job: Job? = null

    @Volatile private var events = Channel<Pair<String, Boolean>>(Channel.UNLIMITED)

    init {
        synth.listener =
            object : SynthListener {
                override fun onDone(id: String) {
                    events.trySend(id to true)
                }

                override fun onError(id: String) {
                    events.trySend(id to false)
                }
            }
    }

    fun isSpeaking(key: String): Boolean = (state.value as? ReadAloudState.Speaking)?.key == key

    /** Reads [markdown] aloud, or stops when [key] is the answer being read right now. */
    fun toggle(
        key: String,
        markdown: String,
        language: String,
        labels: SpeechLabels,
    ) {
        if (isSpeaking(key)) {
            stop()
        } else {
            speak(key, markdown, language, labels)
        }
    }

    fun speak(
        key: String,
        markdown: String,
        language: String,
        labels: SpeechLabels,
    ) {
        stop()
        val sentences = SpeechText.speakable(markdown, labels)
        if (sentences.isEmpty()) return
        val channel = Channel<Pair<String, Boolean>>(Channel.UNLIMITED)
        events = channel
        mutableState.value = ReadAloudState.Speaking(key, 0, sentences.size)
        job =
            scope.launch {
                if (!synth.prepare(language)) {
                    mutableState.value = ReadAloudState.Failed(key)
                    return@launch
                }
                val prefix = "${key.hashCode()}-${System.nanoTime()}-"
                var queued = 0
                while (queued < sentences.size && queued < AHEAD) {
                    synth.enqueue(prefix + queued, sentences[queued])
                    queued++
                }
                var done = 0
                while (done < sentences.size) {
                    val (id, ok) = channel.receive()
                    if (!id.startsWith(prefix)) continue // a late event of an earlier answer
                    if (!ok) {
                        synth.stop()
                        mutableState.value = ReadAloudState.Failed(key)
                        return@launch
                    }
                    done++
                    if (queued < sentences.size) {
                        synth.enqueue(prefix + queued, sentences[queued])
                        queued++
                    }
                    if (done < sentences.size) mutableState.value = ReadAloudState.Speaking(key, done, sentences.size)
                }
                mutableState.value = ReadAloudState.Idle
            }
    }

    fun stop() {
        job?.cancel()
        job = null
        synth.stop()
        events.close()
        if (state.value !is ReadAloudState.Idle) mutableState.value = ReadAloudState.Idle
    }

    /** Clears a failure once the composer showed it. */
    fun consume() {
        if (state.value is ReadAloudState.Failed) mutableState.value = ReadAloudState.Idle
    }

    fun release() {
        stop()
        synth.release()
    }

    private companion object {
        const val AHEAD = 2
    }
}
