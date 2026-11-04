package app.hain.feature.news.api

import app.hain.tools.internal.news.Article
import kotlinx.coroutines.CancellationException

/**
 * Asks the local brain once: [system] instructions and the [user] text, thinking off. Returns the
 * raw answer, or `null` when no brain is installed or it could not answer.
 */
fun interface BrainCall {
    suspend fun complete(
        system: String,
        user: String,
    ): String?
}

/** Pim's answer to a question about the open article (T-078). */
sealed interface AskResult {
    data class Answer(
        val text: String,
    ) : AskResult

    data object NoBrain : AskResult

    data object Failed : AskResult
}

/**
 * Questions about the open article to Pim (T-078): the article text goes along as context, cut to
 * what fits next to the question and the answer in the brain's context ([contextTokens]). Thinking
 * off, answer in [lang], only from the article. Nothing is stored; "Im Chat weiterführen" takes it
 * into a normal chat.
 */
class ArticleAsk(
    private val brain: BrainCall,
    private val contextTokens: () -> Int = { DEFAULT_CONTEXT },
) {
    @Suppress("TooGenericExceptionCaught", "SwallowedException", "ReturnCount")
    suspend fun ask(
        article: Article,
        question: String,
        lang: String,
    ): AskResult {
        val q = question.trim().take(MAX_QUESTION)
        if (q.isEmpty()) return AskResult.Failed
        val user = userText(article, q, budgetChars(contextTokens()))
        val raw =
            try {
                brain.complete(systemPrompt(lang), user)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                return AskResult.Failed
            }
        val answer = raw ?: return AskResult.NoBrain
        val clean = answer.substringAfter("</think>").trim()
        return if (clean.isEmpty()) AskResult.Failed else AskResult.Answer(clean)
    }

    companion object {
        const val DEFAULT_CONTEXT = 4096

        /** Tokens kept free for the instructions, the question and Pim's answer. */
        const val RESERVED_TOKENS = 1_100

        /** German and English news text: about 3.5 characters per token on LFM3; a little less to be safe. */
        const val CHARS_PER_TOKEN = 3
        const val MIN_ARTICLE_CHARS = 1_500
        const val MAX_ARTICLE_CHARS = 24_000
        private const val MAX_QUESTION = 500

        /** How many characters of article text fit into a context of [tokens]. */
        fun budgetChars(tokens: Int): Int {
            val chars = (tokens - RESERVED_TOKENS) * CHARS_PER_TOKEN
            return chars.coerceIn(MIN_ARTICLE_CHARS, MAX_ARTICLE_CHARS)
        }

        /** The article text, cut at a paragraph (or sentence) end to at most [budget] characters. */
        fun trimmed(
            text: String,
            budget: Int,
        ): String {
            if (text.length <= budget) return text
            val cut = text.take(budget)
            val atParagraph = cut.lastIndexOf("\n\n").takeIf { it > budget / 2 }
            val atSentence = cut.lastIndexOf(". ").takeIf { it > budget / 2 }?.plus(1)
            return cut.take(atParagraph ?: atSentence ?: budget).trimEnd() + " …"
        }

        fun userText(
            article: Article,
            question: String,
            budget: Int,
        ): String {
            val source = listOfNotNull(article.siteName, article.byline).joinToString(" · ")
            return buildString {
                append(article.title)
                if (source.isNotEmpty()) append("\n").append(source)
                append("\n\n").append(trimmed(article.text, budget))
                append("\n\n---\n").append(question)
            }
        }

        fun systemPrompt(lang: String): String =
            if (lang == "de") {
                "Du bist Pim, ein freundlicher Assistent. Über der Linie steht ein Nachrichtenartikel, darunter " +
                    "eine Frage dazu. Antworte auf Deutsch, kurz und in einfacher Sprache (höchstens 6 Sätze " +
                    "oder 5 Stichpunkte). Nutze nur, was im Artikel steht; steht es nicht drin, sag das ehrlich. " +
                    "Keine eigene Meinung."
            } else {
                "You are Pim, a friendly assistant. Above the line is a news article, below it a question about " +
                    "it. Answer in English, short and in plain words (at most 6 sentences or 5 bullet points). " +
                    "Only use what the article says; if it does not say it, say so honestly. No opinion."
            }

        /** What "Im Chat weiterführen" puts into a new chat: article, link, the question and Pim's answer. */
        fun chatHandOff(
            article: Article,
            question: String,
            answer: String,
            lang: String,
        ): String =
            if (lang == "de") {
                "Zum Artikel „${article.title}“ (${article.url}):\n\nIch habe gefragt: $question\n\n" +
                    "Du hast geantwortet: $answer\n\n"
            } else {
                "About the article “${article.title}” (${article.url}):\n\nI asked: $question\n\n" +
                    "You answered: $answer\n\n"
            }
    }
}
