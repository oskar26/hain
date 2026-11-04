package app.hain

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import app.hain.common.api.AppIcon

/**
 * T-103: switches the launcher icon between the two `activity-alias` entries in the manifest
 * ("Pim im Wald" 3D = `.Launcher3D`, flat = `.LauncherFlat`). Both start [MainActivity], so
 * intents to `MainActivity` (adb, notifications) keep working. The new alias is enabled first and
 * the old one disabled after, so there is never a moment without a launcher entry. The launcher
 * may need a few seconds to show the change.
 */
object AppIconSwitcher {
    /** Class names of the aliases; the package is the app's (also `app.hain.debug`). */
    const val ALIAS_3D = "app.hain.Launcher3D"
    const val ALIAS_FLAT = "app.hain.LauncherFlat"

    fun aliasFor(icon: AppIcon): String = if (icon == AppIcon.FLAT) ALIAS_FLAT else ALIAS_3D

    /** Makes [icon] the visible launcher entry; does nothing if it already is. */
    fun apply(
        context: Context,
        icon: AppIcon,
    ) {
        val pm = context.packageManager
        val flat = icon == AppIcon.FLAT
        val on = ComponentName(context.packageName, aliasFor(icon))
        val off = ComponentName(context.packageName, if (flat) ALIAS_3D else ALIAS_FLAT)
        if (isEnabled(pm, on, defaultOn = !flat) && !isEnabled(pm, off, defaultOn = flat)) return
        pm.setComponentEnabledSetting(on, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, DONT_KILL)
        pm.setComponentEnabledSetting(off, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, DONT_KILL)
    }

    /** `defaultOn` = what the manifest says (`android:enabled`), used while the state is DEFAULT. */
    private fun isEnabled(
        pm: PackageManager,
        name: ComponentName,
        defaultOn: Boolean,
    ): Boolean =
        when (pm.getComponentEnabledSetting(name)) {
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED -> true
            PackageManager.COMPONENT_ENABLED_STATE_DEFAULT -> defaultOn
            else -> false
        }
}

private const val DONT_KILL = PackageManager.DONT_KILL_APP
