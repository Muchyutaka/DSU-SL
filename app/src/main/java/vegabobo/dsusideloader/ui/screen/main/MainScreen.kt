package vegabobo.dsusideloader.ui.screen.main

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.with
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.InsertDriveFile
import androidx.compose.material.icons.outlined.SystemUpdate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlin.system.exitProcess
import kotlinx.coroutines.flow.collectLatest
import vegabobo.dsusideloader.R
import vegabobo.dsusideloader.access.AccessCheckResult
import vegabobo.dsusideloader.preparation.InstallationStep
import vegabobo.dsusideloader.ui.cards.DsuInfoCard
import vegabobo.dsusideloader.ui.cards.installation.InstallationCard
import vegabobo.dsusideloader.ui.cards.warnings.GrantingPermissionCard
import vegabobo.dsusideloader.ui.cards.warnings.RequiresLogPermissionCard
import vegabobo.dsusideloader.ui.cards.warnings.SetupStorage
import vegabobo.dsusideloader.ui.cards.warnings.StorageWarningCard
import vegabobo.dsusideloader.ui.cards.warnings.UnlockedBootloaderCard
import vegabobo.dsusideloader.ui.cards.warnings.UnsupportedCard
import vegabobo.dsusideloader.ui.components.UpdateCard
import vegabobo.dsusideloader.ui.screen.Destinations
import vegabobo.dsusideloader.ui.screen.home.AdditionalCardState
import vegabobo.dsusideloader.ui.screen.home.ChecksumStatus
import vegabobo.dsusideloader.ui.screen.home.HomeViewModel
import vegabobo.dsusideloader.ui.screen.home.SheetDisplayState
import vegabobo.dsusideloader.ui.sdialogs.CancelSheet
import vegabobo.dsusideloader.ui.sdialogs.ConfirmInstallationSheet
import vegabobo.dsusideloader.ui.sdialogs.DiscardDSUSheet
import vegabobo.dsusideloader.ui.sdialogs.ExpectedChecksumSheet
import vegabobo.dsusideloader.ui.sdialogs.ImageSizeWarningSheet
import vegabobo.dsusideloader.ui.sdialogs.ViewLogsBottomSheet
import vegabobo.dsusideloader.ui.util.KeepScreenOn
import vegabobo.dsusideloader.ui.util.motionDuration
import vegabobo.dsusideloader.ui.util.rememberImagePickerLauncher
import vegabobo.dsusideloader.update.UpdateState
import vegabobo.dsusideloader.util.collectAsStateWithLifecycle

object MainLinks {
    const val DSU_DOCS = "https://source.android.com/devices/tech/ota/dynamic-system-updates"
}

