package app.hain.feature.news.api

import app.hain.tools.internal.news.NewsItem
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

/** T-079: picking the day's stories, the once-a-day cache, "Neu erzeugen" and the brain's sentences. */
class BriefingTest {
    private val zone = ZoneId.of("Europe/Berlin")
    private var now = Instant.parse("2026-11-04T17:00:00Z").toEpochMilli()

    private fun item(
        feed: String,
        title: String,
        hoursAgo: Double,
        teaser: String = "",
    ) = NewsItem(
        feedId = feed,
        source = feed,
        title = title,
        link = "https://example.org/$feed/${title.hashCode()}",
        guid = "$feed/$title",
        published = now - (hoursAgo * HOUR).toLong(),
        fetchedAt = now,
        summary = teaser,
    )

    private val items =
        listOf(
            item("tagesschau", "Demokraten gewinnen Zwischenwahlen in den USA", 2.0, "Trump verliert den Kongress."),
            item("bbc", "Democrats win US midterm elections", 3.0, "Trump loses control of Congress."),
            item("jungewelt", "Zwischenwahlen in den USA: Trump verliert", 4.0, "Niederlage für den Präsidenten."),
            item("dlf", "Zwischenwahlen USA: Demokraten gewinnen Kongress", 1.0, "Die Demokraten holen die Mehrheit."),
            item("taz", "Streik bei Ford im Werk", 5.0, "Die Beschäftigten legen die Arbeit nieder."),
            item("nd", "Streik bei Ford: Beschäftigte legen Arbeit nieder", 6.0, "Gewerkschaft fordert mehr Schutz."),
            item("guardian", "Heatwave records tumble in southern Europe", 7.0, "Scientists see the climate crisis."),
            item("zdf", "Bahn erhöht die Preise im Fernverkehr", 8.0, "Tickets werden im Dezember teurer."),
            item("spiegel", "Neue Regeln für E-Scooter in Städten", 9.0, "Kommunen dürfen Zonen festlegen."),
            item("npr", "NASA delays moon mission again", 10.0, "The launch moves to next year."),
            item("aje", "Floods hit Pakistan after monsoon rain", 30.0, "Old news from yesterday."),
        )

    @Test fun storiesReportedByManySourcesComeFirst() {
        val stories = BriefingPicker.stories(items, now)
        assertTrue(stories.size in BriefingPicker.MIN_POINTS..BriefingPicker.MAX_POINTS)
        val first = stories.first()
        assertTrue(first.sources.size >= 2)
        assertTrue(first.lead.title.contains("Zwischenwahlen") || first.lead.title.contains("midterm"))
        // the Ford strike from taz and nd is one story with two sources
        val ford = stories.first { "Ford" in it.lead.title }
        assertEquals(setOf("taz", "nd"), ford.sources.toSet())
        // older than 24 hours is left out when there is enough news of today
        assertTrue(stories.none { it.lead.feedId == "aje" })
    }

    @Test fun firstSentenceOrHeadline() {
        val a = item("x", "Titel", 1.0, "Erster Satz. Zweiter Satz.")
        assertEquals("Erster Satz.", BriefingPicker.firstSentence(a))
        assertEquals("Titel", BriefingPicker.firstSentence(item("x", "Titel", 1.0)))
    }

    @Test fun madeOnceADayAndKept() =
        runTest {
            var calls = 0
            val maker =
                BriefingMaker(
                    MemoryBriefingStore(),
                    brain =
                        BrainCall { _, user ->
                            calls++
                            user.lines().mapIndexed { i, _ -> "${i + 1}. Satz ${i + 1}" }.joinToString("\n")
                        },
                    clock = { now },
                    zone = { zone },
                )
            val first = maker.today(items, "de")
            assertNotNull(first)
            assertTrue(first!!.byBrain)
            assertEquals("2026-11-04", first.day)
            assertEquals("Satz 1", first.points.first().text)
            assertTrue(first.points.all { it.sources.isNotEmpty() })
            // later the same day: the kept one, no new brain call
            now += 3 * HOUR
            assertSame(first, maker.today(items, "de"))
            assertEquals(1, calls)
            // "Neu erzeugen"
            val again = maker.today(items, "de", force = true)
            assertEquals(2, calls)
            assertTrue(again!!.createdAt > first.createdAt)
            // the next day: a new one
            now += 24 * HOUR
            val tomorrow = maker.today(items.map { it.copy(published = now - HOUR) }, "de")
            assertEquals("2026-11-05", tomorrow!!.day)
            assertEquals(3, calls)
        }

