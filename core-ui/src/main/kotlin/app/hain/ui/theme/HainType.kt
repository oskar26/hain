package app.hain.ui.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import app.hain.ui.R

/**
 * Font families built from the fonts bundled in `core-ui/src/main/res/font` (all OFL-1.1).
 * Hanken Grotesk (T-010, 03.11.2026): a light, calm grotesk that few apps use.
 * Light for big headings, Regular for text, Medium for buttons and titles,
 * SemiBold only for **bold** in answers (markdown asks for Bold, so Bold maps to SemiBold too).
 */
private val HankenFamily =
    FontFamily(
        Font(R.font.hankengrotesk_light, FontWeight.Light),
        Font(R.font.hankengrotesk_regular, FontWeight.Normal),
        Font(R.font.hankengrotesk_medium, FontWeight.Medium),
        Font(R.font.hankengrotesk_semibold, FontWeight.SemiBold),
        Font(R.font.hankengrotesk_semibold, FontWeight.Bold),
    )

private val AtkinsonFamily =
    FontFamily(
        Font(R.font.atkinsonhyperlegible_regular, FontWeight.Normal),
        Font(R.font.atkinsonhyperlegible_bold, FontWeight.Bold),
    )

private val CodeFamily = FontFamily(Font(R.font.jetbrainsmono_regular, FontWeight.Normal))

/**
 * Type scale (docs/design/DESIGN_LANGUAGE.md). Sizes are in sp so the system font scale
 * (up to 200 %) is respected. Running text never goes below 16 sp.
 */
data class HainTypography(
    val display: TextStyle,
    val title: TextStyle,
    val body: TextStyle,
    val bodySmall: TextStyle,
    val label: TextStyle,
    val button: TextStyle,
    val code: TextStyle,
)

object HainType {
    /** Default text: Hanken Grotesk. */
    fun hanken(): HainTypography =
        scale(
            family = HankenFamily,
            heading = FontWeight.Light,
            strong = FontWeight.Medium,
            // a thin font reads better with a hair more room between letters
            tracking = 0.1.sp,
        )

    /** "Gut lesbar" option: Atkinson Hyperlegible, same sizes, bolder headings. */
    fun atkinson(): HainTypography =
        scale(
            family = AtkinsonFamily,
            heading = FontWeight.Bold,
            strong = FontWeight.Bold,
            tracking = 0.sp,
        )

    private fun scale(
        family: FontFamily,
        heading: FontWeight,
        strong: FontWeight,
        tracking: TextUnit,
    ): HainTypography =
        HainTypography(
            display = TextStyle(fontFamily = family, fontWeight = heading, fontSize = 30.sp, lineHeight = 38.sp),
            title = TextStyle(fontFamily = family, fontWeight = strong, fontSize = 20.sp, lineHeight = 28.sp),
            body =
                TextStyle(
                    fontFamily = family,
                    fontWeight = FontWeight.Normal,
                    fontSize = 16.sp,
                    lineHeight = 25.sp,
                    letterSpacing = tracking,
                ),
            bodySmall =
                TextStyle(
                    fontFamily = family,
                    fontWeight = FontWeight.Normal,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    letterSpacing = tracking,
                ),
            label = TextStyle(fontFamily = family, fontWeight = strong, fontSize = 13.sp, lineHeight = 18.sp),
            button = TextStyle(fontFamily = family, fontWeight = strong, fontSize = 15.sp, lineHeight = 20.sp),
            code =
                TextStyle(
                    fontFamily = CodeFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                ),
        )
}
