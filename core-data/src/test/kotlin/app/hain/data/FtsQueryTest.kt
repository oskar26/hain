package app.hain.data

import app.hain.data.dao.FtsQuery
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FtsQueryTest {
    @Test
    fun wordsBecomeQuotedPrefixTerms() {
        assertEquals("\"plastik*\" \"sparen*\"", FtsQuery.of("  Plastik sparen "))
    }

    @Test
    fun operatorsAndQuotesAreDropped() {
        assertEquals("\"a*\" \"or*\" \"b*\"", FtsQuery.of("a\" OR* (b) -"))
        assertEquals("\"grüße*\"", FtsQuery.of("Grüße!"))
    }

    @Test
    fun noWordsMeansNoQuery() {
        assertNull(FtsQuery.of(" *\"- "))
    }
}
