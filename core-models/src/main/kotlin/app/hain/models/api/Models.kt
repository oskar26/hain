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
     * T-260: the brain that was the default before (LFM3 1.7B). Shown only while it is on the phone,
     * never recommended; LFM3-Omni 2B replaces it (same backbone, better tool discipline).
     */
    val isLegacy: Boolean get() = tier == TIER_LEGACY

    /** Packs of several files (dictation: graphs + tokenizer) live in their own folder (T-140). */
    val isMultiFile: Boolean get() = files.size > 1

    companion object {
        /** The default chat brain. */
        const val KIND_BRAIN = "brain"

        /** The pack "Suche nach Bedeutung" for the memory (T-110, MEMORY.md §3). */
        const val KIND_EMBED = "embed"

        /** T-240: brain for every phone (T-260: LFM3-Omni 2B). */
        const val TIER_STANDARD = "standard"

        /** T-240: optional bigger brain, offered only on phones with enough memory. */
        const val TIER_STRONG = "strong"

        /** T-260: the former default brain, kept only for phones that still have it. */
        const val TIER_LEGACY = "legacy"

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

    fun startDownload(model: ModelInfo)

    fun cancelDownload(model: ModelInfo)

    fun delete(model: ModelInfo)

    /** Free bytes where models are stored. */
    fun freeSpace(): Long
}
