package app.hain

import androidx.lifecycle.ViewModel
import app.hain.common.api.AnswerLanguage
import app.hain.common.api.HainSettings

/**
 * What the long-lived view models (chat, news) read from the screen around them: the latest
 * settings, whether a private chat is open and the phone's language.
 *
 * The view models outlive the activity: after turning the phone (or a language change) Android
 * makes a new [MainActivity], but keeps the old view models with the lambdas they were built with.
 * Those lambdas used to read fields of the *old* activity, which nobody updated any more – so the
 * news page kept the old answer language, and the chat kept old switches (offline mode, web search)
 * and an old "private chat" flag. This holder is itself a view model, so every activity of the
 * screen gets the same instance and the lambdas always read the newest values.
 */
class HostState : ViewModel() {
    /** Latest settings from DataStore; written by the activity on every change. */
    @Volatile
    var settings: HainSettings = HainSettings()

    /** T-056 × T-112: a private chat is open; the memory then neither reads nor writes. */
    @Volatile
    var privateChatOpen: Boolean = false

    /** Language code of the phone ("de", "en", …) of the newest activity; set in `onCreate`. */
    @Volatile
    var systemLanguage: String = ""

    /** Language for Wikipedia, weather words, news and voice, from the newest values. */
    fun toolLanguage(): String = toolLanguage(settings.answerLanguage, systemLanguage)

    companion object {
        /** The answer language, or the phone's on "Automatisch" (German only for a German phone). */
        fun toolLanguage(
            answerLanguage: AnswerLanguage,
            systemLanguage: String,
        ): String =
            when (answerLanguage) {
                AnswerLanguage.DE -> "de"
                AnswerLanguage.EN -> "en"
                AnswerLanguage.AUTO -> if (systemLanguage == "de") "de" else "en"
            }
    }
}
