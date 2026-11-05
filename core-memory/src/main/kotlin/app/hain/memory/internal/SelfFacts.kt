@file:Suppress("MaxLineLength", "MagicNumber", "ReturnCount", "TooManyFunctions") // a table of sentence patterns

package app.hain.memory.internal

import app.hain.memory.api.MemoryCategory
import java.util.Locale

/**
 * T-262 (Oskar 05.11.: the memory kept facts about a person he only asked about, and "ich bin neu
 * nach Köln gezogen" did not become "wohnt in Köln"): what a user message says about the user.
 *
 * - [selfSentences]: the sentences of user messages in which the user states something about
 *   themselves in the first person ("ich wohne …", "mein Bruder heißt …", "I moved to …"). Questions,
 *   requests to Pim ("erklär mir …", "ich will wissen, wer …") and Pim's own answers never count, so
 *   facts about a topic or a third person the user only asked about cannot come from them.
 * - [ruleFacts]: the facts that plain patterns find in such a sentence without the brain (where the
 *   user lives or comes from, name, age, school class, job, pets, family names, diet, allergies,
 *   likes). These are reliable and immediate; the brain only adds what the patterns miss.
 * - [grounded]: whether a fact the brain found is backed by the user's own sentences.
 *
 * Pure Kotlin, no Android, so it is tested with positive and negative examples in German and English.
 */
object SelfFacts {
    /** A fact found by a pattern, before it is checked against what is kept. */
    data class Found(
        val text: String,
        val category: MemoryCategory,
        val sensitive: Boolean = false,
        /** Which pattern found it; a newer statement of the same kind replaces an older one. */
        val kind: Int = 0,
    )

    /** The user's own first-person statements in [userTexts] (oldest first), one per sentence. */
    fun selfSentences(userTexts: List<String>): List<String> =
        userTexts.flatMap { sentences(it) }.flatMap { clauses(it) }.filter { isSelfStatement(it) }

    /**
     * A sentence that ends with "?" in clauses: "I just moved to Bristol, any tips?" has the statement
     * "I just moved to Bristol" before the question. Other sentences stay whole.
     */
    private fun clauses(sentence: String): List<String> {
        if (!sentence.trimEnd().endsWith("?") || ',' !in sentence) return listOf(sentence)
        val parts = sentence.split(',').map { it.trim() }.filter { it.isNotEmpty() }
        return parts.dropLast(1) + parts.last()
    }

    /** True when [sentence] states something about the user (first person, no question, no request). */
    fun isSelfStatement(sentence: String): Boolean {
        val s = normal(sentence)
        if (s.isEmpty() || s.endsWith("?")) return false
        val words = WORD.findAll(s).map { it.value }.toList()
        if (words.isEmpty() || words.none { it in FIRST_PERSON }) return false
        if (words.first() in QUESTION_START || words.first() in REQUEST_START) return false
        if (REQUEST.containsMatchIn(s) || HYPOTHETICAL.containsMatchIn(s)) return false
        return true
    }

    /** Facts the patterns find in the self statements of [userTexts], at most one per kind of fact. */
    fun ruleFacts(userTexts: List<String>): List<Found> =
        selfSentences(userTexts)
            .flatMap { sentence -> rules.mapIndexedNotNull { i, rule -> rule.match(sentence)?.copy(kind = kindOf(i)) } }
            .reversed() // the newest statement wins ("wohne in Köln" … "bin nach Bonn gezogen")
            .distinctBy { it.kind }
            .reversed()

    /** The first rules say where the user lives (one kind, the newest wins) or comes from (another). */
    private fun kindOf(rule: Int): Int =
        when (rule) {
            in ORIGIN_RULES -> KIND_ORIGIN
            in 0..LAST_PLACE_RULE -> KIND_HOME
            else -> rule + KIND_OTHER
        }

    /**
     * Whether the brain's fact [text] is backed by the user's own [selfSentences]: most of its content
     * words (not the fact verbs like "wohnt", "mag", "heißt") appear there. A fact about a topic of a
     * question ("Goethe schrieb den Faust") has none of its words in the user's statements.
     */
    fun grounded(
        text: String,
        selfSentences: List<String>,
    ): Boolean {
        val content = Ranking.tokens(text).filter { it.length >= MIN_WORD && it !in FACT_WORDS }
        if (content.isEmpty()) return false
        val said = selfSentences.flatMap { Ranking.tokens(it) }.toSet()
        val hits = content.count { w -> said.any { Ranking.matches(w, it) || Ranking.matches(it, w) } }
        return hits * 2 >= content.size
    }

