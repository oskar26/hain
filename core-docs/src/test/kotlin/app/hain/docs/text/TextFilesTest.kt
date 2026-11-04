package app.hain.docs.text

import app.hain.docs.internal.text.TextFiles
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TextFilesTest {
    @Test
    fun utf8WithBomIsRead() {
        val bytes = byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()) + "Größe\r\nStraße".toByteArray()
        assertEquals("Größe\nStraße", TextFiles.decode(bytes))
    }

    @Test
    fun oldWindowsFileFallsBackToCp1252() {
        val bytes = "Größe".toByteArray(charset("windows-1252"))
        assertEquals("Größe", TextFiles.decode(bytes))
    }

    @Test
    fun longTextIsSplitAtEmptyLines() {
        val para = "Wort ".repeat(100).trim()
        val text = List(10) { para }.joinToString("\n\n")
        val parts = TextFiles.parts(text, max = 1200)
        assertTrue(parts.size > 1)
        assertTrue(parts.all { it.length <= 1200 })
        assertEquals(1000, parts.sumOf { p -> Regex("Wort").findAll(p).count() })
    }

    @Test
    fun emptyTextHasNoParts() {
        assertTrue(TextFiles.parts("  \n ").isEmpty())
    }

    @Test
    fun knowsTextFiles() {
        assertTrue(TextFiles.isText("notizen.md", null))
        assertTrue(TextFiles.isText("liste.TXT", "application/octet-stream"))
        assertTrue(TextFiles.isText("x", "text/plain"))
        assertFalse(TextFiles.isText("bild.jpg", "image/jpeg"))
    }
}
