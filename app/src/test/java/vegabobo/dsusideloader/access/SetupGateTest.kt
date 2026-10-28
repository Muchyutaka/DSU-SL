package vegabobo.dsusideloader.access

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The one-time onboarding rule: the flow is only skipped once it was finished
 * and a method was persisted.
 */
class SetupGateTest {

    @Test
    fun `a finished setup with a method skips onboarding`() {
        assertTrue(SetupGate.canSkipOnboarding(hasCompletedSetup = true, persistedMethod = AccessMethod.ADB))
        assertTrue(
            SetupGate.canSkipOnboarding(
                hasCompletedSetup = true,
                persistedMethod = AccessMethod.SHIZUKU,
            ),
        )
    }

    @Test
    fun `an unfinished setup never skips onboarding`() {
        assertFalse(
            SetupGate.canSkipOnboarding(
                hasCompletedSetup = false,
                persistedMethod = AccessMethod.ROOT,
            ),
        )
    }

    @Test
    fun `a finished setup without a persisted method asks again`() {
        assertFalse(SetupGate.canSkipOnboarding(hasCompletedSetup = true, persistedMethod = null))
    }

    @Test
    fun `the old confirmation flag still counts as a finished setup`() {
        assertTrue(SetupGate.hasFinishedSetup(hasCompletedSetup = false, accessMethodConfirmed = true))
        assertTrue(SetupGate.hasFinishedSetup(hasCompletedSetup = true, accessMethodConfirmed = false))
        assertFalse(SetupGate.hasFinishedSetup(hasCompletedSetup = false, accessMethodConfirmed = false))
    }
}
