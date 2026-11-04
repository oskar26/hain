package app.hain.docs.ocr

import org.junit.Assert.assertEquals
import org.junit.Test

class ReadingOrderTest {
    private fun item(
        text: String,
        left: Float,
        top: Float,
        right: Float,
        height: Float = 30f,
    ) = OcrItem(OcrBox(left, top, right, top + height), text, 0.9f)

    @Test
    fun linesTopToBottomAndWordsLeftToRight() {
        val items =
            listOf(
                item("Welt", 200f, 0f, 300f),
                item("zweite Zeile", 0f, 40f, 300f),
                item("Hallo", 0f, 2f, 150f),
            )
        assertEquals("Hallo Welt\nzweite Zeile", ReadingOrder.text(items))
    }

    @Test
    fun bigGapStartsParagraph() {
        val items = listOf(item("eins", 0f, 0f, 100f), item("zwei", 0f, 40f, 100f), item("drei", 0f, 160f, 100f))
        assertEquals("eins\nzwei\n\ndrei", ReadingOrder.text(items))
    }

    @Test
    fun twoColumnsAreReadOneAfterTheOther() {
        val left = (0 until 4).map { i -> item("L$i", 0f, i * 40f, 400f) }
        val right = (0 until 4).map { i -> item("R$i", 600f, i * 40f + 5f, 1000f) }
        val text = ReadingOrder.text(right + left)
        assertEquals("L0\nL1\nL2\nL3\n\nR0\nR1\nR2\nR3", text)
    }

    @Test
    fun titleOverBothColumnsComesFirst() {
        val title = item("Jahresabrechnung", 0f, 0f, 1000f)
        val left = (1..3).map { i -> item("L$i", 0f, i * 40f + 40f, 400f) }
        val right = (1..3).map { i -> item("R$i", 600f, i * 40f + 40f, 1000f) }
        val text = ReadingOrder.text(left + right + title)
        assertEquals("Jahresabrechnung\n\nL1\nL2\nL3\n\nR1\nR2\nR3", text)
    }

    @Test
    fun tableLikeRowsWithFewLinesStayRows() {
        val items = listOf(item("Name", 0f, 0f, 100f), item("Preis", 600f, 0f, 700f))
        assertEquals("Name Preis", ReadingOrder.text(items))
    }

    @Test
    fun emptyTextIsLeftOut() {
        assertEquals("a", ReadingOrder.text(listOf(item(" ", 0f, 0f, 10f), item("a", 0f, 40f, 10f))))
        assertEquals("", ReadingOrder.text(emptyList()))
    }
}
