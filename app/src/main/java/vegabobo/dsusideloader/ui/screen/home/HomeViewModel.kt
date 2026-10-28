package vegabobo.dsusideloader.ui.screen.home

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.lifecycle.viewModelScope
import com.topjohnwu.superuser.Shell
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.cancellation.CancellationException
import vegabobo.dsusideloader.BuildConfig
import vegabobo.dsusideloader.access.AccessCheckResult
import vegabobo.dsusideloader.access.AccessController
import vegabobo.dsusideloader.access.AccessMethod
import vegabobo.dsusideloader.core.BaseViewModel
import vegabobo.dsusideloader.core.StorageManager
import vegabobo.dsusideloader.diagnostics.ChecksumUtils
import vegabobo.dsusideloader.diagnostics.DiagnosticMapper
import vegabobo.dsusideloader.diagnostics.DiagnosticReporter
import vegabobo.dsusideloader.diagnostics.DiagnosticType
import vegabobo.dsusideloader.diagnostics.PatchLevelComparison
import vegabobo.dsusideloader.diagnostics.PatchLevelUtils
import vegabobo.dsusideloader.installer.adb.AdbInstallationHandler
import vegabobo.dsusideloader.installer.privileged.DsuInstallationHandler
import vegabobo.dsusideloader.installer.privileged.LogcatDiagnostic
import vegabobo.dsusideloader.history.InstallHistoryEntry
import vegabobo.dsusideloader.history.InstallHistoryStore
import vegabobo.dsusideloader.history.InstallResult
import vegabobo.dsusideloader.installer.root.DSUInstaller
import vegabobo.dsusideloader.model.DSUInstallationSource
import vegabobo.dsusideloader.model.Session
import vegabobo.dsusideloader.preferences.AppPrefs
import vegabobo.dsusideloader.preparation.InstallationStep
import vegabobo.dsusideloader.preparation.Preparation
import vegabobo.dsusideloader.service.PrivilegedProvider
import vegabobo.dsusideloader.util.DevicePropUtils
import vegabobo.dsusideloader.util.FileChecksum
import vegabobo.dsusideloader.util.FilenameUtils
import vegabobo.dsusideloader.util.OperationMode
import vegabobo.dsusideloader.util.OperationModeUtils
import vegabobo.dsusideloader.util.StorageUtils

