package vegabobo.dsusideloader.ui.screen.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlin.system.exitProcess
import kotlinx.coroutines.flow.collectLatest
import vegabobo.dsusideloader.R
import vegabobo.dsusideloader.access.AccessCheckResult
import vegabobo.dsusideloader.update.UpdateState
import vegabobo.dsusideloader.ui.cards.DsuInfoCard
import vegabobo.dsusideloader.ui.cards.ImageSizeCard
import vegabobo.dsusideloader.ui.cards.UserdataCard
import vegabobo.dsusideloader.ui.cards.installation.InstallationCard
import vegabobo.dsusideloader.ui.cards.warnings.GrantingPermissionCard
import vegabobo.dsusideloader.ui.cards.warnings.RequiresLogPermissionCard
import vegabobo.dsusideloader.ui.cards.warnings.SetupStorage
import vegabobo.dsusideloader.ui.cards.warnings.StorageWarningCard
import vegabobo.dsusideloader.ui.cards.warnings.UnlockedBootloaderCard
import vegabobo.dsusideloader.ui.cards.warnings.UnsupportedCard
import vegabobo.dsusideloader.ui.components.ApplicationScreen
import vegabobo.dsusideloader.ui.components.TopBar
import vegabobo.dsusideloader.ui.components.UpdateCard
import vegabobo.dsusideloader.ui.screen.Destinations
import vegabobo.dsusideloader.ui.sdialogs.CancelSheet
import vegabobo.dsusideloader.ui.sdialogs.ConfirmInstallationSheet
import vegabobo.dsusideloader.ui.sdialogs.DiscardDSUSheet
import vegabobo.dsusideloader.ui.sdialogs.ExpectedChecksumSheet
import vegabobo.dsusideloader.ui.sdialogs.ImageSizeWarningSheet
import vegabobo.dsusideloader.ui.sdialogs.ViewLogsBottomSheet
import vegabobo.dsusideloader.ui.util.KeepScreenOn
import vegabobo.dsusideloader.ui.util.motionDuration
import vegabobo.dsusideloader.ui.util.rememberImagePickerLauncher
import vegabobo.dsusideloader.util.collectAsStateWithLifecycle

