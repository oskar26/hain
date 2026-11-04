package app.hain.common.api.net

import kotlinx.coroutines.flow.StateFlow
import java.io.IOException

/**
 * Why Hain goes online (TOOLS.md §1). Shown in the network log. [IMAGES] = the small site icons at
 * the source chips (T-064c, "Quellen-Bilder").
 */
enum class Purpose { SEARCH, NEWS, WEATHER, WIKI, GEO, PAGE_READ, MODEL_DOWNLOAD, UPDATE, IMAGES, OTHER }

/**
 * One outbound request. [userStarted] marks a request the user asked for directly (a model
 * download); only such a [Purpose.MODEL_DOWNLOAD] passes while offline mode is on.
 */
data class GateRequest(
    val url: String,
    val purpose: Purpose,
    val method: String = "GET",
    val headers: Map<String, String> = emptyMap(),
    val body: String? = null,
    val userStarted: Boolean = false,
)

/** Answer of a request: HTTP status and the body as text. */
data class GateResponse(
    val code: Int,
    val body: String,
) {
    val ok: Boolean get() = code in OK_RANGE

    private companion object {
        val OK_RANGE = 200..299
    }
}

/** Thrown by [NetworkGate.fetch] while offline mode is on; tools turn it into a friendly result. */
class OfflineException : IOException("offline mode is on")

/**
 * One line of the network log: when, which host, why and how much. [url] is the full address only
 * when the detailed log is switched on; by default Hain keeps no queries.
 */
data class NetworkLogEntry(
    val at: Long,
    val host: String,
    val purpose: Purpose,
    val bytes: Long,
    val code: Int?,
    val url: String? = null,
)

/** The only way Hain talks to the internet (TOOLS.md §1). Every request is checked and logged. */
interface NetworkGate {
    /** Newest first; kept for 7 days. */
    val log: StateFlow<List<NetworkLogEntry>>

    /** Number of requests running right now (the orange "online" dot). */
    val active: StateFlow<Int>

    /** Runs [request]. Throws [OfflineException] in offline mode, [IOException] on network errors. */
    suspend fun fetch(request: GateRequest): GateResponse

    /**
     * A big download that streams straight into a file, like the brain (3b-2). The gate checks the
     * address and offline mode as in [fetch], shows the orange dot while [transfer] runs and logs
     * the bytes it returns. The caller does the transfer itself, because [fetch] keeps answers in memory.
     */
    suspend fun stream(
        request: GateRequest,
        transfer: suspend () -> Long,
    ): Long = transfer()

    fun clearLog()
}

/**
 * The app's [NetworkGate] for code that Android creates by itself, such as the model download
 * worker (3b-2). Set once when the gate is made; `null` before that.
 */
object NetworkGates {
    @Volatile var current: NetworkGate? = null
}