    /** What a fact text is about, for facts that hold one value at a time ("wohnt in …"); `null` = other. */
    enum class Slot { HOME, ORIGIN, NAME, AGE, SCHOOL, JOB }

    /** The [Slot] of a fact text: "wohnt in Köln" → HOME, "ist 15 Jahre alt" → AGE. */
    fun slot(text: String): Slot? {
        val t = normal(text)
        return SLOTS.firstOrNull { (_, re) -> re.containsMatchIn(t) }?.first
    }

    /** The place of the newest home fact ("wohnt in Köln" → "Köln"), or `null`. */
    fun homePlace(texts: List<String>): String? =
        texts.firstNotNullOfOrNull { t ->
            HOME_VALUE
                .find(t.trim())
                ?.groupValues
                ?.get(1)
                ?.trim()
                ?.trimEnd('.', '!')
                ?.takeIf { it.isNotBlank() }
        }

    private val SLOTS =
        listOf(
            Slot.HOME to Regex("^(?:wohnt|lebt|lives) (?:jetzt |now )?in "),
            Slot.ORIGIN to Regex("^(?:kommt aus|is from|stammt aus) "),
            Slot.NAME to Regex("^(?:heißt|is called|name is) \\S+$"),
            Slot.AGE to Regex("^(?:ist|is) \\d{1,2} (?:jahre|years) (?:alt|old)$"),
            Slot.SCHOOL to Regex("^(?:geht in die|is in (?:year|grade)) "),
            Slot.JOB to Regex("^(?:arbeitet als|works as) "),
        )
    private val HOME_VALUE = Regex("^(?:wohnt|lebt|lives) (?:jetzt |now )?in (.+)$", RegexOption.IGNORE_CASE)

    /** Sentences of [text]; a "?" stays at the end of its sentence. */
    fun sentences(text: String): List<String> =
        SENTENCE
            .findAll(text.replace('\n', '.'))
            .map { it.value.trim() }
            .filter { it.isNotEmpty() }
            .toList()

    private fun normal(text: String) = text.trim().lowercase(Locale.GERMAN).replace(SPACES, " ")

    /**
     * The sentence with single spaces and its lower-case form: patterns match the lower case, values
     * are cut from the original so "Pilze" keeps its capital. Where lower-casing changes the length
     * (rare letters), the lower case is used for both.
     */
    private fun cased(sentence: String): Pair<String, String> {
        val orig = sentence.trim().replace(SPACES, " ")
        val low = orig.lowercase(Locale.GERMAN)
        return (if (orig.length == low.length) orig else low) to low
    }

    private interface RuleLike {
        fun match(sentence: String): Found?
    }

    private class Rule(
        val pattern: Regex,
        val category: MemoryCategory,
        val sensitive: Boolean = false,
        val text: (String) -> String?,
    ) : RuleLike {
        override fun match(sentence: String): Found? {
            val (orig, low) = cased(sentence)
            val m = pattern.find(low) ?: return null
            val group = m.groups.drop(1).lastOrNull { it != null && it.value.isNotBlank() } ?: return null
            return text(orig.substring(group.range).trim())?.let { Found(it, category, sensitive) }
        }
    }

    // ---- places and names ----------------------------------------------------------------------

    /** "köln", "frankfurt am main", "st. pauli" → "Köln", "Frankfurt am Main"; `null` for "einer WG". */
    internal fun place(raw: String): String? {
        val words =
            raw
                .split(' ')
                .map { it.trim(',', '!', ';', ':') }
                .filter { it.isNotBlank() }
                .takeWhile { it.lowercase(Locale.GERMAN) !in PLACE_STOP }
                .take(MAX_PLACE_WORDS)
        if (words.isEmpty() || words.first().lowercase(Locale.GERMAN) in NOT_A_PLACE) return null
        val cut = words.dropLastWhile { it.lowercase(Locale.GERMAN) in PLACE_JOINERS }
        if (cut.isEmpty()) return null
        return cut.joinToString(" ") { w -> if (w.lowercase(Locale.GERMAN) in PLACE_JOINERS) w else capitalize(w) }.trimEnd('.')
    }

