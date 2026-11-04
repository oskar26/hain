package app.hain.models.api

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HuggingFaceTest {
    @Test
    fun `repo, page link and direct link are understood`() {
        assertEquals(ModelSource.Repo("LiquidAI/LFM3-1.7B-GGUF"), HuggingFace.source(" LiquidAI/LFM3-1.7B-GGUF "))
        assertEquals(
            ModelSource.Repo("LiquidAI/LFM3-1.7B-GGUF"),
            HuggingFace.source("https://huggingface.co/LiquidAI/LFM3-1.7B-GGUF/tree/main"),
        )
        val direct = HuggingFace.source("https://example.org/models/tiny.gguf?download=1")
        assertTrue(direct is ModelSource.Direct)
        assertEquals("tiny.gguf", (direct as ModelSource.Direct).file.name)
        assertEquals(ModelSource.Invalid, HuggingFace.source("http://example.org/a.gguf"))
        assertEquals(ModelSource.Invalid, HuggingFace.source("nur ein wort"))
        assertEquals(ModelSource.Invalid, HuggingFace.source(""))
    }

    @Test
    fun `api answer becomes gguf files with size and sha, smallest first`() {
        val body =
            """
            {"id":"LiquidAI/LFM3-1.7B-GGUF","siblings":[
              {"rfilename":"README.md","size":100},
              {"rfilename":"LFM3-1.7B-Q8_0.gguf","size":1800000000,"lfs":{"sha256":"aa","size":1800000000}},
              {"rfilename":"LFM3-1.7B-QAD-Q4_0.gguf","size":1049615680,"lfs":{"sha256":"bb","size":1049615680}},
              {"rfilename":"mmproj-F16.gguf","size":5},
              {"rfilename":"big-00001-of-00002.gguf","size":5}
            ]}
            """.trimIndent()
        val files = HuggingFace.parseFiles("LiquidAI/LFM3-1.7B-GGUF", body)!!
        assertEquals(listOf("LFM3-1.7B-QAD-Q4_0.gguf", "LFM3-1.7B-Q8_0.gguf"), files.map { it.name })
        assertEquals("bb", files[0].sha256)
        assertEquals(1049615680L, files[0].size)
        assertEquals(
            "https://huggingface.co/LiquidAI/LFM3-1.7B-GGUF/resolve/main/LFM3-1.7B-QAD-Q4_0.gguf",
            files[0].url,
        )
        assertNull(HuggingFace.parseFiles("a/b", "<html>"))
    }

    @Test
    fun `ram hint and catalogue entry`() {
        assertEquals(4, HuggingFace.minRamGb(1_049_615_680))
        assertEquals(5, HuggingFace.minRamGb(1_800_000_000))
        assertEquals(0, HuggingFace.minRamGb(0))
        val m =
            HuggingFace.asModel(
                GgufFile("a/b", "x.gguf", "https://huggingface.co/a/b/resolve/main/x.gguf", 10, "cc"),
            )
        assertEquals("custom", m.kind)
        assertEquals("dev-a-b-x-gguf", m.id)
        assertEquals("x.gguf", m.files.single().fileName)
        assertEquals("https://huggingface.co/api/models/a/b?blobs=true", HuggingFace.apiUrl("a/b"))
    }
}
