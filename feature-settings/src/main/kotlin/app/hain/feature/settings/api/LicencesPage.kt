package app.hain.feature.settings.api

import android.content.res.AssetManager
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.hain.feature.settings.R
import app.hain.ui.HainIcons
import app.hain.ui.components.HainIconButton
import app.hain.ui.theme.LocalHainColors
import app.hain.ui.theme.LocalHainShapes
import app.hain.ui.theme.LocalHainSpacing
import app.hain.ui.theme.LocalHainTypography
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * T-285: "Open-Source-Lizenzen" below "Über Hain". On top Hain itself (GPL-3.0, link to the code),
 * then the groups (libraries, AI models, fonts, icons, engine, content): name, version and licence
 * per row, a tap shows the full licence text from the APK, the globe opens the project page.
 */
@Composable
internal fun LicencesPage(
    info: AppInfo,
    nav: SubNav,
) {
    val assets = LocalContext.current.assets
    val catalog = remember(assets) { OssLicenses.load(assets) }
    val open = nav.licence?.let(catalog::find)
    if (open != null) {
        BackHandler { nav.openLicence(null) }
        LicenceText(open, assets) { nav.openLicence(null) }
    } else {
        LicenceList(catalog, info, nav.openLicence)
    }
}

@Composable
private fun LicenceList(
    catalog: OssCatalog,
    info: AppInfo,
    onOpen: (String) -> Unit,
) {
    val colors = LocalHainColors.current
    val type = LocalHainTypography.current
    val spacing = LocalHainSpacing.current
    Info(R.string.licences_intro)
    LicenceRow(catalog.app.copy(version = info.versionName)) { onOpen(OssCatalog.APP_KEY) }
    if (catalog.count == 0) {
        BasicText(stringResource(R.string.licences_missing), style = type.bodySmall.copy(color = colors.danger))
    }
    catalog.groups.forEach { (group, entries) ->
        Group(group.label())
        Column(verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
            if (group == OssGroup.MODELS) {
                BasicText(
                    stringResource(R.string.licences_models_note),
                    style = type.bodySmall.copy(color = colors.textMuted),
                    modifier = Modifier.padding(bottom = spacing.xs),
                )
            }
            entries.forEach { entry -> LicenceRow(entry) { onOpen(entry.key(group)) } }
        }
    }
}

/** One project: name, "version · licence" below, the whole row opens the text, the globe the page. */
@Composable
private fun LicenceRow(
    entry: OssEntry,
    onOpen: () -> Unit,
) {
    val colors = LocalHainColors.current
    val type = LocalHainTypography.current
    val uri = LocalUriHandler.current
    Row(
        Modifier
            .fillMaxWidth()
            .clip(LocalHainShapes.current.row)
            .clickable(role = Role.Button, onClickLabel = stringResource(R.string.licences_show_text), onClick = onOpen)
            .padding(vertical = LocalHainSpacing.current.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            BasicText(entry.name, style = type.body.copy(color = colors.text))
            BasicText(entry.subtitle(), style = type.bodySmall.copy(color = colors.textMuted))
        }
        if (entry.url.isNotBlank()) {
            HainIconButton(
                HainIcons.globe,
                stringResource(R.string.licences_open_project, entry.name),
                { uri.openUri(entry.url) },
                tint = colors.icon,
            )
        }
    }
}

/** The full licence text of one project, offline from the APK, with the way back on top. */
@Composable
private fun LicenceText(
    entry: OssEntry,
    assets: AssetManager,
    onBack: () -> Unit,
) {
    val colors = LocalHainColors.current
    val type = LocalHainTypography.current
    val spacing = LocalHainSpacing.current
    val uri = LocalUriHandler.current
    // T-290: a licence text (the GPL is 35 kB) is read and reflowed off the main thread
    val text by produceState("", entry) { value = withContext(Dispatchers.IO) { OssLicenses.text(assets, entry) } }
    Row(
        Modifier
            .clip(LocalHainShapes.current.row)
            .clickable(role = Role.Button, onClick = onBack)
            .padding(vertical = spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.xs),
    ) {
        Image(HainIcons.back, null, Modifier.size(ICON.dp), colorFilter = ColorFilter.tint(colors.accent))
        BasicText(stringResource(R.string.licences_all), style = type.label.copy(color = colors.accent))
    }
    Column(verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
        BasicText(
            entry.name,
            style = type.title.copy(color = colors.text),
            modifier = Modifier.semantics { heading() },
        )
        BasicText(entry.subtitle(), style = type.label.copy(color = colors.textMuted))
    }
    if (entry.url.isNotBlank()) {
        Row(
            Modifier
                .fillMaxWidth()
                .clip(LocalHainShapes.current.row)
                .clickable(role = Role.Button) { uri.openUri(entry.url) }
                .padding(vertical = spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.md),
        ) {
            Image(HainIcons.globe, null, Modifier.size(ICON.dp), colorFilter = ColorFilter.tint(colors.icon))
            Column(Modifier.weight(1f)) {
                BasicText(stringResource(R.string.licences_project_page), style = type.body.copy(color = colors.text))
                BasicText(
                    entry.url.removePrefix("https://").trimEnd('/'),
                    style = type.bodySmall.copy(color = colors.textMuted),
                )
            }
        }
    }
    Group(R.string.licences_text)
    BasicText(text, style = type.bodySmall.copy(color = colors.text))
}

/** "1.13.0 · Apache-2.0", or only the licence when there is no version. */
internal fun OssEntry.subtitle(): String = listOf(version, licence).filter { it.isNotBlank() }.joinToString(" · ")

private fun OssGroup.label(): Int =
    when (this) {
        OssGroup.LIBRARIES -> R.string.licences_group_libraries
        OssGroup.MODELS -> R.string.licences_group_models
        OssGroup.FONTS -> R.string.licences_group_fonts
        OssGroup.ICONS -> R.string.licences_group_icons
        OssGroup.ENGINE -> R.string.licences_group_engine
        OssGroup.CONTENT -> R.string.licences_group_content
    }

private const val ICON = 20
