package app.hain.feature.settings.api

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import app.hain.feature.settings.R
import app.hain.ui.HainIcons
import app.hain.ui.components.HainIconButton
import app.hain.ui.pim.LivePim
import app.hain.ui.pim.PimState
import app.hain.ui.theme.HainLevel
import app.hain.ui.theme.LocalHainColors
import app.hain.ui.theme.LocalHainShapes
import app.hain.ui.theme.LocalHainSpacing
import app.hain.ui.theme.LocalHainTypography
import app.hain.ui.theme.hainSurface

/**
 * T-212: calm banner above the chat when a newer build is out. A tap on "Ansehen" opens the release
 * page in the browser; Hain never downloads or installs anything by itself.
 */
@Composable
fun UpdateBanner(
    versionName: String,
    onOpen: () -> Unit,
    onClose: () -> Unit,
) {
    val colors = LocalHainColors.current
    val spacing = LocalHainSpacing.current
    val type = LocalHainTypography.current
    Row(
        Modifier
            .padding(horizontal = spacing.md, vertical = spacing.xs)
            .fillMaxWidth()
            .background(colors.accentSoft, LocalHainShapes.current.card)
            .padding(start = spacing.md, top = spacing.xs, bottom = spacing.xs, end = spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LivePim(PimState.HAPPY, 28.dp, tappable = false)
        Spacer(Modifier.width(spacing.sm))
        BasicText(
            stringResource(R.string.update_banner, versionName),
            style = type.bodySmall.copy(color = colors.text),
            modifier = Modifier.weight(1f),
        )
        BasicText(
            stringResource(R.string.update_banner_open),
            style = type.label.copy(color = colors.accent),
            modifier =
                Modifier
                    .clickable(role = Role.Button, onClick = onOpen)
                    .padding(horizontal = spacing.sm, vertical = spacing.sm),
        )
        HainIconButton(HainIcons.close, stringResource(R.string.update_banner_close), onClose, tint = colors.textMuted)
    }
}

/**
 * T-212: the one-time question, shown after the third chat (never on the first start): should Hain
 * check GitHub for a newer version once a day? Both answers close it for good.
 */
@Composable
fun UpdateAskCard(
    onYes: () -> Unit,
    onNo: () -> Unit,
) {
    val colors = LocalHainColors.current
    val spacing = LocalHainSpacing.current
    val type = LocalHainTypography.current
    Column(
        Modifier
            .padding(horizontal = spacing.md, vertical = spacing.xs)
            .fillMaxWidth()
            .hainSurface(HainLevel.RAISED, LocalHainShapes.current.card)
            .padding(spacing.md),
        verticalArrangement = Arrangement.spacedBy(spacing.xs),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            LivePim(PimState.IDLE, 28.dp, tappable = false)
            Spacer(Modifier.width(spacing.sm))
            BasicText(stringResource(R.string.update_ask_title), style = type.button.copy(color = colors.text))
        }
        BasicText(stringResource(R.string.update_ask_text), style = type.bodySmall.copy(color = colors.textMuted))
        Row(horizontalArrangement = Arrangement.spacedBy(spacing.lg)) {
            BasicText(
                stringResource(R.string.update_ask_yes),
                style = type.button.copy(color = colors.accent),
                modifier = Modifier.clickable(role = Role.Button, onClick = onYes).padding(vertical = spacing.xs),
            )
            BasicText(
                stringResource(R.string.update_ask_no),
                style = type.button.copy(color = colors.textMuted),
                modifier = Modifier.clickable(role = Role.Button, onClick = onNo).padding(vertical = spacing.xs),
            )
        }
    }
}
