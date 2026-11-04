package app.hain.tools.api

import app.hain.llm.internal.AnswerMode

/**
 * How much research one answer may do (T-069), chosen by the same setting as the thinking choice
 * (Schnell / Auto / Gründlich). The clock starts with the first tool call; when [timeMs] is used up,
 * Pim stops looking and answers at once with what it collected.
 *
 * @property maxRounds tool rounds (the brain asks for tools, gets results) before it must answer.
 * @property maxSearches web searches per answer (a Wikipedia fallback to the web counts as one).
 * @property pagesPerSearch pages read after one search, each from a different site.
 * @property maxPages pages read in the whole answer.
 * @property timeMs research time, counted from the first tool call.
 * @property callsPerRound tool calls of one round; more than one run at the same time.
 * @property charsPerPage the most relevant text kept per page (≈ 4 characters per token).
 * @property liveNotes show the running notes ("6 facts from 4 sources") in the timeline.
 */
data class ResearchBudget(
    val maxRounds: Int,
    val maxSearches: Int,
    val pagesPerSearch: Int,
    val maxPages: Int,
    val timeMs: Long,
    val callsPerRound: Int,
    val charsPerPage: Int,
    val liveNotes: Boolean = false,
) {
    /** Pages the next search may still read when [alreadyRead] pages were read in this answer. */
    fun pagesFor(alreadyRead: Int): Int = minOf(pagesPerSearch, maxPages - alreadyRead).coerceAtLeast(0)

    /** `true` once the research time is used up; [startedAt] `null` = no tool ran yet. */
    fun timeUp(
        startedAt: Long?,
        now: Long,
    ): Boolean = startedAt != null && now - startedAt >= timeMs

    /** Research time left, never below zero; the whole budget while no tool ran yet. */
    fun remaining(
        startedAt: Long?,
        now: Long,
    ): Long = if (startedAt == null) timeMs else (timeMs - (now - startedAt)).coerceAtLeast(0L)

    companion object {
        /** Schnell: one search, one page. */
        val FAST =
            ResearchBudget(
                maxRounds = 2,
                maxSearches = 1,
                pagesPerSearch = 1,
                maxPages = 1,
                timeMs = 15_000L,
                callsPerRound = 1,
                charsPerPage = 1_000,
            )

        /** Auto: up to 3 rounds, 2 searches, 3 pages, about 20 seconds. */
        val AUTO =
            ResearchBudget(
                maxRounds = 3,
                maxSearches = 2,
                pagesPerSearch = 3,
                maxPages = 3,
                timeMs = 20_000L,
                callsPerRound = 2,
                charsPerPage = 800,
            )

        /**
         * Gründlich: one minute (Oskar: „eine Minute“), up to 8 rounds, several searches (reworded,
         * German and English), up to 3 tools at once, 5 pages per search and 8 in total.
         */
        val THOROUGH =
            ResearchBudget(
                maxRounds = 8,
                maxSearches = 6,
                pagesPerSearch = 5,
                maxPages = 8,
                timeMs = 60_000L,
                callsPerRound = 3,
                charsPerPage = 700,
                liveNotes = true,
            )

        fun of(mode: AnswerMode): ResearchBudget =
            when (mode) {
                AnswerMode.FAST -> FAST
                AnswerMode.AUTO -> AUTO
                AnswerMode.THOROUGH -> THOROUGH
            }
    }
}
