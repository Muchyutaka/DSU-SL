package vegabobo.dsusideloader.diagnostics

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import vegabobo.dsusideloader.BuildConfig
import vegabobo.dsusideloader.R
import vegabobo.dsusideloader.util.CmdRunner

/**
 * Builds a bounded, structured diagnostic report and shares it explicitly with
 * another app through the existing [FileProvider].
 *
 * Nothing is ever uploaded automatically: the user has to pick a target app.
 */
class DiagnosticReporter(private val context: Context) {

    private val tag = this.javaClass.simpleName

    fun buildReport(
        operationMode: String? = null,
        installationDetails: String? = null,
        errorInfo: String? = null,
        appLogs: String? = null,
        logcatExcerpt: String? = null,
    ): String {
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z", Locale.US).format(Date())
        val logcat = logcatExcerpt ?: readLogcatExcerpt()
        return buildString {
            appendLine("DSU Sideloader diagnostic report")
            appendLine("generated: $timestamp")
            appendLine()
            appendLine("=== Application ===")
            appendLine(DeviceInfo.appSection(operationMode))
            appendLine()
            appendLine("=== Device ===")
            appendLine(DeviceInfo.deviceSection())
            appendLine()
            appendLine("=== Installation ===")
            appendLine(
                ReportFormatting.truncate(
                    installationDetails,
                    placeholder = "no installation in progress",
                ),
            )
            appendLine()
            appendLine("=== Result ===")
            appendLine(ReportFormatting.truncate(errorInfo, placeholder = "no error recorded"))
            appendLine()
            appendLine("=== Application logs ===")
            appendLine(
                ReportFormatting.truncate(
                    appLogs,
                    placeholder = "no application logs captured",
                ),
            )
            appendLine()
            appendLine("=== Logcat excerpt ===")
            append(ReportFormatting.truncateLines(logcat))
        }
    }

    /**
     * Writes the report to the app files directory and opens the system share
     * sheet.
     *
     * @return true when the share sheet was displayed.
     */
    fun share(report: String): Boolean {
        return try {
            val file = writeReportFile(report)
            val uri = FileProvider.getUriForFile(
                context,
                "${BuildConfig.APPLICATION_ID}.provider",
                file,
            )
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.export_logs_title))
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(
                shareIntent,
                context.getString(R.string.share_logs_title),
            ).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
            true
        } catch (e: Exception) {
            Log.w(tag, "Unable to share the diagnostic report", e)
            false
        }
    }

    private fun writeReportFile(report: String): File {
        val directory = File(context.filesDir, LOGS_DIRECTORY)
        if (!directory.exists()) {
            directory.mkdirs()
        }
        val stamp = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date())
        val file = File(directory, "$REPORT_PREFIX$stamp.txt")
        file.writeText(report)
        pruneOldReports(directory)
        return file
    }

    private fun pruneOldReports(directory: File) {
        val reports = directory
            .listFiles { file -> file.isFile && file.name.startsWith(REPORT_PREFIX) }
            ?.sortedByDescending { it.lastModified() }
            ?: return
        reports.drop(MAX_STORED_REPORTS).forEach { it.delete() }
    }

    private fun readLogcatExcerpt(): String {
        return try {
            // "-d" dumps the current buffers and "-t" limits the output size.
            CmdRunner.run("logcat -d -t ${ReportFormatting.DEFAULT_MAX_LINES}")
        } catch (e: Exception) {
            Log.w(tag, "Unable to read logcat", e)
            ""
        }
    }

    private companion object {
        const val LOGS_DIRECTORY = "logs"
        const val REPORT_PREFIX = "dsu-sideloader-report-"
        const val MAX_STORED_REPORTS = 5
    }
}
