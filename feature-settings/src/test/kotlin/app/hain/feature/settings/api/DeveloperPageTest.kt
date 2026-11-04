package app.hain.feature.settings.api

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DeveloperPageTest {
    @Test
    fun `number fields take dot or comma`() {
        assertEquals(0.7f, parseNumber("0,7"))
        assertEquals(1.1f, parseNumber(" 1.1 "))
        assertEquals(-1f, parseNumber("-1"))
        assertNull(parseNumber("abc"))
        assertNull(parseNumber(""))
    }

    @Test
    fun `context choices cover 512 to 8192`() {
        assertEquals(512, CONTEXT_CHOICES.first())
        assertEquals(8192, CONTEXT_CHOICES.last())
    }
}
