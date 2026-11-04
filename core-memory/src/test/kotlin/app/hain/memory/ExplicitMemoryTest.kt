package app.hain.memory

import app.hain.memory.internal.ExplicitMemory
import app.hain.memory.internal.ExplicitMemory.Command.Forget
import app.hain.memory.internal.ExplicitMemory.Command.Remember
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExplicitMemoryTest {
    private fun parse(message: String) = ExplicitMemory.parse(message)

    @Test
    fun germanRemember() {
        assertEquals(Remember("ich keine Pilze mag"), parse("Merk dir, dass ich keine Pilze mag"))
        assertEquals(Remember("ich habe montags Training"), parse("merke dir: ich habe montags Training."))
        assertEquals(
            Remember("meine Schwester Rosa heißt"),
            parse("Pim, merk dir bitte, dass meine Schwester Rosa heißt!"),
        )
        assertEquals(Remember("ich Vegetarier bin"), parse("Kannst du dir merken, dass ich Vegetarier bin?"))
        assertEquals(Remember("ich morgen Mathe habe"), parse("Vergiss nicht, dass ich morgen Mathe habe"))
    }

    @Test
    fun englishRemember() {
        assertEquals(Remember("I play guitar"), parse("Remember that I play guitar."))
        assertEquals(Remember("my birthday is on 26 March"), parse("please remember my birthday is on 26 March"))
        assertEquals(Remember("I am allergic to nuts"), parse("Don't forget that I am allergic to nuts"))
        assertEquals(Remember("I live in Cologne"), parse("Can you remember that I live in Cologne?"))
    }

    @Test
    fun forget() {
        assertEquals(Forget("ich keine Pilze mag"), parse("Vergiss, dass ich keine Pilze mag"))
        assertEquals(Forget("my birthday"), parse("Forget my birthday please"))
    }

    @Test
    fun normalMessagesAndVagueCommandsAreNoCommand() {
        listOf(
            "Wie geht es dir?",
            "Ich habe vergessen, was wir gemacht haben",
            "Merk dir das",
            "vergiss es",
            "Forget it",
            "Remember when we talked about Rome?",
            "Kannst du dir merken, was ich gesagt habe?",
            "Rememberable things are rare",
            "Was soll ich mir merken?",
        ).forEach { assertNull(it, parse(it)) }
    }

    @Test
    fun longFactsAreCutAtAWord() {
        val long = "Merk dir: " + "Wort ".repeat(80)
        val text = (parse(long) as Remember).text
        assertTrue(text.length <= 200)
        assertTrue(text.endsWith("…"))
    }
}