object HomeLinks {
    const val DSU_LEARN_MORE = "https://developer.android.com/topic/dsu"
    const val DSU_DOCS = "https://source.android.com/devices/tech/ota/dynamic-system-updates"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Home(
    navigate: (String) -> Unit,
    homeViewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by homeViewModel.uiState.collectAsStateWithLifecycle()
    val accessMethod by homeViewModel.accessMethod.collectAsStateWithLifecycle()
    val accessResults by homeViewModel.accessResults.collectAsStateWithLifecycle()
    val updateState by homeViewModel.updateState.collectAsStateWithLifecycle()
    val updateDismissedTag by homeViewModel.updateDismissedTag.collectAsStateWithLifecycle()
    val uriHandler = LocalUriHandler.current

    // Same picker as the installation card, so the hero primary action and the
    // card field behave identically.
    val picker = rememberImagePickerLauncher { uri ->
        homeViewModel.onFileSelectionResult(uri)
    }

    if (uiState.shouldKeepScreenOn) {
        KeepScreenOn()
    }

    LaunchedEffect(Unit) {
        homeViewModel.setupUserPreferences()
        homeViewModel.restoreConfigurationIfRequested()
        homeViewModel.session.operationMode.collectLatest {
            homeViewModel.initialChecks()
        }
    }

    // Re-check the access method every time the home screen is shown, so the
    // hero status never lies about a Shizuku service that died meanwhile.
    LaunchedEffect(accessMethod) {
        if (accessMethod != null) {
            homeViewModel.recheckAccess()
        }
    }

    // Update detection: the manager only hits the network once every 6 hours.
    LaunchedEffect(Unit) {
        homeViewModel.checkForUpdates()
    }

    ApplicationScreen(
        modifier = Modifier.padding(start = 10.dp, end = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        topBar = {
            TopBar(
                barTitle = stringResource(id = R.string.app_name),
                icon = Icons.Outlined.Settings,
                scrollBehavior = it,
                onClickIcon = { navigate(Destinations.Preferences) },
            )
        },
        content = {
            // Update banner: only for an available/downloading/ready release the
            // user did not dismiss, or a failure worth retrying.
            val visibleUpdateTag = updateState.releaseOrNull()?.tag
            AnimatedVisibility(
                visible = updateState is UpdateState.Failed ||
                    (visibleUpdateTag != null && visibleUpdateTag != updateDismissedTag),
                enter = fadeIn(tween(motionDuration(220))) +
                    expandVertically(tween(motionDuration(240))),
                exit = fadeOut(tween(motionDuration(120))) +
                    shrinkVertically(tween(motionDuration(200))),
            ) {
                UpdateCard(
                    state = updateState,
                    currentVersion = homeViewModel.updateCurrentVersion,
                    compact = true,
                    onCheck = { homeViewModel.checkForUpdates(force = true) },
                    onDownload = { homeViewModel.downloadUpdate() },
                    onInstall = { homeViewModel.installUpdate() },
                    onOpenReleasePage = { homeViewModel.openUpdateReleasePage() },
                    onOpenInstallSettings = { homeViewModel.openInstallPermissionSettings() },
                    onDismiss = {
                        homeViewModel.dismissUpdate()
                        homeViewModel.clearUpdateError()
                    },
                )
            }

            AccessHeroCard(
                method = accessMethod,
                result = accessResults[accessMethod] ?: AccessCheckResult.unknown(),
                stage = uiState.installationCard.stage(),
                onChangeMethod = { navigate(Destinations.Onboarding) },
                onRecheck = { homeViewModel.recheckAccess() },
                onSelectImage = { picker.launch() },
            )

            Box(modifier = Modifier.animateContentSize()) {
                when (uiState.additionalCard) {
                    AdditionalCardState.NO_DYNAMIC_PARTITIONS ->
                        UnsupportedCard(
                            onClickClose = { exitProcess(0) },
                            onClickContinueAnyway = { homeViewModel.overrideDynamicPartitionCheck() },
                        )

                    AdditionalCardState.SETUP_STORAGE ->
                        SetupStorage { homeViewModel.takeUriPermission(it) }

                    AdditionalCardState.UNAVAIABLE_STORAGE ->
                        StorageWarningCard(
                            minPercentageFreeStorage = homeViewModel.allocPercentageInt.toString(),
                            onClick = { homeViewModel.overrideUnavaiableStorage() },
                        )

                    AdditionalCardState.MISSING_READ_LOGS_PERMISSION ->
                        RequiresLogPermissionCard(
                            onClickGrant = { homeViewModel.grantReadLogs() },
                            onClickRefuse = { homeViewModel.refuseReadLogs() },
                        )

                    AdditionalCardState.GRANTING_READ_LOGS_PERMISSION ->
                        GrantingPermissionCard()

                    AdditionalCardState.BOOTLOADER_UNLOCKED_WARNING ->
                        UnlockedBootloaderCard { homeViewModel.onClickBootloaderUnlockedWarning() }

                    AdditionalCardState.NONE -> {}
                }
            }

            AnimatedVisibility(
                visible = uiState.passedInitialChecks &&
                    uiState.additionalCard == AdditionalCardState.NONE,
                enter = fadeIn(tween(motionDuration(220))) +
                    expandVertically(tween(motionDuration(240))),
                exit = fadeOut(tween(motionDuration(120))) +
                    shrinkVertically(tween(motionDuration(200))),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    InstallationCard(
                        uiState = uiState.installationCard,
                        onClickInstall = { homeViewModel.onClickInstall() },
                        onClickUnmountSdCardAndRetry = {
                            homeViewModel.onClickUnmountSdCardAndRetry()
                        },
                        onClickSetSeLinuxPermissive = {
                            homeViewModel.onClickSetSeLinuxPermissive()
                        },
                        onClickRetryInstallation = {
                            homeViewModel.onClickRetryInstallation()
                        },
                        onClickClear = { homeViewModel.resetInstallationCard() },
                        onSelectFileSuccess = { homeViewModel.onFileSelectionResult(it) },
                        onClickCancelInstallation = { homeViewModel.onClickCancel() },
                        onClickDiscardInstalledGsiAndInstall = {
                            homeViewModel.onClickDiscardGsiAndStartInstallation()
                        },
                        onClickDiscardDsu = { homeViewModel.showDiscardSheet() },
                        onClickRebootToDynOS = { homeViewModel.onClickRebootToDynOS() },
                        onClickViewLogs = { homeViewModel.showLogsWarning() },
                        onClickViewCommands = { navigate(Destinations.ADBInstallation) },
                        onClickSetExpectedChecksum = {
                            homeViewModel.showExpectedChecksumSheet()
                        },
                        minPercentageOfFreeStorage =
                        homeViewModel.allocPercentageInt.toString(),
                    )

                    SectionLabel(text = stringResource(id = R.string.home_section_options))

                    UserdataCard(
                        isEnabled = uiState.isInstalling(),
                        uiState = uiState.userDataCard,
                        onCheckedChange = { homeViewModel.onCheckUserdataCard() },
                        onValueChange = { homeViewModel.updateUserdataSize(it) },
                    )
                    ImageSizeCard(
                        isEnabled = uiState.isInstalling(),
                        uiState = uiState.imageSizeCard,
                        onCheckedChange = { homeViewModel.onCheckImageSizeCard() },
                        onValueChange = { homeViewModel.updateImageSize(it) },
                    )
                }
            }

            DsuInfoCard(onOpenDocs = { uriHandler.openUri(HomeLinks.DSU_DOCS) })
        },
    )

    when (uiState.sheetDisplay) {
        SheetDisplayState.CONFIRM_INSTALLATION ->
            ConfirmInstallationSheet(
                filename = homeViewModel.obtainSelectedFilename(),
                userdata = homeViewModel.session.userSelection.getUserDataSizeAsGB(),
                fileSize = homeViewModel.session.userSelection.userSelectedImageSize,
                onClickConfirm = { homeViewModel.onConfirmInstallationSheet() },
                onClickCancel = { homeViewModel.dismissSheet() },
            )

        SheetDisplayState.CANCEL_INSTALLATION ->
            CancelSheet(
                onClickConfirm = { homeViewModel.onClickCancelInstallationButton() },
                onClickCancel = { homeViewModel.dismissSheet() },
            )

        SheetDisplayState.IMAGESIZE_WARNING ->
            ImageSizeWarningSheet(
                onClickConfirm = { homeViewModel.dismissSheet() },
                onClickCancel = { homeViewModel.onCheckImageSizeCard() },
            )

        SheetDisplayState.DISCARD_DSU ->
            DiscardDSUSheet(
                onClickConfirm = { homeViewModel.onClickDiscardGsi() },
                onClickCancel = { homeViewModel.dismissSheet() },
            )

        SheetDisplayState.VIEW_LOGS ->
            ViewLogsBottomSheet(
                logs = uiState.installationLogs,
                onClickSaveLogs = { homeViewModel.saveLogs(it) },
                onClickShareLogs = { homeViewModel.shareDiagnosticReport() },
                onDismiss = { homeViewModel.dismissSheet() },
            )

        SheetDisplayState.EXPECTED_CHECKSUM ->
            ExpectedChecksumSheet(
                input = uiState.expectedChecksumInput,
                isError = uiState.expectedChecksumInputError,
                hasExpectedChecksum = uiState.installationCard.expectedChecksum.isNotEmpty(),
                onValueChange = { homeViewModel.updateExpectedChecksumInput(it) },
                onClickConfirm = { homeViewModel.onConfirmExpectedChecksum() },
                onClickRemove = { homeViewModel.onClearExpectedChecksum() },
                onDismiss = { homeViewModel.dismissSheet() },
            )

        SheetDisplayState.NONE -> {}
    }
}

/**
 * Small subdued label that groups the secondary options under the main card.
 */
@Composable
private fun SectionLabel(text: String) {
    Row(
        modifier = Modifier.padding(start = 6.dp, top = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            shape = RoundedCornerShape(50),
            color = MaterialTheme.colorScheme.surfaceVariant,
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            )
        }
    }
}
