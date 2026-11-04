package app.hain.memory

import app.hain.memory.api.ExtractionPolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** T-114: the prompts in the app are the ones in `prompts/`, and the battery rule of MEMORY.md §2.3. */
class ExtractionAssetsTest {
    private fun repoFile(path: String): File =
        generateSequence(File("").absoluteFile) { it.parentFile }.map { File(it, path) }.first { it.exists() }

    @Test
    fun assetPromptsMatchThePromptsFolder() {
        for (name in listOf("extract_facts.txt", "compare_facts.txt")) {
            val source = repoFile("prompts/$name").readText()
            val asset = File("src/main/assets/prompts/$name").readText()
            assertEquals(name, source, asset)
        }
        assertTrue(repoFile("prompts/extract_facts.txt").readText().contains("\"facts\""))
    }

    @Test
    fun runsOnTheChargerOrAbove30Percent() {
        assertTrue(ExtractionPolicy.canRun(charging = true, batteryPercent = 5))
        assertTrue(ExtractionPolicy.canRun(charging = false, batteryPercent = 31))
        assertFalse(ExtractionPolicy.canRun(charging = false, batteryPercent = 30))
        assertTrue(ExtractionPolicy.hasNew(lastUserMessageId = 9, doneUpTo = 4))
        assertFalse(ExtractionPolicy.hasNew(lastUserMessageId = 4, doneUpTo = 4))
        assertFalse(ExtractionPolicy.hasNew(lastUserMessageId = null, doneUpTo = 0))
    }
}
