package vegabobo.dsusideloader.preferences

object AppPrefs {
    /**
     * Releases endpoint used by the update checker. The app reads
     * `releases/latest` from this fork, see ReleaseClient.
     */
    const val UPDATE_CHECK_URL =
        "https://api.github.com/repos/Muchyutaka/DSU-SL/releases/latest"
    const val UPDATE_LAST_CHECK = "update_last_check"
    const val UPDATE_DISMISSED_TAG = "update_dismissed_tag"
    const val USER_PREFERENCES = "user_preferences"
    const val BOOTLOADER_UNLOCKED_WARNING = "bootloader_unlocked_warning"
    const val SAF_PATH = "writable_path"
    const val DEVELOPER_OPTIONS = "developer_options"
    const val USE_BUILTIN_INSTALLER = "builtin_installer"
    const val KEEP_SCREEN_ON = "keep_screen_on"
    const val UMOUNT_SD = "umount_sd"
    const val DISABLE_STORAGE_CHECK = "disable_storage_check"
    const val FULL_LOGCAT_LOGGING = "full_logcat_logging"

    // Access method chosen in the onboarding screen
    const val ACCESS_METHOD = "access_method"
    const val ACCESS_METHOD_CONFIRMED = "access_method_confirmed"

    // Appearance
    const val THEME_MODE = "theme_mode"
    const val DYNAMIC_COLOR = "dynamic_color"
    const val AMOLED_BLACK = "amoled_black"

    // Installation safety
    const val VERIFY_CHECKSUM = "verify_checksum"
    const val STRICT_CHECKSUM = "strict_checksum"
    const val CHECK_PATCH_LEVEL = "check_patch_level"

    // History and presets (serialized as JSON strings)
    const val INSTALL_HISTORY = "install_history"
    const val INSTALL_PRESETS = "install_presets"
}
