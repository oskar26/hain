package app.hain.models.api

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BrainChoiceTest {
    private fun brain(
        id: String,
        bytes: Long,
        minRam: Int,
        isDefault: Boolean = false,
        kind: String = "brain",
    ) = ModelInfo(
        id = id,
        name = id.uppercase(),
        kind = kind,
        files = listOf(ModelFile("https://a/$id.gguf", "0".repeat(64), bytes)),
        minRamGb = minRam,
        licence = "l",
        licenceUrl = "u",
        isDefault = isDefault,
    )

    private val small = brain("small", 1_000_000_000L, 4, isDefault = true)
    private val medium = brain("medium", 2_500_000_000L, 6)
    private val large = brain("large", 5_000_000_000L, 12)
    private val voice = brain("voice", 100L, 1, kind = "voice")
    private val catalog = listOf(large, voice, small, medium)

    private fun phone(ram: Int) = DeviceProfile(ramGb = ram, freeBytes = 1_000_000_000L)

    @Test
    fun picksTheChosenBrainWhenItIsOnThePhone() {
        val active = BrainChoice.active(catalog, "medium") { it != large }
        assertEquals(medium, active)
    }

    @Test
    fun fallsBackToTheDefaultWhenTheChosenOneIsGone() {
        val active = BrainChoice.active(catalog, "large") { it == small }
        assertEquals(small, active)
    }

    @Test
    fun fallsBackToAnyInstalledBrainWhenTheDefaultIsMissing() {
        val active = BrainChoice.active(catalog, null) { it == medium }
        assertEquals(medium, active)
    }

    @Test
    fun usesTheDefaultWhenNothingIsInstalled() {
        assertEquals(small, BrainChoice.active(catalog, "medium") { false })
    }

    @Test
    fun recommendsTheBiggestBrainThatFitsWell() {
        assertEquals(medium, BrainChoice.recommended(catalog, phone(8)))
        assertEquals(large, BrainChoice.recommended(catalog, phone(16)))
        assertEquals(small, BrainChoice.recommended(catalog, phone(4)))
    }

    @Test
    fun recommendsTheSmallestTightBrainOnASmallPhone() {
        assertEquals(small, BrainChoice.recommended(catalog, phone(3)))
    }

    @Test
    fun recommendsNothingWhenNoBrainFits() {
        assertNull(BrainChoice.recommended(catalog, phone(1)))
    }

    @Test
    fun freeSpaceDoesNotChangeTheRecommendation() {
        val full = DeviceProfile(ramGb = 8, freeBytes = 0L)
        assertEquals(medium, BrainChoice.recommended(catalog, full))
    }

    @Test
    fun optionsListInstalledBrainsFirstAndMarkActiveAndRecommended() {
        val installed = setOf(small, large)
        val options = BrainChoice.options(catalog, "large", { it in installed }, phone(8))
        assertEquals(listOf("small", "large", "medium"), options.map { it.model.id })
        assertTrue(options.first { it.model == large }.active)
        assertFalse(options.first { it.model == small }.active)
        assertTrue(options.first { it.model == medium }.recommended)
        assertFalse(options.first { it.model == medium }.installed)
        assertFalse(options.first { it.model == medium }.active)
        assertEquals(1, options.count { it.active })
    }

    @Test
    fun noActiveRowWhenNothingIsInstalled() {
        val options = BrainChoice.options(catalog, null, { false }, phone(8))
        assertTrue(options.none { it.active })
    }

    @Test
    fun emptyCatalogGivesNoOptions() {
        assertTrue(BrainChoice.options(listOf(voice), null, { true }, phone(8)).isEmpty())
    }

    @Test
    fun sizeNamesFollowTheFileSize() {
        assertEquals(BrainSize.SMALL, BrainSize.of(1_049_615_680L))
        assertEquals(BrainSize.MEDIUM, BrainSize.of(2_500_000_000L))
        assertEquals(BrainSize.LARGE, BrainSize.of(5_000_000_000L))
    }
}
