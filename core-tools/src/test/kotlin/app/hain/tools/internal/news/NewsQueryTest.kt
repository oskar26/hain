package app.hain.tools.internal.news

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** T-236: "Nachrichten" without a topic means the top headlines. */
class NewsQueryTest {
    @Test
    fun wordsForNewsAreNoTopic() {
        listOf(null, "", "  ", "news", "Nachrichten", "Aktuelle Nachrichten", "„Neue Schlagzeilen“", "Heute?")
            .forEach { assertNull(it, NewsQuery.topic(it)) }
    }

    @Test
    fun realTopicsStay() {
        assertEquals("Deutschlandticket", NewsQuery.topic(" Deutschlandticket "))
        assertEquals("US Wahl", NewsQuery.topic("US   Wahl"))
    }
}
