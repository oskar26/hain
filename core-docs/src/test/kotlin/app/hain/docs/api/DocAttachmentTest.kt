package app.hain.docs.api

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DocAttachmentTest {
    @Test
    fun jsonRoundTrip() {
        val list =
            listOf(
                DocAttachment(7, "Rechnung \"Nov\".pdf", DocKind.PDF, 3, 420),
                DocAttachment(8, "Foto.jpg", DocKind.IMAGE, 1, 90),
            )
        assertEquals(list, DocAttachment.decode(DocAttachment.encode(list)))
    }

    @Test
    fun noAttachmentsIsNull() {
        assertNull(DocAttachment.encode(emptyList()))
        assertTrue(DocAttachment.decode(null).isEmpty())
        assertTrue(DocAttachment.decode("").isEmpty())
    }

    @Test
    fun brokenJsonGivesNothing() {
        assertTrue(DocAttachment.decode("{kaputt").isEmpty())
        assertTrue(DocAttachment.decode("[{\"name\":\"x\"}]").isEmpty())
    }
}
