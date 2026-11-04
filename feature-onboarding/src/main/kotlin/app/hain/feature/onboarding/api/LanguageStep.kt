package app.hain.feature.onboarding.api

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.hain.feature.onboarding.R
import app.hain.ui.components.HainButton
import app.hain.ui.components.HainButtonStyle
import app.hain.ui.pim.JuicyPim
import app.hain.ui.pim.PimState
import app.hain.ui.theme.LocalHainColors
import app.hain.ui.theme.LocalHainSpacing
import app.hain.ui.theme.LocalHainTypography

/** Language Hain can speak, as a BCP-47 tag ("de", "en"). */
const val LANGUAGE_DE = "de"
const val LANGUAGE_EN = "en"

/** 2f (T-055): first screen of the onboarding. One tap sets the app and answer language. */
@Composable
fun LanguageStep(
    onPick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalHainColors.current
    val type = LocalHainTypography.current
    val spacing = LocalHainSpacing.current
    Column(
        modifier
            .fillMaxSize()
            .background(tourBackground())
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(spacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        JuicyPim(state = PimState.HAPPY, size = 120.dp)
        Spacer(Modifier.height(spacing.xl))
        BasicText(
            stringResource(R.string.language_title),
            style = type.display.copy(color = colors.text, textAlign = TextAlign.Center),
        )
        Spacer(Modifier.height(spacing.md))
        BasicText(
            stringResource(R.string.language_body),
            style = type.body.copy(color = colors.textMuted, textAlign = TextAlign.Center),
        )
        Spacer(Modifier.height(spacing.xl))
        HainButton("Deutsch", { onPick(LANGUAGE_DE) }, Modifier.fillMaxWidth())
        Spacer(Modifier.height(spacing.md))
        HainButton("English", { onPick(LANGUAGE_EN) }, Modifier.fillMaxWidth(), HainButtonStyle.SECONDARY)
    }
}