/**
 * Main screen: header, access status, action row, image card, checksum card,
 * install action, additional settings and the collapsible DSU explanation.
 *
 * Spacing and typography follow one scale: 16 dp between cards, 8 dp between
 * items inside a card, 16 dp corner radius on every card.
 */
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun MainScreen(
    navigate: (String) -> Unit,
    homeViewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by homeViewModel.uiState.collectAsStateWithLifecycle()
    val accessMethod by homeViewModel.accessMethod.collectAsStateWithLifecycle()
    val accessResults by homeViewModel.accessResults.collectAsStateWithLifecycle()
    val updateState by homeViewModel.updateState.collectAsStateWithLifecycle()
    val updateDismissedTag by homeViewModel.updateDismissedTag.collectAsStateWithLifecycle()
    val uriHandler = LocalUriHandler.current

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

    LaunchedEffect(accessMethod) {
        if (accessMethod != null) {
            homeViewModel.recheckAccess()
        }
    }

    val installationCard = uiState.installationCard
    val isInstalling = installationCard.installationStep != InstallationStep.NOT_INSTALLING
    val canInstall = installationCard.isInstallable && !installationCard.isInstallBlocked()
    val accessResult = accessResults[accessMethod] ?: AccessCheckResult.unknown()
    val hasWarningCard = uiState.additionalCard != AdditionalCardState.NONE
    // Evaluated here: the transitionSpec lambda is not a composable scope.
    val cardEnterMillis = motionDuration(220)
    val cardExitMillis = motionDuration(160)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 24.dp),
    ) {
        // Screen title
        Text(
            text = stringResource(id = R.string.app_name),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Start,
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Update: a small non-intrusive banner when a newer release exists
        // (tapping it opens About, where the download happens), the detailed
        // card only while something is actually in flight or failed.
        val release = updateState.releaseOrNull()
        val showUpdateBanner = updateState is UpdateState.Available &&
            release != null &&
            release.tag != updateDismissedTag
        val showUpdateCard = updateState is UpdateState.Failed ||
            updateState is UpdateState.Downloading ||
            updateState is UpdateState.ReadyToInstall

        AnimatedVisibility(
            visible = showUpdateBanner,
            enter = fadeIn(tween(motionDuration(200))) +
                expandVertically(tween(motionDuration(220))),
            exit = fadeOut(tween(motionDuration(120))) +
                shrinkVertically(tween(motionDuration(180))),
        ) {
            Column {
                if (release != null) {
                    UpdateAvailableBanner(
                        version = release.tag,
                        onClick = { navigate(Destinations.About) },
                        onDismiss = { homeViewModel.dismissUpdate() },
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        AnimatedVisibility(
            visible = showUpdateCard,
            enter = fadeIn(tween(motionDuration(200))) +
                expandVertically(tween(motionDuration(220))),
            exit = fadeOut(tween(motionDuration(120))) +
                shrinkVertically(tween(motionDuration(180))),
        ) {
            Column {
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
                    onAcknowledgeConflict = { homeViewModel.clearUpdateError() },
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        MainStatusCard(
            method = accessMethod,
            result = accessResult,
            onRefresh = { homeViewModel.recheckAccess() },
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Action row: three equally sized outline pills.
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ActionPill(
                text = stringResource(id = R.string.main_action_select_image),
                modifier = Modifier.weight(1f),
                onClick = { picker.launch() },
            )
            ActionPill(
                text = stringResource(id = R.string.main_action_recheck),
                modifier = Modifier.weight(1f),
                onClick = { homeViewModel.recheckAccess() },
            )
            ActionPill(
                text = stringResource(id = R.string.main_action_change_method),
                modifier = Modifier.weight(1f),
                onClick = { navigate(Destinations.Onboarding) },
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Warnings that must be resolved before installing.
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

        if (hasWarningCard) {
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Selected image, or the live installation card while installing.
        AnimatedContent(
            targetState = isInstalling,
            transitionSpec = {
                (fadeIn(tween(cardEnterMillis)) +
                    expandVertically(tween(cardEnterMillis))) with
                    (fadeOut(tween(cardExitMillis)) +
                        shrinkVertically(tween(cardExitMillis)))
            },
        ) { installing ->
            if (installing) {
                InstallationCard(
                    uiState = installationCard,
                    onClickInstall = { homeViewModel.onClickInstall() },
                    onClickUnmountSdCardAndRetry = { homeViewModel.onClickUnmountSdCardAndRetry() },
                    onClickSetSeLinuxPermissive = { homeViewModel.onClickSetSeLinuxPermissive() },
                    onClickRetryInstallation = { homeViewModel.onClickRetryInstallation() },
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
                    minPercentageOfFreeStorage = homeViewModel.allocPercentageInt.toString(),
                )
            } else {
                SelectedImageCard(
                    imageName = installationCard.text,
                    sha256 = installationCard.checksum.value
                        .takeIf { installationCard.checksum.status == ChecksumStatus.READY },
                    isInstallable = installationCard.isInstallable,
                    isError = installationCard.isError,
                    onSelect = { picker.launch() },
                    onClear = { homeViewModel.resetInstallationCard() },
                )
            }
        }

        if (!isInstalling) {
            Spacer(modifier = Modifier.height(16.dp))

            ChecksumCard(
                uiState = installationCard,
                expectedChecksumInput = uiState.expectedChecksumInput,
                expectedChecksumInputError = uiState.expectedChecksumInputError,
                onToggleVerification = { homeViewModel.setChecksumVerification(it) },
                onExpectedChecksumChange = { homeViewModel.updateExpectedChecksumInput(it) },
                onApplyExpectedChecksum = { homeViewModel.onConfirmExpectedChecksum() },
                onClearExpectedChecksum = { homeViewModel.onClearExpectedChecksum() },
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { homeViewModel.onClickInstall() },
                enabled = canInstall,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            ) {
                Text(
                    text = stringResource(id = R.string.main_install),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            AdditionalSettingsCard(
                userdataState = uiState.userDataCard,
                imageSizeState = uiState.imageSizeCard,
                isInstalling = isInstalling,
                onToggleUserdata = { homeViewModel.onCheckUserdataCard() },
                onUserdataChange = { homeViewModel.updateUserdataSize(it) },
                onToggleImageSize = { homeViewModel.onCheckImageSizeCard() },
                onImageSizeChange = { homeViewModel.updateImageSize(it) },
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        DsuInfoCard(onOpenDocs = { uriHandler.openUri(MainLinks.DSU_DOCS) })

        Spacer(modifier = Modifier.height(24.dp))
    }

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
                hasExpectedChecksum = installationCard.expectedChecksum.isNotEmpty(),
                onValueChange = { homeViewModel.updateExpectedChecksumInput(it) },
                onClickConfirm = { homeViewModel.onConfirmExpectedChecksum() },
                onClickRemove = { homeViewModel.onClearExpectedChecksum() },
                onDismiss = { homeViewModel.dismissSheet() },
            )

        SheetDisplayState.NONE -> {}
    }
}

/**
 * Non-intrusive "Update available" banner of the main screen. Tapping it opens
 * the About screen, which owns the download/install flow.
 */
@Composable
private fun UpdateAvailableBanner(
    version: String,
    onClick: () -> Unit,
    onDismiss: () -> Unit,
) {
    val colorScheme = MaterialTheme.colorScheme

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        color = colorScheme.tertiaryContainer,
        contentColor = colorScheme.onTertiaryContainer,
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Outlined.SystemUpdate,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1F)) {
                Text(
                    text = stringResource(id = R.string.update_available_banner_title),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Start,
                )
                Text(
                    text = stringResource(id = R.string.update_available_banner_subtitle, version),
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Start,
                )
            }
            TextButton(onClick = onDismiss) {
                Text(
                    text = stringResource(id = R.string.update_later),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}

/** Outline pill of the action row: same height and label style for all three. */
@Composable
private fun ActionPill(
    text: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        shape = RoundedCornerShape(50),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = MaterialTheme.colorScheme.onBackground,
        ),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * "Selected DSU Image" card: the file name (or the empty prompt) in an inner
 * rounded box, plus the computed SHA-256 of the image once it is available.
 * Tapping the box opens the system file picker.
 */
@Composable
private fun SelectedImageCard(
    imageName: String,
    sha256: String?,
    isInstallable: Boolean,
    isError: Boolean,
    onSelect: () -> Unit,
    onClear: () -> Unit,
) {
    val colorScheme = MaterialTheme.colorScheme
    val hasImage = imageName.isNotEmpty()

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = colorScheme.surfaceVariant,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(id = R.string.main_image_card_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = colorScheme.onSurface,
                textAlign = TextAlign.Start,
            )

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = colorScheme.surface,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelect() },
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = if (hasImage) {
                            Icons.Outlined.Description
                        } else {
                            Icons.Outlined.InsertDriveFile
                        },
                        contentDescription = null,
                        tint = if (isError) colorScheme.error else colorScheme.primary,
                        modifier = Modifier.size(24.dp),
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1F)) {
                        Text(
                            text = if (hasImage) {
                                imageName
                            } else {
                                stringResource(id = R.string.main_image_none)
                            },
                            style = MaterialTheme.typography.bodyLarge,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Start,
                            color = when {
                                isError -> colorScheme.error
                                hasImage -> colorScheme.onSurface
                                else -> colorScheme.onSurfaceVariant
                            },
                        )
                        if (!hasImage) {
                            Text(
                                text = stringResource(id = R.string.main_image_hint),
                                style = MaterialTheme.typography.bodySmall,
                                color = colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Start,
                            )
                        } else if (sha256 != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            SelectionContainer {
                                Text(
                                    text = sha256,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontFamily = FontFamily.Monospace,
                                    color = colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Start,
                                )
                            }
                        }
                    }
                }
            }

            if (hasImage && !isInstallable) {
                Text(
                    text = stringResource(id = R.string.file_unsupported),
                    style = MaterialTheme.typography.bodySmall,
                    color = colorScheme.error,
                    textAlign = TextAlign.Start,
                )
            }

            if (hasImage) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = onClear) {
                        Text(
                            text = stringResource(id = R.string.clear),
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                }
            }
        }
    }
}
