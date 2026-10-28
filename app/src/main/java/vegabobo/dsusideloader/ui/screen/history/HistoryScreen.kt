package vegabobo.dsusideloader.ui.screen.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import vegabobo.dsusideloader.R
import vegabobo.dsusideloader.history.InstallHistoryEntry
import vegabobo.dsusideloader.history.InstallPreset
import vegabobo.dsusideloader.history.InstallResult
import vegabobo.dsusideloader.ui.components.ApplicationScreen
import vegabobo.dsusideloader.ui.components.CardBox
import vegabobo.dsusideloader.ui.components.SimpleCard
import vegabobo.dsusideloader.ui.components.Title
import vegabobo.dsusideloader.ui.components.TopBar
import vegabobo.dsusideloader.ui.components.buttons.PrimaryButton
import vegabobo.dsusideloader.ui.components.buttons.SecondaryButton
import vegabobo.dsusideloader.ui.screen.Destinations
import vegabobo.dsusideloader.util.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    navigate: (String) -> Unit,
    historyViewModel: HistoryViewModel = hiltViewModel(),
) {
    val uiState by historyViewModel.uiState.collectAsStateWithLifecycle()

    ApplicationScreen(
        modifier = Modifier.padding(start = 10.dp, end = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        topBar = {
            TopBar(
                barTitle = stringResource(id = R.string.install_history_title),
                scrollBehavior = it,
                onClickBackButton = { navigate(Destinations.Up) },
            )
        },
        content = {
            if (uiState.entries.isEmpty() && uiState.presets.isEmpty() && !uiState.isLoading) {
                SimpleCard(text = stringResource(id = R.string.history_empty))
            }

            if (uiState.entries.isNotEmpty()) {
                Title(title = stringResource(id = R.string.history_entries_title))
                Text(
                    modifier = Modifier.padding(start = 17.dp, end = 17.dp),
                    text = stringResource(id = R.string.history_retry_note),
                    fontSize = 13.sp,
                )
                uiState.entries.forEach { entry ->
                    HistoryEntryCard(
                        entry = entry,
                        onRetry = {
                            historyViewModel.applyEntry(entry)
                            navigate(Destinations.Homepage)
                        },
                        onSavePreset = { historyViewModel.savePresetFromEntry(entry) },
                    )
                }
                Row(modifier = Modifier.fillMaxWidth()) {
                    Spacer(modifier = Modifier.weight(1F))
                    SecondaryButton(
                        text = stringResource(id = R.string.history_clear),
                        onClick = { historyViewModel.clearHistory() },
                    )
                }
            }

            Title(title = stringResource(id = R.string.history_presets_title))
            if (uiState.presets.isEmpty()) {
                SimpleCard(text = stringResource(id = R.string.history_presets_empty))
            } else {
                uiState.presets.forEach { preset ->
                    PresetCard(
                        preset = preset,
                        onApply = {
                            historyViewModel.applyPreset(preset)
                            navigate(Destinations.Homepage)
                        },
                        onDelete = { historyViewModel.deletePreset(preset) },
                    )
                }
            }
        },
    )
}

@Composable
private fun HistoryEntryCard(
    entry: InstallHistoryEntry,
    onRetry: () -> Unit,
    onSavePreset: () -> Unit,
) {
    CardBox(
        cardTitle = entry.fileName.ifEmpty { stringResource(id = R.string.history_unknown_file) },
    ) {
        Text(
            text = formatTimestamp(entry.timestamp),
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.secondary,
        )
        Text(
            text = stringResource(id = R.string.history_result, resultText(entry.result)),
            fontSize = 14.sp,
        )
        ConfigurationText(
            userdataGb = entry.userdataGb,
            imageSizeBytes = entry.imageSizeBytes,
        )
        if (entry.errorSummary.isNotEmpty()) {
            Text(
                modifier = Modifier.padding(top = 4.dp),
                text = stringResource(id = R.string.history_error_summary, entry.errorSummary),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.error,
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
        ) {
            SecondaryButton(
                text = stringResource(id = R.string.history_save_preset),
                onClick = onSavePreset,
            )
            Spacer(modifier = Modifier.weight(1F))
            PrimaryButton(
                text = stringResource(id = R.string.history_retry),
                onClick = onRetry,
            )
        }
    }
}

@Composable
private fun PresetCard(
    preset: InstallPreset,
    onApply: () -> Unit,
    onDelete: () -> Unit,
) {
    CardBox(cardTitle = stringResource(id = R.string.history_preset_title)) {
        ConfigurationText(
            userdataGb = preset.userdataGb,
            imageSizeBytes = preset.imageSizeBytes,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
        ) {
            SecondaryButton(
                text = stringResource(id = R.string.history_preset_delete),
                onClick = onDelete,
            )
            Spacer(modifier = Modifier.weight(1F))
            PrimaryButton(
                text = stringResource(id = R.string.history_preset_apply),
                onClick = onApply,
            )
        }
    }
}

@Composable
private fun ConfigurationText(userdataGb: Long, imageSizeBytes: Long) {
    Text(
        text = stringResource(id = R.string.history_userdata, userdataGb),
        fontSize = 14.sp,
    )
    Text(
        text =
        if (imageSizeBytes > 0) {
            stringResource(id = R.string.history_image_size, imageSizeBytes)
        } else {
            stringResource(id = R.string.history_image_size_auto)
        },
        fontSize = 14.sp,
    )
}

@Composable
private fun resultText(result: InstallResult): String {
    return when (result) {
        InstallResult.SUCCESS -> stringResource(id = R.string.history_result_success)
        InstallResult.ERROR -> stringResource(id = R.string.history_result_error)
        InstallResult.CANCELED -> stringResource(id = R.string.history_result_canceled)
    }
}

private fun formatTimestamp(timestamp: Long): String {
    if (timestamp <= 0) {
        return ""
    }
    return SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(timestamp))
}
