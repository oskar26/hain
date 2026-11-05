package app.hain.feature.chat.api

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import app.hain.feature.chat.R
import app.hain.models.api.BrainUpgrade
import app.hain.ui.HainIcons
import app.hain.ui.components.HainIconButton
import app.hain.ui.pim.LivePim
import app.hain.ui.pim.PimState
import app.hain.ui.theme.LocalHainColors
import app.hain.ui.theme.LocalHainShapes
import app.hain.ui.theme.LocalHainSpacing
import app.hain.ui.theme.LocalHainTypography

/**
 * T-260: a quiet line above the chat while the switch to the new default brain (LFM3-Omni 2B) is open:
 * "get the new brain" while chats still run on LFM3 1.7B, then "remove the old one" to free its space.
 * Nothing changes by itself; the old brain keeps working until the new one is on the phone.
 */
@Composable
fun BrainUpgradeBanner(
    upgrade: BrainUpgrade,
    onGet: () -> Unit,
    onRemoveOld: () -> Unit,
    onLater: () -> Unit,
) {
    if (upgrade == BrainUpgrade.NONE) return
    val colors = LocalHainColors.current
    val spacing = LocalHainSpacing.current
    val type = LocalHainTypography.current
    val getNew = upgrade == BrainUpgrade.GET_NEW
    Row(
        Modifier
            .padding(horizontal = spacing.md)
            .fillMaxWidth()
            .background(colors.accentSoft, LocalHainShapes.current.card)
            .padding(start = spacing.md, top = spacing.sm, bottom = spacing.sm, end = spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LivePim(if (getNew) PimState.IDLE else PimState.HAPPY, 32.dp, tappable = false)
        Spacer(Modifier.width(spacing.sm))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            BasicText(
                stringResource(if (getNew) R.string.chat_brain_upgrade_title else R.string.chat_brain_old_title),
                style = type.label.copy(color = colors.text),
            )
            BasicText(
                stringResource(if (getNew) R.string.chat_brain_upgrade_body else R.string.chat_brain_old_body),
                style = type.bodySmall.copy(color = colors.textMuted),
            )
        }
        BasicText(
            stringResource(if (getNew) R.string.chat_brain_get else R.string.chat_brain_old_remove),
            style = type.label.copy(color = colors.accent),
            modifier =
                Modifier
                    .heightIn(min = 48.dp)
                    .clickable(role = Role.Button, onClick = if (getNew) onGet else onRemoveOld)
                    .padding(horizontal = spacing.sm, vertical = spacing.md),
        )
        HainIconButton(HainIcons.close, stringResource(R.string.chat_brain_later), onLater, tint = colors.textMuted)
    }
}
