package vegabobo.dsusideloader.access

import vegabobo.dsusideloader.util.OperationMode

/**
 * How the app is allowed to install the DSU on this device.
 *
 * This is the user facing choice made in the onboarding screen. It is more
 * restrictive than [OperationMode]: the system (Magisk module) and
 * root/system combinations are detected automatically and are never selected
 * manually.
 */
enum class AccessMethod {
    ADB,
    SHIZUKU,
    ROOT;

    /** Value persisted in the DataStore. */
    fun toPreferenceValue(): String = name.lowercase()

    /**
     * Operation mode this method maps to when it is confirmed by the user.
     * Note that a confirmed method is still downgraded at runtime when the
     * device turns out to be able to do better (see AccessChecks).
     */
    fun toOperationMode(): OperationMode = when (this) {
        ADB -> OperationMode.ADB
        SHIZUKU -> OperationMode.SHIZUKU
        ROOT -> OperationMode.ROOT
    }

    companion object {
        fun fromPreferenceValue(value: String?): AccessMethod? {
            val normalized = value?.trim()?.lowercase()
            return values().firstOrNull { it.toPreferenceValue() == normalized }
        }

        /**
         * Method that best describes an operation mode, used to preselect the
         * card that matches what the app is currently doing.
         */
        fun fromOperationMode(operationMode: OperationMode): AccessMethod = when (operationMode) {
            OperationMode.SHIZUKU -> SHIZUKU
            OperationMode.ROOT,
            OperationMode.SYSTEM,
            OperationMode.SYSTEM_AND_ROOT -> ROOT
            OperationMode.ADB -> ADB
        }
    }
}
