package vegabobo.dsusideloader.ui.screen.settings

import android.app.Application
import android.os.Build
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import vegabobo.dsusideloader.access.AccessCheckResult
import vegabobo.dsusideloader.access.AccessController
import vegabobo.dsusideloader.access.AccessMethod
import vegabobo.dsusideloader.core.BaseViewModel
import vegabobo.dsusideloader.diagnostics.DiagnosticReporter
import vegabobo.dsusideloader.model.Session
import vegabobo.dsusideloader.preferences.AppPrefs
import vegabobo.dsusideloader.ui.theme.ThemeMode
import vegabobo.dsusideloader.util.OperationMode
import vegabobo.dsusideloader.util.OperationModeUtils

@HiltViewModel
class SettingsViewModel @Inject constructor(
    override val dataStore: DataStore<Preferences>,
    private val session: Session,
    private val accessController: AccessController,
    val application: Application,
) : BaseViewModel(dataStore) {

    private val tag = this.javaClass.simpleName

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    /** Access method confirmed in the onboarding screen. */
    val accessMethod: StateFlow<AccessMethod?> = accessController.selectedMethod

    /** Live access check results, shared with the onboarding and home screens. */
    val accessResults: StateFlow<Map<AccessMethod, AccessCheckResult>> =
        accessController.checkResults

    fun reloadPreferences() {
        uiState.value.preferences.forEach { entry ->
            viewModelScope.launch {
                // The current value doubles as the default of the preference,
                // so never written preferences keep their initial value.
                val isEnabled = readBoolPref(entry.key, entry.value)
                togglePreference(entry.key, isEnabled)
            }
        }

        viewModelScope.launch {
            val themeMode = ThemeMode.fromPreferenceValue(readStringPref(AppPrefs.THEME_MODE))
            _uiState.update { it.copy(themeMode = themeMode) }
        }

        if (session.isRoot()) {
            _uiState.update { it.copy(isRoot = true) }
        }
    }

    init {
        reloadPreferences()
    }

    fun togglePreference(preference: String, value: Boolean) {
        viewModelScope.launch {
            updateBoolPref(preference, value) {
                _uiState.update {
                    val cloneMap = hashMapOf<String, Boolean>()
                    cloneMap.putAll(uiState.value.preferences)
                    cloneMap[preference] = value
                    Log.d(tag, "preference: $preference, isEnabled: $value")
                    it.copy(preferences = cloneMap)
                }
            }
        }
    }

    /**
     * Cycles between "follow system", "light" and "dark" themes.
     */
    fun cycleThemeMode() {
        val newThemeMode = when (uiState.value.themeMode) {
            ThemeMode.SYSTEM -> ThemeMode.LIGHT
            ThemeMode.LIGHT -> ThemeMode.DARK
            ThemeMode.DARK -> ThemeMode.SYSTEM
        }
        viewModelScope.launch {
            updateStringPref(AppPrefs.THEME_MODE, newThemeMode.toPreferenceValue()) {
                _uiState.update { it.copy(themeMode = newThemeMode) }
            }
        }
    }

    /**
     * Builds and shares a diagnostic report (device info + bounded logcat).
     * Nothing is uploaded automatically: the user picks the target app.
     */
    fun shareDiagnosticReport() {
        val reporter = DiagnosticReporter(application)
        val operationMode = checkOperationMode()
        viewModelScope.launch {
            val report = withContext(Dispatchers.IO) {
                reporter.buildReport(operationMode = operationMode)
            }
            reporter.share(report)
        }
    }

    /** Re-runs the real access check for the confirmed method. */
    fun recheckAccess() = accessController.recheck()

    /**
     * Deletes the diagnostic reports stored by [DiagnosticReporter]. Nothing
     * else is removed, and nothing is uploaded.
     */
    fun clearStoredReports() {
        viewModelScope.launch {
            val deleted = withContext(Dispatchers.IO) {
                DiagnosticReporter(application).clearStoredReports()
            }
            Log.d(tag, "Deleted $deleted saved diagnostic report(s)")
        }
    }

    fun isAndroidQ(): Boolean = Build.VERSION.SDK_INT == 29

    fun isDynamicColorSupported(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    fun updateSheetDisplay(sheet: DialogSheetState) {
        _uiState.update { it.copy(dialogSheetDisplay = sheet) }
    }

    fun checkOperationMode(): String {
        return OperationModeUtils.getOperationModeAsString(session.getOperationMode())
    }

    fun getOperationMode(): OperationMode {
        return session.getOperationMode()
    }

    fun checkDevOpt() {
        viewModelScope.launch {
            val isDevOptEnabled = readBoolPref(AppPrefs.DEVELOPER_OPTIONS)
            _uiState.update { it.copy(isDevOptEnabled = isDevOptEnabled) }
            if (isDevOptEnabled) {
                reloadPreferences()
            }
        }
    }
}
