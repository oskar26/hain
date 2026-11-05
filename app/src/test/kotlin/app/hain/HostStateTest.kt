package app.hain

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import app.hain.common.api.AnswerLanguage
import app.hain.common.api.HainSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class HostStateTest {
    /** Like Android after turning the phone: a new activity, but the same view model store. */
    private fun hostOf(store: ViewModelStore): HostState =
        ViewModelProvider(store, ViewModelProvider.NewInstanceFactory())[HostState::class.java]

    @Test
    fun aNewActivityGetsTheSameHost() {
        val store = ViewModelStore()
        assertSame(hostOf(store), hostOf(store))
    }

    @Test
    fun aLambdaFromBeforeTurningReadsTheNewLanguage() {
        val store = ViewModelStore()
        // the news view model is built by the first activity …
        val first = hostOf(store)
        first.systemLanguage = "de"
        first.settings = HainSettings(answerLanguage = AnswerLanguage.AUTO)
        val lang: () -> String = { first.toolLanguage() }
        assertEquals("de", lang())
        // … the phone is turned, the new activity writes the newest settings
        val second = hostOf(store)
        second.settings = HainSettings(answerLanguage = AnswerLanguage.EN)
        assertEquals("en", lang())
        // a language change of the phone also reaches the old lambda
        second.settings = HainSettings(answerLanguage = AnswerLanguage.AUTO)
        second.systemLanguage = "en"
        assertEquals("en", lang())
    }

    @Test
    fun thePrivateChatFlagIsSharedToo() {
        val store = ViewModelStore()
        val first = hostOf(store)
        val privateChat: () -> Boolean = { first.privateChatOpen }
        assertFalse(privateChat())
        hostOf(store).privateChatOpen = true
        assertTrue(privateChat())
    }

    @Test
    fun toolLanguageFollowsTheAnswerLanguage() {
        assertEquals("de", HostState.toolLanguage(AnswerLanguage.DE, "en"))
        assertEquals("en", HostState.toolLanguage(AnswerLanguage.EN, "de"))
        assertEquals("de", HostState.toolLanguage(AnswerLanguage.AUTO, "de"))
        assertEquals("en", HostState.toolLanguage(AnswerLanguage.AUTO, "fr"))
        assertEquals("en", HostState.toolLanguage(AnswerLanguage.AUTO, ""))
    }
}
