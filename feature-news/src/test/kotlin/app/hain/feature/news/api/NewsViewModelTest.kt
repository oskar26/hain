package app.hain.feature.news.api

import app.hain.common.api.net.GateResponse
import app.hain.tools.internal.news.MemoryNewsStore
import app.hain.tools.internal.news.NewsFeed
import app.hain.tools.internal.news.NewsItem
import app.hain.tools.internal.news.NewsMode
import app.hain.tools.internal.news.NewsRepository
import app.hain.tools.internal.news.NewsSnapshot
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
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

    private fun TestScope.viewModel(
        gate: FakeGate = FakeGate(),
        brain: BrainCall = BrainCall { _, _ -> "- a" },
    ): NewsViewModel {
        val snapshot = NewsSnapshot(ownFeeds = listOf(own), items = items, lastRefresh = now)
        val repo = NewsRepository(gate, MemoryNewsStore(snapshot), standard)
        return NewsViewModel(repo, SummarizeArticle(gate, brain), { "de" }, backgroundScope)
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

    @Test fun summaryRunsOnTapAndIsOnlyKeptInMemory() =
        runTest {
            val sentence = "Ein langer Satz über den Haushalt des Bundes. ".repeat(3)
            val article = "<html><body>" + "<p>$sentence</p>".repeat(10)
            val answer = CompletableDeferred<String>()
            val vm = viewModel(FakeGate { GateResponse(200, article) }, BrainCall { _, _ -> answer.await() })
            vm.start()
            advanceUntilIdle()
            val item =
                vm.state.value.items
                    .first()
            vm.summarize(item)
            advanceUntilIdle()
            assertEquals(SummaryState.Loading, vm.state.value.summaries[item.guid])
            answer.complete("- Eins\n- Zwei")
            advanceUntilIdle()
            val ready = vm.state.value.summaries[item.guid] as SummaryState.Ready
            assertEquals(SummaryResult.Bullets(listOf("Eins", "Zwei")), ready.result)
            vm.hideSummary(item.guid)
            advanceUntilIdle()
            assertTrue(
                vm.state.value.summaries
                    .isEmpty(),
            )
        }
}
