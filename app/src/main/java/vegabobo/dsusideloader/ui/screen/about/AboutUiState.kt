package vegabobo.dsusideloader.ui.screen.about

import kotlinx.coroutines.flow.MutableStateFlow

enum class DevOptToastDisplay {
    NONE, ENABLED_DEV_OPT, DISABLED_DEV_OPT
}
data class AboutScreenUiState(
    val toastDisplay: MutableStateFlow<DevOptToastDisplay> = MutableStateFlow(DevOptToastDisplay.NONE),
)
