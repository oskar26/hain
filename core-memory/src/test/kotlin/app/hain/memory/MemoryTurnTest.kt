package app.hain.memory

import app.hain.memory.api.MemoryTurn
import app.hain.memory.internal.DefaultMemoryStore
import app.hain.memory.internal.InMemoryFactSource
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MemoryTurnTest {
    @Test
    fun merkDirSavesOnceAndTellsTheBrain() =
        runTest(UnconfinedTestDispatcher()) {
            val store = DefaultMemoryStore(InMemoryFactSource(), backgroundScope)
            val turn = MemoryTurn(store, backgroundScope)
            val hint = turn.hint(listOf("Merk dir, dass ich keine Pilze mag"), "de")!!
            assertTrue(hint.startsWith("Du hast dir gerade gemerkt: „ich keine Pilze mag“"))
            // the same message again (regenerate): no second copy
            assertEquals(hint, turn.hint(listOf("Merk dir, dass ich keine Pilze mag"), "de"))
            assertEquals(listOf("ich keine Pilze mag"), store.facts.value.map { it.text })
        }

    @Test
    fun privateChatNeitherSavesNorReads() =
        runTest(UnconfinedTestDispatcher()) {
            val store = DefaultMemoryStore(InMemoryFactSource(), backgroundScope)
            store.remember("mag keine Pilze")
            val turn = MemoryTurn(store, backgroundScope)
            assertNull(turn.hint(listOf("Merk dir, dass ich Gitarre spiele"), "de", privateChat = true))
            assertNull(turn.hint(listOf("Vergiss, dass ich keine Pilze mag"), "de", privateChat = true))
            assertNull(turn.hint(listOf("Hast du ein Rezept mit Pilzen?"), "de", privateChat = true))
            assertEquals(listOf("mag keine Pilze"), store.facts.value.map { it.text })
        }

    @Test
    fun laterQuestionsGetTheFittingFact() =
        runTest(UnconfinedTestDispatcher()) {
            val store = DefaultMemoryStore(InMemoryFactSource(), backgroundScope)
            store.remember("mag keine Pilze")
            store.remember("spielt Gitarre")
            val turn = MemoryTurn(store, backgroundScope)
            val hint = turn.hint(listOf("Hallo", "Hast du ein Rezept mit Pilzen?"), "de")!!
            assertTrue(hint.contains("- mag keine Pilze"))
            assertTrue(!hint.contains("Gitarre"))
            // T-271: the remember rule only when the message asks to remember something
            assertTrue(!hint.contains("remember"))
            assertNull(turn.hint(listOf("Hi"), "en"))
            assertEquals(
                "If the user asks you to remember something, call remember.",
                turn.hint(listOf("Could you keep in mind my exam dates?"), "en"),
            )
        }

    @Test
    fun vergissDeletesAndSaysWhat() =
        runTest(UnconfinedTestDispatcher()) {
            val store = DefaultMemoryStore(InMemoryFactSource(), backgroundScope)
            store.remember("mag keine Pilze")
            val turn = MemoryTurn(store, backgroundScope)
            assertEquals(
                "Du hast vergessen: „mag keine Pilze“. Bestätige das kurz.",
                turn.hint(listOf("Vergiss, dass ich keine Pilze mag"), "de"),
            )
            assertTrue(store.facts.value.isEmpty())
            assertTrue(turn.hint(listOf("forget that I live in Rome"), "en")!!.startsWith("Nothing like that"))
        }

    @Test
    fun offMeansNoHint() =
        runTest(UnconfinedTestDispatcher()) {
            val store = DefaultMemoryStore(InMemoryFactSource(), backgroundScope)
            store.setEnabled(false)
            assertNull(MemoryTurn(store, backgroundScope).hint(listOf("Merk dir, dass ich Rosa heiße"), "de"))
            assertTrue(store.facts.value.isEmpty())
        }

    @Test
    fun chipsKnowWhatWasUsedSavedAndForgotten() =
        runTest(UnconfinedTestDispatcher()) {
            val store = DefaultMemoryStore(InMemoryFactSource(), backgroundScope)
            store.remember("mag keine Pilze")
            val turn = MemoryTurn(store, backgroundScope)

            val ask = listOf("Hallo", "Hast du ein Rezept mit Pilzen?")
            turn.hint(ask, "de")
            assertEquals(
                listOf("mag keine Pilze"),
                turn.uses.value[MemoryTurn.key(ask)]!!
                    .used
                    .map { it.text },
            )

            val save = listOf("Merk dir, dass ich Gitarre spiele")
            turn.hint(save, "de")
            val saved = turn.uses.value[MemoryTurn.key(save)]!!
            assertEquals(listOf("ich Gitarre spiele"), saved.saved.map { it.text })
            // "Rückgängig" deletes it again, once
            turn.undo(MemoryTurn.key(save))
            assertTrue(turn.uses.value[MemoryTurn.key(save)]!!.undone)
            assertEquals(listOf("mag keine Pilze"), store.facts.value.map { it.text })

            val forget = listOf("Vergiss, dass ich keine Pilze mag")
            turn.hint(forget, "de")
            assertTrue(store.facts.value.isEmpty())
            turn.undo(MemoryTurn.key(forget))
            assertEquals(listOf("mag keine Pilze"), store.facts.value.map { it.text })
        }

    @Test
    fun theRememberToolSavesIntoTheCurrentTurn() =
        runTest(UnconfinedTestDispatcher()) {
            val store = DefaultMemoryStore(InMemoryFactSource(), backgroundScope)
            val turn = MemoryTurn(store, backgroundScope)
            val ask = listOf("Ich heiße übrigens Oskar, das darfst du behalten")
            turn.hint(ask, "de")
            val fact = store.remember("heißt Oskar")!!
            turn.onToolSaved(fact)
            assertEquals(listOf(fact), turn.uses.value[MemoryTurn.key(ask)]!!.saved)
            // equal last messages in different chats are different turns
            assertTrue(MemoryTurn.key(listOf("a", "Hallo")) != MemoryTurn.key(listOf("Hallo")))
            // the chat may hold more user messages than the prompt: the newest ones decide
            assertEquals(MemoryTurn.key(listOf("x", "a", "b", "c")), MemoryTurn.key(listOf("a", "b", "c")))
        }
}
