package app.hain.llm.internal

import app.hain.common.api.AnswerLanguage
import java.util.Locale

/** Language the system prompt is written in (there are prompts in German and English only). */
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
 * A language Pim can be asked to answer in: an ISO 639-1 [tag] plus its name in German and English, so
 * the hint before the answer can name it in the prompt's own language ("Antworte auf Französisch.").
 */
data class MessageLanguage(
    val tag: String,
    val nameDe: String,
    val nameEn: String,
) {
    /** The prompt language that fits best: German for German, English for every other language. */
    val prompt: ReplyLanguage get() = if (tag == ReplyLanguage.DE.tag) ReplyLanguage.DE else ReplyLanguage.EN

    companion object {
        val GERMAN = MessageLanguage("de", "Deutsch", "German")
        val ENGLISH = MessageLanguage("en", "Englisch", "English")

        fun of(language: ReplyLanguage): MessageLanguage = if (language == ReplyLanguage.DE) GERMAN else ENGLISH

        /** Name of the device language via the JDK, e.g. "fr" → Französisch / French. */
        fun fromLocale(locale: Locale): MessageLanguage =
            when (locale.language) {
                "", ENGLISH.tag -> ENGLISH
                GERMAN.tag -> GERMAN
                else ->
                    MessageLanguage(
                        locale.language,
                        locale.getDisplayLanguage(Locale.GERMAN),
                        locale.getDisplayLanguage(Locale.ENGLISH),
                    )
            }
    }
}

/**
 * Decides in which language Pim answers (I18N.md, T-080).
 *
 * Rule (Oskar, 03.11.2026): Pim answers in the language of the user's message, whatever language it is,
 * so everyone can use Hain. A 1.7B model follows the language of its system prompt more than a rule in
 * it, so (1) the prompt is German or English, (2) a short hint right before the answer names the
 * language of the last message ("Answer in Turkish."). A fixed setting (DE or EN) always wins.
 */
object ReplyLanguagePolicy {
    private val WORD = Regex("""\p{L}+(?:'\p{L}+)?""")
    private const val MARK_WEIGHT = 2

    private class Profile(
        val language: MessageLanguage,
        words: String,
        marks: String = "",
    ) {
        val words = words.split(" ").toSet()
        val marks = if (marks.isEmpty()) null else Regex("[$marks]")
    }

    private val PROFILES =
        listOf(
            Profile(
                MessageLanguage.GERMAN,
                "ich du er sie es wir ihr und oder aber nicht kein keine ist bin bist sind war hast habe hat " +
                    "kann kannst können soll sollte mir mich dir dich uns euch was wie wo wer warum wieso wann " +
                    "welche welcher welches das der die den dem des ein eine einen einem mit für auf aus bei von " +
                    "zu zum zur nach über unter noch auch nur schon sehr bitte danke hallo moin servus tschüss " +
                    "ja nein gut geht gehts heute morgen gestern erklär erkläre hilf hilfe mal mein meine dein " +
                    "deine gibt gib machen mache kochen ich's",
                "äöüßÄÖÜ",
            ),
            Profile(
                MessageLanguage.ENGLISH,
                "i you he she we they and or but not no is am are were have has had can could should " +
                    "would my me your our what how where who why when which the a with for on from at of to " +
                    "about only very please thanks thank hello yes good today tomorrow yesterday explain " +
                    "help do does did don't i'm it's what's you're make cook give tell",
            ),
            Profile(
                MessageLanguage("fr", "Französisch", "French"),
                "je tu il elle nous vous ils elles et ou mais pas ne est suis es sont ai as avez peux pouvez " +
                    "quoi comment où qui pourquoi quand quel quelle le la les un une des du au avec pour sur dans " +
                    "très merci bonjour salut oui non bien ça va moi toi mon ma mes ton ta c'est qu'est",
                "çéèêëàâîïôûœÇÉÈÊ",
            ),
            Profile(
                MessageLanguage("es", "Spanisch", "Spanish"),
                "yo tú él ella nosotros ellos y o pero no es soy eres son está estás estoy tengo tienes puedo " +
                    "puedes qué cómo dónde quién por qué cuándo cual el la los las un una unos con para en muy " +
                    "gracias hola sí bien mi tu me te hoy mañana",
                "ñ¿¡áíóúÑ",
            ),
            Profile(
                MessageLanguage("it", "Italienisch", "Italian"),
                "io tu lui lei noi voi loro e o ma non è sono sei ho hai posso puoi cosa come dove chi perché " +
                    "quando quale il lo la gli le un una con per su in molto grazie ciao sì bene mio mia ti mi " +
                    "oggi domani stai sta",
                "àèéìòù",
            ),
            Profile(
                MessageLanguage("nl", "Niederländisch", "Dutch"),
                "ik jij je hij zij wij jullie en of maar niet geen is ben bent zijn heb hebt heeft kan kun kunt " +
                    "wat hoe waar wie waarom wanneer welke de het een met voor op uit bij van naar ook nog heel " +
                    "dank bedankt hoi ja nee goed gaat vandaag morgen mij mijn jouw",
            ),
            Profile(
                MessageLanguage("pt", "Portugiesisch", "Portuguese"),
                "eu você ele ela nós eles e ou mas não é sou são está estou tenho tem posso pode que como onde " +
                    "quem porque quando qual o a os as um uma com para em muito obrigado obrigada olá oi sim bem " +
                    "meu minha hoje amanhã tudo",
                "ãõçÃÕ",
            ),
            Profile(
                MessageLanguage("tr", "Türkisch", "Turkish"),
                "ben sen o biz siz onlar ve veya ama değil var yok mı mi mu mü ne nasıl nerede kim neden niçin " +
                    "zaman bir bu şu için ile çok teşekkürler teşekkür merhaba selam evet hayır iyi bugün yarın " +
                    "nasılsın",
                "ıİşŞğĞ",
            ),
            Profile(
                MessageLanguage("pl", "Polnisch", "Polish"),
                "ja ty on ona my wy oni i lub ale nie jest jestem jesteś są mam masz mogę możesz co jak gdzie " +
                    "kto dlaczego kiedy który w z na do od dla bardzo dziękuję cześć tak dobrze mój moja dzisiaj " +
                    "jutro się",
                "łąęśźżńćŁŚŻ",
            ),
        )

