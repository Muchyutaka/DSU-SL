package vegabobo.dsusideloader.ui.screen.settings

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.NewReleases
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import vegabobo.dsusideloader.BuildConfig
import vegabobo.dsusideloader.R
import vegabobo.dsusideloader.access.AccessCheckResult
import vegabobo.dsusideloader.preferences.AppPrefs
import vegabobo.dsusideloader.ui.components.ApplicationScreen
import vegabobo.dsusideloader.ui.components.DialogLikeBottomSheet
import vegabobo.dsusideloader.ui.components.PreferenceItem
import vegabobo.dsusideloader.ui.components.SettingsSection
import vegabobo.dsusideloader.ui.components.TopBar
import vegabobo.dsusideloader.ui.screen.Destinations
import vegabobo.dsusideloader.ui.screen.onboarding.accessMethodTitle
import vegabobo.dsusideloader.ui.screen.onboarding.accessStatusLabel
import vegabobo.dsusideloader.ui.theme.ThemeMode
import vegabobo.dsusideloader.util.OperationMode
import vegabobo.dsusideloader.util.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Settings(
    navigate: (String) -> Unit,
    settingsViewModel: SettingsViewModel = hiltViewModel(),
) {
    val uiState by settingsViewModel.uiState.collectAsStateWithLifecycle()
    val accessMethod by settingsViewModel.accessMethod.collectAsStateWithLifecycle()
    val accessResults by settingsViewModel.accessResults.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        settingsViewModel.checkDevOpt()
    }

    val currentAccessResult = accessMethod?.let { accessResults[it] }
        ?: AccessCheckResult.unknown()

    ApplicationScreen(
        topBar = {
            TopBar(
                barTitle = stringResource(id = R.string.settings),
                scrollBehavior = it,
                onClickBackButton = { navigate(Destinations.Up) },
            )
        },
    ) {
        SettingsSection(title = stringResource(id = R.string.settings_section_appearance)) {
            PreferenceItem(
                title = stringResource(id = R.string.dynamic_color_title),
                description =
                if (settingsViewModel.isDynamicColorSupported()) {
                    stringResource(id = R.string.dynamic_color_description)
                } else {
                    stringResource(id = R.string.requires_android_12)
                },
                showToggle = true,
                isEnabled = settingsViewModel.isDynamicColorSupported(),
                isChecked = uiState.preferences[AppPrefs.DYNAMIC_COLOR]!!,
                onClick = { settingsViewModel.togglePreference(AppPrefs.DYNAMIC_COLOR, !it) },
            )
            PreferenceItem(
                title = stringResource(id = R.string.theme_title),
                description = when (uiState.themeMode) {
                    ThemeMode.SYSTEM -> stringResource(id = R.string.theme_mode_system)
                    ThemeMode.LIGHT -> stringResource(id = R.string.theme_mode_light)
                    ThemeMode.DARK -> stringResource(id = R.string.theme_mode_dark)
                },
                onClick = { settingsViewModel.cycleThemeMode() },
            )
            PreferenceItem(
                title = stringResource(id = R.string.amoled_title),
                description = stringResource(id = R.string.amoled_description),
                showToggle = true,
                isChecked = uiState.preferences[AppPrefs.AMOLED_BLACK]!!,
                onClick = { settingsViewModel.togglePreference(AppPrefs.AMOLED_BLACK, !it) },
            )
        }

        SettingsSection(title = stringResource(id = R.string.settings_section_access)) {
            PreferenceItem(
                title = stringResource(id = R.string.settings_current_access),
                description = accessMethod?.let { method ->
                    "${accessMethodTitle(method)} - " +
                        accessStatusLabel(currentAccessResult.status)
                } ?: stringResource(id = R.string.access_status_unknown),
                onClick = { navigate(Destinations.Onboarding) },
            )
            PreferenceItem(
                title = stringResource(id = R.string.access_recheck),
                description = stringResource(id = R.string.access_recheck_done),
                onClick = { settingsViewModel.recheckAccess() },
            )
            PreferenceItem(
                title = stringResource(id = R.string.access_change_method),
                onClick = { navigate(Destinations.Onboarding) },
            )
        }

        SettingsSection(title = stringResource(id = R.string.settings_section_installation)) {
            PreferenceItem(
                title = stringResource(id = R.string.verify_checksum_title),
                description = stringResource(id = R.string.verify_checksum_description),
                showToggle = true,
                isChecked = uiState.preferences[AppPrefs.VERIFY_CHECKSUM]!!,
                onClick = { settingsViewModel.togglePreference(AppPrefs.VERIFY_CHECKSUM, !it) },
            )
            PreferenceItem(
                title = stringResource(id = R.string.strict_checksum_title),
                description = stringResource(id = R.string.strict_checksum_description),
                showToggle = true,
                isEnabled = uiState.preferences[AppPrefs.VERIFY_CHECKSUM]!!,
                isChecked = uiState.preferences[AppPrefs.STRICT_CHECKSUM]!!,
                onClick = { settingsViewModel.togglePreference(AppPrefs.STRICT_CHECKSUM, !it) },
            )
            PreferenceItem(
                title = stringResource(id = R.string.check_patch_level_title),
                description = stringResource(id = R.string.check_patch_level_description),
                showToggle = true,
                isChecked = uiState.preferences[AppPrefs.CHECK_PATCH_LEVEL]!!,
                onClick = { settingsViewModel.togglePreference(AppPrefs.CHECK_PATCH_LEVEL, !it) },
            )
            PreferenceItem(
                title = stringResource(id = R.string.builtin_installer),
                description =
                if (settingsViewModel.isAndroidQ()) {
                    stringResource(id = R.string.unsupported)
                } else if (uiState.isRoot) {
                    stringResource(id = R.string.builtin_installer_description)
                } else {
                    stringResource(id = R.string.requires_root)
                },
                showToggle = true,
                isEnabled = uiState.isRoot && !settingsViewModel.isAndroidQ(),
                isChecked = uiState.preferences[AppPrefs.USE_BUILTIN_INSTALLER]!!,
                onClick = {
                    if (!it) {
                        settingsViewModel.updateSheetDisplay(DialogSheetState.BUILT_IN_INSTALLER)
                    }
                    settingsViewModel.togglePreference(AppPrefs.USE_BUILTIN_INSTALLER, !it)
                },
            )
            PreferenceItem(
                title = stringResource(id = R.string.unmount_sd_title),
                description = stringResource(id = R.string.unmount_sd_description),
                showToggle = true,
                isChecked = uiState.preferences[AppPrefs.UMOUNT_SD]!!,
                onClick = { settingsViewModel.togglePreference(AppPrefs.UMOUNT_SD, !it) },
            )
            PreferenceItem(
                title = stringResource(id = R.string.keep_screen_on),
                showToggle = true,
                isChecked = uiState.preferences[AppPrefs.KEEP_SCREEN_ON]!!,
                onClick = { settingsViewModel.togglePreference(AppPrefs.KEEP_SCREEN_ON, !it) },
            )
            PreferenceItem(
                title = stringResource(id = R.string.install_history_title),
                description = stringResource(id = R.string.install_history_description),
                onClick = { navigate(Destinations.History) },
            )
            PreferenceItem(
                title = stringResource(id = R.string.settings_presets),
                description = stringResource(id = R.string.settings_presets_description),
                onClick = { navigate(Destinations.History) },
            )
        }

        if (uiState.isDevOptEnabled) {
            SettingsSection(title = stringResource(id = R.string.developer_options)) {
                PreferenceItem(
                    title = stringResource(id = R.string.storage_check_title),
                    description = stringResource(id = R.string.storage_check_description),
                    showToggle = true,
                    isChecked = uiState.preferences[AppPrefs.DISABLE_STORAGE_CHECK]!!,
                    onClick = {
                        if (!it) {
                            settingsViewModel.updateSheetDisplay(
                                DialogSheetState.DISABLE_STORAGE_CHECK,
                            )
                        }
                        settingsViewModel.togglePreference(
                            AppPrefs.DISABLE_STORAGE_CHECK,
                            !it,
                        )
                    },
                )
                if (settingsViewModel.getOperationMode() != OperationMode.ADB) {
                    PreferenceItem(
                        title = stringResource(id = R.string.full_logcat_logging_title),
                        description = stringResource(
                            id = R.string.full_logcat_logging_description,
                        ),
                        showToggle = true,
                        isChecked = uiState.preferences[AppPrefs.FULL_LOGCAT_LOGGING]!!,
                        onClick = {
                            settingsViewModel.togglePreference(
                                AppPrefs.FULL_LOGCAT_LOGGING,
                                !it,
                            )
                        },
                    )
                }
            }
        }

        SettingsSection(title = stringResource(id = R.string.settings_section_logs)) {
            PreferenceItem(
                title = stringResource(id = R.string.export_logs_title),
                description = stringResource(id = R.string.export_logs_description),
                onClick = { settingsViewModel.shareDiagnosticReport() },
            )
            PreferenceItem(
                title = stringResource(id = R.string.clear_logs_title),
                description = stringResource(id = R.string.clear_logs_description),
                onClick = { settingsViewModel.clearStoredReports() },
            )
        }

        SettingsSection(title = stringResource(id = R.string.settings_section_about)) {
            PreferenceItem(
                title = stringResource(id = R.string.about),
                description = stringResource(id = R.string.settings_version, BuildConfig.VERSION_NAME),
                onClick = { navigate(Destinations.About) },
            )
            PreferenceItem(
                title = stringResource(id = R.string.operation_mode),
                description = settingsViewModel.checkOperationMode(),
            )
            PreferenceItem(
                title = stringResource(id = R.string.settings_source_code),
                description = stringResource(id = R.string.settings_license),
                onClick = { navigate(Destinations.About) },
            )
        }

        Spacer(modifier = Modifier.padding(top = 24.dp))
    }

    when (uiState.dialogSheetDisplay) {
        DialogSheetState.BUILT_IN_INSTALLER ->
            DialogLikeBottomSheet(
                title = stringResource(id = R.string.experimental_feature),
                icon = Icons.Outlined.NewReleases,
                text = stringResource(id = R.string.experimental_feature_description),
                confirmText = stringResource(id = R.string.yes),
                cancelText = stringResource(id = R.string.cancel),
                onClickCancel = {
                    settingsViewModel.togglePreference(AppPrefs.USE_BUILTIN_INSTALLER, false)
                    settingsViewModel.updateSheetDisplay(DialogSheetState.NONE)
                },
                onClickConfirm = { settingsViewModel.updateSheetDisplay(DialogSheetState.NONE) },
            )

        DialogSheetState.DISABLE_STORAGE_CHECK ->
            DialogLikeBottomSheet(
                title = stringResource(id = R.string.developer_options),
                icon = Icons.Outlined.WarningAmber,
                text = stringResource(id = R.string.storage_check_description),
                confirmText = stringResource(id = R.string.yes),
                cancelText = stringResource(id = R.string.cancel),
                onClickCancel = {
                    settingsViewModel.togglePreference(AppPrefs.DISABLE_STORAGE_CHECK, false)
                    settingsViewModel.updateSheetDisplay(DialogSheetState.NONE)
                },
                onClickConfirm = { settingsViewModel.updateSheetDisplay(DialogSheetState.NONE) },
            )

        DialogSheetState.NONE -> {}
    }
}
