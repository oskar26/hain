package app.hain.models.api

import kotlinx.coroutines.flow.Flow

/** One downloadable file of a model (MODELS.md, catalog.json). */
data class ModelFile(
    val url: String,
    val sha256: String,
    val size: Long,
) {
    val fileName: String get() = url.substringAfterLast('/').substringBefore('?')
}

/** A catalogue entry. */
data class ModelInfo(
    val id: String,
    val name: String,
    val kind: String,
    val files: List<ModelFile>,
    val minRamGb: Int,
    val licence: String,
    val licenceUrl: String,
    val isDefault: Boolean,
) {
    val totalSize: Long get() = files.sumOf { it.size }

    companion object {
        /** The default chat brain. */
        const val KIND_BRAIN = "brain"

        /** The pack "Suche nach Bedeutung" for the memory (T-110, MEMORY.md §3). */
        const val KIND_EMBED = "embed"
    }
}

/** Progress of one model download. */
sealed interface DownloadState {
    data object Idle : DownloadState

    data object Queued : DownloadState

    data class Running(
        val downloaded: Long,
        val total: Long,
    ) : DownloadState {
        val fraction: Float get() = if (total > 0) (downloaded.toDouble() / total).toFloat() else 0f
    }

    data object Verifying : DownloadState

    data object Installed : DownloadState

    data class Failed(
        val reason: FailReason,
    ) : DownloadState
}

enum class FailReason { NETWORK, STORAGE, CHECKSUM, UNKNOWN }

/** Catalogue, installed files and downloads (T-081/T-082). */
interface ModelRepository {
    val catalog: List<ModelInfo>

    val defaultBrain: ModelInfo

    /** Main file of [model] on disk (may not exist yet). */
    fun mainFile(model: ModelInfo): java.io.File

    fun isInstalled(model: ModelInfo): Boolean

    fun downloadState(model: ModelInfo): Flow<DownloadState>

    fun startDownload(model: ModelInfo)

    fun cancelDownload(model: ModelInfo)

    fun delete(model: ModelInfo)

    /** Free bytes where models are stored. */
    fun freeSpace(): Long
}
