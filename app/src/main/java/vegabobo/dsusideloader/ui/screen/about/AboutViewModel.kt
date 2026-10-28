package vegabobo.dsusideloader.ui.screen.about

import android.app.Application
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import vegabobo.dsusideloader.core.BaseViewModel
import vegabobo.dsusideloader.preferences.AppPrefs
import vegabobo.dsusideloader.update.ReleaseInfo
import vegabobo.dsusideloader.update.UpdateManager
import vegabobo.dsusideloader.update.UpdateState

@HiltViewModel
class AboutViewModel @Inject constructor(
    val application: Application,
    override val dataStore: DataStore<Preferences>,
    private val updateManager: UpdateManager,
) : BaseViewModel(dataStore) {
    private val tag = this.javaClass.simpleName

    private val _uiState = MutableStateFlow(AboutScreenUiState())
    val uiState: StateFlow<AboutScreenUiState> = _uiState.asStateFlow()

    /** Update state shared with the home screen and Settings. */
    val updateState: StateFlow<UpdateState> = updateManager.state

    val currentVersion: String = updateManager.currentVersion

    var developerOptionsCounter = 0

    init {
        // Cheap when it ran recently, the manager throttles to once every 6h.
        updateManager.check()
    }

    fun onClickCheckUpdates() = updateManager.check(force = true)

    fun onClickDownloadUpdate() {
        val release: ReleaseInfo = updateState.value.releaseOrNull() ?: return
        updateManager.download(release)
    }

    fun onClickInstallUpdate() {
        updateManager.install()
    }

    fun onClickOpenReleasePage() = updateManager.openReleasePage()

    /** Acknowledges a failed install (for example a signature conflict). */
    fun clearUpdateError() = updateManager.clearError()

    fun onClickOpenInstallSettings() = updateManager.openInstallPermissionSettings()

    fun resetDeveloperOptionsCounter() {
        developerOptionsCounter = 0
    }

    fun onClickImage() {
        developerOptionsCounter++
        if (developerOptionsCounter > 7) {
            resetDeveloperOptionsCounter()
            viewModelScope.launch {
                val newDevOptPrefValue = !readBoolPref(AppPrefs.DEVELOPER_OPTIONS)
                Log.d(tag, "newDevOptPrefValue: $newDevOptPrefValue")
                updateBoolPref(
                    AppPrefs.DEVELOPER_OPTIONS,
                    newDevOptPrefValue,
                ) { preferenceValue ->
                    _uiState.value.toastDisplay.update { if (preferenceValue) DevOptToastDisplay.ENABLED_DEV_OPT else DevOptToastDisplay.DISABLED_DEV_OPT }
                }
                // if developer options have been disabled
                // then restore developer preferences to their default values
                if (!newDevOptPrefValue) {
                    updateBoolPref(AppPrefs.DISABLE_STORAGE_CHECK, false)
                    updateBoolPref(AppPrefs.FULL_LOGCAT_LOGGING, false)
                }
            }
        }
    }
}