    private fun name(raw: String): String? {
        val w = raw.split(' ').first().trim('.', ',', '!')
        val low = w.lowercase(Locale.GERMAN)
        if (w.length < 2 || low in NOT_A_PLACE || low in PLACE_STOP) return null
        return capitalize(w)
    }

    private fun capitalize(w: String) = w.split('-').joinToString("-") { part -> part.replaceFirstChar { it.titlecase(Locale.GERMAN) } }

    /** What someone learns, one word ("Spanisch", "Gitarre"); `null` for "für", "heute", "to" …. */
    private fun learned(raw: String): String? {
        val word = raw.trim().trimEnd('.', ',', '!')
        if (word.length < MIN_WORD || word.lowercase(Locale.GERMAN) in LEARN_STOP) return null
        return capitalize(word)
    }

    private val LEARN_STOP =
        setOf(
            "für",
            "fuer",
            "heute",
            "morgen",
            "noch",
            "mit",
            "zu",
            "so",
            "viel",
            "gern",
            "gerne",
            "das",
            "es",
            "auf",
            "dass",
            "wie",
            "was",
            "alles",
            "nichts",
            "immer",
            "nie",
            "to",
            "for",
            "how",
            "about",
            "the",
            "a",
            "and",
        )

    private fun thing(raw: String): String? {
        val t =
            raw
                .trim()
                .trimEnd('.', ',', '!')
                .split(' ')
                .take(MAX_THING_WORDS)
                .joinToString(" ")
        return t.takeIf { it.length >= MIN_WORD && t.split(' ').first().lowercase(Locale.GERMAN) !in THING_STOP }
    }

    private const val PLACE = "([\\p{L}][\\p{L}.\\-]*(?: [\\p{L}][\\p{L}.\\-]*){0,3})"
    private const val ICH = "(?:\\bich |\\bund |^)"
    private const val LAST_PLACE_RULE = 7
    private val ORIGIN_RULES = setOf(3, 7)
    private const val KIND_HOME = -2
    private const val KIND_ORIGIN = -1
    private const val KIND_OTHER = 0
    private const val ADV =
        "(?:(?:gerade|neu|jetzt|erst|vor kurzem|kürzlich|letzte woche|letztes jahr|gestern|schon|auch|hier|" +
            "seit \\S+(?: \\S+)?) )*"

    /** "in einer WG in Bonn": the kind of home before the place is skipped. */
    private const val FLAT = "(?:(?:in|im) (?:einer|einem|meiner|meinem) [\\p{L}\\-]+ )?"
    private const val MAX_PLACE_WORDS = 3
    private const val MAX_THING_WORDS = 5
    private const val MIN_WORD = 3

