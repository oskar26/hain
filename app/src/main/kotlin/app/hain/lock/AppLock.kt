package app.hain.lock

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import app.hain.common.api.LockDelay
import app.hain.common.api.lock.AppLockGate
import kotlinx.coroutines.flow.StateFlow

/**
 * App lock (T-056) with androidx.biometric: fingerprint/face of the phone, or its PIN, pattern or
 * password as fallback (BiometricPrompt with DEVICE_CREDENTIAL; BIOMETRIC_WEAK because STRONG plus
 * DEVICE_CREDENTIAL is not allowed on Android 10). Nothing is stored by Hain: the phone checks.
 * Create in onCreate (BiometricPrompt must exist before the activity is resumed).
 */
class AppLock(
    private val activity: FragmentActivity,
    initiallyLocked: Boolean,
) {
    private val gate = AppLockGate(initiallyLocked)
    val locked: StateFlow<Boolean> = gate.locked

    private var pending: ((Boolean) -> Unit)? = null

    private val prompt =
        BiometricPrompt(
            activity,
            ContextCompat.getMainExecutor(activity),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) = finish(true)

                override fun onAuthenticationError(
                    errorCode: Int,
                    errString: CharSequence,
                ) = finish(false)
                // onAuthenticationFailed (one wrong finger): the system prompt stays open, nothing to do
            },
        )

    private fun finish(success: Boolean) {
        gate.endAuth(success)
        val done = pending
        pending = null
        done?.invoke(success)
    }

    private fun status(): Int = BiometricManager.from(activity).canAuthenticate(AUTHENTICATORS)

    /** The phone has a screen lock Hain can ask for. */
    fun available(): Boolean = status() == BiometricManager.BIOMETRIC_SUCCESS

    /** Shows the system prompt; [onResult] gets `true` when the phone confirmed it is you. */
    fun authenticate(
        title: String,
        subtitle: String,
        onResult: (Boolean) -> Unit,
    ) {
        if (pending != null) return
        pending = onResult
        gate.startAuth()
        val info =
            BiometricPrompt.PromptInfo
                .Builder()
                .setTitle(title)
                .setSubtitle(subtitle)
                .setAllowedAuthenticators(AUTHENTICATORS)
                .setConfirmationRequired(false)
                .build()
        runCatching { prompt.authenticate(info) }.onFailure { finish(false) }
    }

    /**
     * Unlock from the lock screen. If the phone no longer has any screen lock at all, nothing could
     * be checked and nobody could ever get back in, so Hain opens (the phone itself is open anyway).
     */
    fun unlock(
        title: String,
        subtitle: String,
    ) {
        if (status() == BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED) {
            gate.startAuth()
            gate.endAuth(true)
            return
        }
        authenticate(title, subtitle) {}
    }

    fun onBackground(now: Long) = gate.onBackground(now)

    fun onForeground(
        now: Long,
        enabled: Boolean,
        delay: LockDelay,
    ) = gate.onForeground(now, enabled, delay)

    /** A file picker or share sheet opened from Hain: coming back from it does not lock. */
    fun expectReturn() = gate.expectReturn()

    fun disable() = gate.disable()

    private companion object {
        const val AUTHENTICATORS = BIOMETRIC_WEAK or DEVICE_CREDENTIAL
    }
}
