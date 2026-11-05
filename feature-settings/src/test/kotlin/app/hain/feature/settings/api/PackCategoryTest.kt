package app.hain.feature.settings.api

import app.hain.models.api.ModelFile
import app.hain.models.api.ModelInfo
import org.junit.Assert.assertEquals
import org.junit.Test

/** T-234: every pack lands in its group under "Modelle & Pakete". */
class PackCategoryTest {
    private val file = ModelFile("https://a/b", "0".repeat(64), 1)

    private fun pack(kind: String) = ModelInfo("x", "x", kind, listOf(file), 1, "l", "u", false)

    @Test
    fun groupsByKind() {
        assertEquals(PackCategory.KNOWLEDGE, PackCategory.of(pack(ModelInfo.KIND_KNOWLEDGE)))
        assertEquals(PackCategory.BRAINS, PackCategory.of(pack(ModelInfo.KIND_BRAIN)))
        assertEquals(PackCategory.MEDIA, PackCategory.of(pack(ModelInfo.KIND_EMBED)))
        assertEquals(PackCategory.VOICE, PackCategory.of(pack("stt")))
        assertEquals(PackCategory.EXTRAS, PackCategory.of(pack("something-new")))
        assertEquals(PackCategory.MEDIA, ExtraPack.Kind.OCR.category)
    }
}
