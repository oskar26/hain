package app.hain.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExternalLaunchTest {
    @Test
    fun appLockIsToldBeforeTheArticleOpens() {
        val calls = mutableListOf<String>()
        val ok = openExternally({ calls += "lock" }) { calls += "open" }
        assertTrue(ok)
        assertEquals(listOf("lock", "open"), calls)
    }

    @Test
    fun noBrowserIsSwallowedButTheLockWasStillTold() {
        var told = false
        val ok = openExternally({ told = true }) { error("no browser") }
        assertFalse(ok)
        assertTrue(told)
    }
}
