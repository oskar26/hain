package app.hain.ui

import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Called right before Hain hands you to another app or a system screen you asked for (file picker,
 * share sheet). The app lock (T-056) then does not lock when you come back from it.
 */
val LocalExternalLaunch = staticCompositionLocalOf<() -> Unit> { {} }

/**
 * Opens something in another app (an article in the browser): tells the app lock first through
 * [external] ([LocalExternalLaunch]), then runs [open]. A failed [open] (no browser) is swallowed;
 * `true` when it worked.
 */
inline fun openExternally(
    external: () -> Unit,
    open: () -> Unit,
): Boolean {
    external()
    return runCatching { open() }.isSuccess
}