    /** Scripts that name their language on their own (checked before the word lists). */
    private val SCRIPTS =
        listOf(
            Regex("[\\u3040-\\u30FF]") to MessageLanguage("ja", "Japanisch", "Japanese"),
            Regex("[\\uAC00-\\uD7AF]") to MessageLanguage("ko", "Koreanisch", "Korean"),
            Regex("[\\u4E00-\\u9FFF]") to MessageLanguage("zh", "Chinesisch", "Chinese"),
            Regex("[іїєґІЇЄҐ]") to MessageLanguage("uk", "Ukrainisch", "Ukrainian"),
            Regex("[\\u0400-\\u04FF]") to MessageLanguage("ru", "Russisch", "Russian"),
            Regex("[\\u0600-\\u06FF]") to MessageLanguage("ar", "Arabisch", "Arabic"),
            Regex("[\\u0590-\\u05FF]") to MessageLanguage("he", "Hebräisch", "Hebrew"),
            Regex("[\\u0370-\\u03FF]") to MessageLanguage("el", "Griechisch", "Greek"),
            Regex("[\\u0900-\\u097F]") to MessageLanguage("hi", "Hindi", "Hindi"),
            Regex("[\\u0E00-\\u0E7F]") to MessageLanguage("th", "Thailändisch", "Thai"),
        )

    /**
     * Language of the system prompt: fixed by the setting, else the device language. It does not follow
     * each message, so the cached prompt start stays valid; the hint names the answer's language (host
     * test 03.11.2026: a German or English prompt plus "Answer in Turkish." answers in Turkish).
     */
    fun promptLanguage(
        setting: AnswerLanguage,
        device: ReplyLanguage,
    ): ReplyLanguage =
        when (setting) {
            AnswerLanguage.DE -> ReplyLanguage.DE
            AnswerLanguage.EN -> ReplyLanguage.EN
            AnswerLanguage.AUTO -> device
        }

    /**
     * Language of the next answer: fixed by the setting, else the newest user message whose language is
     * clear (short ones like "Hi" or "ok" keep the language of the chat), else the device language.
     */
    fun replyLanguage(
        setting: AnswerLanguage,
        userMessages: List<String>,
        device: MessageLanguage,
    ): MessageLanguage =
        when (setting) {
            AnswerLanguage.DE -> MessageLanguage.GERMAN
            AnswerLanguage.EN -> MessageLanguage.ENGLISH
            AnswerLanguage.AUTO -> userMessages.asReversed().firstNotNullOfOrNull { detectLanguage(it) } ?: device
        }

    /** German/English only (kept for callers that only need the prompt language). */
    fun detect(text: String): ReplyLanguage? = detectLanguage(text)?.takeIf { it.tag == "de" || it.tag == "en" }?.prompt

    /**
     * Small offline guess: first the script (Cyrillic, Arabic, CJK, …), then stop words and special
     * letters for German, English, French, Spanish, Italian, Dutch, Portuguese, Turkish and Polish.
     * `null` when there is no clear winner, e.g. for "Hi", "ok" or a name.
     */
    fun detectLanguage(text: String): MessageLanguage? {
        SCRIPTS.firstOrNull { (script, _) -> script.containsMatchIn(text) }?.let { return it.second }
        val words = WORD.findAll(text.lowercase(Locale.ROOT)).map { it.value }.toList()
        val scores =
            PROFILES
                .map { p ->
                    val marks = if (p.marks?.containsMatchIn(text) == true) MARK_WEIGHT else 0
                    p.language to words.count { it in p.words } + marks
                }.sortedByDescending { it.second }
        val (best, top) = scores[0]
        return if (top > 0 && top > scores[1].second) best else null
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
                    "Antworte immer in der Sprache, in der der Nutzer zuletzt geschrieben hat, egal welche " +
                        "Sprache. Bleib in einer Antwort bei einer Sprache und mische nie."
                } else {
                    "Antworte immer nur auf Deutsch. Misch keine englischen Wörter oder Sätze hinein " +
                        "(auch nicht „Sure“ oder „Okay“)."
                }
            ReplyLanguage.EN ->
                if (auto) {
                    "Always answer in the language the user last wrote in, whatever language it is. " +
                        "Keep one language per answer, never mix."
                } else {
                    "Always answer in English only. Never mix in German words or sentences."
                }
        }
    }

    /** Short hint sent right before the answer (after the chat, so the cached start stays the same). */
    fun turnHint(
        promptLanguage: ReplyLanguage,
        target: MessageLanguage,
    ): String =
        when (promptLanguage) {
            ReplyLanguage.DE -> "Antworte auf ${target.nameDe}."
            ReplyLanguage.EN -> "Answer in ${target.nameEn}."
        }
}
