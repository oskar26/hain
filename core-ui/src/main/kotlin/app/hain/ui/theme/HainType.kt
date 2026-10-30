package app.hain.ui.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import app.hain.ui.R

/** Font families built from the fonts bundled in `core-ui/src/main/res/font` (all OFL). */
private val InterFamily =
    FontFamily(
        Font(R.font.inter_regular, FontWeight.Normal),
        Font(R.font.inter_medium, FontWeight.Medium),
        Font(R.font.inter_semibold, FontWeight.SemiBold),
    )

private val AtkinsonFamily =
    FontFamily(
        Font(R.font.atkinsonhyperlegible_regular, FontWeight.Normal),
        Font(R.font.atkinsonhyperlegible_bold, FontWeight.Bold),
    )

private val CodeFamily = FontFamily(Font(R.font.jetbrainsmono_regular, FontWeight.Normal))

/**
 * Type scale, ported verbatim from DESIGN_SYSTEM.md §2.
 * Sizes are in sp so the system font scale (up to 200 %) is respected.
 */
data class HainTypography(
    val display: TextStyle,
    val title: TextStyle,
    val body: TextStyle,
    val bodySmall: TextStyle,
    val label: TextStyle,
    val code: TextStyle,
)

object HainType {
    /** Default text: Inter. */
    fun inter(): HainTypography =
        HainTypography(
            display =
                TextStyle(
                    fontFamily = InterFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 28.sp,
                    lineHeight = 34.sp,
                ),
            title =
                TextStyle(
                    fontFamily = InterFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 20.sp,
                    lineHeight = 26.sp,
                ),
            body =
                TextStyle(
                    fontFamily = InterFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 16.sp,
                    lineHeight = 24.sp,
                ),
            bodySmall =
                TextStyle(
                    fontFamily = InterFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                ),
            label =
                TextStyle(
                    fontFamily = InterFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.sp,
                    lineHeight = 16.sp,
                ),
            code =
                TextStyle(
                    fontFamily = CodeFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                ),
        )

    /** "Gut lesbar" option: Atkinson Hyperlegible. */
    fun atkinson(): HainTypography =
        HainTypography(
            display =
                TextStyle(
                    fontFamily = AtkinsonFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 28.sp,
                    lineHeight = 34.sp,
                ),
            title =
                TextStyle(
                    fontFamily = AtkinsonFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    lineHeight = 26.sp,
                ),
            body =
                TextStyle(
                    fontFamily = AtkinsonFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 16.sp,
                    lineHeight = 24.sp,
                ),
            bodySmall =
                TextStyle(
                    fontFamily = AtkinsonFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                ),
            label =
                TextStyle(
                    fontFamily = AtkinsonFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    lineHeight = 16.sp,
                ),
            code =
                TextStyle(
                    fontFamily = CodeFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                ),
        )
}
