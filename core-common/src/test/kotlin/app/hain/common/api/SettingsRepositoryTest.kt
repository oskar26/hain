package app.hain.common.api

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class SettingsRepositoryTest {
    @get:Rule val tmp = TemporaryFolder()

    private fun TestScope.repo(): SettingsRepository =
        SettingsRepository(
            PreferenceDataStoreFactory.create(
                scope = backgroundScope,
                produceFile = { tmp.newFile("s.preferences_pb").also { it.delete() } },
            ),
        )

    @Test
    fun defaultsMatchSpec() =
        runTest(UnconfinedTestDispatcher()) {
            assertEquals(HainSettings(), repo().settings.first())
        }

    @Test
    fun writesAreReadBackAndResetRestoresDefaults() =
        runTest(UnconfinedTestDispatcher()) {
            val r = repo()
            r.setTheme(ThemeChoice.DARK)
            r.setReadableFont(true)
            r.setAnswerLanguage(AnswerLanguage.DE)
            r.setAnswerMode(AnswerMode.FAST)
            r.setAboutMe("Ich bin Oskar")
            val s = r.settings.first()
            assertEquals(ThemeChoice.DARK, s.theme)
            assertEquals(true, s.readableFont)
            assertEquals(AnswerLanguage.DE, s.answerLanguage)
            assertEquals(AnswerMode.FAST, s.answerMode)
            assertEquals("Ich bin Oskar", s.aboutMe)
            r.reset()
            assertEquals(HainSettings(), r.settings.first())
        }

    @Test
    fun customInstructionsListOnlyWhatIsSet() {
        assertEquals("", HainSettings().customInstructions())
        val text = HainSettings(answerLanguage = AnswerLanguage.EN, style = " short ").customInstructions()
        assertEquals("Always answer in English.\nHow the user wants answers: short", text)
    }
}
