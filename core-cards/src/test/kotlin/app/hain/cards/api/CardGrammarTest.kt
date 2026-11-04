package app.hain.cards.api

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * T-131: the GBNF grammar (assets/card.gbnf) is well formed and every card it allows is read by
 * [CardParser] completely. Char classes are cut before strings, since a class like `[^"]` holds a quote.
 */
class CardGrammarTest {
    private val grammar = File("src/main/assets/card.gbnf").readText()

    private val rules =
        grammar
            .lines()
            .filter { it.isNotBlank() && !it.trimStart().startsWith("#") }
            .associate { it.substringBefore("::=").trim() to it.substringAfter("::=") }

    @Test
    fun everyReferencedRuleIsDefined() {
        assertTrue("root" in rules)
        val refs =
            rules.values.flatMap { body ->
                body
                    .replace(Regex("""\[(?:[^\]\\]|\\.)*]"""), " ")
                    .replace(Regex(""""(?:[^"\\]|\\.)*""""), " ")
                    .replace(Regex("""\{\d+(,\d*)?}"""), " ")
                    .split(Regex("""[\s()|?*+]+"""))
                    .filter { it.isNotEmpty() }
            }
        refs.forEach { assertTrue("undefined rule $it", it in rules) }
    }

    @Test
    fun grammarCardsAreReadCompletely() {
        val samples =
            mapOf(
                "Card title=\"Einkauf\"\nChecklist\n- Tomaten\n- Basilikum [x]\n" +
                    "Button label=\"Kopieren\" action=copy\n" to 2,
                "Table\n| Stadt | Grad |\n| Köln | 9 |\nProgress value=40 label=\"Woche\"\n" to 2,
                "List\n- eins\n- zwei\nButton label=\"Mehr\" action=ask:\"Und dann?\"\n" +
                    "Button label=\"T\" action=share\n" to 3,
                "Chart type=bar title=\"Schlaf\" unit=\"h\"\n- Mo: 7.5\n- Di: 6\n" +
                    "Chart type=line\n- 9 Uhr: -2\n- 12 Uhr: 4\n" to 2,
            )
        samples.forEach { (body, blocks) -> assertEquals(body, blocks, CardParser.parse(body)!!.blocks.size) }
    }
}
