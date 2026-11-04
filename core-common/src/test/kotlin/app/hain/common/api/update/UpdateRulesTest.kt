package app.hain.common.api.update

import app.hain.common.api.HainSettings
import app.hain.common.api.UpdateChannel
import app.hain.common.api.UpdateCheck
import app.hain.common.api.net.GateRequest
import app.hain.common.api.net.GateResponse
import app.hain.common.api.net.NetworkGate
import app.hain.common.api.net.NetworkLogEntry
import app.hain.common.api.net.OfflineException
import app.hain.common.api.net.Purpose
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateRulesTest {
    private val sample =
        """{"versionCode": 412, "versionName": "0.1.3.1", "commit": "abc1234def", "date": "2026-11-04",
           "channel": "nightly", "extra": [1, 2]}"""

    @Test
    fun `parses version json`() {
        val r = UpdateRules.parse(sample)!!
        assertEquals(412, r.versionCode)
        assertEquals("0.1.3.1", r.versionName)
        assertEquals("abc1234def", r.commit)
        assertEquals("2026-11-04", r.date)
        assertEquals("nightly", r.channel)
        assertNull(r.apkUrl)
    }

    @Test
    fun `versionCode as text works, broken json does not`() {
        assertEquals(7, UpdateRules.parse("""{"versionCode":"7"}""")!!.versionCode)
        assertNull(UpdateRules.parse("""{"versionName":"1"}"""))
        assertNull(UpdateRules.parse("""{"versionCode":0}"""))
        assertNull(UpdateRules.parse("<html>Not Found</html>"))
        assertNull(UpdateRules.parse(""))
        assertNull(UpdateRules.parse("""{"versionCode":5,"apk":"http://x"}""")!!.apkUrl)
    }

    @Test
    fun `newer means a higher versionCode`() {
        val r = ReleaseInfo(412, "0.1.3.1")
        assertTrue(UpdateRules.isNewer(r, 411))
        assertFalse(UpdateRules.isNewer(r, 412))
        assertFalse(UpdateRules.isNewer(r, 500))
    }

    @Test
    fun `addresses per channel`() {
        assertEquals(
            "https://github.com/oskar26/hain/releases/latest/download/version.json",
            UpdateRules.versionUrl(UpdateChannel.STABLE),
        )
        assertEquals(
            "https://github.com/oskar26/hain/releases/download/nightly/version.json",
            UpdateRules.versionUrl(UpdateChannel.NIGHTLY),
        )
        assertEquals(
            "https://github.com/oskar26/hain/releases/tag/nightly",
            UpdateRules.releasePage(UpdateChannel.NIGHTLY),
        )
    }

    @Test
    fun `daily check is due once a day, never when off or offline`() {
        val day = 24L * 60 * 60 * 1000
        val on = HainSettings(updateCheck = UpdateCheck.DAILY, lastUpdateCheck = 10 * day)
        assertTrue(UpdateRules.due(on, 11 * day))
        assertFalse(UpdateRules.due(on, 10 * day + 1000))
        assertFalse(UpdateRules.due(on.copy(updateCheck = UpdateCheck.OFF), 20 * day))
        assertFalse(UpdateRules.due(on.copy(offlineMode = true), 20 * day))
    }

    @Test
    fun `asks once, after the third chat`() {
        assertFalse(UpdateRules.shouldAsk(HainSettings(), 0))
        assertFalse(UpdateRules.shouldAsk(HainSettings(), 2))
        assertTrue(UpdateRules.shouldAsk(HainSettings(), 3))
        assertFalse(UpdateRules.shouldAsk(HainSettings(updateAsked = true), 5))
    }

    @Test
    fun `checker goes through the gate with purpose UPDATE`() =
        runTest {
            val gate = FakeGate { GateResponse(200, sample) }
            assertEquals(
                UpdateResult.Newer(UpdateRules.parse(sample)!!, UpdateRules.releasePage(UpdateChannel.NIGHTLY)),
                UpdateChecker(gate).check(UpdateChannel.NIGHTLY, 400),
            )
            assertEquals(Purpose.UPDATE, gate.asked.single().purpose)
            assertEquals(UpdateResult.UpToDate, UpdateChecker(gate).check(UpdateChannel.NIGHTLY, 412))
            assertEquals(
                UpdateResult.Failed,
                UpdateChecker(FakeGate { GateResponse(404, "") }).check(UpdateChannel.STABLE, 1),
            )
            assertEquals(
                UpdateResult.Failed,
                UpdateChecker(FakeGate { throw OfflineException() }).check(UpdateChannel.STABLE, 1),
            )
        }

    private class FakeGate(
        val answer: () -> GateResponse,
    ) : NetworkGate {
        val asked = mutableListOf<GateRequest>()
        override val log: StateFlow<List<NetworkLogEntry>> = MutableStateFlow(emptyList())
        override val active: StateFlow<Int> = MutableStateFlow(0)

        override suspend fun fetch(request: GateRequest): GateResponse {
            asked += request
            return answer()
        }

        override fun clearLog() = Unit
    }
}
