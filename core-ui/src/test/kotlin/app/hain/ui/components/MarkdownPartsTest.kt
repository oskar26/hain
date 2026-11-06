package app.hain.ui.components

import androidx.compose.ui.graphics.Color
import app.hain.ui.theme.HainType
import org.intellij.markdown.MarkdownElementTypes
import org.intellij.markdown.ast.ASTNode
import org.intellij.markdown.flavours.gfm.GFMElementTypes
import org.intellij.markdown.flavours.gfm.GFMFlavourDescriptor
import org.intellij.markdown.flavours.gfm.GFMTokenTypes
import org.intellij.markdown.parser.MarkdownParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** T-298: heading scale and spacing, and that the showcase answer really holds every element. */
class MarkdownPartsTest {
    private val prepared = MathText.prepare(MarkdownShowcase.TEXT)
    private val tree = MarkdownParser(GFMFlavourDescriptor()).buildMarkdownTreeFromString(prepared)

    private fun ASTNode.all(): Sequence<ASTNode> = sequenceOf(this) + children.asSequence().flatMap { it.all() }

    private fun count(type: org.intellij.markdown.IElementType) = tree.all().count { it.type == type }

    @Test
    fun headingsGetSmallerFromOneToSix() {
        val styles = headingStyles(HainType.hanken(), Color.Black, Color.Gray)
        assertEquals(6, styles.size)
        for (i in 1 until 5) {
            assertTrue("H$i ≥ H${i + 1}", styles[i - 1].fontSize.value >= styles[i].fontSize.value)
        }
        assertTrue(styles[0].fontSize.value > styles[3].fontSize.value)
        assertEquals(Color.Gray, styles[5].color)
    }

    @Test
    fun headingSpaceShrinksWithLevel() {
        val space = (1..6).map { headingSpace(it).value }
        assertEquals(space.sortedDescending(), space)
        assertTrue(space.first() > space.last())
        assertEquals(headingSpace(6), headingSpace(9))
    }

    @Test
    fun showcaseHasAllHeadingLevels() {
        listOf(
            MarkdownElementTypes.ATX_1,
            MarkdownElementTypes.ATX_2,
            MarkdownElementTypes.ATX_3,
            MarkdownElementTypes.ATX_4,
            MarkdownElementTypes.ATX_5,
            MarkdownElementTypes.ATX_6,
        ).forEach { assertEquals(it.toString(), 1, count(it)) }
    }

    @Test
    fun showcaseHasInlineElements() {
        assertTrue(count(MarkdownElementTypes.STRONG) >= 1)
        assertTrue(count(MarkdownElementTypes.EMPH) >= 1)
        assertTrue(count(GFMElementTypes.STRIKETHROUGH) >= 1)
        assertTrue(count(MarkdownElementTypes.CODE_SPAN) >= 1)
        assertTrue(count(MarkdownElementTypes.INLINE_LINK) >= 1)
    }

    @Test
    fun showcaseHasBlocks() {
        // the kotlin block and the formula block (MathText turned $$…$$ into ```hain-math)
        assertEquals(2, count(MarkdownElementTypes.CODE_FENCE))
        assertTrue(prepared.contains("```hain-math"))
        assertTrue(count(MarkdownElementTypes.UNORDERED_LIST) >= 3)
        assertTrue(count(MarkdownElementTypes.ORDERED_LIST) >= 2)
        assertEquals(2, count(GFMTokenTypes.CHECK_BOX))
        assertEquals(1, count(MarkdownElementTypes.BLOCK_QUOTE))
        assertEquals(1, count(GFMElementTypes.TABLE))
        assertEquals(1, count(org.intellij.markdown.MarkdownTokenTypes.HORIZONTAL_RULE))
    }

    @Test
    fun inlineFormulaBecomesReadable() {
        assertTrue(prepared, prepared.contains("a² + b² = c²"))
        assertTrue(!prepared.contains("\\frac"))
    }

    @Test
    fun labelsDefaultEmpty() {
        val l = MarkdownLabels()
        assertEquals("", l.copy + l.copied + l.code + l.taskDone + l.taskOpen)
    }
}
