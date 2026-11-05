package app.hain.memory

import app.hain.memory.api.MemoryCategory
import app.hain.memory.internal.SelfFacts
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** T-262: first-person statements about the user count, questions and requests about others never. */
class SelfFactsTest {
    private fun facts(text: String) = SelfFacts.ruleFacts(listOf(text)).map { it.text }

    @Test
    fun movingToACityBecomesTheHomePlace() {
        assertEquals(listOf("wohnt in Köln"), facts("ich bin neu nach köln gezogen was machen"))
        assertEquals(listOf("wohnt in Köln"), facts("Ich bin neu nach Köln gezogen. Was kann ich machen?"))
        assertEquals(listOf("wohnt in Frankfurt am Main"), facts("bin letzte woche nach Frankfurt am Main gezogen"))
        assertEquals(listOf("wohnt in Münster"), facts("Ich bin neu in Münster"))
        assertEquals(listOf("wohnt in Bonn"), facts("ich wohne in einer WG in Bonn"))
        assertEquals(listOf("wohnt in Hamburg"), facts("Wir sind nach Hamburg gezogen"))
        assertEquals(listOf("lives in Bristol"), facts("I just moved to Bristol, any tips?"))
        assertEquals(listOf("lives in Leeds"), facts("I live in Leeds"))
        assertEquals(listOf("lives in Manchester"), facts("I'm new to Manchester"))
    }

    @Test
    fun theNewestHomeWinsAndOriginIsKeptApart() {
        assertEquals(listOf("wohnt in Bonn"), facts("Ich wohne in Köln. Letzte Woche bin ich nach Bonn gezogen"))
        val both = facts("Ich komme aus Graz und wohne jetzt in Köln").toSet()
        assertEquals(setOf("wohnt in Köln", "kommt aus Graz"), both)
        assertEquals(listOf("kommt aus Graz"), facts("Ich komme aus Graz"))
    }

    @Test
    fun otherSelfFacts() {
        assertEquals(
            listOf("wohnt in Leipzig", "arbeitet als Bäcker"),
            facts("Ich wohne seit 2 Jahren in Leipzig und arbeite als Bäcker"),
        )
        assertEquals(listOf("heißt Mia", "ist 15 Jahre alt"), facts("ich heiße Mia und bin 15 Jahre alt"))
        assertEquals(listOf("Bruder heißt Tim"), facts("Mein Bruder heißt Tim."))
        assertEquals(listOf("mag keine Pilze"), facts("ich mag keine Pilze"))
        assertEquals(listOf("isst vegetarisch"), facts("Ich bin Vegetarierin"))
        assertEquals(listOf("hat einen Hund"), facts("ich habe einen Hund"))
        assertEquals(listOf("geht in die 9. Klasse"), facts("Ich gehe in die 9. Klasse"))
        assertEquals(listOf("is called Sam", "is 16 years old"), facts("My name is Sam and I'm 16 years old"))
        assertEquals(listOf("ist 15 Jahre alt", "mag Mathe"), facts("Ich bin 15. Ich mag Mathe."))
        val allergy = SelfFacts.ruleFacts(listOf("Ich bin allergisch gegen Nüsse")).single()
        assertEquals(MemoryCategory.HEALTH, allergy.category)
    }

    @Test
    fun questionsAndRequestsAboutOthersAreNoStatements() {
        listOf(
            "Wer ist Taylor Swift?",
            "Was weißt du über Goethe",
            "Erzähl mir was über Einstein",
            "ich frage mich, wer Napoleon war",
            "Wie wird das Wetter in Berlin?",
            "Kannst du mir sagen, wo Angela Merkel wohnt?",
            "Wo wohnt der Papst",
            "Wenn ich in Paris wohnen würde, was würde ich machen?",
            "Tell me about Elon Musk",
            "Where does the king live?",
            "Ich will wissen, wo Goethe gewohnt hat",
            "Mein Lieblingsschauspieler wohnt in LA?",
            "Schreib mir ein Gedicht über Köln",
            "Who is the president of France? I wonder.",
        ).forEach { q ->
            assertTrue(q, SelfFacts.selfSentences(listOf(q)).isEmpty())
            assertTrue(q, facts(q).isEmpty())
        }
    }

    @Test
    fun aBrainFactNeedsTheUsersOwnWords() {
        val said = SelfFacts.selfSentences(listOf("Wer ist Goethe?", "ich wohne in Köln"))
        assertTrue(SelfFacts.grounded("wohnt in Köln", said))
        assertFalse(SelfFacts.grounded("Goethe schrieb den Faust", said))
        assertFalse(SelfFacts.grounded("mag Faust", said))
    }

    @Test
    fun slotsAndHomePlace() {
        assertEquals(SelfFacts.Slot.HOME, SelfFacts.slot("wohnt in Köln"))
        assertEquals(SelfFacts.Slot.AGE, SelfFacts.slot("ist 15 Jahre alt"))
        assertNull(SelfFacts.slot("mag Mathe"))
        assertEquals("Köln", SelfFacts.homePlace(listOf("wohnt in Köln")))
        assertEquals("Bristol", SelfFacts.homePlace(listOf("lives in Bristol.")))
        assertNull(SelfFacts.homePlace(listOf("kommt aus Graz")))
    }

    @Test
    fun learningSomethingIsASelfFact() {
        // T-271: Oskar's device test, "hi ich lerne gerade spanisch" → chip "Merken: lernt Spanisch"
        assertEquals(listOf("lernt Spanisch"), facts("hi ich lerne gerade spanisch"))
        assertEquals(listOf("lernt Gitarre"), facts("Ich lerne seit zwei Jahren Gitarre."))
        assertEquals(listOf("is learning Spanish"), facts("I'm learning Spanish"))
        assertEquals(emptyList<String>(), facts("ich lerne für die Matheklausur"))
        assertEquals(emptyList<String>(), facts("Lerntipps für Mathe"))
        assertEquals(emptyList<String>(), facts("Wie lerne ich am besten Vokabeln?"))
    }
}
