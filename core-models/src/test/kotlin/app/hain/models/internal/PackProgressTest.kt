package app.hain.models.internal

import app.hain.models.api.DownloadState
import app.hain.models.api.FailReason
import app.hain.models.internal.FileProgress.Phase
import org.junit.Assert.assertEquals
import org.junit.Test

class PackProgressTest {
    @Test
    fun noFilesIsIdle() {
        assertEquals(DownloadState.Idle, PackProgress.combine(emptyList()))
    }

    @Test
    fun allCompleteIsInstalled() {
        val files = listOf(FileProgress(10, Phase.COMPLETE), FileProgress(20, Phase.COMPLETE))
        assertEquals(DownloadState.Installed, PackProgress.combine(files))
    }

    @Test
    fun bytesOfAllFilesAddUp() {
        val files =
            listOf(
                FileProgress(100, Phase.COMPLETE),
                FileProgress(200, Phase.RUNNING, done = 50),
                FileProgress(700, Phase.QUEUED),
            )
        assertEquals(DownloadState.Running(150, 1000), PackProgress.combine(files))
    }

    @Test
    fun nothingStartedIsQueued() {
        val files = listOf(FileProgress(100, Phase.QUEUED), FileProgress(100, Phase.QUEUED))
        assertEquals(DownloadState.Queued, PackProgress.combine(files))
    }

    @Test
    fun betweenTwoFilesItStillCountsAsRunning() {
        val files = listOf(FileProgress(100, Phase.COMPLETE), FileProgress(100, Phase.QUEUED))
        assertEquals(DownloadState.Running(100, 200), PackProgress.combine(files))
    }

    @Test
    fun oneFailedFileFailsThePackWithItsReason() {
        val files =
            listOf(
                FileProgress(100, Phase.COMPLETE),
                FileProgress(100, Phase.FAILED, fail = FailReason.CHECKSUM),
                FileProgress(100, Phase.FAILED),
            )
        assertEquals(DownloadState.Failed(FailReason.CHECKSUM), PackProgress.combine(files))
    }

    @Test
    fun failureWithoutReasonIsUnknown() {
        val files = listOf(FileProgress(100, Phase.FAILED))
        assertEquals(DownloadState.Failed(FailReason.UNKNOWN), PackProgress.combine(files))
    }

    @Test
    fun lastFileVerifyingShowsVerifying() {
        val files = listOf(FileProgress(100, Phase.COMPLETE), FileProgress(100, Phase.VERIFYING, done = 100))
        assertEquals(DownloadState.Verifying, PackProgress.combine(files))
    }

    @Test
    fun verifyingWithMoreToComeKeepsTheBar() {
        val files = listOf(FileProgress(100, Phase.VERIFYING, done = 100), FileProgress(100, Phase.QUEUED))
        assertEquals(DownloadState.Running(100, 200), PackProgress.combine(files))
    }

    @Test
    fun cancelledPackIsIdle() {
        val files = listOf(FileProgress(100, Phase.COMPLETE), FileProgress(100, Phase.CANCELLED))
        assertEquals(DownloadState.Idle, PackProgress.combine(files))
    }
}
