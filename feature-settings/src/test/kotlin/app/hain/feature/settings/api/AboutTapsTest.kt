package app.hain.feature.settings.api

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AboutTapsTest {
    @Test
    fun `hint counts down from the fourth tap`() {
        assertNull(tapsLeft(0))
        assertNull(tapsLeft(3))
        assertEquals(3, tapsLeft(4))
        assertEquals(2, tapsLeft(5))
        assertEquals(1, tapsLeft(6))
        assertNull(tapsLeft(7))
    }
}
