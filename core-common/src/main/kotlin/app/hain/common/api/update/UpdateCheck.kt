package app.hain.common.api.update

import app.hain.common.api.HainSettings
import app.hain.common.api.UpdateChannel
import app.hain.common.api.UpdateCheck
import app.hain.common.api.net.GateRequest
import app.hain.common.api.net.NetworkGate
import app.hain.common.api.net.OfflineException
import app.hain.common.api.net.Purpose
import kotlinx.coroutines.delay
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.longOrNull
import java.io.IOException

/**
 * What the APK workflow puts next to a GitHub release as `version.json` (T-212):
 * `{"versionCode": 412, "versionName": "0.1.3.1", "commit": "…", "date": "2026-11-04", "channel": "nightly"}`.
 */
data class ReleaseInfo(
    val versionCode: Int,
    val versionName: String,
    val commit: String = "",
    val date: String = "",
    val channel: String = "",
    /** Direct APK link when the release has one. */
    val apkUrl: String? = null,
)

/** Result of one update check. */
sealed interface UpdateResult {
    /** A newer build is out; [page] is the release page the banner opens in the browser. */
    data class Newer(
        val release: ReleaseInfo,
        val page: String,
    ) : UpdateResult

    data object UpToDate : UpdateResult

    /**
     * T-281: what went wrong, said plainly. Before, every failure was "Bist du online?" — also when
     * the phone was online and there simply was no release for the channel yet (Oskar 06.11.).
     */
    sealed interface Failed : UpdateResult

    /** Hain's own offline mode is on. */
    data object OfflineMode : Failed

    /** The phone has no internet (no network, or the name could not be looked up). */
    data object Offline : Failed

    /** Online, but GitHub did not answer in time, refused (rate limit) or had an error, also after retries. */
    data object Unreachable : Failed

    /** GitHub answered, but this channel has no release with a readable `version.json` yet. */
    data object NoRelease : Failed
}

/** Pure rules of the update check (unit-tested). Hain never installs anything by itself. */
object UpdateRules {
    const val REPO = "https://github.com/oskar26/hain"

    /** At most one automatic check per this many ms (a day, with some room for "at start"). */
    const val MIN_GAP_MS = 20L * 60 * 60 * 1000

    /** Ask "check for updates?" only after this many chats, never on the first start. */
    const val ASK_AFTER_CHATS = 3

    private val json = Json { ignoreUnknownKeys = true }

    /** Reads `version.json`; `null` when it is not usable (no or bad versionCode). */
    @Suppress("ReturnCount")
    fun parse(text: String): ReleaseInfo? {
        val root = runCatching { json.parseToJsonElement(text).jsonObject }.getOrNull() ?: return null
        val code = root.int("versionCode") ?: root.string("versionCode")?.toIntOrNull() ?: return null
        if (code <= 0) return null
        return ReleaseInfo(
            versionCode = code,
            versionName = root.string("versionName").orEmpty(),
            commit = root.string("commit").orEmpty(),
            date = root.string("date").orEmpty(),
            channel = root.string("channel").orEmpty(),
            apkUrl = root.string("apk")?.takeIf { it.startsWith("https://") },
        )
    }

    /** Newer means a higher versionCode (versionName is only for people). */
    fun isNewer(
        remote: ReleaseInfo,
        ownVersionCode: Int,
    ): Boolean = remote.versionCode > ownVersionCode

    /** Address of `version.json` for [channel]: newest tagged release, or the nightly pre-release. */
    fun versionUrl(channel: UpdateChannel): String =
        when (channel) {
            UpdateChannel.STABLE -> "$REPO/releases/latest/download/version.json"
            UpdateChannel.NIGHTLY -> "$REPO/releases/download/nightly/version.json"
        }

    /**
     * T-281: the channel that is really asked. A debug build (`.debug`, the nightly APK) can only be
     * updated by the next nightly, so it always asks the nightly; a release build asks its setting.
     */
    fun channelFor(
        setting: UpdateChannel,
        debugBuild: Boolean,
    ): UpdateChannel = if (debugBuild) UpdateChannel.NIGHTLY else setting

