package app.hain.common.api

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
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
    fun phoneActionsAskFirstByDefault() =
        runTest(UnconfinedTestDispatcher()) {
            val r = repo()
            assertEquals(true, r.settings.first().confirmActions)
            r.setConfirmActions(false)
            assertEquals(false, r.settings.first().confirmActions)
            r.reset()
            assertEquals(true, r.settings.first().confirmActions)
        }

    @Test
    fun appIconDefaultsTo3dAndIsKeptOnReset() =
        runTest(UnconfinedTestDispatcher()) {
            val r = repo()
            assertEquals(AppIcon.THREE_D, r.settings.first().appIcon)
            r.setAppIcon(AppIcon.FLAT)
            assertEquals(AppIcon.FLAT, r.settings.first().appIcon)
            r.setTheme(ThemeChoice.DARK)
            r.reset()
            val s = r.settings.first()
            assertEquals(AppIcon.FLAT, s.appIcon)
            assertEquals(ThemeChoice.SYSTEM, s.theme)
            r.setAppIcon(AppIcon.THREE_D)
            assertEquals(AppIcon.THREE_D, r.settings.first().appIcon)
        }

    @Test
    fun unknownStoredAppIconFallsBackTo3d() =
        runTest(UnconfinedTestDispatcher()) {
            val store =
                PreferenceDataStoreFactory.create(
                    scope = backgroundScope,
                    produceFile = { tmp.newFile("u.preferences_pb").also { it.delete() } },
                )
            store.edit { it[stringPreferencesKey("app_icon")] = "RAINBOW" }
            assertEquals(AppIcon.THREE_D, SettingsRepository(store).settings.first().appIcon)
        }

    @Test
    fun customInstructionsListOnlyWhatIsSet() {
        assertEquals(
            "Answer in the language the user writes in. If unsure, answer in German.",
            HainSettings().customInstructions("German"),
        )
        val text = HainSettings(answerLanguage = AnswerLanguage.EN, style = " short ").customInstructions()
        assertEquals("Always answer in English.\nHow the user wants answers: short", text)
    }

    @Test
    fun developerValuesRoundTripAndSurviveReset() =
        runTest(UnconfinedTestDispatcher()) {
            val r = repo()
            val dev =
                DevSettings(
                    enabled = true,
                    temperature = 0.7f,
                    topK = 40,
                    topP = 0.9f,
                    minP = 0.05f,
                    repeatPenalty = 1.2f,
                    contextSize = 4096,
                    maxTokens = 512,
                    seed = 42,
                    thinking = ThinkingOverride.ON,
                    thinkingBudget = 1024,
                    showRawThinking = true,
                    systemPrompt = "Du bist ein Test.",
                    showStats = true,
                    customModelPath = "/x/model.gguf",
                    customModelName = "a/b · model.gguf",
                )
            r.setDev(dev)
            assertEquals(dev, r.settings.first().dev)
            r.reset()
            assertEquals(dev, r.settings.first().dev)
            assertEquals(
                DevSettings(enabled = true, customModelPath = "/x/model.gguf", customModelName = "a/b · model.gguf"),
                dev.resetTuning(),
            )
        }

    @Test
    fun updateChoiceIsSavedAndMarksTheQuestionAnswered() =
        runTest(UnconfinedTestDispatcher()) {
            val r = repo()
            assertEquals(UpdateCheck.OFF, r.settings.first().updateCheck)
            r.setUpdateCheck(UpdateCheck.DAILY)
            r.setUpdateChannel(UpdateChannel.NIGHTLY)
            r.setLastUpdateCheck(123L)
            val s = r.settings.first()
            assertEquals(UpdateCheck.DAILY, s.updateCheck)
            assertEquals(UpdateChannel.NIGHTLY, s.updateChannel)
            assertEquals(true, s.updateAsked)
            assertEquals(123L, s.lastUpdateCheck)
        }
}
