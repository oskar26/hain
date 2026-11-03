package app.hain.ui.theme

import android.content.res.Configuration
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.tooling.preview.Preview

val LocalHainColors = staticCompositionLocalOf { NothingLight }
val LocalHainTypography = staticCompositionLocalOf { HainType.hanken() }
val LocalHainShapes = staticCompositionLocalOf { StandardHainShapes }
val LocalHainSpacing = staticCompositionLocalOf { StandardHainSpacing }

/**
 * Provides the Hain design tokens (DESIGN_SYSTEM.md §2) via CompositionLocals.
 * Components read them through `LocalHainColors.current` & friends —
 * never import a Material theme.
 *
 * @param darkTheme     dark palette (follows the system by default)
 * @param highLegibility "Gut lesbar" setting → Atkinson Hyperlegible type
 * @param style         visual style A "Nothing Clean" (default) or B "Soft Gradient" (STYLE_OPTIONS.md)
 */
@Composable
fun HainTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    highLegibility: Boolean = false,
    style: HainStyle = HainStyle.A,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalHainStyle provides style,
        LocalHainColors provides hainColors(style, darkTheme),
        LocalHainTypography provides
            when {
                highLegibility -> HainType.atkinson()
                style == HainStyle.A -> HainType.geist()
                else -> HainType.hanken()
            },
        LocalHainShapes provides hainShapes(style),
        LocalHainSpacing provides StandardHainSpacing,
        content = content,
    )
}

@Preview(name = "Light", showBackground = true)
@Preview(
    name = "Dark",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
internal fun HainThemePreview() {
    HainTheme {
        Column {
            BasicText("Display", style = LocalHainTypography.current.display.copy(color = LocalHainColors.current.text))
            BasicText("Body", style = LocalHainTypography.current.body.copy(color = LocalHainColors.current.textMuted))
            BasicText("Accent", style = LocalHainTypography.current.label.copy(color = LocalHainColors.current.accent))
        }
    }
}
