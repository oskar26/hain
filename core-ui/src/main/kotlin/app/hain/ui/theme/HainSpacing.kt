package app.hain.ui.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Spacing scale, ported verbatim from DESIGN_SYSTEM.md §2: 4/8/12/16/24/32.
 * Layouts must pick from this scale only.
 */
data class HainSpacing(
    val xs: Dp,
    val sm: Dp,
    val md: Dp,
    val lg: Dp,
    val xl: Dp,
    val xxl: Dp,
)

val StandardHainSpacing =
    HainSpacing(
        xs = 4.dp,
        sm = 8.dp,
        md = 12.dp,
        lg = 16.dp,
        xl = 24.dp,
        xxl = 32.dp,
    )
