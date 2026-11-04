package app.hain.feature.news.api

import app.hain.common.api.net.GateResponse
import app.hain.tools.internal.news.MemoryNewsStore
import app.hain.tools.internal.news.NewsFeed
import app.hain.tools.internal.news.NewsItem
import app.hain.tools.internal.news.NewsMode
import app.hain.tools.internal.news.NewsRepository
import app.hain.tools.internal.news.NewsSnapshot
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** T-073/T-074: the news screen state with a fake gate and a fake brain. */
class NewsViewModelTest {
    private val now = System.currentTimeMillis()
    private val standard =
        listOf(NewsFeed("tagesschau", "tagesschau", "https://www.tagesschau.de/index~rss2.xml", "de"))
    private val own = NewsFeed("own-1", "Mein Blog", "https://blog.example.org/feed", own = true)
    private val items =
        listOf(
            NewsItem("tagesschau", "tagesschau", "Haushalt beschlossen", "https://t.de/1", "t1", now - 1, now, ""),
            NewsItem("own-1", "Mein Blog", "Neuer Beitrag", "https://blog.example.org/1", "b1", now - 2, now, ""),
        )

    private lateinit var repoUsed: NewsRepository

    // Not backgroundScope: advanceUntilIdle() skips work that only runs there.
    private val scopes = mutableListOf<CoroutineScope>()

    @After fun cancelScopes() {
        scopes.forEach { it.cancel() }
    }

    private fun TestScope.viewModel(
        gate: FakeGate = FakeGate(),
        brain: BrainCall = BrainCall { _, _ -> "- a" },
    ): NewsViewModel {
        val snapshot = NewsSnapshot(ownFeeds = listOf(own), items = items, lastRefresh = now)
        val repo = NewsRepository(gate, MemoryNewsStore(snapshot), standard)
        val scope = CoroutineScope(StandardTestDispatcher(testScheduler) + SupervisorJob())
        scopes += scope
        repoUsed = repo
        return NewsViewModel(
            repository = repo,
            reader = ArticleReader(gate, Dispatchers.Unconfined),
            asker = ArticleAsk(brain),
            briefings = BriefingMaker(MemoryBriefingStore(), brain = null, clock = { now }),
            lang = { "de" },
            scope = scope,
        )
    }

    @Test fun modeSwitchPicksTheFeeds() =
        runTest {
            val vm = viewModel()
            vm.start()
            advanceUntilIdle()
            assertEquals(
                listOf("Haushalt beschlossen"),
                vm.state.value.items
                    .map { it.title },
            )
            vm.setMode(NewsMode.OWN_ONLY)
            advanceUntilIdle()
            assertEquals(
                listOf("Neuer Beitrag"),
                vm.state.value.items
                    .map { it.title },
            )
            vm.setMode(NewsMode.STANDARD_PLUS_OWN)
            advanceUntilIdle()
            assertEquals(2, vm.state.value.items.size)
            assertEquals(listOf(own), vm.state.value.ownFeeds)
        }

    @Test fun addingAFeedThatIsNoFeedShowsAnError() =
        runTest {
            val vm = viewModel(FakeGate { GateResponse(200, "<html></html>") })
            vm.start()
            advanceUntilIdle()
            vm.addFeed("https://www.tagesschau.de/index~rss2.xml")
            advanceUntilIdle()
            assertEquals(AddFeedError.DUPLICATE, vm.state.value.addError)
            vm.clearAddError()
            advanceUntilIdle()
            assertEquals(null, vm.state.value.addError)
            vm.addFeed("not an address")
            advanceUntilIdle()
            assertEquals(AddFeedError.NOT_A_FEED, vm.state.value.addError)
        }

    @Test fun renameAndDelete() =
        runTest {
            val vm = viewModel()
            vm.start()
            vm.renameFeed("own-1", "Blog")
            advanceUntilIdle()
            assertEquals(
                "Blog",
                vm.state.value.ownFeeds
                    .single()
                    .name,
            )
            vm.deleteFeed("own-1")
            advanceUntilIdle()
            assertTrue(
                vm.state.value.ownFeeds
                    .isEmpty(),
            )
        }