@HiltViewModel
class HomeViewModel @Inject constructor(
    val application: Application,
    override val dataStore: DataStore<Preferences>,
    private val storageManager: StorageManager,
    private val historyStore: InstallHistoryStore,
    private val accessController: AccessController,
    var session: Session,
) : BaseViewModel(dataStore) {

    private val tag = this.javaClass.simpleName

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    /** Access method confirmed in the onboarding screen. */
    val accessMethod: StateFlow<AccessMethod?> = accessController.selectedMethod

    /** Live check results, shared with the onboarding and settings screens. */
    val accessResults: StateFlow<Map<AccessMethod, AccessCheckResult>> =
        accessController.checkResults

    var checkDynamicPartitions = true
    var checkUnavaiableStorage = true
    var checkReadLogsPermission = true
    var disabledStorageCheck = false

    var installationJob: Job = Job()
    var logger: LogcatDiagnostic? = null
    private var checksumJob: Job? = null

    /** Prevents recording the same installation twice in the history. */
    private var historyRecorded = false

    private val allocPercentage = DevicePropUtils.getGsidBinaryAllowedPerc()
    val allocPercentageInt = String.format("%.0f", allocPercentage * 100).toInt()

    private val storageStats = StorageUtils.getAllocInfo(allocPercentage)
    private val hasAvailableStorage = storageStats.first
    private val maximumAllowedForAllocation = storageStats.second

    //
    // Helper methods used for controlling UI State
    //

    private fun updateAdditionalCardState(additionalCard: AdditionalCardState) =
        _uiState.update { it.copy(additionalCard = additionalCard) }

    private fun updateUserdataCard(update: (UserDataCardState) -> UserDataCardState) =
        _uiState.update { it.copy(userDataCard = update(it.userDataCard.copy())) }

    private fun updateInstallationCard(update: (InstallationCardState) -> InstallationCardState) =
        _uiState.update { it.copy(installationCard = update(it.installationCard.copy())) }

    private fun updateImageSizeCard(update: (ImageSizeCardState) -> ImageSizeCardState) =
        _uiState.update { it.copy(imageSizeCard = update(it.imageSizeCard.copy())) }

    private fun updateSheetState(sheetDisplay: SheetDisplayState) =
        _uiState.update { it.copy(sheetDisplay = sheetDisplay) }

    /** Re-runs the real access check for the confirmed method. */
    fun recheckAccess() = accessController.recheck()

    fun resetInstallationCard() {
        checksumJob?.cancel()
        checksumJob = null
        historyRecorded = false
        _uiState.update {
            it.copy(
                installationCard = InstallationCardState(),
                sheetDisplay = SheetDisplayState.NONE,
            )
        }
    }

    fun dismissSheet() = updateSheetState(SheetDisplayState.NONE)

    //
    // Home startup and checks
    //

    init {
        // Check if a DSU is already installed
        // Root-only because MANAGE_DYNAMIC_SYSTEM is required
        if (session.isRoot()) {
            PrivilegedProvider.run {
                if (isInUse) {
                    updateInstallationCard { it.copy(installationStep = InstallationStep.DSU_ALREADY_RUNNING_DYN_OS) }
                    return@run
                }
                if (isInstalled) {
                    updateInstallationCard { it.copy(installationStep = InstallationStep.DSU_ALREADY_INSTALLED) }
                    return@run
                }
            }
        }
    }

    fun initialChecks() {
        if (checkDynamicPartitions && !DevicePropUtils.hasDynamicPartitions()) {
            updateAdditionalCardState(AdditionalCardState.NO_DYNAMIC_PARTITIONS)
            return
        }

        if (checkUnavaiableStorage && !hasAvailableStorage) {
            updateAdditionalCardState(AdditionalCardState.UNAVAIABLE_STORAGE)
            return
        }

        viewModelScope.launch {
            val result = readStringPref(AppPrefs.SAF_PATH)
            if (!storageManager.arePermissionsGrantedToFolder(result)) {
                updateAdditionalCardState(AdditionalCardState.SETUP_STORAGE)
                return@launch
            }

            if (session.getOperationMode() == OperationMode.SHIZUKU && checkReadLogsPermission &&
                !OperationModeUtils.isReadLogsPermissionGranted(application)
            ) {
                _uiState.update { it.copy(passedInitialChecks = false) }
                updateAdditionalCardState(AdditionalCardState.MISSING_READ_LOGS_PERMISSION)
                return@launch
            }

            val seenUnlockedBootloaderWarning = readBoolPref(AppPrefs.BOOTLOADER_UNLOCKED_WARNING)
            if (!seenUnlockedBootloaderWarning) {
                _uiState.update { it.copy(passedInitialChecks = false) }
                updateAdditionalCardState(AdditionalCardState.BOOTLOADER_UNLOCKED_WARNING)
                return@launch
            }

            updateAdditionalCardState(AdditionalCardState.NONE)
            _uiState.update { it.copy(passedInitialChecks = true) }
        }
    }

    fun setupUserPreferences() {
        viewModelScope.launch {
            val shouldKeepScreenOn = readBoolPref(AppPrefs.KEEP_SCREEN_ON)
            Log.d(tag, "shouldKeepScreenOn: $shouldKeepScreenOn")
            _uiState.update { it.copy(shouldKeepScreenOn = shouldKeepScreenOn) }

            disabledStorageCheck = readBoolPref(AppPrefs.DISABLE_STORAGE_CHECK)
            Log.d(tag, "disabledStorageCheck: $shouldKeepScreenOn")

            val strictChecksum = readBoolPref(AppPrefs.STRICT_CHECKSUM)
            updateInstallationCard { it.copy(strictChecksum = strictChecksum) }
        }
    }

    fun overrideDynamicPartitionCheck() {
        checkDynamicPartitions = false
        Log.d(tag, "checkDynamicPartitions: $checkDynamicPartitions")
        initialChecks()
    }

    fun overrideUnavaiableStorage() {
        checkUnavaiableStorage = false
        Log.d(tag, "checkUnavaiableStorage: $checkUnavaiableStorage")
        initialChecks()
    }

    fun onClickBootloaderUnlockedWarning() {
        viewModelScope.launch {
            updateBoolPref(AppPrefs.BOOTLOADER_UNLOCKED_WARNING, true)
            initialChecks()
        }
    }

    //
    // Installation
    //

    fun obtainSelectedFilename(): String = session.userSelection.selectedFileName

    fun onClickCancel() {
        if (uiState.value.isInstalling()) {
            updateSheetState(SheetDisplayState.CANCEL_INSTALLATION)
        }
    }

    fun onClickInstall() {
        if (uiState.value.installationCard.isInstallBlocked()) {
            Log.d(tag, "Installation blocked: strict checksum mode and mismatching checksum")
            return
        }
        session.userSelection.setUserDataSize(uiState.value.userDataCard.text)
        session.userSelection.setImageSize(uiState.value.imageSizeCard.text)
        updateSheetState(SheetDisplayState.CONFIRM_INSTALLATION)
    }

    fun onConfirmInstallationSheet() {
        if (uiState.value.installationCard.isInstallBlocked()) {
            dismissSheet()
            return
        }
        historyRecorded = false
        dismissSheet()
        updateInstallationCard { it.copy(installationStep = InstallationStep.PROCESSING) }
        installationJob = Job()
        viewModelScope.launch(Dispatchers.IO + installationJob) {
            session.preferences.isUnmountSdCard = readBoolPref(AppPrefs.UMOUNT_SD)
            session.preferences.useBuiltinInstaller = readBoolPref(AppPrefs.USE_BUILTIN_INSTALLER)
            try {
                Preparation(
                    storageManager = storageManager,
                    session = session,
                    job = installationJob,
                    onStepUpdate = this@HomeViewModel::onStepUpdate,
                    onPreparationProgressUpdate = this@HomeViewModel::onPreparationProgressUpdate,
                    onCanceled = this@HomeViewModel::onClickCancelInstallationButton,
                    onPreparationFinished = this@HomeViewModel::onPreparationFinished,
                ).invoke()
            } catch (e: CancellationException) {
                Log.d(tag, "Preparation cancelled")
                throw e
            } catch (e: Exception) {
                Log.e(tag, "Preparation failed", e)
                onPreparationFailed(e)
            }
        }
    }

    private fun onPreparationFinished(dsuInstallation: DSUInstallationSource) {
        Log.d(tag, "DSU preparation finished, result: $dsuInstallation")
        session.dsuInstallation = dsuInstallation
        startInstallation()
    }

    private fun startInstallation() {
        Log.d(tag, "startInstallation(), session: \n$session")
        historyRecorded = false
        updateInstallationCard { it.copy(installationStep = InstallationStep.PROCESSING) }

        if (session.getOperationMode() == OperationMode.ADB) {
            setupAdbInstallation()
            return
        }

        if (session.preferences.useBuiltinInstaller && session.isRoot()) {
            startDSUInstallation()
            return
        }

        startPrivilegedInstallation()
    }

    private fun setupAdbInstallation() {
        AdbInstallationHandler(storageManager, session).generate { scriptPath ->
            Log.d(tag, "Installation script generated: $scriptPath")
            session.installationScriptPath = scriptPath
            resetInstallationCard()
            updateInstallationCard { it.copy(installationStep = InstallationStep.REQUIRES_ADB_CMD_TO_CONTINUE) }
        }
    }

    private fun startDSUInstallation() {
        DSUInstaller(
            application = application,
            userdataSize = session.userSelection.userSelectedUserdata,
            dsuInstallation = session.dsuInstallation,
            installationJob = installationJob,
            onInstallationError = this::onInstallationError,
            onInstallationProgressUpdate = this::onInstallationProgressUpdate,
            onCreatePartition = this::onCreatePartition,
            onInstallationStepUpdate = this::onStepUpdate,
            onInstallationSuccess = this::onRootInstallationSuccess,
        ).invoke()
    }

    private fun startPrivilegedInstallation() {
        updateInstallationCard { it.copy(installationStep = InstallationStep.WAITING_USER_CONFIRMATION) }
        DsuInstallationHandler(session).startInstallation()
        if (session.isRoot() || OperationModeUtils.isReadLogsPermissionGranted(application)) {
            startLogging()
        } else {
            updateInstallationCard { it.copy(installationStep = InstallationStep.INSTALL_SUCCESS) }
        }
    }

    // Track and diagnose installation by reading logcat
    private fun startLogging() {
        if (logger == null) {
            logger = LogcatDiagnostic(
                onInstallationError = this::onInstallationError,
                onStepUpdate = this::onStepUpdate,
                onInstallationProgressUpdate = this::onInstallationProgressUpdate,
                onInstallationSuccess = this::onInstallationSuccess,
                onLogLineReceived = this::onLogLineReceived,
            )
        }
        viewModelScope.launch(Dispatchers.IO + installationJob) {
            logger!!.shouldLogEverything = readBoolPref(AppPrefs.FULL_LOGCAT_LOGGING)
            logger!!.startLogging(generateUsefulLogInfo())
        }
    }

    private fun generateUsefulLogInfo(): String {
        return "Device: ${Build.MODEL}\n" +
            "SDK: Android ${Build.VERSION.RELEASE} (${Build.VERSION.SDK_INT})\n" +
            "$session\n" +
            "Package: ${BuildConfig.APPLICATION_ID}\n" +
            "Version: ${BuildConfig.VERSION_NAME} - ${BuildConfig.VERSION_CODE} (${BuildConfig.BUILD_TYPE}})\n" +
            "checkDynamicPartitions: $checkDynamicPartitions\n" +
            "checkUnavaiableStorage: $checkUnavaiableStorage\n" +
            "checkReadLogsPermission: $checkReadLogsPermission\n" +
            "allocPercentage: $allocPercentage\n" +
            "hasAvailableStorage: $hasAvailableStorage\n" +
            "maximumAllowedForAllocation: $maximumAllowedForAllocation\n"
    }

    //
    // Install history
    //

    private fun recordHistory(result: InstallResult, errorSummary: String = "") {
        if (historyRecorded) {
            return
        }
        historyRecorded = true
        val selection = session.userSelection
        val entry = InstallHistoryEntry(
            timestamp = System.currentTimeMillis(),
            fileName = selection.selectedFileName,
            userdataGb = selection.getUserDataSizeAsGB().toLongOrNull() ?: 0L,
            imageSizeBytes = session.dsuInstallation.fileSize,
            result = result,
            errorSummary = errorSummary,
        )
        viewModelScope.launch {
            try {
                historyStore.appendHistory(entry)
            } catch (e: Exception) {
                Log.w(tag, "Unable to store install history", e)
            }
        }
    }

    /**
     * Applies a configuration chosen in the history screen (userdata/image size).
     * The image itself still has to be selected again, because content uris are
     * not kept in the history.
     */
    fun restoreConfigurationIfRequested() {
        if (!session.pendingConfigurationRestore) {
            return
        }
        session.pendingConfigurationRestore = false
        val selection = session.userSelection
        Log.d(tag, "Restoring configuration from history: $selection")
        updateUserdataCard {
            it.copy(isSelected = true, text = "${selection.getUserDataSizeAsGB()}GB")
        }
        updateImageSizeCard {
            it.copy(
                isSelected = selection.isCustomImageSize(),
                text = if (selection.isCustomImageSize()) {
                    "${selection.userSelectedImageSize}b"
                } else {
                    ""
                },
            )
        }
    }

    //
    // Diagnostics export (explicit sharing, never automatic)
    //

    fun shareDiagnosticReport() {
        val card = uiState.value.installationCard
        val reporter = DiagnosticReporter(application)
        val operationMode = OperationModeUtils.getOperationModeAsString(session.getOperationMode())
        val installationDetails = buildInstallationDetails()
        val errorInfo = buildErrorSummary(card)
        val appLogs = uiState.value.installationLogs

        // Reading logcat and writing the file must not run on the main thread.
        viewModelScope.launch {
            val report = withContext(Dispatchers.IO) {
                reporter.buildReport(
                    operationMode = operationMode,
                    installationDetails = installationDetails,
                    errorInfo = errorInfo,
                    appLogs = appLogs,
                )
            }
            reporter.share(report)
        }
    }

    private fun buildInstallationDetails(): String {
        val card = uiState.value.installationCard
        val selection = session.userSelection
        return buildString {
            appendLine("selected file: ${selection.selectedFileName.ifEmpty { "none" }}")
            appendLine("file uri: ${selection.selectedFileUri}")
            appendLine("userdata size (GB): ${selection.getUserDataSizeAsGB()}")
            appendLine("image size (bytes): ${selection.userSelectedImageSize}")
            appendLine("checksum status: ${card.checksum.status}")
            appendLine("sha256: ${card.checksum.value.ifEmpty { "not available" }}")
            appendLine("expected sha256: ${card.expectedChecksum.ifEmpty { "none" }}")
            appendLine("checksum mismatch: ${card.checksumMismatch}")
            appendLine("device patch level: ${card.devicePatchLevel.ifEmpty { "unknown" }}")
            appendLine("image patch level: ${card.gsiPatchLevel.ifEmpty { "unknown" }}")
            appendLine("unmount sd card: ${session.preferences.isUnmountSdCard}")
            appendLine("builtin installer: ${session.preferences.useBuiltinInstaller}")
            appendLine("diagnostic: ${card.diagnosticType}")
            append("session: $session")
        }
    }

    private fun buildErrorSummary(card: InstallationCardState): String? {
        if (card.errorText.isBlank() && card.rawError.isBlank()) {
            return null
        }
        return buildString {
            appendLine("step: ${card.installationStep}")
            appendLine("diagnostic: ${card.diagnosticType}")
            appendLine("error: ${card.errorText.ifEmpty { card.rawError }}")
            append("raw: ${card.rawError}")
        }
    }

    fun saveLogs(uriToSaveLogs: Uri) {
        Log.d(tag, "Writing logs to: $uriToSaveLogs")
        storageManager.writeStringToUri(uiState.value.installationLogs, uriToSaveLogs)
    }

    fun onClickCancelInstallationButton() {
        recordHistory(InstallResult.CANCELED)
        resetInstallationCard()
        if (session.getOperationMode() != OperationMode.ADB &&
            logger != null && logger!!.isLogging.get()
        ) {
            logger!!.destroy()
            // Since stopping installation requires MANAGE_DYNAMIC_SYSTEM
            // then, we stop installation using other way, not so polite, but works :)))
            PrivilegedProvider.run { forceStopPackage("com.android.dynsystem") }
        }

        if (installationJob.isActive) {
            installationJob.cancel()
        }
        session.dsuInstallation = DSUInstallationSource()
    }

    //
    // Installation Card actions
    //

    fun onClickRebootToDynOS() {
        updateInstallationCard { it.copy(installationStep = InstallationStep.PROCESSING) }
        PrivilegedProvider.run {
            setEnable(true, true)
            Shell.cmd("reboot").exec()
        }
    }

    fun onClickDiscardGsiAndStartInstallation() {
        updateInstallationCard { it.copy(installationStep = InstallationStep.PROCESSING) }
        PrivilegedProvider.run {
            remove()
            forceStopPackage("com.android.dynsystem")
            startDSUInstallation()
        }
    }

    fun onClickDiscardGsi() {
        updateInstallationCard { it.copy(installationStep = InstallationStep.PROCESSING) }
        PrivilegedProvider.run {
            remove()
            forceStopPackage("com.android.dynsystem")
            dismissSheet()
            resetInstallationCard()
        }
    }

    fun onClickRetryInstallation() {
        updateInstallationCard { it.copy(installationStep = InstallationStep.PROCESSING) }
        startInstallation()
    }

    fun onClickUnmountSdCardAndRetry() {
        updateInstallationCard { it.copy(installationStep = InstallationStep.PROCESSING) }
        session.preferences.isUnmountSdCard = true
        startInstallation()
    }

    fun onClickSetSeLinuxPermissive() {
        updateInstallationCard { it.copy(installationStep = InstallationStep.PROCESSING) }
        viewModelScope.launch {
            Shell.cmd("setenforce 0").exec()
            delay(5000)
            startInstallation()
        }
    }

    fun showDiscardSheet() = updateSheetState(SheetDisplayState.DISCARD_DSU)

    //
    // Userdata card
    //

    fun onCheckUserdataCard() =
        updateUserdataCard { it.copy(isSelected = !it.isSelected, text = "") }

    fun updateUserdataSize(input: String) {
        val selectedSize = FilenameUtils.getDigits(input)
        val sizeWithSuffix = FilenameUtils.appendToDigitsToString(input, "GB")
        Log.d(
            tag,
            "disabledStorageCheck: $disabledStorageCheck, selectedSize: $selectedSize, maximumAllowedForAllocation: $maximumAllowedForAllocation",
        )

        if (!disabledStorageCheck && selectedSize.isNotEmpty() && selectedSize.toInt() > maximumAllowedForAllocation) {
            val fixedSize =
                FilenameUtils.appendToDigitsToString("$maximumAllowedForAllocation", "GB")
            updateUserdataCard {
                it.copy(
                    text = fixedSize,
                    isError = true,
                    maximumAllowed = maximumAllowedForAllocation,
                )
            }
            viewModelScope.launch {
                delay(5000)
                updateUserdataCard { it.copy(isError = false) }
            }
            return
        }

        updateUserdataCard { it.copy(text = sizeWithSuffix) }
    }

    //
    // Image size card
    //

    fun onCheckImageSizeCard() {
        if (!uiState.value.imageSizeCard.isSelected) {
            updateSheetState(SheetDisplayState.IMAGESIZE_WARNING)
        } else {
            dismissSheet()
        }
        updateImageSizeCard { it.copy(isSelected = !it.isSelected, text = "") }
    }

    fun updateImageSize(input: String) {
        val inputWithSuffix = FilenameUtils.appendToDigitsToString(input, "b")
        updateImageSizeCard { it.copy(text = inputWithSuffix) }
    }

    //
    // File selection
    //

    fun takeUriPermission(uri: Uri) {
        application.contentResolver.takePersistableUriPermission(
            uri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
        )
        viewModelScope.launch {
            if (storageManager.arePermissionsGrantedToFolder(uri.toString())) {
                updateStringPref(AppPrefs.SAF_PATH, uri.toString()) { initialChecks() }
            }
        }
    }

    fun onFileSelectionResult(uri: Uri) {
        val filename = FilenameUtils.queryName(application.contentResolver, uri)
        val extension = filename.substringAfterLast(".", "")
        val supportedFiles = arrayListOf("gz", "xz", "img", "gzip")

        // DSU packages (zip files), are only supported in R+
        if (Build.VERSION.SDK_INT > 29) {
            supportedFiles.add("zip")
        }
        val isFileSupported = supportedFiles.contains(extension)
        Log.d(tag, "isFileSupported: $isFileSupported, extension: $extension, filename: $filename")

        if (!isFileSupported) {
            viewModelScope.launch {
                updateInstallationCard { it.copy(isError = true, isTextFieldEnabled = false) }
                delay(2000)
                updateInstallationCard { it.copy(isError = false, isTextFieldEnabled = true) }
            }
            return
        }

        session.userSelection.selectedFileName = filename
        session.userSelection.selectedFileUri = uri
        updateInstallationCard {
            it.copy(
                text = filename,
                isTextFieldEnabled = false,
                isInstallable = true,
            )
        }

        startVerification(filename)
    }

    //
    // Image verification (SHA-256 and security patch level)
    //

    private fun startVerification(filename: String) {
        checksumJob?.cancel()
        checksumJob = null
        updatePatchLevelInfo(filename)
        startChecksumComputation()
    }

    private fun startChecksumComputation() {
        checksumJob = viewModelScope.launch {
            val verifyChecksum = readBoolPref(AppPrefs.VERIFY_CHECKSUM, true)
            val strictChecksum = readBoolPref(AppPrefs.STRICT_CHECKSUM)
            updateInstallationCard { it.copy(strictChecksum = strictChecksum) }

            if (!verifyChecksum) {
                Log.d(tag, "SHA-256 verification is disabled")
                updateInstallationCard {
                    it.copy(checksum = ChecksumState(status = ChecksumStatus.SKIPPED))
                }
                return@launch
            }

            val uri = session.userSelection.selectedFileUri
            if (uri == Uri.EMPTY) {
                return@launch
            }

            val totalBytes = try {
                withContext(Dispatchers.IO) { storageManager.getFilesizeFromUri(uri) }
            } catch (e: Exception) {
                Log.w(tag, "Unable to obtain file size for checksum progress", e)
                -1L
            }

            updateInstallationCard {
                it.copy(checksum = ChecksumState(status = ChecksumStatus.COMPUTING))
            }

            try {
                val hash = withContext(Dispatchers.IO) {
                    FileChecksum.computeSha256(
                        contentResolver = application.contentResolver,
                        uri = uri,
                        totalBytes = totalBytes,
                        isCancelled = { !isActive },
                        onProgress = { progress -> updateChecksumProgress(progress) },
                    )
                }
                updateInstallationCard {
                    it.copy(checksum = ChecksumState(ChecksumStatus.READY, 1F, hash))
                }
                updateChecksumMismatch()
            } catch (e: CancellationException) {
                Log.d(tag, "SHA-256 computation cancelled")
                throw e
            } catch (e: Exception) {
                Log.w(tag, "Unable to compute SHA-256", e)
                updateInstallationCard {
                    it.copy(checksum = ChecksumState(status = ChecksumStatus.FAILED))
                }
            }
        }
    }

    private fun updateChecksumProgress(progress: Float) =
        updateInstallationCard {
            it.copy(checksum = it.checksum.copy(progress = progress))
        }

    private fun updateChecksumMismatch() {
        val card = uiState.value.installationCard
        val actual = card.checksum.value
        val mismatch = card.expectedChecksum.isNotEmpty() &&
            actual.isNotEmpty() &&
            !ChecksumUtils.matches(card.expectedChecksum, actual)
        updateInstallationCard { it.copy(checksumMismatch = mismatch) }
    }

    private fun updatePatchLevelInfo(filename: String) {
        viewModelScope.launch {
            if (!readBoolPref(AppPrefs.CHECK_PATCH_LEVEL, true)) {
                updateInstallationCard { it.copy(patchLevelChecked = false, patchLevelWarning = false) }
                return@launch
            }

            val devicePatchLevel = PatchLevelUtils.parse(Build.VERSION.SECURITY_PATCH)
            val imagePatchLevel = PatchLevelUtils.parseFromFileName(filename)
            val comparison = PatchLevelUtils.compare(devicePatchLevel, imagePatchLevel)
            Log.d(
                tag,
                "patch level: device=$devicePatchLevel image=$imagePatchLevel comparison=$comparison",
            )

            updateInstallationCard {
                it.copy(
                    devicePatchLevel = devicePatchLevel?.toIsoString().orEmpty(),
                    gsiPatchLevel = imagePatchLevel?.toIsoString().orEmpty(),
                    patchLevelChecked = true,
                    patchLevelWarning = comparison == PatchLevelComparison.GSI_OLDER,
                )
            }
        }
    }

    fun showExpectedChecksumSheet() {
        _uiState.update {
            it.copy(
                sheetDisplay = SheetDisplayState.EXPECTED_CHECKSUM,
                expectedChecksumInput = it.installationCard.expectedChecksum,
                expectedChecksumInputError = false,
            )
        }
    }

    fun updateExpectedChecksumInput(input: String) {
        _uiState.update {
            it.copy(expectedChecksumInput = input, expectedChecksumInputError = false)
        }
    }

    fun onConfirmExpectedChecksum() {
        val input = uiState.value.expectedChecksumInput
        if (input.isNotBlank() && !ChecksumUtils.isValid(input)) {
            _uiState.update { it.copy(expectedChecksumInputError = true) }
            return
        }
        updateInstallationCard { it.copy(expectedChecksum = ChecksumUtils.canonical(input)) }
        updateChecksumMismatch()
        dismissSheet()
    }

    fun onClearExpectedChecksum() {
        updateInstallationCard { it.copy(expectedChecksum = "") }
        updateChecksumMismatch()
        dismissSheet()
    }

    //
    // Read logs permission warning
    //

    fun grantReadLogs() {
        updateAdditionalCardState(AdditionalCardState.GRANTING_READ_LOGS_PERMISSION)
        val intent = Intent()
        intent.setClassName(
            BuildConfig.APPLICATION_ID,
            "${BuildConfig.APPLICATION_ID}.MainActivity",
        )
        intent.flags += Intent.FLAG_ACTIVITY_NEW_TASK
        PrivilegedProvider.run {
            grantPermission("android.permission.READ_LOGS")
            if (Build.VERSION.SDK_INT <= 30) {
                forceStopPackage(BuildConfig.APPLICATION_ID)
            }
            startActivity(intent)
        }
    }

    fun refuseReadLogs() {
        checkReadLogsPermission = false
        Log.d(tag, "checkReadLogsPermission: $checkReadLogsPermission")
        initialChecks()
    }

    fun showLogsWarning() {
        updateSheetState(SheetDisplayState.VIEW_LOGS)
    }

    //
    // Progress tracking
    //

    private fun onRootInstallationSuccess() {
        updateInstallationCard { it.copy(installationStep = InstallationStep.INSTALL_SUCCESS_REBOOT_DYN_OS) }
        recordHistory(InstallResult.SUCCESS)
    }

    private fun onInstallationSuccess() {
        updateInstallationCard { it.copy(installationStep = InstallationStep.INSTALL_SUCCESS) }
        recordHistory(InstallResult.SUCCESS)
    }

    private fun onLogLineReceived() {
        _uiState.update { it.copy(installationLogs = logger!!.logs) }
    }

    private fun onStepUpdate(step: InstallationStep) =
        updateInstallationCard { it.copy(installationStep = step) }

    private fun onPreparationProgressUpdate(progress: Float) =
        updateInstallationCard { it.copy(installationProgress = progress) }

    private fun onInstallationError(error: InstallationStep, errorContent: String) {
        val diagnosticType = DiagnosticMapper.fromStep(error, errorContent)
        if (error == InstallationStep.ERROR_CANCELED) {
            recordHistory(InstallResult.CANCELED, errorContent)
        } else {
            recordHistory(InstallResult.ERROR, errorContent)
        }
        updateInstallationCard {
            if (error == InstallationStep.ERROR_SELINUX && !session.isRoot()) {
                it.copy(
                    installationStep = InstallationStep.ERROR_SELINUX_ROOTLESS,
                    errorText = errorContent,
                    diagnosticType = diagnosticType,
                    rawError = errorContent,
                )
            } else {
                it.copy(
                    installationStep = error,
                    errorText = errorContent,
                    diagnosticType = diagnosticType,
                    rawError = errorContent,
                )
            }
        }
    }

    /**
     * Preparation failures (unreadable/corrupted images, revoked SAF permissions,
     * missing storage...) used to escape the coroutine and crash the app. They are
     * now mapped to a regular error card with a diagnostic.
     */
    private fun onPreparationFailed(throwable: Throwable) {
        val diagnosticType = DiagnosticMapper.fromThrowable(throwable)
        val step = if (diagnosticType == DiagnosticType.CORRUPTED_IMAGE) {
            InstallationStep.ERROR_CORRUPTED_IMAGE
        } else {
            InstallationStep.ERROR
        }
        val rawError = throwable.message ?: throwable.toString()
        recordHistory(InstallResult.ERROR, rawError)
        updateInstallationCard {
            it.copy(
                installationStep = step,
                errorText = rawError,
                diagnosticType = diagnosticType,
                rawError = throwable.toString(),
            )
        }
    }

    private fun onInstallationProgressUpdate(progress: Float, partition: String) =
        updateInstallationCard {
            it.copy(currentPartitionText = partition, installationProgress = progress)
        }

    private fun onCreatePartition(partition: String) =
        updateInstallationCard {
            it.copy(
                installationStep = InstallationStep.CREATING_PARTITION,
                currentPartitionText = partition,
            )
        }
}
