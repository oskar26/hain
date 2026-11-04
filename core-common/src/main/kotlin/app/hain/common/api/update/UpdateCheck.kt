package app.hain.common.api.update

import app.hain.common.api.HainSettings
import app.hain.common.api.UpdateChannel
import app.hain.common.api.UpdateCheck
import app.hain.common.api.net.GateRequest
import app.hain.common.api.net.NetworkGate
import app.hain.common.api.net.Purpose
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

    /** No answer, offline mode, or no readable `version.json` (e.g. no tagged release yet). */
    data object Failed : UpdateResult
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

/** Asks GitHub for the newest build, through the [NetworkGate] (purpose UPDATE, in the network log). */
class UpdateChecker(
    private val gate: NetworkGate,
) {
    @Suppress("ReturnCount", "SwallowedException") // every failure is simply "could not check"
    suspend fun check(
        channel: UpdateChannel,
        ownVersionCode: Int,
    ): UpdateResult {
        val response =
            try {
                gate.fetch(GateRequest(UpdateRules.versionUrl(channel), Purpose.UPDATE))
            } catch (e: IOException) {
                return UpdateResult.Failed
            } catch (e: IllegalArgumentException) {
                return UpdateResult.Failed
            }
        if (!response.ok) return UpdateResult.Failed
        val release = UpdateRules.parse(response.body) ?: return UpdateResult.Failed
        return if (UpdateRules.isNewer(release, ownVersionCode)) {
            UpdateResult.Newer(release, UpdateRules.releasePage(channel))
        } else {
            UpdateResult.UpToDate
        }
    }
}
