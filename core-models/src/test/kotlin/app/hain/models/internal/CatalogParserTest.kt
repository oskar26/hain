package app.hain.models.internal

import app.hain.models.api.ModelInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class CatalogParserTest {
    @Test
    fun bundledCatalogHasOneDefaultBrainWithVerifiedFile() {
        val models = CatalogParser.parse(File("src/main/assets/catalog.json").readText())
        val brains = models.filter { it.kind == "brain" && it.isDefault }
        assertEquals(1, brains.size)
        val file = brains.single().files.single()
        assertEquals("LFM3-1.7B-QAD-Q4_0.gguf", file.fileName)
        assertEquals(1_049_615_680L, file.size)
        assertTrue(file.url.startsWith("https://huggingface.co/"))
    }

    @Test
    fun bundledCatalogHasTheStrongBrainAsOptionalPack() {
        // T-240: Underdog Poodle 4B Q4_K_M, pinned revision, not the default (LFM3 stays standard)
        val models = CatalogParser.parse(File("src/main/assets/catalog.json").readText())
        val strong = models.single { it.kind == "brain" && it.isStrong }
        assertEquals("Underdog-Poodle-4B.Q4_K_M.gguf", strong.files.single().fileName)
        assertEquals(2_783_447_424L, strong.totalSize)
        assertEquals("6068fc8fe09a94a8f53a815d06365c857847ec4966307acd7c6b0e4f625da686", strong.files.single().sha256)
        assertTrue(
            strong.files
                .single()
                .url
                .contains("/resolve/86a171fc122c3989f57826cfd8eb589d3b879b81/"),
        )
        assertEquals(8, strong.minRamGb)
        assertEquals(12, strong.recommendRamGb)
        assertTrue(!strong.isDefault)
        assertEquals(ModelInfo.TIER_STANDARD, models.single { it.kind == "brain" && it.isDefault }.tier)
    }

    @Test
    fun bundledCatalogHasTheEmbedderPack() {
        val models = CatalogParser.parse(File("src/main/assets/catalog.json").readText())
        val embed = models.single { it.kind == ModelInfo.KIND_EMBED }
        assertEquals("LFM2.5-Embedding-350M-Q4_0.gguf", embed.files.single().fileName)
        assertEquals(219_308_800L, embed.totalSize)
        assertEquals(3, embed.minRamGb)
        assertTrue(!embed.isDefault)
        assertEquals("08bb9bfdfb516146b7aeb6d51994206b38e8b31c9e6e66a7bdcebbbc0afa3cea", embed.files.single().sha256)
    }

    @Test
    fun bundledCatalogHasWikipediaPacksInBothLanguages() {
        // T-237: offline Wikipedia (Kiwix "mini" ZIM), German and English, small and big
        val models = CatalogParser.parse(File("src/main/assets/catalog.json").readText())
        val packs = models.filter { it.kind == ModelInfo.KIND_KNOWLEDGE }
        assertEquals(setOf("de", "en"), packs.flatMap { it.lang }.toSet())
        assertTrue(
            packs.all {
                it.files
                    .single()
                    .fileName
                    .endsWith(".zim") &&
                    it.files
                        .single()
                        .sha256.length == 64
            },
        )
        assertTrue(
            packs.all {
                it.files
                    .single()
                    .url
                    .startsWith("https://download.kiwix.org/zim/wikipedia/")
            },
        )
        assertEquals(136_444_709L, packs.single { it.id == "wikipedia-de-top-mini" }.totalSize)
        assertTrue(packs.none { it.isDefault })
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsInventedChecksum() {
        CatalogParser.parse(
            """{"models":[{"id":"x","name":"x","kind":"brain","files":[{"url":"https://a/b.gguf",""" +
                """"sha256":"todo","size":1}],"licence":"l","licence_url":"u"}]}""",
        )
    }
}
