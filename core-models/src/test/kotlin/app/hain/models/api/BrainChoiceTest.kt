package app.hain.models.api

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BrainChoiceTest {
    @Suppress("LongParameterList") // a test catalogue entry, all with defaults
    private fun brain(
        id: String,
        bytes: Long,
        minRam: Int,
        isDefault: Boolean = false,
        kind: String = "brain",
        tier: String = ModelInfo.TIER_STANDARD,
    ) = ModelInfo(
        id = id,
        name = id.uppercase(),
        kind = kind,
        files = listOf(ModelFile("https://a/$id.gguf", "0".repeat(64), bytes)),
        minRamGb = minRam,
        licence = "l",
        licenceUrl = "u",
        isDefault = isDefault,
        tier = tier,
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

    // T-240: the optional "Großes Gehirn" (Jan-v3.5-4B): offered from 7 GB, recommended from 12 GB
    private val jan =
        brain("jan", 2_716_066_656L, 8).copy(tier = ModelInfo.TIER_STRONG, recommendRamGb = 12)
    private val lfm = brain("lfm", 1_049_615_680L, 4, isDefault = true)
    private val shipped = listOf(lfm, jan)

    @Test
    fun strongBrainIsHiddenOnPhonesThatAreTooSmall() {
        val options = BrainChoice.options(shipped, null, { it == lfm }, phone(6))
        assertEquals(listOf("lfm"), options.map { it.model.id })
        assertFalse(BrainChoice.offered(jan, phone(6), installed = false))
    }

    @Test
    fun strongBrainStaysVisibleWhenItIsAlreadyOnThePhone() {
        assertTrue(BrainChoice.offered(jan, phone(4), installed = true))
    }

    @Test
    fun strongBrainIsOfferedButNotRecommendedOnEightGigabytes() {
        val options = BrainChoice.options(shipped, null, { it == lfm }, phone(8))
        assertEquals(listOf("lfm", "jan"), options.map { it.model.id })
        assertTrue(options.first { it.model == lfm }.recommended)
        assertFalse(options.first { it.model == jan }.recommended)
        assertTrue(options.first { it.model == jan }.strong)
    }

    @Test
    fun strongBrainIsRecommendedOnTwelveGigabytes() {
        assertEquals(jan, BrainChoice.recommended(shipped, phone(12)))
    }

    @Test
    fun standardBrainStaysTheDefaultEvenWhenTheStrongOneIsRecommended() {
        // no migration: without a pick the default brain is used, whatever is recommended
        assertEquals(lfm, BrainChoice.active(shipped, null) { true })
        assertEquals(jan, BrainChoice.active(shipped, "jan") { true })
    }

    @Test
    fun standardBrainsAreAlwaysOffered() {
        assertTrue(BrainChoice.offered(lfm, phone(1), installed = false))
    }

    // ---- T-270: LFM3 1.7B is the default again, LFM3-Omni 2B is an optional download

    private val std = brain("lfm3", 1_049_615_680L, 4, isDefault = true)
    private val omni = brain("omni", 1_049_617_920L, 4, tier = ModelInfo.TIER_OPTIONAL)
    private val janStrong = brain("jan", 2_716_066_656L, 8, tier = ModelInfo.TIER_STRONG)
    private val catalogWithOmni = listOf(std, omni, janStrong)

    @Test
    fun optionalBrainIsOfferedButNeverRecommended() {
        assertTrue(BrainChoice.offered(omni, phone(4), installed = false))
        assertEquals(std, BrainChoice.recommended(catalogWithOmni, phone(6)))
        assertTrue(BrainChoice.recommended(catalogWithOmni, phone(12)) != omni)
        val rows = BrainChoice.options(catalogWithOmni, null, { false }, phone(6))
        assertTrue(rows.any { it.model == omni && !it.recommended })
    }

    @Test
    fun newUsersGetTheStandardBrainAndOmniUsersKeepTheirs() {
        // nothing on the phone yet: onboarding fetches the default (LFM3 1.7B)
        assertEquals(std, BrainChoice.active(catalogWithOmni, null) { false })
        // Omni was picked (or is the only brain on the phone): it stays in use
        assertEquals(omni, BrainChoice.active(catalogWithOmni, "omni") { it == omni || it == std })
        assertEquals(omni, BrainChoice.active(catalogWithOmni, null) { it == omni })
        // both on the phone, nothing picked: the default
        assertEquals(std, BrainChoice.active(catalogWithOmni, null) { it == omni || it == std })
    }
}
