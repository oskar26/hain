package app.hain.models.internal

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** T-140: the dictation packs in catalog.json (downloaded on demand, never in the APK). */
class VoicePackCatalogTest {
    private val models = CatalogParser.parse(File("src/main/assets/catalog.json").readText())
    private val packs = models.filter { it.kind == "stt" }
    private val pinned = Regex("^https://huggingface\\.co/[\\w.-]+/[\\w.-]+/resolve/[0-9a-f]{40}/[\\w.-]+$")

    @Test
    fun germanAndEnglishDictationPacksExist() {
        assertEquals(setOf("moonshine-small-de", "moonshine-small-en"), packs.map { it.id }.toSet())
        assertEquals(listOf("de"), packs.first { it.id == "moonshine-small-de" }.lang)
        assertEquals(listOf("en"), packs.first { it.id == "moonshine-small-en" }.lang)
    }

    @Test
    fun everyPackHasTheFiveGraphsTokenizerAndConfig() {
        val expected =
            setOf(
                "frontend.onnx",
                "encoder_int8.onnx",
                "adapter_int8.onnx",
                "cross_kv_int8.onnx",
                "decoder_kv_int8.onnx",
                "tokenizer.json",
                "streaming_config.json",
            )
        packs.forEach { pack ->
            assertEquals(pack.id, expected, pack.files.map { it.fileName }.toSet())
            assertEquals(pack.id, expected.size, pack.files.size)
            assertTrue(pack.isMultiFile)
        }
    }

    @Test
    fun filesArePinnedToACommitWithSizeAndChecksum() {
        packs.flatMap { it.files }.forEach { file ->
            assertTrue(file.url, pinned.matches(file.url))
            assertTrue(file.url, file.size > 0)
            assertTrue(file.url, Regex("^[0-9a-f]{64}$").matches(file.sha256))
        }
        // no two files share a checksum by accident (the DE and EN graphs differ)
        val shas = packs.flatMap { it.files }.map { it.sha256 }
        assertEquals(shas.size, shas.toSet().size)
    }

    @Test
    fun packsAreSmallMitAndNeverTheDefaultBrain() {
        packs.forEach { pack ->
            assertEquals("MIT", pack.licence)
            assertFalse(pack.isDefault)
            assertTrue(pack.id, pack.totalSize in 100_000_000L..300_000_000L)
        }
        assertEquals(1, models.count { it.kind == "brain" && it.isDefault })
    }
}