    @Test fun stepsAreReportedReadingThenSummingUp() =
        runTest {
            // T-265: the card shows "Artikel lesen", then "Zusammenfassen" (only with a brain)
            val steps = mutableListOf<BriefingStep>()
            val brain = BrainCall { _, user -> user.lines().mapIndexed { i, _ -> "${i + 1}. S" }.joinToString("\n") }
            BriefingMaker(MemoryBriefingStore(), brain = brain, clock = { now }, zone = { zone })
                .today(items, "de", onStep = { steps += it })
            assertEquals(listOf(BriefingStep.READING, BriefingStep.SUMMING_UP), steps)
            steps.clear()
            BriefingMaker(MemoryBriefingStore(), brain = null, clock = { now }, zone = { zone })
                .today(items, "de", onStep = { steps += it })
            assertEquals(listOf(BriefingStep.READING), steps)
        }

    @Test fun withoutBrainTheStoriesOwnSentencesAreUsed() =
        runTest {
            val maker = BriefingMaker(MemoryBriefingStore(), brain = null, clock = { now }, zone = { zone })
            val b = maker.today(items, "de")!!
            assertFalse(b.byBrain)
            assertTrue(b.points.size >= BriefingPicker.MIN_POINTS)
            val strike = "Die Beschäftigten legen die Arbeit nieder."
            assertTrue(b.points.any { it.text == strike || it.text.startsWith("Gewerkschaft") })
        }

    @Test fun quickBrainThatIsNotLoadedDoesNotRewriteTodaysBriefing() =
        runTest {
            val store = MemoryBriefingStore()
            val plain = BriefingMaker(store, brain = null, clock = { now }, zone = { zone }).today(items, "de")!!
            val maker =
                BriefingMaker(
                    store,
                    brain =
                        BrainCall {
                            _,
                            _,
                            ->
                            "1. x"
                        },
                    quickBrain = BrainCall { _, _ -> null },
                    clock = { now },
                    zone = { zone },
                )
            assertSame(plain, maker.today(items, "de"))
        }

    @Test fun tooFewHeadlinesGiveNoBriefing() =
        runTest {
            val maker = BriefingMaker(MemoryBriefingStore(), clock = { now }, zone = { zone })
            assertNull(maker.today(items.take(1), "de"))
        }

    @Test fun numberedLinesOfTheBrain() {
        val raw = "<think>hm</think>\nHier dein Briefing:\n1. Eins\n2) **Zwei**\n4. Vier\n9. zu viel"
        assertEquals(listOf("Eins", "Zwei", null, "Vier"), BriefingMaker.numbered(raw, 4))
    }

    @Test fun briefingSurvivesJson() {
        val point = BriefingPoint("Satz", "Titel", listOf("taz", "nd"), "https://taz.de/1")
        val b = Briefing("2026-11-04", 5, true, listOf(point))
        assertEquals(b, BriefingJson.decode(BriefingJson.encode(b)))
    }

    @Test fun morningNoteRunsAtSevenTodayOrTomorrow() {
        val early = ZonedDateTime.of(2026, 11, 4, 5, 30, 0, 0, zone)
        assertEquals(90, BriefingDaily.delayUntilNext(early).toMinutes())
        val late = ZonedDateTime.of(2026, 11, 4, 22, 0, 0, 0, zone)
        assertEquals(9 * 60, BriefingDaily.delayUntilNext(late).toMinutes())
    }

    private companion object {
        const val HOUR = 60 * 60 * 1000L
    }
}
