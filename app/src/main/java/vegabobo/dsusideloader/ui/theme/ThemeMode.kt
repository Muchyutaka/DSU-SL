package vegabobo.dsusideloader.ui.theme

/**
 * User selectable theme mode.
 *
 * Stored in preferences as a lowercase string to keep the DataStore entry stable
 * across renames of the enum constants.
 */
enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK;

    fun toPreferenceValue(): String = name.lowercase()

    companion object {
        fun fromPreferenceValue(value: String?): ThemeMode {
            val normalized = value?.trim()?.lowercase()
            return values().firstOrNull { it.toPreferenceValue() == normalized } ?: SYSTEM
        }
    }
}
