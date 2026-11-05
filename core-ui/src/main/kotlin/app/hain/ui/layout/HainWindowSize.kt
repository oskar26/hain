package app.hain.ui.layout

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import app.hain.ui.theme.LocalHainColors

/**
 * T-235: window size class of the app window (the breakpoints of Android's WindowSizeClass, own code,
 * no Material). Phones are [COMPACT]; from [EXPANDED_DP] on (tablet landscape, foldables open, big
 * split screens) the chat list stays open on the left and settings show list and page side by side.
 */
enum class HainWindowSize {
    COMPACT,
    MEDIUM,
    EXPANDED,
    ;

    val isExpanded: Boolean get() = this == EXPANDED

    companion object {
        /** From here on the window counts as [MEDIUM] (tablet portrait, foldable inner screen). */
        const val MEDIUM_DP = 600

        /** From here on a permanent side panel and list-detail layouts fit next to the content. */
        const val EXPANDED_DP = 840

        /** Width class of a window that is [widthDp] wide. */
        fun fromWidth(widthDp: Int): HainWindowSize =
            when {
                widthDp >= EXPANDED_DP -> EXPANDED
                widthDp >= MEDIUM_DP -> MEDIUM
                else -> COMPACT
            }
    }
}

/** Width class of the current window; follows rotation, split screen and folding. */
@Composable
@ReadOnlyComposable
fun currentHainWindowSize(): HainWindowSize = HainWindowSize.fromWidth(LocalConfiguration.current.screenWidthDp)

/** The quiet vertical hairline between a permanent side panel and the content (Lichtung). */
@Composable
fun HainPaneDivider(modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxHeight()
            .width(1.dp)
            .background(LocalHainColors.current.border),
    )
}
