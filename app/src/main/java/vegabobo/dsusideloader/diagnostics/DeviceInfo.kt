package vegabobo.dsusideloader.diagnostics

import android.os.Build
import vegabobo.dsusideloader.BuildConfig

/**
 * Collects the app and device information included in diagnostic reports.
 *
 * Every property is read defensively: some vendors ship builds where a system
 * property is missing or throws, and a diagnostic report must never crash.
 */
object DeviceInfo {

    fun appSection(operationMode: String? = null): String = buildString {
        appendLine("package: ${safe { BuildConfig.APPLICATION_ID }}")
        appendLine("version: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
        appendLine("build type: ${BuildConfig.BUILD_TYPE}")
        if (!operationMode.isNullOrBlank()) {
            appendLine("operation mode: $operationMode")
        }
    }.trimEnd()

    fun deviceSection(): String = buildString {
        appendLine("manufacturer: ${safe { Build.MANUFACTURER }}")
        appendLine("model: ${safe { Build.MODEL }}")
        appendLine("device: ${safe { Build.DEVICE }}")
        appendLine("android: ${safe { Build.VERSION.RELEASE }} (SDK ${safe { Build.VERSION.SDK_INT.toString() }})")
        appendLine("security patch: ${safe { Build.VERSION.SECURITY_PATCH }}")
        appendLine("abis: ${safe { Build.SUPPORTED_ABIS?.joinToString(", ") }}")
        appendLine("fingerprint: ${safe { Build.FINGERPRINT }}")
    }.trimEnd()

    private fun safe(value: () -> String?): String {
        return try {
            value()?.takeIf { it.isNotBlank() } ?: "unknown"
        } catch (e: Exception) {
            "unknown"
        }
    }
}