    /** Waits before the 2nd and 3rd try (T-281). */
    val BACKOFF_MS = listOf(FIRST_PAUSE_MS, SECOND_PAUSE_MS)
    private const val FIRST_PAUSE_MS = 1_000L
    private const val SECOND_PAUSE_MS = 3_000L

    private const val HTTP_NOT_FOUND = 404
    private const val HTTP_GONE = 410

    /** A missing release for the channel (no tagged release yet, or the asset is gone). */
    fun missing(code: Int): Boolean = code == HTTP_NOT_FOUND || code == HTTP_GONE

    /** The release page the banner opens (only after a tap). */
    fun releasePage(channel: UpdateChannel): String =
        when (channel) {
            UpdateChannel.STABLE -> "$REPO/releases/latest"
            UpdateChannel.NIGHTLY -> "$REPO/releases/tag/nightly"
        }

    /** The automatic check at start runs when switched on and the last one is a day ago. */
    fun due(
        settings: HainSettings,
        now: Long,
    ): Boolean =
        settings.updateCheck == UpdateCheck.DAILY &&
            !settings.offlineMode &&
            (now - settings.lastUpdateCheck >= MIN_GAP_MS || now < settings.lastUpdateCheck)

    /** The one-time card: not answered yet, still off, and after the third chat. */
    fun shouldAsk(
        settings: HainSettings,
        chats: Int,
    ): Boolean = !settings.updateAsked && settings.updateCheck == UpdateCheck.OFF && chats >= ASK_AFTER_CHATS

    private fun JsonObject.string(key: String): String? = (this[key] as? JsonPrimitive)?.takeIf { it.isString }?.content

    private fun JsonObject.int(key: String): Int? =
        (this[key] as? JsonPrimitive)?.takeIf { !it.isString }?.let { it.intOrNull ?: it.longOrNull?.toInt() }
}

/**
 * Asks GitHub for the newest build, through the [NetworkGate] (purpose UPDATE, in the network log).
 * T-281: tells offline mode, no internet, GitHub not reachable and "no release yet" apart, and tries
 * up to three times with a pause when GitHub does not answer. [online] is the phone's own view
 * (a network with internet, not necessarily validated); [pause] is swapped in tests.
 */
class UpdateChecker(
    private val gate: NetworkGate,
    private val online: () -> Boolean = { true },
    private val pause: suspend (Long) -> Unit = { delay(it) },
) {
    suspend fun check(
        channel: UpdateChannel,
        ownVersionCode: Int,
    ): UpdateResult {
        var last: UpdateResult = UpdateResult.Unreachable
        for (attempt in 0..UpdateRules.BACKOFF_MS.size) {
            if (attempt > 0) pause(UpdateRules.BACKOFF_MS[attempt - 1])
            last = once(channel, ownVersionCode)
            if (last != UpdateResult.Unreachable) return last
        }
        return last
    }

    @Suppress("ReturnCount", "SwallowedException") // one early return per kind of answer
    private suspend fun once(
        channel: UpdateChannel,
        ownVersionCode: Int,
    ): UpdateResult {
        val response =
            try {
                gate.fetch(GateRequest(UpdateRules.versionUrl(channel), Purpose.UPDATE))
            } catch (e: OfflineException) {
                return UpdateResult.OfflineMode
            } catch (e: IOException) {
                return if (online()) UpdateResult.Unreachable else UpdateResult.Offline
            } catch (e: IllegalArgumentException) {
                return UpdateResult.NoRelease
            }
        return when {
            UpdateRules.missing(response.code) -> UpdateResult.NoRelease
            // rate limit, server error, a captive portal page: online, but no answer from GitHub
            !response.ok -> UpdateResult.Unreachable
            else -> verdict(UpdateRules.parse(response.body), channel, ownVersionCode)
        }
    }

    private fun verdict(
        release: ReleaseInfo?,
        channel: UpdateChannel,
        ownVersionCode: Int,
    ): UpdateResult =
        when {
            release == null -> UpdateResult.NoRelease
            UpdateRules.isNewer(release, ownVersionCode) ->
                UpdateResult.Newer(release, UpdateRules.releasePage(channel))
            else -> UpdateResult.UpToDate
        }
}
