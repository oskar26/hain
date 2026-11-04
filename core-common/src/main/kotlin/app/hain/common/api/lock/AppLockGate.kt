package app.hain.common.api.lock

import app.hain.common.api.LockDelay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** When Hain locks again after it went to the background (T-056), without Android. */
object AppLockPolicy {
    /**
     * `true` when the lock is on and Hain was in the background for at least [delay]. A clock that
     * went backwards counts as "long ago" (better locked than open).
     */
    fun shouldLock(
        enabled: Boolean,
        delay: LockDelay,
        backgroundSince: Long?,
        now: Long,
    ): Boolean {
        if (!enabled || backgroundSince == null) return false
        val away = now - backgroundSince
        return away < 0 || away >= delay.millis
    }

    /**
     * A file picker or share sheet you opened from Hain does not lock it, unless you stay away
     * longer than this (then you probably went somewhere else).
     */
    const val EXTERNAL_GRACE_MS = 5 * 60 * 1000L
}

/**
 * The state of the app lock (T-056): locked or not, fed with the activity's background and
 * foreground moments. The fingerprint / PIN screen of the system itself sends Hain to the background
 * for a moment, so those moments are ignored while [startAuth] runs.
 */
class AppLockGate(
    initiallyLocked: Boolean,
) {
    private val mutableLocked = MutableStateFlow(initiallyLocked)
    val locked: StateFlow<Boolean> = mutableLocked.asStateFlow()

    private var backgroundSince: Long? = null
    private var expectingReturn = false
    private var authenticating = false

    /** Hain left the screen (activity stopped, not on a rotation). */
    fun onBackground(now: Long) {
        if (authenticating) return
        if (backgroundSince == null) backgroundSince = now
    }

    /** Hain is back on screen; locks when [AppLockPolicy] says so. */
    fun onForeground(
        now: Long,
        enabled: Boolean,
        delay: LockDelay,
    ) {
        if (authenticating) return
        val since = backgroundSince
        backgroundSince = null
        val lock =
            if (expectingReturn) {
                enabled && since != null && (now < since || now - since >= AppLockPolicy.EXTERNAL_GRACE_MS)
            } else {
                AppLockPolicy.shouldLock(enabled, delay, since, now)
            }
        expectingReturn = false
        if (lock) mutableLocked.value = true
    }

    /** You are about to open a file picker or share sheet from Hain. */
    fun expectReturn() {
        expectingReturn = true
    }

    fun startAuth() {
        authenticating = true
    }

    /** The system prompt closed; [success] unlocks. */
    fun endAuth(success: Boolean) {
        authenticating = false
        backgroundSince = null
        if (success) mutableLocked.value = false
    }

    /** Lock switched off in the settings: open at once. */
    fun disable() {
        mutableLocked.value = false
    }
}
