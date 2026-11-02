package app.hain.models.api

import org.junit.Assert.assertEquals
import org.junit.Test

class DeviceCheckTest {
    private val model =
        ModelInfo(
            id = "m",
            name = "M",
            kind = "brain",
            files = listOf(ModelFile("https://a/b.gguf", "0".repeat(64), 1_000L)),
            minRamGb = 4,
            licence = "l",
            licenceUrl = "u",
            isDefault = true,
        )

    @Test
    fun goodWhenRamAndSpaceAreEnough() {
        assertEquals(Fit.GOOD, DeviceCheck.fit(model, DeviceProfile(ramGb = 12, freeBytes = 5_000L)))
    }

    @Test
    fun tightWhenOneGigabyteShort() {
        assertEquals(Fit.TIGHT, DeviceCheck.fit(model, DeviceProfile(ramGb = 3, freeBytes = 5_000L)))
    }

    @Test
    fun tooSmallWhenFarBelow() {
        assertEquals(Fit.TOO_SMALL, DeviceCheck.fit(model, DeviceProfile(ramGb = 2, freeBytes = 5_000L)))
    }

    @Test
    fun noSpaceWhenStorageIsShort() {
        assertEquals(Fit.NO_SPACE, DeviceCheck.fit(model, DeviceProfile(ramGb = 8, freeBytes = 1_050L)))
    }
}
