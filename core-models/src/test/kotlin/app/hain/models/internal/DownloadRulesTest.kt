package app.hain.models.internal

import app.hain.models.api.DownloadState
import app.hain.models.api.FailReason
import app.hain.models.internal.FileProgress.Phase
import org.junit.Assert.assertEquals
import org.junit.Test

/** T-287: what the models page shows after a download, a stop, a delete and a restart of the app. */
class DownloadRulesTest {
    @Test
    fun installedWinsOverAnyJob() {
        val job = FileProgress(100, Phase.CANCELLED)
        assertEquals(DownloadState.Installed, DownloadRules.single(true, job, 0, 100))
    }

    @Test
    fun runningShowsBytes() {
        val job = FileProgress(100, Phase.RUNNING, done = 40)
        assertEquals(DownloadState.Running(40, 100), DownloadRules.single(false, job, 40, 100))
    }

    @Test
    fun stoppedWithPartFileIsPausedNotIdle() {
        val job = FileProgress(100, Phase.CANCELLED)
        assertEquals(DownloadState.Paused(40, 100), DownloadRules.single(false, job, 40, 100))
    }

    @Test
    fun partFileAfterAppRestartIsPaused() {
        assertEquals(DownloadState.Paused(40, 100), DownloadRules.single(false, null, 40, 100))
    }

    @Test
    fun stoppedWithoutPartFileIsIdle() {
        assertEquals(DownloadState.Idle, DownloadRules.single(false, FileProgress(100, Phase.CANCELLED), 0, 100))
        assertEquals(DownloadState.Idle, DownloadRules.single(false, null, 0, 100))
    }

    @Test
    fun deletedAfterInstallIsIdleAgain() {
        // a finished job stays SUCCEEDED in WorkManager; the file is gone after "Entfernen"
        assertEquals(DownloadState.Idle, DownloadRules.single(false, FileProgress(100, Phase.COMPLETE), 0, 100))
    }

    @Test
    fun queuedVerifyingAndFailed() {
        assertEquals(DownloadState.Queued, DownloadRules.single(false, FileProgress(100, Phase.QUEUED), 0, 100))
        assertEquals(DownloadState.Verifying, DownloadRules.single(false, FileProgress(100, Phase.VERIFYING), 0, 100))
        val failed = FileProgress(100, Phase.FAILED, fail = FailReason.CHECKSUM)
        assertEquals(DownloadState.Failed(FailReason.CHECKSUM), DownloadRules.single(false, failed, 0, 100))
    }

    @Test
    fun unknownSizeNeverShowsMoreThanAll() {
        val paused = DownloadRules.single(false, null, 500, -1) as DownloadState.Paused
        assertEquals(0f, paused.fraction, 0f)
    }

    @Test
    fun packStoppedWithBytesIsPaused() {
        val files = listOf(FileProgress(100, Phase.COMPLETE), FileProgress(300, Phase.CANCELLED))
        assertEquals(DownloadState.Paused(150, 400), DownloadRules.pack(files, 150))
    }

    @Test
    fun packWithoutBytesIsIdle() {
        val files = listOf(FileProgress(100, Phase.NONE), FileProgress(300, Phase.NONE))
        assertEquals(DownloadState.Idle, DownloadRules.pack(files, 0))
    }

    @Test
    fun pausedFractionIsClamped() {
        assertEquals(0.4f, DownloadState.Paused(40, 100).fraction, 0.0001f)
        assertEquals(1f, DownloadState.Paused(140, 100).fraction, 0f)
    }
}