    private val rules: List<RuleLike> by lazy {
        listOf(
            // where the user lives (DE)
            Rule(Regex("$ICH(?:wohne|lebe) $ADV$FLAT(?:in|im) $PLACE"), MemoryCategory.PLACE) { p ->
                place(p)?.let { "wohnt in $it" }
            },
            Rule(Regex("$ICH?(?:bin|sind) (?:ich |wir )?$ADV(?:nach|in) $PLACE (?:um)?gezogen"), MemoryCategory.PLACE) { p ->
                place(p)?.let { "wohnt in $it" }
            },
            Rule(Regex("\\b(?:bin|sind) (?:ganz |noch |gerade )?neu (?:in|hier in) $PLACE"), MemoryCategory.PLACE) { p ->
                place(p)?.let { "wohnt in $it" }
            },
            Rule(Regex("${ICH}komme (?:ursprünglich |eigentlich )?aus $PLACE"), MemoryCategory.PLACE) { p ->
                place(p)?.let { "kommt aus $it" }
            },
            // where the user lives (EN)
            Rule(Regex("\\bi (?:live|am living|'m living) $ADV(?:in) $PLACE"), MemoryCategory.PLACE) { p ->
                place(p)?.let { "lives in $it" }
            },
            Rule(Regex("\\bi(?:'ve| have)? (?:just |recently |now )?moved to $PLACE"), MemoryCategory.PLACE) { p ->
                place(p)?.let { "lives in $it" }
            },
            Rule(Regex("\\bi(?:'m| am) (?:new|just new) (?:to|in) $PLACE"), MemoryCategory.PLACE) { p ->
                place(p)?.let { "lives in $it" }
            },
            Rule(Regex("\\bi(?:'m| am) (?:originally )?from $PLACE"), MemoryCategory.PLACE) { p -> place(p)?.let { "is from $it" } },
            // name and age
            Rule(Regex("${ICH}heiße (\\p{L}[\\p{L}\\-]+)"), MemoryCategory.OTHER) { n -> name(n)?.let { "heißt $it" } },
            Rule(Regex("\\bmein name ist (\\p{L}[\\p{L}\\-]+)"), MemoryCategory.OTHER) { n -> name(n)?.let { "heißt $it" } },
            Rule(Regex("\\bmy name is (\\p{L}[\\p{L}\\-]+)"), MemoryCategory.OTHER) { n -> name(n)?.let { "is called $it" } },
            Rule(Regex("${ICH}bin (\\d{1,2})(?: (?:jahre|jahr)\\b|[.!]*$)"), MemoryCategory.OTHER) { a -> "ist $a Jahre alt" },
            Rule(Regex("\\bi(?:'m| am) (\\d{1,2}) years? old"), MemoryCategory.OTHER) { a -> "is $a years old" },
            // school and work
            Rule(Regex("$ICH(?:gehe|bin) (?:jetzt )?in die (\\d{1,2})\\.? klasse"), MemoryCategory.WORK) { k ->
                "geht in die $k. Klasse"
            },
            Rule(Regex("\\bi(?:'m| am) in (?:year|grade) (\\d{1,2})"), MemoryCategory.WORK) { k -> "is in year $k" },
            Rule(Regex("${ICH}arbeite (?:jetzt )?als ([\\p{L}\\-]+(?: [\\p{L}\\-]+)?)"), MemoryCategory.WORK) { j ->
                thing(j)?.let { "arbeitet als $it" }
            },
            Rule(Regex("${ICH}studiere ([\\p{L}\\-]+)"), MemoryCategory.WORK) { s -> thing(s)?.let { "studiert $it" } },
            Rule(Regex("\\bi work as (?:an? )?([\\p{L}\\-]+(?: [\\p{L}\\-]+)?)"), MemoryCategory.WORK) { j ->
                thing(j)?.let { "works as $it" }
            },
            Rule(Regex("\\bi study ([\\p{L}\\-]+)"), MemoryCategory.WORK) { s -> thing(s)?.let { "studies $it" } },
            // T-271: "hi ich lerne gerade spanisch" → chip "Merken: lernt Spanisch"
            Rule(Regex("${ICH}lerne $ADV([\\p{L}\\-]+)"), MemoryCategory.OTHER) { l -> learned(l)?.let { "lernt $it" } },
            Rule(Regex("\\bi(?:'m| am) (?:currently |now |just )?learning ([\\p{L}\\-]+)"), MemoryCategory.OTHER) { l ->
                learned(l)?.let { "is learning " + l.trim().trimEnd('.', ',', '!') }
            },
            // family and pets
            familyRule,
            Rule(Regex("${ICH}habe (?:einen|eine|ein|zwei|drei|\\d) ($PETS)"), MemoryCategory.OTHER) { p -> "hat ${pet(p)}" },
            Rule(Regex("\\bi have (?:a|an|two|three|\\d) ($PETS_EN)"), MemoryCategory.OTHER) { p -> "has a $p" },
            // food, health, likes
            Rule(Regex("$ICH(?:bin|esse|lebe) (vegetarisch|vegetarier(?:in)?|vegan|veganer(?:in)?)"), MemoryCategory.PREFERENCE) { v ->
                if (v.startsWith("vegan")) "lebt vegan" else "isst vegetarisch"
            },
            Rule(Regex("\\bi(?:'m| am) (vegetarian|vegan)"), MemoryCategory.PREFERENCE) { v -> "is $v" },
            Rule(Regex("$ICH(?:bin|habe eine) allergi\\S* (?:gegen|auf) (.+)"), MemoryCategory.HEALTH, sensitive = true) { a ->
                thing(a)?.let { "ist allergisch gegen $it" }
            },
            Rule(Regex("\\bi(?:'m| am) allergic to (.+)"), MemoryCategory.HEALTH, sensitive = true) { a ->
                thing(a)?.let { "is allergic to $it" }
            },
            likeRule,
            Rule(Regex("\\bi (?:really )?(?:like|love) (.+?)(?: (?:and|but|because) .*)?$"), MemoryCategory.PREFERENCE) { t ->
                thing(t)?.let { "likes $it" }
            },
            Rule(Regex("\\bi (?:don't|do not|really don't) like (.+?)(?: (?:and|but|because) .*)?$"), MemoryCategory.PREFERENCE) { t ->
                thing(t)?.let { "doesn't like $it" }
            },
        )
    }

