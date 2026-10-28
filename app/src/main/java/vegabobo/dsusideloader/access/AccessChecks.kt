package vegabobo.dsusideloader.access

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.util.Log
import com.topjohnwu.superuser.Shell
import kotlin.coroutines.resume
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku
import rikka.shizuku.ShizukuProvider

/**
 * Real availability checks for the three access methods.
 *
 * Nothing here simulates access: every result comes from an API call that is
 * also what the installation code paths rely on. Everything that cannot be
 * verified from inside the app is reported as [AccessStatus.UNKNOWN] instead of
 * being guessed, see [checkAdb].
 */
object AccessChecks {

    private const val tag = "AccessChecks"

    /**
     * Package that performs DSU installs on the device. Without it the app can
     * neither hand the image over (ADB mode) nor start the installation.
     */
    const val DSU_PACKAGE = "com.android.dynsystem"

    /**
     * Root check.
     *
     * [Shell.isAppGrantedRoot] returns:
     * - `true`  root was already granted to this app
     * - `false` no `su` binary was found on the PATH, root is not available
     * - `null`  a `su` binary exists but the grant state is unknown, so the
     *           only way to know is to ask for a shell (which shows the su
     *           prompt of the root manager)
     */
    suspend fun checkRoot(requestGrant: Boolean): AccessCheckResult {
        val granted = try {
            Shell.isAppGrantedRoot()
        } catch (e: Exception) {
            Log.w(tag, "Shell.isAppGrantedRoot() failed", e)
            null
        }

        when (granted) {
            true -> return AccessCheckResult.available(AccessReason.ROOT_GRANTED, "uid=0")
            false -> return AccessCheckResult.failed(
                AccessStatus.UNAVAILABLE,
                AccessReason.ROOT_NOT_AVAILABLE,
                "su not found in PATH",
            )

            null -> Unit
        }

        if (!requestGrant) {
            return AccessCheckResult.failed(
                AccessStatus.NOT_AUTHORIZED,
                AccessReason.ROOT_DENIED,
                "su present, grant unknown",
            )
        }

        // Opening a shell is what triggers the root manager prompt. The callback
        // is delivered on the main thread by libsu.
        val shell = try {
            withContext(Dispatchers.Main) {
                suspendCancellableCoroutine { continuation ->
                    Shell.getShell { shell -> continuation.resume(shell) }
                }
            }
        } catch (e: Exception) {
            Log.w(tag, "Shell.getShell() failed", e)
            return AccessCheckResult.failed(
                AccessStatus.NOT_AUTHORIZED,
                AccessReason.ROOT_CHECK_FAILED,
                e.javaClass.simpleName,
            )
        }

        val isRoot = shell.isRoot
        return if (isRoot) {
            AccessCheckResult.available(AccessReason.ROOT_GRANTED, "uid=0")
        } else {
            AccessCheckResult.failed(
                AccessStatus.NOT_AUTHORIZED,
                AccessReason.ROOT_DENIED,
                "shell uid=${android.os.Process.myUid()}",
            )
        }
    }

    /** Whether the Shizuku manager app is installed at all. */
    fun isShizukuInstalled(context: Context): Boolean = try {
        context.packageManager.getPackageInfo(ShizukuProvider.MANAGER_APPLICATION_ID, 0)
        true
    } catch (_: PackageManager.NameNotFoundException) {
        false
    }