    @Test fun openingAnArticleReadsItInTheAppAndMarksOnlyCutOffOnes() =
        runTest {
            val free =
                "<html><head><title>Haushalt beschlossen</title></head><body><article>" +
                    (1..8).joinToString("") { "<p>Satz $it über den Haushalt des Bundes, mit Zahlen.</p>" } +
                    "</article></body></html>"
            val vm = viewModel(FakeGate { GateResponse(200, free) })
            vm.start()
            advanceUntilIdle()
            val item =
                vm.state.value.items
                    .first { it.title == "Haushalt beschlossen" }
            vm.open(item)
            advanceUntilIdle()
            val open = vm.state.value.open!!
            val ready = open.result as ReaderResult.Ready
            assertTrue(ready.article.textLength > 400)
            assertFalse(ready.article.locked)
            assertFalse(
                vm.state.value.items
                    .first { it.guid == item.guid }
                    .paywall,
            )
            vm.close()
            advanceUntilIdle()
            assertEquals(null, vm.state.value.open)
        }

    @Test fun cutOffArticleGetsThePaywallMarkAfterReading() =
        runTest {
            val teaser =
                "<html><head><script type=\"application/ld+json\">{\"isAccessibleForFree\":false}</script></head>" +
                    "<body><article><p>Nur der Anfang des Artikels, dann ist Schluss, leider, ganz ehrlich, " +
                    "es geht hier nicht weiter, und das ist schade, weil es spannend wäre, mehr zu lesen.</p>" +
                    "<p>Weiterlesen mit Abo, Sie haben bereits ein Abo? Anmelden, bitte, hier oder dort.</p>" +
                    "</article></body></html>"
            val vm = viewModel(FakeGate { GateResponse(200, teaser) })
            vm.start()
            advanceUntilIdle()
            val item =
                vm.state.value.items
                    .first { it.title == "Haushalt beschlossen" }
            assertFalse(item.paywall)
            vm.open(item)
            advanceUntilIdle()
            val ready =
                vm.state.value.open!!
                    .result as ReaderResult.Ready
            assertTrue(ready.article.locked)
            assertTrue(
                vm.state.value.items
                    .first { it.guid == item.guid }
                    .paywall,
            )
            assertTrue(
                repoUsed.state.value.locked
                    .contains(item.link),
            )
        }

    @Test fun pimAnswersAboutTheOpenArticleAndHandsItToTheChat() =
        runTest {
            val page =
                "<html><body><article>" +
                    (1..10).joinToString("") { "<p>Der Bundestag hat den Haushalt beschlossen, Teil $it.</p>" } +
                    "</article></body></html>"
            val answer = CompletableDeferred<String>()
            var context = ""
            val vm =
                viewModel(
                    FakeGate { GateResponse(200, page) },
                    BrainCall { _, user ->
                        context = user
                        answer.await()
                    },
                )
            vm.start()
            advanceUntilIdle()
            vm.open(
                vm.state.value.items
                    .first { it.title == "Haushalt beschlossen" },
            )
            advanceUntilIdle()
            vm.ask("Worum geht es?")
            advanceUntilIdle()
            assertEquals(AskState("Worum geht es?"), vm.state.value.ask)
            answer.complete("Um den Haushalt.")
            advanceUntilIdle()
            assertEquals(
                AskResult.Answer("Um den Haushalt."),
                vm.state.value.ask
                    ?.result,
            )
            assertTrue(context.contains("Der Bundestag hat den Haushalt beschlossen"))
            assertTrue(context.endsWith("Worum geht es?"))
            val handOff = vm.chatHandOff()!!
            assertTrue(handOff.contains("Worum geht es?") && handOff.contains("Um den Haushalt."))
            assertTrue(handOff.contains("https://t.de/1"))
            vm.closeAsk()
            advanceUntilIdle()
            assertEquals(null, vm.state.value.ask)
        }

    @Test fun settingsChangeRightAway() =
        runTest {
            val vm = viewModel()
            vm.updatePrefs { it.copy(textScale = 1.3f, theme = ReaderTheme.SEPIA, listImages = false) }
            advanceUntilIdle()
            assertEquals(1.3f, vm.state.value.prefs.textScale)
            assertEquals(ReaderTheme.SEPIA, vm.state.value.prefs.theme)
            assertFalse(vm.state.value.prefs.listImages)
            assertFalse(vm.state.value.prefs.briefingNotification)
        }
}
