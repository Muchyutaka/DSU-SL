package vegabobo.dsusideloader.access

/**
 * Result of a real access check for one [AccessMethod].
 *
 * Every value is derived from an actual API call, never from a guess:
 * - [AVAILABLE]      the method was verified to work right now
 * - [NOT_INSTALLED]  the companion app (Shizuku) is not installed
 * - [NOT_RUNNING]    Shizuku is installed but its service is not running
 * - [NOT_AUTHORIZED] installed/running but this app was not granted access
 * - [UNAVAILABLE]    the device cannot use this method at all
 * - [CHECKING]       a check or a grant request is in flight
 * - [UNKNOWN]        not checked yet, or the check could not be performed
 */
enum class AccessStatus {
    AVAILABLE,
    NOT_INSTALLED,
    NOT_RUNNING,
    NOT_AUTHORIZED,
    UNAVAILABLE,
    CHECKING,
    UNKNOWN;

    /**
     * Only a verified method may be confirmed, this is what gates the
     * "Continue" button.
     */
    fun canContinue(): Boolean = this == AVAILABLE

    /** Whether the user has something actionable to do (grant / install / start). */
    fun isActionable(): Boolean = when (this) {
        NOT_INSTALLED, NOT_RUNNING, NOT_AUTHORIZED -> true
        AVAILABLE, UNAVAILABLE, CHECKING, UNKNOWN -> false
    }

    /** Whether the badge should be tinted as a success. */
    fun isPositive(): Boolean = this == AVAILABLE

    /** Whether the badge should be tinted as a problem the user can fix. */
    fun isWarning(): Boolean = isActionable()

    /** Whether the badge should be tinted as an error. */
    fun isNegative(): Boolean = this == UNAVAILABLE
}

/**
 * Why a check ended with its [AccessStatus].
 *
 * Kept as an enum (instead of a formatted sentence) so every user facing text
 * lives in strings.xml and can be translated. [AccessCheckResult.detail] holds
 * the raw technical value (uid, version number, ...) for troubleshooting.
 */
enum class AccessReason {
    // Root
    ROOT_GRANTED,
    ROOT_DENIED,
    ROOT_NOT_AVAILABLE,
    ROOT_CHECK_FAILED,

    // Shizuku
    SHIZUKU_AVAILABLE,
    SHIZUKU_NOT_INSTALLED,
    SHIZUKU_NOT_RUNNING,
    SHIZUKU_NOT_AUTHORIZED,
    SHIZUKU_CHECK_FAILED,

    // ADB
    ADB_READY,
    ADB_DEBUGGING_DISABLED,
    ADB_DSU_APP_MISSING,
    ADB_CHECK_FAILED,

    // App installed as a system app (Magisk module)
    SYSTEM_PRIVILEGES,

    // Nothing checked yet
    NOT_CHECKED;

    fun isSuccess(): Boolean = when (this) {
        ROOT_GRANTED, SHIZUKU_AVAILABLE, ADB_READY, SYSTEM_PRIVILEGES -> true
        else -> false
    }
}

/**
 * A check result with an optional technical detail that is shown under the
 * method card.
 */
data class AccessCheckResult(
    val status: AccessStatus = AccessStatus.UNKNOWN,
    val reason: AccessReason = AccessReason.NOT_CHECKED,
    val detail: String? = null,
) {
    fun canContinue(): Boolean = status.canContinue()

    companion object {
        fun checking(): AccessCheckResult = AccessCheckResult(AccessStatus.CHECKING)

        fun available(reason: AccessReason, detail: String? = null): AccessCheckResult =
            AccessCheckResult(AccessStatus.AVAILABLE, reason, detail)

        fun failed(status: AccessStatus, reason: AccessReason, detail: String? = null) =
            AccessCheckResult(status, reason, detail)

        fun unknown(): AccessCheckResult = AccessCheckResult(AccessStatus.UNKNOWN)
    }
}

/**
 * What the app should show: the onboarding flow or the main screen.
 */
sealed interface AccessGateState {
    /** Preferences are still being read. */
    object Loading : AccessGateState

    /**
     * No usable method is confirmed. [previousMethod] is the method that was
     * confirmed before but stopped working, so the UI can explain why the
     * onboarding screen is shown again.
     */
    data class NeedsOnboarding(
        val previousMethod: AccessMethod? = null,
        val reason: String? = null,
    ) : AccessGateState

    /** A method was confirmed, [automatic] is true for system app installs. */
    data class Confirmed(
        val method: AccessMethod,
        val operationMode: vegabobo.dsusideloader.util.OperationMode,
        val automatic: Boolean = false,
    ) : AccessGateState
}