    /** "mein Bruder heißt Tim" → "Bruder heißt Tim" (two groups, so its own rule). */
    private val familyRule =
        object : RuleLike {
            private val pattern =
                Regex(
                    "\\bmein(?:e)? (bruder|schwester|mutter|vater|mama|papa|oma|opa|freund|freundin|hund|katze|kater) heißt (\\p{L}+)",
                )

            override fun match(sentence: String): Found? {
                val (orig, low) = cased(sentence)
                val m = pattern.find(low) ?: return null
                val who = capitalize(m.groupValues[1])
                val n = name(orig.substring(m.groups[2]!!.range)) ?: return null
                return Found("$who heißt $n", MemoryCategory.PERSON)
            }
        }

    /** "ich mag keine Pilze" → "mag keine Pilze"; "ich mag es, wenn …" and "ich mag dich" are left out. */
    private val likeRule =
        object : RuleLike {
            private val pattern = Regex("$ICH(mag|liebe) (.+?)(?: (?:und|aber|weil|wenn) .*)?$")

            override fun match(sentence: String): Found? {
                val (orig, low) = cased(sentence)
                val m = pattern.find(low) ?: return null
                val what = thing(orig.substring(m.groups[2]!!.range)) ?: return null
                return Found("${m.groupValues[1]} $what", MemoryCategory.PREFERENCE)
            }
        }

    private const val PETS = "hund|katze|kater|hamster|kaninchen|meerschweinchen|pferd|wellensittich|schildkröte|fische"
    private const val PETS_EN = "dog|cat|hamster|rabbit|guinea pig|horse|budgie|turtle|tortoise"

    private fun pet(word: String): String =
        when (word) {
            "katze", "schildkröte" -> "eine " + capitalize(word)
            "fische" -> "Fische"
            "kaninchen", "meerschweinchen", "pferd" -> "ein " + capitalize(word)
            else -> "einen " + capitalize(word)
        }

    /** Sentences end at . ! ? ; but not at the dot of "9. Klasse", "3. stock" or "z.B.". */
    private val SENTENCE =
        Regex(
            "(?:[^.!?;]|(?<=\\d)\\.(?= ?(?:[a-zäöü]|Klasse|Jahrgang|Semester|Stock|Etage|Lehrjahr|Schuljahr))|" +
                "(?<=\\b\\p{L})\\.(?=\\p{L}))+[.!?;]*",
        )
    private val SPACES = Regex("\\s+")
    private val WORD = Regex("[\\p{L}']+")

    private val FIRST_PERSON =
        setOf(
            "ich",
            "mich",
            "mir",
            "mein",
            "meine",
            "meinen",
            "meinem",
            "meiner",
            "meins",
            "bin",
            "wir",
            "uns",
            "unser",
            "unsere",
            "i",
            "i'm",
            "i've",
            "me",
            "my",
            "mine",
            "we",
            "we've",
            "we're",
            "our",
            "us",
        )

    /** A sentence starting like this is a question, also without "?" (users often leave it out). */
    private val QUESTION_START =
        setOf(
            "wer",
            "was",
            "wie",
            "wo",
            "wann",
            "warum",
            "wieso",
            "weshalb",
            "welche",
            "welcher",
            "welches",
            "welchen",
            "woher",
            "wohin",
            "kannst",
            "könntest",
            "weißt",
            "kennst",
            "hast",
            "bist",
            "ist",
            "gibt",
            "who",
            "what",
            "how",
            "where",
            "when",
            "why",
            "which",
            "can",
            "could",
            "do",
            "does",
            "did",
            "is",
            "are",
            "have",
            "would",
            "should",
        )

