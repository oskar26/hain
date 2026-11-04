package app.hain.common.api.lock

import app.hain.common.api.LockDelay
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppLockGateTest {
    @Test
    fun policyLocksOnlyWhenOnAndAwayLongEnough() {
        assertFalse(AppLockPolicy.shouldLock(false, LockDelay.IMMEDIATE, 0L, 10_000L))
        assertFalse(AppLockPolicy.shouldLock(true, LockDelay.IMMEDIATE, null, 10_000L))
        assertTrue(AppLockPolicy.shouldLock(true, LockDelay.IMMEDIATE, 5_000L, 5_000L))
        assertFalse(AppLockPolicy.shouldLock(true, LockDelay.ONE_MINUTE, 0L, 59_999L))
        assertTrue(AppLockPolicy.shouldLock(true, LockDelay.ONE_MINUTE, 0L, 60_000L))
        assertFalse(AppLockPolicy.shouldLock(true, LockDelay.FIVE_MINUTES, 0L, 299_999L))
        assertTrue(AppLockPolicy.shouldLock(true, LockDelay.FIVE_MINUTES, 0L, 300_000L))
    }

    @Test
    fun clockGoingBackwardsLocks() {
        assertTrue(AppLockPolicy.shouldLock(true, LockDelay.FIVE_MINUTES, 10_000L, 5_000L))
    }

    @Test
    fun startsLockedWhenTheLockIsOnAndUnlocksAfterAuth() {
        val gate = AppLockGate(initiallyLocked = true)
        assertTrue(gate.locked.value)
        gate.startAuth()
        gate.endAuth(success = false)
        assertTrue(gate.locked.value)
        gate.startAuth()
        gate.endAuth(success = true)
        assertFalse(gate.locked.value)
    }

    @Test
    fun locksAfterTheChosenTimeInTheBackground() {
        val gate = AppLockGate(initiallyLocked = false)
        gate.onBackground(0L)
        gate.onForeground(30_000L, enabled = true, delay = LockDelay.ONE_MINUTE)
        assertFalse(gate.locked.value)
        gate.onBackground(100_000L)
        gate.onForeground(161_000L, enabled = true, delay = LockDelay.ONE_MINUTE)
        assertTrue(gate.locked.value)
    }

    @Test
    fun neverLocksWhileTheLockIsOff() {
        val gate = AppLockGate(initiallyLocked = false)
        gate.onBackground(0L)
        gate.onForeground(10_000_000L, enabled = false, delay = LockDelay.IMMEDIATE)
        assertFalse(gate.locked.value)
    }

    @Test
    fun theSystemPromptItselfDoesNotLockAgain() {
        val gate = AppLockGate(initiallyLocked = true)
        gate.startAuth()
        // the PIN screen of the system sends Hain to the background and back
        gate.onBackground(0L)
        gate.onForeground(20_000L, enabled = true, delay = LockDelay.IMMEDIATE)
        gate.endAuth(success = true)
        assertFalse(gate.locked.value)
        // the next real trip to the background locks again
        gate.onBackground(30_000L)
        gate.onForeground(31_000L, enabled = true, delay = LockDelay.IMMEDIATE)
        assertTrue(gate.locked.value)
    }

    @Test
    fun filePickerOrShareSheetDoesNotLockUnlessYouStayAway() {
        val gate = AppLockGate(initiallyLocked = false)
        gate.expectReturn()
        gate.onBackground(0L)
        gate.onForeground(60_000L, enabled = true, delay = LockDelay.IMMEDIATE)
        assertFalse(gate.locked.value)
        // only once: the next trip locks right away
        gate.onBackground(70_000L)
        gate.onForeground(70_500L, enabled = true, delay = LockDelay.IMMEDIATE)
        assertTrue(gate.locked.value)
    }

    @Test
    fun longTripAfterSharingStillLocks() {
        val gate = AppLockGate(initiallyLocked = false)
        gate.expectReturn()
        gate.onBackground(0L)
        gate.onForeground(AppLockPolicy.EXTERNAL_GRACE_MS, enabled = true, delay = LockDelay.FIVE_MINUTES)
        assertTrue(gate.locked.value)
    }

    @Test
    fun disableOpensAtOnce() {
        val gate = AppLockGate(initiallyLocked = true)
        gate.disable()
        assertFalse(gate.locked.value)
    }
}
