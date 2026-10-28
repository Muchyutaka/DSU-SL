package vegabobo.dsusideloader.ui.screen.settings

import vegabobo.dsusideloader.preferences.AppPrefs
import vegabobo.dsusideloader.ui.theme.ThemeMode

enum class DialogSheetState {
    NONE,
    BUILT_IN_INSTALLER,
    DISABLE_STORAGE_CHECK,
}

data class SettingsUiState(
    /**
     * Boolean preferences, the initial values are used as defaults when the
     * preference was never written to the DataStore.
     */
    val preferences: HashMap<String, Boolean> = hashMapOf(
        AppPrefs.USE_BUILTIN_INSTALLER to false,
        AppPrefs.KEEP_SCREEN_ON to false,
        AppPrefs.UMOUNT_SD to false,
        AppPrefs.DISABLE_STORAGE_CHECK to false,
        AppPrefs.FULL_LOGCAT_LOGGING to false,
        AppPrefs.DYNAMIC_COLOR to true,
        AppPrefs.AMOLED_BLACK to false,
        AppPrefs.VERIFY_CHECKSUM to true,
        AppPrefs.STRICT_CHECKSUM to false,
        AppPrefs.CHECK_PATCH_LEVEL to true,
    ),
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dialogSheetDisplay: DialogSheetState = DialogSheetState.NONE,
    val isRoot: Boolean = false,
    val isDevOptEnabled: Boolean = false,
)
