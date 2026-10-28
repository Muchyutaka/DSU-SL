package vegabobo.dsusideloader.diagnostics

import vegabobo.dsusideloader.preparation.InstallationStep

/**
 * Kinds of installation problems the app knows how to explain.
 *
 * The UI maps each type to a human readable cause, a suggested fix and (when
 * available) a documentation link.
 */
enum class DiagnosticType {
    UNKNOWN,
    SELINUX_DENIAL,
    PARTITION_CREATION_FAILED,
    CORRUPTED_IMAGE,
    INSUFFICIENT_STORAGE,
    UNSUPPORTED_IMAGE,
    EXTERNAL_SD_ALLOCATION,
    F2FS_KERNEL,
    EXTENTS_TOO_FRAGMENTED,
    ALREADY_RUNNING_DSU,
    CANCELLED,
}

/**
 * Maps an [InstallationStep] and/or a raw error line to a [DiagnosticType].
 *
 * Pure Kotlin (no Android APIs) so the mapping can be unit tested.
 */
object DiagnosticMapper {

    fun fromStep(step: InstallationStep?, rawError: String? = null): DiagnosticType {
        return when (step) {
            InstallationStep.ERROR_SELINUX,
            InstallationStep.ERROR_SELINUX_ROOTLESS -> DiagnosticType.SELINUX_DENIAL

            InstallationStep.ERROR_CREATE_PARTITION -> DiagnosticType.PARTITION_CREATION_FAILED
            InstallationStep.ERROR_NO_AVAIL_STORAGE -> DiagnosticType.INSUFFICIENT_STORAGE
            InstallationStep.ERROR_EXTERNAL_SDCARD_ALLOC -> DiagnosticType.EXTERNAL_SD_ALLOCATION
            InstallationStep.ERROR_F2FS_WRONG_PATH -> DiagnosticType.F2FS_KERNEL
            InstallationStep.ERROR_EXTENTS -> DiagnosticType.EXTENTS_TOO_FRAGMENTED
            InstallationStep.ERROR_ALREADY_RUNNING_DYN_OS -> DiagnosticType.ALREADY_RUNNING_DSU
            InstallationStep.ERROR_CANCELED -> DiagnosticType.CANCELLED
            InstallationStep.ERROR_CORRUPTED_IMAGE -> DiagnosticType.CORRUPTED_IMAGE

            // Generic failures are classified from the raw log line/exception.
            InstallationStep.ERROR,
            InstallationStep.ERROR_REQUIRES_DISCARD_DSU -> fromRawError(rawError)

            else -> DiagnosticType.UNKNOWN
        }
    }

    fun fromRawError(rawError: String?): DiagnosticType {
        if (rawError.isNullOrBlank()) {
            return DiagnosticType.UNKNOWN
        }
        val error = rawError.lowercase()

        // Order matters: some lines contain more than one keyword.
        if (error.contains("not in gzip format") ||
            error.contains("not in the .xz format") ||
            error.contains("unexpected end of") ||
            error.contains("truncated") ||
            error.contains("invalid xz") ||
            error.contains("invalid header magic") ||
            error.contains("corrupt")
        ) {
            return DiagnosticType.CORRUPTED_IMAGE
        }

        if (error.contains("realpath failed") ||
            (error.contains("sdcard") && error.contains("permission denied")) ||
            (error.contains("external") && error.contains("permission denied"))
        ) {
            return DiagnosticType.EXTERNAL_SD_ALLOCATION
        }

        if (error.contains("avc: denied") ||
            error.contains("selinux") ||
            error.contains("failed to get stat for block device")
        ) {
            return DiagnosticType.SELINUX_DENIAL
        }

        if (error.contains("below the minimum threshold") ||
            error.contains("no space left on device") ||
            error.contains("enospc") ||
            error.contains("insufficient storage")
        ) {
            return DiagnosticType.INSUFFICIENT_STORAGE
        }

        if (error.contains("createpartition") ||
            error.contains("create partition") ||
            error.contains("failed to create partition")
        ) {
            return DiagnosticType.PARTITION_CREATION_FAILED
        }

        if (error.contains("file is too fragmented") || error.contains("extents")) {
            return DiagnosticType.EXTENTS_TOO_FRAGMENTED
        }

        if (error.contains("read failed") && error.contains("f2fs")) {
            return DiagnosticType.F2FS_KERNEL
        }

        if (error.contains("we are already running in dynamicsystem")) {
            return DiagnosticType.ALREADY_RUNNING_DSU
        }

        if (error.contains("exec format error") ||
            error.contains("unsupported architecture") ||
            error.contains("unsupported file") ||
            error.contains("unsupported filetype") ||
            error.contains("abi mismatch")
        ) {
            return DiagnosticType.UNSUPPORTED_IMAGE
        }

        return DiagnosticType.UNKNOWN
    }

    fun fromThrowable(throwable: Throwable?): DiagnosticType {
        if (throwable == null) {
            return DiagnosticType.UNKNOWN
        }
        val message = throwable.message.orEmpty()
        val type = fromRawError(message)
        if (type != DiagnosticType.UNKNOWN) {
            return type
        }
        // Archive/stream failures while unpacking usually mean a truncated or
        // incomplete image.
        return if (throwable is java.io.IOException) {
            DiagnosticType.CORRUPTED_IMAGE
        } else {
            DiagnosticType.UNKNOWN
        }
    }
}
