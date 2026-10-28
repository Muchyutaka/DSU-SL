package vegabobo.dsusideloader.history

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.first
import vegabobo.dsusideloader.preferences.AppPrefs

/**
 * Persists the installation history and the saved presets in the existing
 * preferences DataStore (as bounded JSON strings).
 */
class InstallHistoryStore(
    private val dataStore: DataStore<Preferences>,
) {

    private val historyKey = stringPreferencesKey(AppPrefs.INSTALL_HISTORY)
    private val presetsKey = stringPreferencesKey(AppPrefs.INSTALL_PRESETS)

    suspend fun readHistory(): List<InstallHistoryEntry> =
        InstallHistoryCodec.decodeHistory(dataStore.data.first()[historyKey])

    suspend fun appendHistory(entry: InstallHistoryEntry) {
        dataStore.edit { preferences ->
            val current = InstallHistoryCodec.decodeHistory(preferences[historyKey])
            preferences[historyKey] =
                InstallHistoryCodec.encodeHistory(InstallHistoryOps.addEntry(current, entry))
        }
    }

    suspend fun clearHistory() {
        dataStore.edit { preferences -> preferences.remove(historyKey) }
    }

    suspend fun readPresets(): List<InstallPreset> =
        InstallHistoryCodec.decodePresets(dataStore.data.first()[presetsKey])

    suspend fun savePreset(preset: InstallPreset) {
        dataStore.edit { preferences ->
            val current = InstallHistoryCodec.decodePresets(preferences[presetsKey])
            preferences[presetsKey] =
                InstallHistoryCodec.encodePresets(InstallHistoryOps.addPreset(current, preset))
        }
    }

    suspend fun deletePreset(id: String) {
        dataStore.edit { preferences ->
            val current = InstallHistoryCodec.decodePresets(preferences[presetsKey])
            preferences[presetsKey] =
                InstallHistoryCodec.encodePresets(InstallHistoryOps.removePreset(current, id))
        }
    }
}
