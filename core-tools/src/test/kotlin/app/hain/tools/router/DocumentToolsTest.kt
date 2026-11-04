package app.hain.tools.router

import app.hain.llm.api.ToolSchema
import app.hain.tools.api.Tool
import app.hain.tools.api.ToolContext
import app.hain.tools.api.ToolResult
import app.hain.tools.api.ToolRouter
import app.hain.tools.internal.calc.CalculatorTool
import kotlinx.serialization.json.JsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Häppchen 9: search_document is only offered while a long document is attached. */
class DocumentToolsTest {
    private class DocTool : Tool {
        override val online = false
        override val schema = ToolSchema("search_document", "Search.", JsonObject(emptyMap()))

        override suspend fun run(
            args: Map<String, Any?>,
            ctx: ToolContext,
        ) = ToolResult("{}")
    }

    private val router = ToolRouter(listOf(CalculatorTool(), DocTool()))

    @Test
    fun hiddenWithoutDocuments() {
        assertEquals(listOf("calculate"), router.schemas(offline = false).map { it.name })
    }

    @Test
    fun shownWithDocumentsEvenOffline() {
        val names = router.schemas(offline = true, documents = true).map { it.name }
        assertTrue("search_document" in names)
        assertTrue("calculate" in names)
    }
}
