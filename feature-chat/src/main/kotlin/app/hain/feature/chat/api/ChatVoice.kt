package app.hain.feature.chat.api

import android.content.Context
import android.util.Log
import androidx.compose.runtime.staticCompositionLocalOf
import app.hain.common.api.net.GateRequest
import app.hain.common.api.net.NetworkGate
import app.hain.common.api.net.Purpose
import app.hain.llm.internal.ReplyLanguagePolicy
import app.hain.models.api.DownloadState
import app.hain.models.api.ModelInfo
import app.hain.models.api.ModelRepository
import app.hain.voice.api.DictationController
import app.hain.voice.api.ReadAloudController
import app.hain.voice.api.VoiceEngines
import app.hain.voice.api.VoicePacks
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import java.io.IOException

/**
 * Voice in the chat (Häppchen 10): dictation with Moonshine into the message box (T-140) and read
 * aloud under every answer (T-141). The app builds it once; the dictation pack is a download of its
 * own and Hain never records anything before you tap the mic.
 */
class ChatVoice(
    val dictation: DictationController,
    val readAloud: ReadAloudController,
    private val models: ModelRepository,
    private val gate: NetworkGate?,
    /** Language for dictation: the answer language, or the phone's on "Automatisch" (`de`/`en`). */
    val language: () -> String,
    private val scope: CoroutineScope,
) {
    /** The dictation pack for [lang], `null` when the catalogue has none. */
    fun sttPack(lang: String = language()): ModelInfo? =
        models.catalog.firstOrNull { it.id == VoicePacks.sttPackId(lang) && it.kind == VoicePacks.KIND_STT }

    fun isInstalled(pack: ModelInfo): Boolean = models.isInstalled(pack)

    fun packState(pack: ModelInfo): Flow<DownloadState> = models.downloadState(pack)

    /**
     * Starts the pack download after you said yes. A first quick request goes through the
     * [NetworkGate], so it shows up in the network log (purpose MODEL_DOWNLOAD) like every request.
     */
    fun downloadPack(pack: ModelInfo) {
        scope.launch {
            val first = pack.files.firstOrNull() ?: return@launch
            try {
                gate?.fetch(GateRequest(first.url, Purpose.MODEL_DOWNLOAD, method = "HEAD", userStarted = true))
            } catch (e: IOException) {
                Log.w(TAG, "preflight failed, the downloader retries on its own", e)
            }
            models.startDownload(pack)
        }
    }

    fun cancelPack(pack: ModelInfo) = models.cancelDownload(pack)

    /** Language to read [text] in: what it is written in, else the dictation language. */
    fun speechLanguage(text: String): String = ReplyLanguagePolicy.detectLanguage(text)?.tag ?: language()

    /** Stops speaking and frees the dictation engine (the app went to the background). */
    fun pause() {
        readAloud.stop()
        dictation.release()
    }

    companion object {
        private const val TAG = "HainVoice"

        /** Moonshine from the installed pack, the system voice in Pim's pitch and speed. */
        fun create(
            context: Context,
            models: ModelRepository,
            gate: NetworkGate?,
            language: () -> String,
        ): ChatVoice {
            val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

            fun loadStt(lang: String) =
                models.catalog
                    .firstOrNull { it.id == VoicePacks.sttPackId(lang) }
                    ?.takeIf { models.isInstalled(it) }
                    ?.let { VoiceEngines.moonshine(models.packDir(it)) }
            return ChatVoice(
                dictation = DictationController(scope, VoiceEngines::microphone, ::loadStt),
                readAloud = ReadAloudController(scope, VoiceEngines.systemSpeech(context)),
                models = models,
                gate = gate,
                language = language,
                scope = scope,
            )
        }
    }
}

/** The chat's voice for the parts deep in the message list (read aloud under each answer). */
internal val LocalChatVoice = staticCompositionLocalOf<ChatVoice?> { null }
