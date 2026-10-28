package vegabobo.dsusideloader.util

import android.content.Context
import android.content.pm.PackageManager
import rikka.shizuku.Shizuku
import rikka.shizuku.ShizukuProvider

/**
 * DSU Sideloader operation modes
 *
 * ADB (unrooted): Default operation mode when other modes aren't available
 * Features:
 *   - Only prepares selected GSI to be installed using DSU system-app
 *   - Starting DSU installation requires a adb command
 *
 * Shizuku (as adb): When running app with Shizuku permission
 * Features:
 *  - Prepares selected GSI to be installed using DSU system-app
 *  - No adb command is required to start installation
 *  - Track installation progress
 *  - Diagnostics DSU installation
 *
 * Root: When running app with root permissions
 * Features:
 *  - All features presents when using Shizuku mode
 *  - Detects if there is a DSU installed
 *  - Supports reboot into installed DSU without relying on DSU system-app
 *  - Supports built-in DSU installer
 *  - Enhanced DSU installation diagnostics
 *
 * System mode: When running as system-app (eg: when using our Magisk module)
 * Features:
 *  - All features presents when using Shizuku mode
 *  - Custom gsid binary when available
 *  - Fixes for some selinux denials
 *
 *  Root/System mode: When running as system-app with granted root permission
 *  Features:
 *   - All features available in System and Root mode.
 *
 */
enum class OperationMode {
    // ///////////////// priority
    SYSTEM_AND_ROOT, // #1
    SYSTEM, // #2
    ROOT, // #3
    SHIZUKU, // #4
    ADB, // #5
}

class OperationModeUtils {

    companion object {

        /**
         * The access method is no longer auto-detected: the user picks and
         * verifies it in the onboarding screen (see AccessChecks and
         * AccessController) and the result is applied by MainActivity.
         */
        fun getOperationModeAsString(operationMode: OperationMode): String {
            return when (operationMode) {
                OperationMode.SYSTEM_AND_ROOT -> "Root/System"
                OperationMode.SYSTEM -> "System"
                OperationMode.ROOT -> "Root"
                OperationMode.ADB -> "ADB"
                OperationMode.SHIZUKU -> "Shizuku"
            }
        }

        private fun isPermissionGranted(context: Context, permission: String): Boolean {
            return context.checkCallingOrSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
        }

        fun isReadLogsPermissionGranted(context: Context): Boolean {
            val readLogsPermission = "android.permission.READ_LOGS"
            return isPermissionGranted(context, readLogsPermission)
        }

        fun isShizukuPermissionGranted(context: Context): Boolean {
            return if (Shizuku.isPreV11() || Shizuku.getVersion() < 11) {
                isPermissionGranted(context, ShizukuProvider.PERMISSION)
            } else {
                Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
            }
        }
    }
}
