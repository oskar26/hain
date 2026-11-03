package app.hain.llm.internal

import app.hain.common.api.AnswerLanguage
import java.util.Locale

/** Language of the system prompt and of Pim's answer (I18N.md, T-080 language fix). */
enum class ReplyLanguage(
    val tag: String,
) {
    DE("de"),
    EN("en"),
    ;

    companion object {
        /** German for a German device, English for every other device language. */
        fun fromLocale(locale: Locale): ReplyLanguage = if (locale.language == DE.tag) DE else EN
    }
}

/**
 * Decides in which language the system prompt is written and in which language Pim answers.
 *
 * A 1.7B model follows the language of its system prompt much more than a rule inside it, so the whole
 * prompt is German when Pim should answer in German. With [AnswerLanguage.AUTO] the prompt follows the
 * device language and a short hint right before the answer names the language of the last message.
 */
object ReplyLanguagePolicy {
    private val WORD = Regex("""\p{L}+(?:'\p{L}+)?""")
    private val GERMAN_LETTERS = Regex("[äöüßÄÖÜ]")
    private const val UMLAUT_WEIGHT = 2

    private val GERMAN =
        (
            "ich du er sie es wir ihr und oder aber nicht kein keine ist bin bist sind war hast habe hat " +
                "kann kannst können soll sollte mir mich dir dich uns euch was wie wo wer warum wieso wann " +
                "welche welcher welches das der die den dem des ein eine einen einem mit für auf aus bei von " +
                "zu zum zur nach über unter noch auch nur schon sehr bitte danke hallo moin servus tschüss " +
                "ja nein gut geht gehts heute morgen gestern erklär erkläre hilf hilfe mal mein meine dein " +
                "deine gibt gib machen mache kochen ich's"
        ).split(" ").toSet()

    private val ENGLISH =
        (
            "i you he she we they and or but not no is am are were have has had can could should " +
                "would my me your our what how where who why when which the a with for on from at of to " +
                "about only very please thanks thank hello yes good today tomorrow yesterday explain " +
                "help do does did don't i'm it's what's you're make cook give tell"
        ).split(" ").toSet()

    /** Language the system prompt is written in. */
    fun promptLanguage(
        setting: AnswerLanguage,
        device: ReplyLanguage,
    ): ReplyLanguage =
        when (setting) {
            AnswerLanguage.DE -> ReplyLanguage.DE
            AnswerLanguage.EN -> ReplyLanguage.EN
            AnswerLanguage.AUTO -> device
        }

    /** Language of the next answer: fixed by the setting, or the last user message, else the device. */
    fun replyLanguage(
        setting: AnswerLanguage,
        lastUserMessage: String?,
        device: ReplyLanguage,
    ): ReplyLanguage =
        when (setting) {
            AnswerLanguage.DE -> ReplyLanguage.DE
            AnswerLanguage.EN -> ReplyLanguage.EN
            AnswerLanguage.AUTO -> detect(lastUserMessage.orEmpty()) ?: device
        }

    /**
     * Tiny German/English guess from stop words (umlauts count double). `null` when there is no clear
     * winner, e.g. for "Hi", "ok" or a name: then the device language decides.
     */
    fun detect(text: String): ReplyLanguage? {
        val words = WORD.findAll(text.lowercase(Locale.ROOT)).map { it.value }.toList()
        val umlauts = if (GERMAN_LETTERS.containsMatchIn(text)) UMLAUT_WEIGHT else 0
        val german = words.count { it in GERMAN } + umlauts
        val english = words.count { it in ENGLISH }
        return when {
            german > english -> ReplyLanguage.DE
            english > german -> ReplyLanguage.EN
            else -> null
        }
    }

    /** Language rule, the last lines of the system prompt (in the prompt's own language). */
    fun rule(
        promptLanguage: ReplyLanguage,
        setting: AnswerLanguage,
    ): String {
        val auto = setting == AnswerLanguage.AUTO
        return when (promptLanguage) {
            ReplyLanguage.DE ->
                if (auto) {
                    "Antworte in der Sprache der letzten Nachricht des Nutzers: Deutsch oder Englisch. " +
                        "Bleib in einer Antwort bei einer Sprache und mische nie."
                } else {
                    "Antworte immer nur auf Deutsch. Misch keine englischen Wörter oder Sätze hinein " +
                        "(auch nicht „Sure“ oder „Okay“)."
                }
            ReplyLanguage.EN ->
                if (auto) {
                    "Answer in the language of the user's last message: English or German. " +
                        "Keep one language per answer, never mix."
                } else {
                    "Always answer in English only. Never mix in German words or sentences."
                }
        }
    }

    /** Short hint sent right before the answer (after the chat, so the cached start stays the same). */
    fun turnHint(language: ReplyLanguage): String =
        when (language) {
            ReplyLanguage.DE -> "Antworte auf Deutsch."
            ReplyLanguage.EN -> "Answer in English."
        }
}
