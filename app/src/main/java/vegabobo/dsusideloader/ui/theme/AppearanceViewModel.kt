package vegabobo.dsusideloader.ui.theme

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import vegabobo.dsusideloader.core.BaseViewModel
import vegabobo.dsusideloader.preferences.AppPrefs

/**
 * Observes the appearance related preferences so the activity can re-theme itself
 * as soon as the user changes a value in Settings.
 */
@HiltViewModel
class AppearanceViewModel @Inject constructor(
    override val dataStore: DataStore<Preferences>,
) : BaseViewModel(dataStore) {

    data class AppearanceState(
        val themeMode: ThemeMode = ThemeMode.SYSTEM,
        val dynamicColor: Boolean = true,
        val amoledDark: Boolean = false,
    )

    private val _uiState = MutableStateFlow(AppearanceState())
    val uiState: StateFlow<AppearanceState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            dataStore.data.collect { preferences ->
                _uiState.value = AppearanceState(
                    themeMode = ThemeMode.fromPreferenceValue(
                        preferences[stringPreferencesKey(AppPrefs.THEME_MODE)],
                    ),
                    dynamicColor = preferences[booleanPreferencesKey(AppPrefs.DYNAMIC_COLOR)] ?: true,
                    amoledDark = preferences[booleanPreferencesKey(AppPrefs.AMOLED_BLACK)] ?: false,
                )
            }
        }
    }
}
