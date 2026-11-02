package app.hain.models.internal

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

    @Test(expected = IllegalArgumentException::class)
    fun rejectsInventedChecksum() {
        CatalogParser.parse(
            """{"models":[{"id":"x","name":"x","kind":"brain","files":[{"url":"https://a/b.gguf",""" +
                """"sha256":"todo","size":1}],"licence":"l","licence_url":"u"}]}""",
        )
    }
}
