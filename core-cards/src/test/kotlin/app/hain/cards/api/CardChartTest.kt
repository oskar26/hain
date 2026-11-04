package app.hain.cards.api

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

/** 0.1.6.1: bar and line charts in model cards (CARDS.md §2) — parser, maths, plain text. */
class CardChartTest {
    private fun chart(body: String): CardBlock.Chart? =
        CardParser
            .parse(body)
            ?.blocks
            ?.filterIsInstance<CardBlock.Chart>()
            ?.firstOrNull()

    @Test
    fun specChartParses() {
        val card = CardParser.parse("Card title=\"Schlaf\"\nChart type=bar\n- Mo: 7\n- Di: 6,5\n- Mi: 8")!!
        assertEquals("Schlaf", card.title)
        assertEquals(
            listOf(
                CardBlock.Chart(
                    ChartType.BAR,
                    listOf(ChartPoint("Mo", 7.0), ChartPoint("Di", 6.5), ChartPoint("Mi", 8.0)),
                ),
            ),
            card.blocks,
        )
    }

    @Test
    fun lineChartWithTitleAndUnit() {
        val block = chart("Chart type=line title=\"Temperatur\" unit=\"°C\"\n- 9 Uhr: 4\n- 12 Uhr: 9\n- 15 Uhr: 11")!!
        assertEquals(ChartType.LINE, block.type)
        assertEquals("Temperatur", block.title)
        assertEquals("°C", block.unit)
        assertEquals(listOf(4.0, 9.0, 11.0), block.points.map { it.value })
        assertEquals("9 Uhr", block.points.first().label)
    }

    @Test
    fun germanKeywordsAndLooseLines() {
        val block = chart("Diagramm typ=linie titel=„Ausgaben“\nJanuar = 120 €\n* Februar 95 €\n1. März: −20")!!
        assertEquals(ChartType.LINE, block.type)
        assertEquals("Ausgaben", block.title)
        assertEquals("€", block.unit)
        assertEquals(listOf("Januar", "Februar", "März"), block.points.map { it.label })
        assertEquals(listOf(120.0, 95.0, -20.0), block.points.map { it.value })
    }

    @Test
    fun unitFromThePoints() {
        assertEquals("h", chart("Chart\n- Mo: 7,5 h\n- Di: 6 h")!!.unit)
        assertNull(chart("Chart\n- Mo: 7\n- Di: 6")!!.unit)
    }

    @Test
    fun aLineWithOnePointIsABar() {
        assertEquals(ChartType.BAR, chart("Chart type=line\n- Heute: 3")!!.type)
        assertEquals(ChartType.BAR, chart("Chart type=pie\n- a: 1\n- b: 2")!!.type)
    }

    @Test
    fun linesWithoutNumbersAndImagesAreIgnored() {
        val block = chart("Chart type=bar\n- Mo: viel\nImage src=attachment:1\n- Di: 3\nirgendwas\n- Mi: 4")!!
        assertEquals(listOf("Di", "Mi"), block.points.map { it.label })
    }

    @Test
    fun aChartWithoutPointsIsNoBlock() {
        assertNull(CardParser.parse("Chart type=bar\nsonst nichts"))
        val card = CardParser.parse("Chart type=bar\nList\n- a")!!
        assertEquals(listOf(CardBlock.Bullets(listOf("a"))), card.blocks)
    }

    @Test
    fun pointsAndLabelsAreCapped() {
        val body = "Chart\n" + (1..20).joinToString("\n") { "- Tag $it mit einem sehr langen Namen hier: $it" }
        val block = chart(body)!!
        assertEquals(CardParser.MAX_POINTS, block.points.size)
        assertTrue(block.points.all { it.label.length <= CardParser.MAX_LABEL })
    }

    @Test
    fun chartAndListInOneCard() {
        val card = CardParser.parse("Chart type=bar\n- a: 1\n- b: 2\nList\n- Tipp eins\nProgress value=50")!!
        assertEquals(3, card.blocks.size)
        assertTrue(card.blocks[0] is CardBlock.Chart)
        assertTrue(card.blocks[1] is CardBlock.Bullets)
    }

    @Test
    fun theAxisHasRoundEndsAndHoldsZero() {
        assertEquals(ChartMath.Range(0.0, 10.0), ChartMath.range(listOf(7.0, 6.5, 8.0)))
        assertEquals(ChartMath.Range(0.0, 250.0), ChartMath.range(listOf(120.0, 210.0)))
        assertEquals(ChartMath.Range(-25.0, 200.0), ChartMath.range(listOf(120.0, -21.0, 200.0)))
        assertEquals(ChartMath.Range(0.0, 1.0), ChartMath.range(listOf(0.0)))
        assertEquals(ChartMath.Range(0.0, 0.5), ChartMath.range(listOf(0.3, 0.45)))
        assertEquals(0.5f, ChartMath.range(listOf(10.0)).fraction(5.0))
    }

    @Test
    fun niceNumbers() {
        assertEquals(1.0, ChartMath.nice(0.7), 1e-9)
        assertEquals(2.0, ChartMath.nice(2.0), 1e-9)
        assertEquals(2.5, ChartMath.nice(2.1), 1e-9)
        assertEquals(5.0, ChartMath.nice(4.2), 1e-9)
        assertEquals(1000.0, ChartMath.nice(999.0), 1e-9)
    }

    @Test
    fun numbersInThePhonesLanguage() {
        assertEquals("7,5", ChartMath.format(7.5, Locale.GERMAN))
        assertEquals("7.5", ChartMath.format(7.5, Locale.ENGLISH))
        assertEquals("12", ChartMath.format(12.0, Locale.GERMAN))
        assertEquals("0,33", ChartMath.format(1.0 / 3, Locale.GERMAN))
        assertEquals("7,5 h", ChartMath.withUnit(7.5, "h", Locale.GERMAN))
        val week = listOf(ChartPoint("Mo", 7.0), ChartPoint("Di", 6.5))
        assertEquals("Mo 7, Di 6,5", ChartMath.spoken(week, null, Locale.GERMAN))
    }

    @Test
    fun thePeakIsTheFirstHighest() {
        assertEquals(1, ChartMath.peak(listOf(ChartPoint("a", 1.0), ChartPoint("b", 3.0), ChartPoint("c", 3.0))))
    }

    @Test
    fun copiedChartIsReadable() {
        val card = CardParser.parse("Card title=\"Schlaf\"\nChart type=bar title=\"Woche\"\n- Mo: 7,5 h\n- Di: 6 h")!!
        assertEquals("Schlaf\nWoche\n- Mo: 7.5 h\n- Di: 6 h", CardText.plain(card))
    }
}
