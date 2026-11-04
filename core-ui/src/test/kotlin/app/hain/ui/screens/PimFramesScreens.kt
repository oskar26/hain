package app.hain.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.hain.ui.pim.PimCanvasForTest
import app.hain.ui.pim.PimMove
import app.hain.ui.pim.PimMoves
import app.hain.ui.pim.PimState
import app.hain.ui.theme.HainTheme
import app.hain.ui.theme.LocalHainColors
import com.github.takahirom.roborazzi.ExperimentalRoborazziApi
import com.github.takahirom.roborazzi.captureScreenRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Screenshot frames of every Pim move (T-089): five points in time per move, to check by eye that the
 * leaf stays on his head (the numbers are checked in PimRigTest). CI workflow screens.yml only.
 */
@OptIn(ExperimentalRoborazziApi::class)
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "de-w392dp-h850dp-xxhdpi")
class PimFramesScreens {
    @get:Rule
    val compose = createComposeRule()

    private fun sheet(
        moves: List<PimMove>,
        name: String,
    ) {
        compose.setContent {
            HainTheme(darkTheme = false) {
                Column(
                    Modifier.fillMaxSize().background(LocalHainColors.current.bg).padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    moves.forEach { move ->
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            BasicText(move.name.lowercase(), Modifier.width(64.dp), style = TextStyle(fontSize = 9.sp))
                            listOf(0f, 0.25f, 0.5f, 0.75f, 1f).forEach { t ->
                                val pose = PimMoves.pose(move, PimMoves.basePose(PimState.IDLE), t)
                                PimCanvasForTest(pose, 56.dp)
                            }
                        }
                    }
                }
            }
        }
        captureScreenRoboImage("build/outputs/roborazzi/pim/$name.png")
    }

    @Test
    fun idleAndStateMoves() = sheet(PimMove.entries.take(HALF), "pim_frames_1")

    @Test
    fun tapMoves() = sheet(PimMove.entries.drop(HALF), "pim_frames_2")

    private companion object {
        const val HALF = 10
    }
}
