package app.hain.feature.chat.internal

import org.junit.Assert.assertEquals
import org.junit.Test

/** 4b-2: after a phone action Pim says "vorbereitet", never "eingestellt". */
class ActionWordingTest {
    @Test
    fun germanClaimsBecomePrepared() {
        assertEquals(
            "Ich habe deinen Wecker für 6:30 vorbereitet.",
            ActionWording.clean("Ich habe deinen Wecker für 6:30 eingestellt."),
        )
        assertEquals("Der Termin ist vorbereitet.", ActionWording.clean("Der Termin ist eingetragen."))
        assertEquals("Timer vorbereitet!", ActionWording.clean("Timer gestartet!"))
        assertEquals("Vorbereitet: Wecker um 7.", ActionWording.clean("Eingestellt: Wecker um 7."))
        // a longer word that only ends like a claim stays
        assertEquals("Das Brot ist hergestellt.", ActionWording.clean("Das Brot ist hergestellt."))
    }

    @Test
    fun englishClaimsBecomePrepared() {
        assertEquals("Your alarm is prepared for 6:30.", ActionWording.clean("Your alarm is set for 6:30."))
        assertEquals("I've prepared a timer.", ActionWording.clean("I've started a timer."))
        assertEquals("It's now prepared.", ActionWording.clean("It's now set."))
        // no helper verb: left alone
        assertEquals("A set of three alarms.", ActionWording.clean("A set of three alarms."))
    }

    @Test
    fun codeAndOtherTextStay() {
        val text = "Tipp auf Stellen.\n```\nalarm gestellt\n```\n`is set`"
        assertEquals(text, ActionWording.clean(text))
        assertEquals("Hallo, alles gut?", ActionWording.clean("Hallo, alles gut?"))
    }
}
