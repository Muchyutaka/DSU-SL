package vegabobo.dsusideloader.ui.screen.history

import vegabobo.dsusideloader.history.InstallHistoryEntry
import vegabobo.dsusideloader.history.InstallPreset

data class HistoryUiState(
    val entries: List<InstallHistoryEntry> = emptyList(),
    val presets: List<InstallPreset> = emptyList(),
    val isLoading: Boolean = true,
)
