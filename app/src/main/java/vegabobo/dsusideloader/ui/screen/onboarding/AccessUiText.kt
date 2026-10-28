package vegabobo.dsusideloader.ui.screen.onboarding

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import vegabobo.dsusideloader.R
import vegabobo.dsusideloader.access.AccessCheckResult
import vegabobo.dsusideloader.access.AccessMethod
import vegabobo.dsusideloader.access.AccessReason
import vegabobo.dsusideloader.access.AccessStatus

/** What the primary button of an access card should do. */
enum class AccessPrimaryAction {
    CHECK,
    GRANT,
    OPEN_APP,
    INSTALL_APP,
}

@Composable
fun accessStatusLabel(status: AccessStatus): String = when (status) {
    AccessStatus.AVAILABLE -> stringResource(R.string.access_status_available)
    AccessStatus.NOT_INSTALLED -> stringResource(R.string.access_status_not_installed)
    AccessStatus.NOT_RUNNING -> stringResource(R.string.access_status_not_running)
    AccessStatus.NOT_AUTHORIZED -> stringResource(R.string.access_status_not_authorized)
    AccessStatus.UNAVAILABLE -> stringResource(R.string.access_status_unavailable)
    AccessStatus.CHECKING -> stringResource(R.string.access_status_checking)
    AccessStatus.UNKNOWN -> stringResource(R.string.access_status_unknown)
}

@Composable
fun accessReasonText(reason: AccessReason): String = when (reason) {
    AccessReason.ROOT_GRANTED -> stringResource(R.string.access_reason_root_granted)
    AccessReason.ROOT_DENIED -> stringResource(R.string.access_reason_root_denied)
    AccessReason.ROOT_NOT_AVAILABLE -> stringResource(R.string.access_reason_root_not_available)
    AccessReason.ROOT_CHECK_FAILED -> stringResource(R.string.access_reason_root_check_failed)
    AccessReason.SHIZUKU_AVAILABLE -> stringResource(R.string.access_reason_shizuku_available)
    AccessReason.SHIZUKU_NOT_INSTALLED -> stringResource(R.string.access_reason_shizuku_not_installed)
    AccessReason.SHIZUKU_NOT_RUNNING -> stringResource(R.string.access_reason_shizuku_not_running)
    AccessReason.SHIZUKU_NOT_AUTHORIZED ->
        stringResource(R.string.access_reason_shizuku_not_authorized)

    AccessReason.SHIZUKU_CHECK_FAILED -> stringResource(R.string.access_reason_shizuku_check_failed)
    AccessReason.ADB_READY -> stringResource(R.string.access_reason_adb_ready)
    AccessReason.ADB_DEBUGGING_DISABLED -> stringResource(R.string.access_reason_adb_debugging_disabled)
    AccessReason.ADB_DSU_APP_MISSING -> stringResource(R.string.access_reason_adb_dsu_app_missing)
    AccessReason.ADB_CHECK_FAILED -> stringResource(R.string.access_reason_adb_check_failed)
    AccessReason.SYSTEM_PRIVILEGES -> stringResource(R.string.access_system_privileges)
    AccessReason.NOT_CHECKED -> stringResource(R.string.access_reason_not_checked)
}

@Composable
fun accessMethodTitle(method: AccessMethod): String = when (method) {
    AccessMethod.ADB -> stringResource(R.string.access_method_adb)
    AccessMethod.SHIZUKU -> stringResource(R.string.access_method_shizuku)
    AccessMethod.ROOT -> stringResource(R.string.access_method_root)
}

@Composable
fun accessMethodSummary(method: AccessMethod): String = when (method) {
    AccessMethod.ADB -> stringResource(R.string.access_method_adb_summary)
    AccessMethod.SHIZUKU -> stringResource(R.string.access_method_shizuku_summary)
    AccessMethod.ROOT -> stringResource(R.string.access_method_root_summary)
}

/** Container/content colors of the status badge, all from the M3 scheme. */
data class AccessStatusColors(val container: Color, val content: Color)

@Composable
fun accessStatusColors(status: AccessStatus): AccessStatusColors {
    val colorScheme = MaterialTheme.colorScheme
    return when {
        status.isPositive() -> AccessStatusColors(
            container = colorScheme.primaryContainer,
            content = colorScheme.onPrimaryContainer,
        )

        status.isWarning() -> AccessStatusColors(
            container = colorScheme.secondaryContainer,
            content = colorScheme.onSecondaryContainer,
        )

        status.isNegative() -> AccessStatusColors(
            container = colorScheme.errorContainer,
            content = colorScheme.onErrorContainer,
        )

        else -> AccessStatusColors(
            container = colorScheme.surfaceVariant,
            content = colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * Label and behaviour of the card button, derived from the real check result.
 * Returns null while a check is running.
 */
@Composable
fun accessPrimaryAction(
    method: AccessMethod,
    result: AccessCheckResult,
): AccessPrimaryAction? = when {
    result.status == AccessStatus.CHECKING -> null
    result.status == AccessStatus.AVAILABLE -> AccessPrimaryAction.CHECK
    // A method the device simply cannot use has nothing to grant.
    result.status == AccessStatus.UNAVAILABLE -> AccessPrimaryAction.CHECK
    method == AccessMethod.SHIZUKU && result.reason == AccessReason.SHIZUKU_NOT_INSTALLED ->
        AccessPrimaryAction.INSTALL_APP

    method == AccessMethod.SHIZUKU && result.reason == AccessReason.SHIZUKU_NOT_RUNNING ->
        AccessPrimaryAction.OPEN_APP

    method == AccessMethod.ROOT && result.status == AccessStatus.NOT_AUTHORIZED ->
        AccessPrimaryAction.GRANT

    method == AccessMethod.SHIZUKU && result.status == AccessStatus.NOT_AUTHORIZED ->
        AccessPrimaryAction.GRANT

    else -> AccessPrimaryAction.CHECK
}

@Composable
fun accessPrimaryActionLabel(
    action: AccessPrimaryAction,
    method: AccessMethod,
): String = when (action) {
    AccessPrimaryAction.CHECK -> stringResource(R.string.access_check_button)
    AccessPrimaryAction.GRANT -> stringResource(R.string.access_grant_button)
    AccessPrimaryAction.OPEN_APP -> stringResource(R.string.access_open_app_button, "Shizuku")
    AccessPrimaryAction.INSTALL_APP -> stringResource(R.string.access_shizuku_install_hint)
}
