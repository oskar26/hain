package app.hain.llm.internal

import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File

/** T-031: the app's prompt assets are exact copies of prompts/ (edit there, then copy). */
class PromptAssetsTest {
    @Test
    fun assetsMatchPromptsFolder() {
        val assets = File("src/main/assets/prompts").listFiles().orEmpty().sortedBy { it.name }
        assertEquals(
            listOf("system_full.de.txt", "system_full.en.txt", "system_short.de.txt", "system_short.en.txt"),
            assets.map { it.name },
        )
        assets.forEach { assertEquals(it.name, File("../prompts/${it.name}").readText(), it.readText()) }
    }
}
