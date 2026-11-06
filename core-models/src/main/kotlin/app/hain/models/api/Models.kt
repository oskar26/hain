package app.hain.models.api

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

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
    /** Languages of the model (catalog `lang`); a dictation pack has exactly one (T-140). */
    val lang: List<String> = emptyList(),
    /** T-240: brain tier (catalog `tier`): [TIER_STANDARD] for every phone, [TIER_STRONG] = "Großes Gehirn". */
    val tier: String = TIER_STANDARD,
    /** T-240: memory in GB from which this pack is recommended (catalog `recommend_ram_gb`), else [minRamGb]. */
    val recommendRamGb: Int = minRamGb,
) {
    val totalSize: Long get() = files.sumOf { it.size }

    /** T-240: the optional "Großes Gehirn" for strong phones (Jan-v3.5-4B, ADR-24). */
    val isStrong: Boolean get() = tier == TIER_STRONG

    /**
     * T-270: an optional brain (LFM3-Omni 2B): always offered as a download, never recommended or
     * picked by itself; LFM3 1.7B stays the default brain.
     */
    val isOptional: Boolean get() = tier == TIER_OPTIONAL

    /** Packs of several files (dictation: graphs + tokenizer) live in their own folder (T-140). */
    val isMultiFile: Boolean get() = files.size > 1

    companion object {
        /** The default chat brain. */
        const val KIND_BRAIN = "brain"

        /** The pack "Suche nach Bedeutung" for the memory (T-110, MEMORY.md §3). */
        const val KIND_EMBED = "embed"

        /** T-240: brain for every phone (LFM3 1.7B, again the default since T-270). */
        const val TIER_STANDARD = "standard"

        /** T-240: optional bigger brain, offered only on phones with enough memory. */
        const val TIER_STRONG = "strong"

        /** T-270: optional brain, offered as a download but never recommended (LFM3-Omni 2B). */
        const val TIER_OPTIONAL = "optional"

        /** T-237: a knowledge pack (offline Wikipedia, Kiwix ZIM) for the `wikipedia` tool. */
        const val KIND_KNOWLEDGE = "knowledge"
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

    /**
     * T-287: stopped by you (in the app or in the notification) with [downloaded] bytes already on the
     * phone: "Fortsetzen" resumes with an HTTP Range request, "Verwerfen" removes the part file.
     */
    data class Paused(
        val downloaded: Long,
        val total: Long,
    ) : DownloadState {
        val fraction: Float get() = if (total > 0) (downloaded.toDouble() / total).toFloat().coerceIn(0f, 1f) else 0f
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

    /** Folder that holds the files of [model] (its own folder for a pack of several files, T-140). */
    fun packDir(model: ModelInfo): java.io.File = mainFile(model).parentFile ?: mainFile(model)

    fun downloadState(model: ModelInfo): Flow<DownloadState>

    /**
     * T-287: ids of every catalogue pack that is completely on the phone, again after each download,
     * delete or stop (the "2 von 4 auf dem Handy" counts read this instead of reading the files once).
     */
    val installedIds: Flow<Set<String>>
        get() = flowOf(catalog.filter(::isInstalled).map { it.id }.toSet())

    fun startDownload(model: ModelInfo)

    fun cancelDownload(model: ModelInfo)

    fun delete(model: ModelInfo)

    /** Free bytes where models are stored. */
    fun freeSpace(): Long
}