    /** Requests to Pim ("erklär mir …", "tell me …"): about a topic, not about the user. */
    private val REQUEST_START =
        setOf(
            "erzähl",
            "erzähle",
            "erklär",
            "erkläre",
            "sag",
            "sage",
            "zeig",
            "zeige",
            "gib",
            "schreib",
            "schreibe",
            "mach",
            "hilf",
            "nenn",
            "nenne",
            "such",
            "suche",
            "find",
            "finde",
            "fasse",
            "fass",
            "übersetze",
            "übersetz",
            "merk",
            "merke",
            "vergiss",
            "tell",
            "explain",
            "show",
            "give",
            "write",
            "help",
            "find",
            "search",
            "list",
            "translate",
            "remember",
            "forget",
            "please",
            "bitte",
        )

    /** "ich frage mich, wer …", "ich will wissen …", "kannst du mir …": a question in disguise. */
    private val REQUEST =
        Regex(
            "\\b(?:frage mich|frag mich|will wissen|möchte wissen|wüsste gern|würde gern wissen|interessiert mich|" +
                "suche (?:nach |ein|eine)|brauche (?:hilfe|ein|eine|einen|infos)|hätte gern|i wonder|want to know|" +
                "would like to know|i need (?:help|a|an|some)|i'm looking for|i am looking for)\\b",
        )

    /** "wenn ich in Paris wohnen würde", "if I lived in …": not a fact. */
    private val HYPOTHETICAL = Regex("\\b(?:wenn ich|falls ich|würde ich|wäre ich|if i|i would|i'd)\\b")

    private val NOT_A_PLACE =
        setOf(
            "ein",
            "eine",
            "einer",
            "einem",
            "einen",
            "der",
            "die",
            "das",
            "dem",
            "den",
            "des",
            "meiner",
            "meinem",
            "meine",
            "unserer",
            "unserem",
            "einer",
            "a",
            "an",
            "the",
            "my",
            "our",
            "dich",
            "dir",
            "mich",
            "mir",
            "es",
            "ihn",
            "sie",
            "hier",
            "dort",
            "da",
        )

    /** Words that end a place name ("nach köln gezogen", "in berlin und …"). */
    private val PLACE_STOP =
        setOf(
            "gezogen",
            "umgezogen",
            "und",
            "aber",
            "oder",
            "was",
            "wie",
            "weil",
            "seit",
            "mit",
            "für",
            "zu",
            "um",
            "so",
            "jetzt",
            "gerade",
            "and",
            "but",
            "or",
            "what",
            "how",
            "since",
            "with",
            "for",
            "to",
            "so",
            "now",
            "because",
            "da",
            "dort",
            "hier",
            "here",
            "there",
            "bin",
            "ist",
            "sind",
            "is",
            "am",
            "are",
            "was",
            "machen",
            "tun",
            "gekommen",
            "zurück",
            "geboren",
            "wohne",
            "lebe",
            "neu",
            "new",
            "last",
            "letzte",
            "letztes",
            "vor",
            "ago",
        ).minus("am")

    /** Small words that stay lower case inside a place name ("Frankfurt am Main"). */
    private val PLACE_JOINERS = setOf("am", "an", "der", "im", "ob", "upon", "on", "de", "la", "le", "bei")

    private val THING_STOP = setOf("es", "das", "dich", "dass", "it", "that", "you", "this", "wenn", "if", "when", "to")

    /** Fact verbs and fillers that say nothing about what the fact is about. */
    private val FACT_WORDS =
        setOf(
            "wohnt",
            "wohnen",
            "lebt",
            "leben",
            "heißt",
            "mag",
            "mögen",
            "liebt",
            "ist",
            "hat",
            "geht",
            "arbeitet",
            "kommt",
            "aus",
            "keine",
            "kein",
            "nicht",
            "gern",
            "gerne",
            "sehr",
            "jahre",
            "alt",
            "lives",
            "likes",
            "loves",
            "has",
            "is",
            "works",
            "goes",
            "from",
            "called",
            "doesn't",
            "years",
            "old",
            "user",
            "nutzer",
            "nutzerin",
            "the",
            "der",
            "die",
            "das",
            "und",
            "and",
        )
}
