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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import app.hain.feature.onboarding.R
import app.hain.ui.components.AppIconPicker
import app.hain.ui.components.HainButton
import app.hain.ui.theme.LocalHainColors
import app.hain.ui.theme.LocalHainSpacing
import app.hain.ui.theme.LocalHainTypography

/**
 * T-103: short step right after the language: pick the launcher icon "Pim im Wald" (3D or flat).
 * [flat] is the current choice (default 3D); [onSelect] switches it at once, [onNext] goes on.
 */
@Composable
fun IconStep(
    flat: Boolean,
    onSelect: (flat: Boolean) -> Unit,
    onNext: () -> Unit,
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
            .verticalScroll(rememberScrollState())
            .padding(spacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        BasicText(
            stringResource(R.string.icon_step_title),
            style = type.display.copy(color = colors.text, textAlign = TextAlign.Center),
        )
        Spacer(Modifier.height(spacing.md))
        BasicText(
            stringResource(R.string.icon_step_body),
            style = type.body.copy(color = colors.textMuted, textAlign = TextAlign.Center),
        )
        Spacer(Modifier.height(spacing.xl))
        AppIconPicker(
            flat = flat,
            label3d = stringResource(R.string.icon_step_3d),
            labelFlat = stringResource(R.string.icon_step_flat),
            onSelect = onSelect,
        )
        Spacer(Modifier.height(spacing.md))
        BasicText(
            stringResource(R.string.icon_step_hint),
            style = type.bodySmall.copy(color = colors.textMuted, textAlign = TextAlign.Center),
        )
        Spacer(Modifier.height(spacing.xl))
        HainButton(stringResource(R.string.icon_step_next), onNext, Modifier.fillMaxWidth())
    }
}
