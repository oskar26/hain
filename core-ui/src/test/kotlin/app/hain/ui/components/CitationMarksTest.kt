package app.hain.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CitationMarksTest {
    @Test
    fun markersBecomeChipLinksWithoutTheSpaceBefore() {
        assertEquals(
            "Paris ist mild[1](hain-source:1). Der Turm[2](hain-source:2)[3](hain-source:3) ist alt.",
            CitationMarks.toLinks("Paris ist mild [1]. Der Turm [2, 3] ist alt.", 3),
        )
        assertEquals("a[1](hain-source:1)[2](hain-source:2)", CitationMarks.toLinks("a [1][2]", 2))
        assertEquals("a[1](hain-source:1)[2](hain-source:2)", CitationMarks.toLinks("a [1-2]", 2))
    }

    @Test
    fun unknownNumbersCodeAndLinksStay() {
        assertEquals("a [4]", CitationMarks.toLinks("a [4]", 3))
        assertEquals("`x[1]` und [Seite](https://x.de)", CitationMarks.toLinks("`x[1]` und [Seite](https://x.de)", 3))
        assertEquals("```\nlist[1]\n```", CitationMarks.toLinks("```\nlist[1]\n```", 3))
        assertEquals("a [1]", CitationMarks.toLinks("a [1]", 0))
        assertEquals("[[1]]", CitationMarks.toLinks("[[1]]", 3))
    }

    @Test
    fun stripRemovesMarkersForCopyShareAndSpeak() {
        val text = "Paris ist mild [1]. Der Turm [2, 3] ist alt."
        assertEquals("Paris ist mild. Der Turm ist alt.", CitationMarks.strip(text))
        assertEquals("ohne Marker", CitationMarks.strip("ohne Marker"))
        assertEquals("`a[1]`", CitationMarks.strip("`a[1]`"))
    }

    @Test
    fun numberOfReadsOnlyChipLinks() {
        assertEquals(2, CitationMarks.numberOf("hain-source:2"))
        assertNull(CitationMarks.numberOf("https://hain.app"))
    }
}
