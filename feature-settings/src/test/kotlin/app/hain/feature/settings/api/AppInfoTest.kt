package app.hain.feature.settings.api

import org.junit.Assert.assertEquals
import org.junit.Test

class AppInfoTest {
    @Test
    fun `version line has name, code and short commit`() {
        val info = AppInfo("0.1.3.1-debug", 412, "a1b2c3d4e5f6", "2026-11-04")
        assertEquals("0.1.3.1-debug (412) · a1b2c3d", info.versionLine())
    }

    @Test
    fun `local build without code shows only what is known`() {
        assertEquals("0.1.3.1 · local", AppInfo("0.1.3.1", 0, "local").versionLine())
    }
}
