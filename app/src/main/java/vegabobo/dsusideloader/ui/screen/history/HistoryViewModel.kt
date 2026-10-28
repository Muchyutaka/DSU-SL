package vegabobo.dsusideloader.ui.screen.history

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
import vegabobo.dsusideloader.history.InstallHistoryEntry
import vegabobo.dsusideloader.history.InstallHistoryOps
import vegabobo.dsusideloader.history.InstallHistoryStore
import vegabobo.dsusideloader.history.InstallPreset
import vegabobo.dsusideloader.model.Session

@HiltViewModel
class HistoryViewModel @Inject constructor(
    override val dataStore: DataStore<Preferences>,
    private val historyStore: InstallHistoryStore,
    private val session: Session,
) : BaseViewModel(dataStore) {

    private val tag = this.javaClass.simpleName

    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    init {
        reload()
    }

    fun reload() {
        viewModelScope.launch {
            val entries = try {
                historyStore.readHistory()
            } catch (e: Exception) {
                Log.w(tag, "Unable to read install history", e)
                emptyList()
            }
            val presets = try {
                historyStore.readPresets()
            } catch (e: Exception) {
                Log.w(tag, "Unable to read install presets", e)
                emptyList()
            }
            _uiState.update {
                it.copy(entries = entries, presets = presets, isLoading = false)
            }
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            try {
                historyStore.clearHistory()
                _uiState.update { it.copy(entries = emptyList()) }
            } catch (e: Exception) {
                Log.w(tag, "Unable to clear install history", e)
            }
        }
    }

    /**
     * Turns a past installation into a reusable preset.
     */
    fun savePresetFromEntry(entry: InstallHistoryEntry) {
        viewModelScope.launch {
            try {
                historyStore.savePreset(
                    InstallHistoryOps.presetFromEntry(
                        entry = entry,
                        id = entry.timestamp.toString(),
                        createdAt = System.currentTimeMillis(),
                    ),
                )
                reload()
            } catch (e: Exception) {
                Log.w(tag, "Unable to save install preset", e)
            }
        }
    }

    fun deletePreset(preset: InstallPreset) {
        viewModelScope.launch {
            try {
                historyStore.deletePreset(preset.id)
                reload()
            } catch (e: Exception) {
                Log.w(tag, "Unable to delete install preset", e)
            }
        }
    }

    fun applyEntry(entry: InstallHistoryEntry) =
        applyConfiguration(entry.userdataGb, entry.imageSizeBytes)

    fun applyPreset(preset: InstallPreset) =
        applyConfiguration(preset.userdataGb, preset.imageSizeBytes)

    /**
     * Restores userdata/image size into the shared session, the home screen
     * picks it up on its next composition.
     */
    private fun applyConfiguration(userdataGb: Long, imageSizeBytes: Long) {
        session.userSelection.setUserDataSize(if (userdataGb > 0) "${userdataGb}GB" else "")
        session.userSelection.setImageSize(if (imageSizeBytes > 0) "${imageSizeBytes}b" else "")
        session.pendingConfigurationRestore = true
        Log.d(tag, "Applied configuration: ${session.userSelection}")
    }
}
