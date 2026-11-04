package app.hain.common.api

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class BrainChoiceStoreTest {
    @get:Rule val tmp = TemporaryFolder()

    private fun TestScope.store(): BrainChoiceStore =
        BrainChoiceStore(
            PreferenceDataStoreFactory.create(
                scope = backgroundScope,
                produceFile = { tmp.newFile("b.preferences_pb").also { it.delete() } },
            ),
        )

    @Test
    fun nothingPickedAtFirst() {
        runTest(UnconfinedTestDispatcher()) {
            assertNull(store().selectedId.first())
        }
    }

    @Test
    fun pickIsReadBackAndClearForgetsIt() {
        runTest(UnconfinedTestDispatcher()) {
            val s = store()
            s.select("lfm3-1.7b-qad-q4_0")
            assertEquals("lfm3-1.7b-qad-q4_0", s.selectedId.first())
            s.select("other")
            assertEquals("other", s.selectedId.first())
            s.clear()
            assertNull(s.selectedId.first())
        }
    }
}
