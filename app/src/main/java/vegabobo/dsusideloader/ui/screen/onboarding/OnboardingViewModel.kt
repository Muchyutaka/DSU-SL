package vegabobo.dsusideloader.ui.screen.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import vegabobo.dsusideloader.access.AccessCheckResult
import vegabobo.dsusideloader.access.AccessController
import vegabobo.dsusideloader.access.AccessGateState
import vegabobo.dsusideloader.access.AccessMethod
import vegabobo.dsusideloader.access.AccessStatus
import vegabobo.dsusideloader.ui.components.access.AccessPrimaryAction

/**
 * Drives the onboarding screen. All the real checks live in [AccessController]
 * so the home screen and the settings screen see the same results.
 */
@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val accessController: AccessController,
) : ViewModel() {

    val selectedMethod: StateFlow<AccessMethod?> = accessController.selectedMethod
    val checkResults: StateFlow<Map<AccessMethod, AccessCheckResult>> =
        accessController.checkResults
    val isResolving: StateFlow<Boolean> = accessController.isResolving
    val gateState: StateFlow<AccessGateState> = accessController.gateState

    /** Set once the user confirmed a verified method, the UI navigates away. */
    private val _isConfirmed = MutableStateFlow(false)
    val isConfirmed: StateFlow<Boolean> = _isConfirmed.asStateFlow()

    /** URL the UI should open in the browser (Shizuku is not on Play Store). */
    private val _openUrl = MutableStateFlow<String?>(null)
    val openUrl: StateFlow<String?> = _openUrl.asStateFlow()

    init {
        // Check the card of the method the app is already using (or ADB by
        // default) right away, so the screen never shows a stale "not checked"
        // badge for the current method.
        viewModelScope.launch {
            accessController.check(selectedMethod.value ?: AccessMethod.ADB)
        }
    }

    fun select(method: AccessMethod) = accessController.select(method)

    fun onPrimaryAction(method: AccessMethod, action: AccessPrimaryAction) {
        when (action) {
            AccessPrimaryAction.CHECK -> accessController.recheck(method)
            AccessPrimaryAction.GRANT -> accessController.requestGrant(method)
            AccessPrimaryAction.OPEN_APP -> {
                val launched = accessController.openShizukuApp()
                if (launched) {
                    // Shizuku is in the foreground now; re-check when the user
                    // comes back with the card button.
                    accessController.recheck(method)
                } else {
                    _openUrl.value = SHIZUKU_INSTALL_URL
                }
            }

            AccessPrimaryAction.INSTALL_APP -> _openUrl.value = SHIZUKU_INSTALL_URL
        }
    }

    /**
     * Confirms the method. Only a method whose latest real check passed is
     * persisted; the controller re-checks otherwise.
     */
    fun confirm(method: AccessMethod) {
        val result = checkResults.value[method]
        if (result?.status != AccessStatus.AVAILABLE) {
            accessController.recheck(method)
            return
        }
        accessController.confirm(method)
        _isConfirmed.value = true
    }

    fun consumeOpenUrl() {
        _openUrl.value = null
    }

    companion object {
        /** Official Shizuku website, Shizuku is not published on Play Store. */
        const val SHIZUKU_INSTALL_URL = "https://shizuku.rikka.app/download/"
    }
}
