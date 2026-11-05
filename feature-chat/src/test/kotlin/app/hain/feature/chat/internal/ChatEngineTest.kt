package app.hain.feature.chat.internal

import app.hain.llm.api.ChatMessage
import app.hain.llm.api.Role
import app.hain.llm.api.StopReason
import app.hain.llm.internal.AnswerMode
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatEngineTest {
    @Test
    fun fastModeStreamsTextWithoutThinking() =
        runTest {
            val llm = FakeLlm(listOf("Pa", "ris", "."))
            val updates = ChatEngine(llm, { "SYS" }).respond(user("Hauptstadt?"), AnswerMode.FAST).toList()
            val last = updates.last() as TurnUpdate.Finished
            assertEquals("Paris.", last.output.text)
            assertEquals(null, last.thinkingMillis)
            assertTrue(llm.lastPrompt.startsWith("<|startoftext|><|im_start|>system\nSYS<|im_end|>"))
            assertTrue(llm.lastPrompt.endsWith("<|im_start|>assistant\n<think></think>"))
            assertEquals(1024, llm.lastParams?.maxTokens)
            // T-264: the first text at once, then at most every 40 ms; the last live state is the whole text
            val live = updates.filterIsInstance<TurnUpdate.Streaming>().map { it.text }
            assertTrue(live.first { it.isNotEmpty() }.startsWith("Pa"))
            assertEquals("Paris.", live.last())
        }

    @Test
    fun thoroughModeSplitsReasoning() =
        runTest {
            val llm = FakeLlm(listOf("erst denken", "</think>", "Antwort"))
            val updates = ChatEngine(llm, { "S" }).respond(user("warum?"), AnswerMode.THOROUGH).toList()
            val last = updates.last() as TurnUpdate.Finished
            assertEquals("erst denken", last.output.reasoning)
            assertEquals("Antwort", last.output.text)
            assertTrue(llm.lastPrompt.endsWith("<|im_start|>assistant\n<think>"))
            assertEquals(4096, llm.lastParams?.maxTokens)
        }

    @Test
    fun stoppedWhileThinkingKeepsThinkingOutOfTheAnswer() =
        runTest {
            val llm = FakeLlm(listOf("noch am denken"), end = StopReason.CANCELLED)
            val updates = ChatEngine(llm, { "S" }).respond(user("warum?"), AnswerMode.THOROUGH).toList()
            val last = updates.last() as TurnUpdate.Finished
            assertEquals("", last.output.text)
            assertEquals("noch am denken", last.output.reasoning)
        }

    @Test
    fun checkpointsMarkFixedSystemTextSystemEndAndHistory() =
        runTest {
            val llm = FakeLlm(listOf("A"))
            val system = "Be kind.\n\n# Context\nNow: 23:10"
            val history =
                listOf(
                    ChatMessage(Role.USER, "eins"),
                    ChatMessage(Role.ASSISTANT, "erste Antwort"),
                    ChatMessage(Role.USER, "zwei"),
                )
            ChatEngine(llm, { system }).respond(history, AnswerMode.FAST).toList()
            val prompt = llm.lastPrompt
            val cuts = llm.checkpoints.last()
            assertEquals(4, cuts.size)
            assertTrue(prompt.substring(cuts[0]).startsWith("# Context"))
            assertTrue(prompt.substring(0, cuts[1]).endsWith("Now: 23:10<|im_end|>\n"))
            assertTrue(prompt.substring(0, cuts[2]).endsWith("erste Antwort<|im_end|>\n"))
            assertTrue(prompt.substring(0, cuts[3]).endsWith("zwei<|im_end|>\n"))
        }

    @Test
    fun languageHintGoesAfterTheHistoryAndKeepsTheCheckpoints() =
        runTest {
            val llm = FakeLlm(listOf("Gut!"))
            val system = "Sei nett.\n\n# Kontext\nJetzt: 07:00"
            val history = listOf(ChatMessage(Role.USER, "wie geht es dir"))
            var askedFor = emptyList<String>()
            val hint = { messages: List<String> ->
                askedFor = messages
                "Antworte auf Deutsch."
            }
            ChatEngine(llm, { system }, turnHint = hint)
                .respond(history, AnswerMode.FAST)
                .toList()
            val prompt = llm.lastPrompt
            assertEquals(listOf("wie geht es dir"), askedFor)
            assertTrue(
                prompt,
                prompt.endsWith(
                    "wie geht es dir<|im_end|>\n<|im_start|>system\nAntworte auf Deutsch.<|im_end|>\n" +
                        "<|im_start|>assistant\n<think></think>",
                ),
            )
            val cuts = llm.checkpoints.last()
            assertTrue(prompt.substring(cuts.first()).startsWith("# Kontext"))
            assertTrue(prompt.substring(0, cuts.last()).endsWith("wie geht es dir<|im_end|>\n"))
        }

    @Test
    fun memoryLineFollowsTheLanguageHintOffTheMainThread() =
        runTest {
            val llm = FakeLlm(listOf("Ohne Pilze."))
            var thread = ""
            val engine =
                ChatEngine(
                    llm,
                    { "S" },
                    turnHint = { "Antworte auf Deutsch." },
                    memoryHint = { messages ->
                        thread = Thread.currentThread().name
                        "Relevant: - mag keine Pilze (${messages.size})"
                    },
                )
            engine.respond(user("Rezept?"), AnswerMode.FAST).toList()
            val prompt = llm.lastPrompt
            // one system line after the history: language first, then the memory
            val line = prompt.indexOf("system\nAntworte auf Deutsch.\nRelevant: - mag keine Pilze (1)")
            assertTrue(prompt, line > prompt.indexOf("Rezept?"))
            assertTrue(thread, thread.startsWith("DefaultDispatcher"))
        }

    @Test
    fun noHintWhenNoneIsGiven() =
        runTest {
            val llm = FakeLlm(listOf("A"))
            ChatEngine(llm, { "S" }, turnHint = { null }).respond(user("Hi"), AnswerMode.FAST).toList()
            assertTrue(llm.lastPrompt.endsWith("Hi<|im_end|>\n<|im_start|>assistant\n<think></think>"))
        }

    @Test
    fun systemPromptTimeMovesOnlyEveryHalfHour() {
        var now = 1_000_000L
        val engine = ChatEngine(FakeLlm(), { "S" }, clock = { now })
        val first = engine.promptAnchor()
        now += ChatEngine.ANCHOR_MS - 1
        assertEquals(first, engine.promptAnchor())
        now += 1
        assertEquals(now, engine.promptAnchor())
    }

    @Test
    fun warmUpReadsTheSystemPrompt() =
        runTest {
            val llm = FakeLlm()
            ChatEngine(llm, { "SYS" }).warmUp()
            assertEquals("<|startoftext|><|im_start|>system\nSYS<|im_end|>\n", llm.prefills.single())
        }

    @Test
    fun titleIsAskedAfterTheHistoryWithoutThinking() =
        runTest {
            val llm = FakeLlm(listOf("\"Plastik sparen im Alltag.\""))
            val history = listOf(ChatMessage(Role.USER, "Wie spare ich Plastik?"), ChatMessage(Role.ASSISTANT, "So."))
            val title = ChatEngine(llm, { "S" }).title(history)
            assertEquals("Plastik sparen im Alltag", title)
            assertTrue(llm.lastPrompt.endsWith("<|im_start|>assistant\n<think></think>"))
            val historyEnd = llm.checkpoints.last().last()
            assertTrue(llm.lastPrompt.substring(0, historyEnd).endsWith("So.<|im_end|>\n"))
        }

    @Test
    fun cleanTitleKeepsSixWordsAndDropsLabels() {
        val long = "Titel: eins zwei drei vier fünf sechs sieben"
        assertEquals("Eins zwei drei vier fünf sechs", ChatEngine.cleanTitle(long))
        assertEquals(null, ChatEngine.cleanTitle("  \n \"\" "))
        assertEquals("Wetter morgen", ChatEngine.cleanTitle("**Wetter morgen**\nmehr Text"))
    }

    private fun user(text: String) = listOf(ChatMessage(Role.USER, text))

    @Test
    fun answerWithoutToolsLosesFakeCitationsLiveAndAtTheEnd() =
        runTest {
            // T-220, Oskar's phone: no tool ran, yet the brain wrote "[1]", a source line and tool talk
            val llm =
                FakeLlm(
                    listOf(
                        "**Fakt:** Eichen werden 200+ Jahre alt [1].",
                        "\n\n[1] Wikipedia (falls verfügbar) – hier kein aktuelles Werkzeug nötig für die Frage.",
                        "\n\nKurz gesagt: sehr alt.",
                    ),
                )
            val updates = ChatEngine(llm, { "SYS" }).respond(user("Wie alt werden Bäume?"), AnswerMode.FAST).toList()
            val last = updates.last() as TurnUpdate.Finished
            assertEquals("Eichen werden 200+ Jahre alt.\n\nKurz gesagt: sehr alt.", last.output.text)
            val live = updates.filterIsInstance<TurnUpdate.Streaming>().map { it.text }
            assertTrue(live.none { it.contains("[1]") || it.contains("Fakt:") || it.contains("Werkzeug") })
        }
}
