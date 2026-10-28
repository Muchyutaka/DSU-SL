package vegabobo.dsusideloader.ui.screen.log

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import vegabobo.dsusideloader.diagnostics.DiagnosticReporter
import vegabobo.dsusideloader.log.InstallationLogStore

/**
 * Log tab: shows the installation log lines shared with the home screen and
 * offers the same explicit export actions (nothing is uploaded automatically).
 */
@HiltViewModel
class LogViewModel @Inject constructor(
    private val application: Application,
    private val logStore: InstallationLogStore,
) : ViewModel() {

    val lines: StateFlow<List<String>> = logStore.lines

    fun isEmpty(): Boolean = logStore.isEmpty()

    fun clear() = logStore.clear()

    /**
     * Builds a diagnostic report from the shared log and opens the share
     * chooser. Runs off the main thread: the report includes device info and a
     * bounded logcat excerpt.
     */
    fun shareReport() {
        val reporter = DiagnosticReporter(application)
        val logs = logStore.text()
        viewModelScope.launch {
            val report = withContext(Dispatchers.IO) {
                reporter.buildReport(appLogs = logs)
            }
            reporter.share(report)
        }
    }
}
