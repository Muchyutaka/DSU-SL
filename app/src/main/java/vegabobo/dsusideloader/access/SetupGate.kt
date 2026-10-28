package vegabobo.dsusideloader.access

/**
 * Small, pure helpers for the one-time setup decision of the access gate.
 *
 * They are kept out of [AccessController] so the rules can be unit tested
 * without Android, a DataStore or a Shizuku service.
 */
object SetupGate {

    /**
     * Whether the app may open the main screen directly.
     *
     * The user has to walk through the onboarding flow once. Afterwards the
     * main screen is the entry point, even if the chosen method currently
     * fails: the status card explains the problem and offers "Re-check access"
     * and "Change method" instead of forcing the whole flow again on start-up.
     */
    fun canSkipOnboarding(hasCompletedSetup: Boolean, persistedMethod: AccessMethod?): Boolean =
        hasCompletedSetup && persistedMethod != null

    /**
     * Whether the persisted confirmation (written by versions without
     * [hasCompletedSetup]) counts as a finished setup. Used to migrate users
     * upgrading from an older build, so they are not sent through the flow
     * again.
     */
    fun hasFinishedSetup(hasCompletedSetup: Boolean, accessMethodConfirmed: Boolean): Boolean =
        hasCompletedSetup || accessMethodConfirmed
}
