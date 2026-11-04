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
    fun bundledCatalogHasTheEmbedderPack() {
        val models = CatalogParser.parse(File("src/main/assets/catalog.json").readText())
        val embed = models.single { it.kind == ModelInfo.KIND_EMBED }
        assertEquals("LFM2.5-Embedding-350M-Q4_0.gguf", embed.files.single().fileName)
        assertEquals(219_308_800L, embed.totalSize)
        assertEquals(3, embed.minRamGb)
        assertTrue(!embed.isDefault)
        assertEquals("08bb9bfdfb516146b7aeb6d51994206b38e8b31c9e6e66a7bdcebbbc0afa3cea", embed.files.single().sha256)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsInventedChecksum() {
        CatalogParser.parse(
            """{"models":[{"id":"x","name":"x","kind":"brain","files":[{"url":"https://a/b.gguf",""" +
                """"sha256":"todo","size":1}],"licence":"l","licence_url":"u"}]}""",
        )
    }
}
