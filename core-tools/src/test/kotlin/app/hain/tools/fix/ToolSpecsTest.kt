package app.hain.tools.fix

import app.hain.tools.api.HainTools
import app.hain.tools.api.ToolSpecs
import app.hain.tools.search.FakeGate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ToolSpecsTest {
    private val names =
        HainTools
            .router(FakeGate { error("no network in tests") })
            .schemas(offline = false)
            .map { it.name }

    @Test
    fun everyToolHasASpec() {
        assertEquals(names.toSet(), ToolSpecs.all.keys)
    }

    @Test
    fun descriptionsAreOneShortSentencePair() {
        ToolSpecs.all.values.forEach { s ->
            assertTrue(s.name, s.description.length <= 120)
            assertTrue(s.name, s.params.all { it.description.length <= 100 })
            assertTrue(s.name, s.required.isNotEmpty())
        }
    }

    @Test
    fun aliasesPointToRealParametersAndEnums() {
        ToolSpecs.all.values.forEach { s ->
            val params = s.params.map { it.name }.toSet()
            s.aliases.forEach { (alias, real) ->
                assertTrue("${s.name}: $alias → $real", real in params)
                assertTrue("${s.name}: alias $alias is a real name", alias !in params)
            }
            s.params.forEach { p -> p.aliases.values.forEach { assertTrue("${s.name}.${p.name}: $it", it in p.enum) } }
        }
    }

    @Test
    fun wrongNamesAreUniqueAndNotRealNames() {
        val all = ToolSpecs.all.values.flatMap { it.names }
        assertEquals(all.size, all.toSet().size)
        assertTrue(all.none { it in ToolSpecs.all.keys })
    }

    @Test
    fun schemaKeepsEnumAndRequired() {
        val nav =
            ToolSpecs
                .of("navigate")
                .schema.parameters
                .toString()
        assertTrue(nav.contains(""""enum":["walk","bike","drive","transit"]"""))
        assertTrue(nav.endsWith(""""required":["destination"]}"""))
    }

    @Test
    fun toolLinesStayInBudget() {
        // all tool lines together, ~4 characters per token: at most ~600 tokens of the 2048 context
        val chars =
            ToolSpecs.all.values.sumOf {
                it.schema.parameters
                    .toString()
                    .length + it.description.length + 40
            }
        assertTrue("tool lines: $chars chars", chars < 2600)
    }
}