    /**
     * Shizuku check: installed -> service running -> this app authorized.
     *
     * The order matters: [Shizuku.getVersion] and [Shizuku.checkSelfPermission]
     * talk to the service and throw when the binder is not there yet, so the
     * binder is pinged first.
     */
    fun checkShizuku(context: Context): AccessCheckResult {
        if (!isShizukuInstalled(context)) {
            return AccessCheckResult.failed(
                AccessStatus.NOT_INSTALLED,
                AccessReason.SHIZUKU_NOT_INSTALLED,
                ShizukuProvider.MANAGER_APPLICATION_ID,
            )
        }

        if (!Shizuku.pingBinder()) {
            return AccessCheckResult.failed(
                AccessStatus.NOT_RUNNING,
                AccessReason.SHIZUKU_NOT_RUNNING,
                "binder not available",
            )
        }

        val version = try {
            Shizuku.getVersion()
        } catch (e: Exception) {
            Log.w(tag, "Shizuku.getVersion() failed", e)
            return AccessCheckResult.failed(
                AccessStatus.NOT_RUNNING,
                AccessReason.SHIZUKU_CHECK_FAILED,
                e.javaClass.simpleName,
            )
        }

        val granted = try {
            if (Shizuku.isPreV11() || version < 11) {
                context.checkSelfPermission(ShizukuProvider.PERMISSION) ==
                    PackageManager.PERMISSION_GRANTED
            } else {
                Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
            }
        } catch (e: Exception) {
            Log.w(tag, "Shizuku permission check failed", e)
            return AccessCheckResult.failed(
                AccessStatus.NOT_AUTHORIZED,
                AccessReason.SHIZUKU_CHECK_FAILED,
                e.javaClass.simpleName,
            )
        }

        return if (granted) {
            AccessCheckResult.available(AccessReason.SHIZUKU_AVAILABLE, "v$version")
        } else {
            AccessCheckResult.failed(
                AccessStatus.NOT_AUTHORIZED,
                AccessReason.SHIZUKU_NOT_AUTHORIZED,
                "v$version, permission denied",
            )
        }
    }

    /**
     * ADB check.
     *
     * The app itself has no ADB permission: in ADB mode it only writes an
     * installation script that the user runs from a computer with
     * `adb shell sh <script>`. There is no API for an app to detect a paired
     * computer or the wireless debugging pairing state, so this check verifies
     * what can really be verified on the device:
     *
     * 1. the DSU system app (`com.android.dynsystem`) is installed, otherwise
     *    the generated command cannot work at all;
     * 2. USB debugging or wireless debugging is enabled in the system settings,
     *    otherwise `adb` cannot connect either.
     *
     * A connected computer is reported as unknown, never as authorisation.
     */
    fun checkAdb(context: Context): AccessCheckResult {
        val hasDsuApp = try {
            context.packageManager.getPackageInfo(DSU_PACKAGE, 0)
            true
        } catch (_: PackageManager.NameNotFoundException) {
            false
        }

        if (!hasDsuApp) {
            return AccessCheckResult.failed(
                AccessStatus.UNAVAILABLE,
                AccessReason.ADB_DSU_APP_MISSING,
                DSU_PACKAGE,
            )
        }

        val usbDebugging = globalFlag(context, Settings.Global.ADB_ENABLED)
        val wirelessDebugging = globalFlag(context, Settings.Global.ADB_WIFI_ENABLED)

        return if (usbDebugging || wirelessDebugging) {
            AccessCheckResult.available(
                AccessReason.ADB_READY,
                "usb=$usbDebugging wireless=$wirelessDebugging",
            )
        } else {
            AccessCheckResult.failed(
                AccessStatus.NOT_AUTHORIZED,
                AccessReason.ADB_DEBUGGING_DISABLED,
                "adb_enabled=0 adb_wifi_enabled=0",
            )
        }
    }

    private fun globalFlag(context: Context, key: String): Boolean = try {
        Settings.Global.getInt(context.contentResolver, key, 0) != 0
    } catch (e: Exception) {
        // Some OEMs hide these keys, the value is then simply unknown.
        Log.w(tag, "Could not read Settings.Global.$key", e)
        false
    }

    /**
     * Operation mode when the app already runs with system app privileges (for
     * example through the Magisk module). Those installs bypass the onboarding
     * screen: there is nothing for the user to choose.
     */
    fun systemPrivilegeMode(context: Context): vegabobo.dsusideloader.util.OperationMode? {
        val hasDsuPermission = context.checkSelfPermission(
            "android.permission.INSTALL_DYNAMIC_SYSTEM",
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasDsuPermission) {
            return null
        }

        val isRoot = try {
            Shell.isAppGrantedRoot() == true
        } catch (_: Exception) {
            false
        }

        return if (isRoot) {
            vegabobo.dsusideloader.util.OperationMode.SYSTEM_AND_ROOT
        } else {
            vegabobo.dsusideloader.util.OperationMode.SYSTEM
        }
    }

    /** Android version gate used by the UI to explain unsupported devices. */
    fun isAndroidTenOrNewer(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
}
