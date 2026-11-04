package app.hain.feature.chat.api

import org.junit.Assert.assertEquals
import org.junit.Test

class ChatVoiceUiTest {
    @Test
    fun dictationFillsAnEmptyBox() {
        assertEquals("Hallo Pim", appendDictation("", " Hallo Pim "))
        assertEquals("Hallo Pim", appendDictation("   ", "Hallo Pim"))
    }

    @Test
    fun dictationGoesBehindTheTextWithOneSpace() {
        assertEquals("Wie wird das Wetter morgen", appendDictation("Wie wird", "das Wetter morgen"))
        assertEquals("Erst das. Dann das", appendDictation("Erst das. ", "Dann das"))
    }

    @Test
    fun emptyDictationKeepsTheBox() {
        assertEquals("Text", appendDictation("Text", "  "))
    }

    @Test
    fun clockShowsMinutesAndSeconds() {
        assertEquals("0:00", dictationClock(0))
        assertEquals("0:07", dictationClock(7_400))
        assertEquals("1:00", dictationClock(60_000))
        assertEquals("0:00", dictationClock(-5))
    }
}
