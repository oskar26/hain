package app.hain.llm.internal

import org.junit.Assert.assertArrayEquals
import org.junit.Test

class ByteOffsetsTest {
    @Test
    fun charOffsetsBecomeUtf8ByteOffsets() {
        // "ü" and "ß" take two bytes in UTF-8
        assertArrayEquals(intArrayOf(1, 3, 5), byteOffsets("aüßb", listOf(1, 2, 3)))
    }

    @Test
    fun offsetsOutsideTheTextAreDropped() {
        assertArrayEquals(intArrayOf(2), byteOffsets("abc", listOf(0, 2, 3, 9)))
    }
}
