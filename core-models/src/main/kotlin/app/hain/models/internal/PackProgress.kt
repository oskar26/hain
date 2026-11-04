package app.hain.models.internal

import app.hain.models.api.DownloadState
import app.hain.models.api.FailReason

/** Where the download of one file of a pack stands (from its WorkManager job, T-140). */
internal data class FileProgress(
    val size: Long,
    val phase: Phase,
    val done: Long = 0L,
    val fail: FailReason? = null,
) {
    enum class Phase { COMPLETE, RUNNING, VERIFYING, QUEUED, FAILED, CANCELLED, NONE }
}

/**
 * One state for a pack of several files, downloaded one after another (T-140): the bytes of all
 * files add up to one progress bar; one failed file fails the pack.
 */
internal object PackProgress {
    @Suppress("CyclomaticComplexMethod") // one flat rule per phase, easier to read than split up
    fun combine(files: List<FileProgress>): DownloadState {
        if (files.isEmpty()) return DownloadState.Idle
        val total = files.sumOf { it.size }
        val done =
            files.sumOf {
                when (it.phase) {
                    FileProgress.Phase.COMPLETE -> it.size
                    FileProgress.Phase.RUNNING, FileProgress.Phase.VERIFYING -> it.done.coerceIn(0L, it.size)
                    else -> 0L
                }
            }
        val phases = files.map { it.phase }.toSet()
        return when {
            phases == setOf(FileProgress.Phase.COMPLETE) -> DownloadState.Installed
            FileProgress.Phase.FAILED in phases ->
                DownloadState.Failed(files.firstNotNullOfOrNull { it.fail } ?: FailReason.UNKNOWN)
            FileProgress.Phase.VERIFYING in phases && files.none { it.phase.pending() } -> DownloadState.Verifying
            FileProgress.Phase.RUNNING in phases || FileProgress.Phase.VERIFYING in phases ->
                DownloadState.Running(done, total)
            FileProgress.Phase.QUEUED in phases ->
                if (done > 0) DownloadState.Running(done, total) else DownloadState.Queued
            else -> DownloadState.Idle
        }
    }

    private fun FileProgress.Phase.pending(): Boolean =
        this == FileProgress.Phase.QUEUED || this == FileProgress.Phase.RUNNING || this == FileProgress.Phase.NONE
}
