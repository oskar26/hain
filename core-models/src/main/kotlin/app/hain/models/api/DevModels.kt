package app.hain.models.api

import app.hain.common.api.net.GateRequest
import app.hain.common.api.net.NetworkGate
import app.hain.common.api.net.Purpose
import kotlinx.coroutines.flow.Flow
import java.io.IOException

/**
 * Own models in developer mode (T-211): list a Hugging Face repo through the [NetworkGate], fetch a
 * file with the existing pack downloader (progress, resume, sha256 when known) and say where it lies.
 */
class DevModels(
    private val models: ModelRepository,
    private val gate: NetworkGate,
    /** Phone memory in GB, for the fit hint. */
    val ramGb: Int,
) {
    /** Files for what the developer typed: a repo listing or the one direct file. */
    suspend fun list(input: String): HfListing =
        when (val source = HuggingFace.source(input)) {
            is ModelSource.Repo -> HuggingFace.list(gate, source.id)
            is ModelSource.Direct -> HfListing.Files(listOf(source.file))
            ModelSource.Invalid -> HfListing.Failed("invalid")
        }

    /** How well [file] fits this phone (memory and free space). */
    fun fit(file: GgufFile): Fit = DeviceCheck.fit(HuggingFace.asModel(file), DeviceProfile(ramGb, models.freeSpace()))

    fun state(file: GgufFile): Flow<DownloadState> = models.downloadState(HuggingFace.asModel(file))

    /**
     * Starts the download. A first quick request goes through the gate, so the download shows up in
     * the network log (purpose MODEL_DOWNLOAD) like every other request.
     */
    suspend fun download(file: GgufFile) {
        try {
            gate.fetch(GateRequest(file.url, Purpose.MODEL_DOWNLOAD, method = "HEAD", userStarted = true))
        } catch (e: IOException) {
            android.util.Log.w(TAG, "preflight failed, the downloader retries on its own", e)
        }
        models.startDownload(HuggingFace.asModel(file))
    }

    fun cancel(file: GgufFile) = models.cancelDownload(HuggingFace.asModel(file))

    fun delete(file: GgufFile) = models.delete(HuggingFace.asModel(file))

    /** Where the finished file lies on the phone. */
    fun path(file: GgufFile): String = models.mainFile(HuggingFace.asModel(file)).absolutePath

    private companion object {
        const val TAG = "HainDevModels"
    }
}
