package vegabobo.dsusideloader.access

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku
import rikka.shizuku.ShizukuProvider
import vegabobo.dsusideloader.model.Session
import vegabobo.dsusideloader.preferences.AppPrefs
import vegabobo.dsusideloader.util.DataStoreUtils
import vegabobo.dsusideloader.util.OperationMode

/**
 * Single source of truth for "how may this app install a DSU right now".
 *
 * Owns:
 * - the gate state used to decide between the onboarding screen and the main
 *   screen ([gateState]);
 * - the per method check results shown as badges on the onboarding cards;
 * - the persistence of the confirmed method.
 *
 * The controller is a singleton so the activity (which binds the privileged
 * service) and the UI observe the same state.
 */
@Singleton
class AccessController @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dataStore: DataStore<Preferences>,
    private val session: Session,
) {

    private val tag = this.javaClass.simpleName

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _gateState = MutableStateFlow<AccessGateState>(AccessGateState.Loading)
    val gateState: StateFlow<AccessGateState> = _gateState.asStateFlow()

    private val _selectedMethod = MutableStateFlow<AccessMethod?>(null)
    val selectedMethod: StateFlow<AccessMethod?> = _selectedMethod.asStateFlow()

    private val _checkResults = MutableStateFlow<Map<AccessMethod, AccessCheckResult>>(
        AccessMethod.values().associateWith { AccessCheckResult.unknown() },
    )
    val checkResults: StateFlow<Map<AccessMethod, AccessCheckResult>> =
        _checkResults.asStateFlow()

    /** True while the persisted method is being re-validated on start-up. */
    private val _isResolving = MutableStateFlow(true)
    val isResolving: StateFlow<Boolean> = _isResolving.asStateFlow()

    private var permissionListener: Shizuku.OnRequestPermissionResultListener? = null

    init {
        resolveGate()
    }

    //
    // Gate
    //

    /**
     * Decides what to show: system app installs and a previously confirmed
     * method (re-validated with a real check) go straight to the main screen,
     * everything else goes to the onboarding screen.
     */
    private fun resolveGate() {
        scope.launch {
            _isResolving.value = true

            val systemMode = AccessChecks.systemPrivilegeMode(context)
            if (systemMode != null) {
                Log.d(tag, "System app privileges detected, skipping onboarding")
                val method = AccessMethod.fromOperationMode(systemMode)
                val result = AccessCheckResult.available(
                    AccessReason.SYSTEM_PRIVILEGES,
                    systemMode.name,
                )
                _selectedMethod.value = method
                _checkResults.update { results -> results + (method to result) }
                _gateState.value = AccessGateState.Confirmed(
                    method = method,
                    operationMode = systemMode,
                    automatic = true,
                )
                _isResolving.value = false
                return@launch
            }

            val persistedMethod = AccessMethod.fromPreferenceValue(
                DataStoreUtils.readStringPref(dataStore, AppPrefs.ACCESS_METHOD, ""),
            )
            val persistedConfirmed = DataStoreUtils.readBoolPref(
                dataStore,
                AppPrefs.ACCESS_METHOD_CONFIRMED,
                false,
            )

            if (persistedMethod != null && persistedConfirmed) {
                _selectedMethod.value = persistedMethod
                val result = check(persistedMethod, requestGrant = false)
                if (result.canContinue()) {
                    _gateState.value = AccessGateState.Confirmed(
                        method = persistedMethod,
                        operationMode = persistedMethod.toOperationMode(),
                    )
                    _isResolving.value = false
                    return@launch
                }
                // The method worked before but not anymore (Shizuku stopped,
                // root revoked, ...): ask the user again instead of silently
                // falling back to another method.
                _gateState.value = AccessGateState.NeedsOnboarding(
                    previousMethod = persistedMethod,
                    reason = result.reason.name,
                )
                _isResolving.value = false
                return@launch
            }

            _gateState.value = AccessGateState.NeedsOnboarding()
            _isResolving.value = false
        }
    }

    //
    // Checks
    //

    /** Runs the real check for [method] and stores the result. */
    suspend fun check(method: AccessMethod, requestGrant: Boolean = false): AccessCheckResult {
        _checkResults.update { it + (method to AccessCheckResult.checking()) }
        val result = withContext(Dispatchers.Default) {
            when (method) {
                AccessMethod.ADB -> AccessChecks.checkAdb(context)
                AccessMethod.SHIZUKU -> AccessChecks.checkShizuku(context)
                AccessMethod.ROOT -> AccessChecks.checkRoot(requestGrant)
            }
        }
        _checkResults.update { it + (method to result) }
        Log.d(tag, "check($method) -> ${result.status} (${result.reason})")
        return result
    }

    /** User tapped a method card: select it and check it immediately. */
    fun select(method: AccessMethod) {
        _selectedMethod.value = method
        scope.launch { check(method) }
    }

    /** Re-check without changing the selection (Settings > Access > Re-check). */
    fun recheck(method: AccessMethod? = _selectedMethod.value ?: currentMethod()) {
        if (method == null) return
        scope.launch { check(method) }
    }

    /**
     * "Grant" / "Check" button of a card.
     *
     * - Shizuku asks its own service for the permission (the Shizuku app shows
     *   the dialog), then re-checks.
     * - Root opens a shell, which makes the root manager show its su prompt,
     *   then re-checks.
     * - ADB has no grant to request, the check is simply repeated.
     */
    fun requestGrant(method: AccessMethod) {
        when (method) {
            AccessMethod.SHIZUKU -> requestShizukuPermission()
            AccessMethod.ROOT -> scope.launch {
                _checkResults.update { it + (method to AccessCheckResult.checking()) }
                val result = withContext(Dispatchers.Default) {
                    AccessChecks.checkRoot(requestGrant = true)
                }
                _checkResults.update { it + (method to result) }
            }

            AccessMethod.ADB -> recheck(method)
        }
    }

    private fun requestShizukuPermission() {
        if (!AccessChecks.isShizukuInstalled(context)) {
            recheck(AccessMethod.SHIZUKU)
            return
        }
        if (!Shizuku.pingBinder()) {
            recheck(AccessMethod.SHIZUKU)
            return
        }

        val version = try {
            Shizuku.getVersion()
        } catch (e: Exception) {
            Log.w(tag, "Shizuku.getVersion() failed", e)
            recheck(AccessMethod.SHIZUKU)
            return
        }

        if (Shizuku.isPreV11() || version < 11) {
            // Pre-v11 Shizuku has no requestPermission API: the user has to
            // grant it from the Shizuku manager once, then the re-check passes.
            Log.d(tag, "Shizuku pre-v11, permission must be granted from the manager app")
            recheck(AccessMethod.SHIZUKU)
            return
        }

        removePermissionListener()
        val listener = Shizuku.OnRequestPermissionResultListener { _, grantResult ->
            Log.d(tag, "Shizuku permission result: $grantResult")
            removePermissionListener()
            recheck(AccessMethod.SHIZUKU)
        }
        permissionListener = listener
        Shizuku.addRequestPermissionResultListener(listener)

        _checkResults.update { it + (AccessMethod.SHIZUKU to AccessCheckResult.checking()) }
        try {
            Shizuku.requestPermission(SHIZUKU_REQUEST_CODE)
        } catch (e: Exception) {
            Log.w(tag, "Shizuku.requestPermission() failed", e)
            removePermissionListener()
            recheck(AccessMethod.SHIZUKU)
        }
    }

    private fun removePermissionListener() {
        permissionListener?.let {
            try {
                Shizuku.removeRequestPermissionResultListener(it)
            } catch (e: Exception) {
                Log.w(tag, "Could not remove Shizuku listener", e)
            }
        }
        permissionListener = null
    }

    /**
     * Opens the Shizuku manager app so the user can start the service.
     * Returns false when it is not installed.
     */
    fun openShizukuApp(): Boolean {
        val intent = context.packageManager
            .getLaunchIntentForPackage(ShizukuProvider.MANAGER_APPLICATION_ID)
            ?: return false
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return try {
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            Log.w(tag, "Could not launch Shizuku", e)
            false
        }
    }

    /** Whether the Shizuku manager declares the permission this app needs. */
    fun isShizukuPermissionDeclared(): Boolean = try {
        context.packageManager.getPermissionInfo(ShizukuProvider.PERMISSION, 0)
        true
    } catch (_: PackageManager.NameNotFoundException) {
        false
    }

    //
    // Confirmation
    //

    /**
     * Persists [method] as the confirmed access method and applies it to the
     * session. Only allowed for a method whose check passed, the caller is
     * expected to gate the button with [AccessCheckResult.canContinue].
     */
    fun confirm(method: AccessMethod) {
        scope.launch {
            DataStoreUtils.updateStringPref(
                dataStore,
                AppPrefs.ACCESS_METHOD,
                method.toPreferenceValue(),
            )
            DataStoreUtils.updateBoolPref(dataStore, AppPrefs.ACCESS_METHOD_CONFIRMED, true)
            session.setOperationMode(method.toOperationMode())
            _selectedMethod.value = method
            _gateState.value = AccessGateState.Confirmed(
                method = method,
                operationMode = method.toOperationMode(),
            )
            Log.d(tag, "Access method confirmed: $method")
        }
    }

    /** Method matching the operation mode the session currently runs with. */
    fun currentMethod(): AccessMethod? {
        val mode = session.getOperationMode()
        return AccessMethod.fromOperationMode(mode)
    }

    fun currentOperationMode(): OperationMode = session.getOperationMode()
}

/** Request code used for the Shizuku permission request. */
const val SHIZUKU_REQUEST_CODE = 1000
