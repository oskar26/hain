package app.hain.feature.settings.api

import app.hain.models.api.DownloadState
import app.hain.models.api.FailReason
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** T-287: the progress bar under a pack. */
class PackStatusTest {
    @Test
    fun barWhileLoadingPausedAndChecking() {
        assertEquals(0.5f, PackStatus.fraction(DownloadState.Running(50, 100))!!, 0f)
        assertEquals(0.25f, PackStatus.fraction(DownloadState.Paused(25, 100))!!, 0f)
        assertEquals(1f, PackStatus.fraction(DownloadState.Verifying)!!, 0f)
    }

    @Test
    fun noBarOtherwise() {
        assertNull(PackStatus.fraction(DownloadState.Idle))
        assertNull(PackStatus.fraction(DownloadState.Installed))
        assertNull(PackStatus.fraction(DownloadState.Queued))
        assertNull(PackStatus.fraction(DownloadState.Failed(FailReason.NETWORK)))
    }

    @Test
    fun percentIsClamped() {
        assertEquals(42, PackStatus.percent(0.42f))
        assertEquals(100, PackStatus.percent(1.7f))
        assertEquals(0, PackStatus.percent(-1f))
    }
}
