package vegabobo.dsusideloader.ui.screen.home

import vegabobo.dsusideloader.diagnostics.DiagnosticType
import vegabobo.dsusideloader.preparation.InstallationStep

enum class ChecksumStatus {
    /** No image selected yet, or checksum computation was not requested. */
    NONE,

    /** Computing. */
    COMPUTING,

    /** Computed successfully, [ChecksumState.value] holds the hash. */
    READY,

    /** Verification is disabled in settings. */
    SKIPPED,

    /** The image could not be read. */
    FAILED,
}

data class ChecksumState(
    val status: ChecksumStatus = ChecksumStatus.NONE,
    val progress: Float = 0F,
    val value: String = "",
)

data class InstallationCardState(
    val installationStep: InstallationStep = InstallationStep.NOT_INSTALLING,
    val isTextFieldEnabled: Boolean = true,
    val isInstallable: Boolean = false,
    val isError: Boolean = false,
    val text: String = "",
    val errorText: String = "",
    val isProgressBarIndeterminate: Boolean = false,
    val installationProgress: Float = 0F,
    val currentPartitionText: String = "",

    // Image verification
    val checksum: ChecksumState = ChecksumState(),
    val expectedChecksum: String = "",
    val checksumMismatch: Boolean = false,
    val strictChecksum: Boolean = false,

    // Security patch level
    val devicePatchLevel: String = "",
    val gsiPatchLevel: String = "",
    val patchLevelChecked: Boolean = false,
    val patchLevelWarning: Boolean = false,

    // Diagnostics
    val diagnosticType: DiagnosticType = DiagnosticType.UNKNOWN,
    val rawError: String = "",
) {

    /**
     * Strict checksum mode blocks the installation when the user provided an
     * expected checksum that does not match the image, or while the checksum is
     * still being computed for an image with an expected checksum.
     */
    fun isInstallBlocked(): Boolean {
        if (!strictChecksum || expectedChecksum.isEmpty()) {
            return false
        }
        return checksum.status == ChecksumStatus.COMPUTING || checksumMismatch
    }
}

data class UserDataCardState(
    val isSelected: Boolean = false,
    val isError: Boolean = false,
    val text: String = "",
    val maximumAllowed: Int = 0,
)

data class ImageSizeCardState(
    val isSelected: Boolean = false,
    val text: String = "",
)

/**
 * High level stage of the home screen, used by the hero card to animate
 * between "nothing selected", "ready to install", "installing" and "finished".
 *
 * Pure function of [InstallationCardState] so it can be unit tested.
 */
enum class HomeStage {
    EMPTY,
    SELECTED,
    INSTALLING,
    RESULT,
}

fun InstallationCardState.stage(): HomeStage = when {
    installationStep == InstallationStep.NOT_INSTALLING -> {
        if (text.isNotEmpty() || isInstallable) HomeStage.SELECTED else HomeStage.EMPTY
    }

    installationStep.isResultStep() -> HomeStage.RESULT
    else -> HomeStage.INSTALLING
}

/**
 * Steps that describe a terminal state: the installation finished, failed or
 * cannot continue without the user doing something.
 */
fun InstallationStep.isResultStep(): Boolean = when (this) {
    InstallationStep.INSTALL_SUCCESS,
    InstallationStep.INSTALL_SUCCESS_REBOOT_DYN_OS,
    InstallationStep.DSU_ALREADY_INSTALLED,
    InstallationStep.DSU_ALREADY_RUNNING_DYN_OS,
    InstallationStep.REQUIRES_ADB_CMD_TO_CONTINUE,
    InstallationStep.ERROR,
    InstallationStep.ERROR_CANCELED,
    InstallationStep.ERROR_REQUIRES_DISCARD_DSU,
    InstallationStep.ERROR_ALREADY_RUNNING_DYN_OS,
    InstallationStep.ERROR_CREATE_PARTITION,
    InstallationStep.ERROR_CORRUPTED_IMAGE,
    InstallationStep.ERROR_EXTERNAL_SDCARD_ALLOC,
    InstallationStep.ERROR_NO_AVAIL_STORAGE,
    InstallationStep.ERROR_F2FS_WRONG_PATH,
    InstallationStep.ERROR_EXTENTS,
    InstallationStep.ERROR_SELINUX,
    InstallationStep.ERROR_SELINUX_ROOTLESS -> true

    else -> false
}

enum class AdditionalCardState {
    NONE,
    SETUP_STORAGE,
    UNAVAIABLE_STORAGE,
    NO_DYNAMIC_PARTITIONS,
    MISSING_READ_LOGS_PERMISSION,
    GRANTING_READ_LOGS_PERMISSION,
    BOOTLOADER_UNLOCKED_WARNING,
}

enum class SheetDisplayState {
    NONE,
    IMAGESIZE_WARNING,
    CONFIRM_INSTALLATION,
    CANCEL_INSTALLATION,
    DISCARD_DSU,
    VIEW_LOGS,
    EXPECTED_CHECKSUM,
}

data class HomeUiState(
    val installationCard: InstallationCardState = InstallationCardState(),
    val userDataCard: UserDataCardState = UserDataCardState(),
    val imageSizeCard: ImageSizeCardState = ImageSizeCardState(),
    val additionalCard: AdditionalCardState = AdditionalCardState.NONE,
    val sheetDisplay: SheetDisplayState = SheetDisplayState.NONE,
    val installationLogs: String = "",
    val passedInitialChecks: Boolean = false,
    val shouldKeepScreenOn: Boolean = false,
    val expectedChecksumInput: String = "",
    val expectedChecksumInputError: Boolean = false,
) {
    fun isInstalling(): Boolean {
        return installationCard.installationStep != InstallationStep.NOT_INSTALLING
    }
}
