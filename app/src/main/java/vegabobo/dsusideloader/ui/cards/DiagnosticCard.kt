package vegabobo.dsusideloader.ui.cards

import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import vegabobo.dsusideloader.R
import vegabobo.dsusideloader.diagnostics.DiagnosticType
import vegabobo.dsusideloader.diagnostics.ReportFormatting
import vegabobo.dsusideloader.ui.components.CardBox
import vegabobo.dsusideloader.ui.components.buttons.ActionButton
import vegabobo.dsusideloader.ui.screen.about.AboutLinks

private const val DOC_OPERATION_MODES = "#operation-modes"
private const val DOC_COMMON_QUESTIONS = "#common-questions"

private data class DiagnosticContent(
    @StringRes val title: Int,
    @StringRes val cause: Int,
    @StringRes val fix: Int,
    val docUrl: String? = null,
)

/**
 * Maps a [DiagnosticType] to the texts shown on the diagnostic card.
 *
 * @return null when the type has no friendly explanation (unknown/cancelled).
 */
private fun contentFor(type: DiagnosticType): DiagnosticContent? {
    return when (type) {
        DiagnosticType.SELINUX_DENIAL -> DiagnosticContent(
            R.string.diagnostic_selinux_title,
            R.string.diagnostic_selinux_cause,
            R.string.diagnostic_selinux_fix,
            AboutLinks.REPOSITORY_URL + DOC_OPERATION_MODES,
        )

        DiagnosticType.PARTITION_CREATION_FAILED -> DiagnosticContent(
            R.string.diagnostic_partition_title,
            R.string.diagnostic_partition_cause,
            R.string.diagnostic_partition_fix,
            AboutLinks.REPOSITORY_URL + DOC_COMMON_QUESTIONS,
        )

        DiagnosticType.CORRUPTED_IMAGE -> DiagnosticContent(
            R.string.diagnostic_corrupted_title,
            R.string.diagnostic_corrupted_cause,
            R.string.diagnostic_corrupted_fix,
        )

        DiagnosticType.INSUFFICIENT_STORAGE -> DiagnosticContent(
            R.string.diagnostic_storage_title,
            R.string.diagnostic_storage_cause,
            R.string.diagnostic_storage_fix,
            AboutLinks.REPOSITORY_URL + DOC_COMMON_QUESTIONS,
        )

        DiagnosticType.UNSUPPORTED_IMAGE -> DiagnosticContent(
            R.string.diagnostic_unsupported_title,
            R.string.diagnostic_unsupported_cause,
            R.string.diagnostic_unsupported_fix,
        )

        DiagnosticType.EXTERNAL_SD_ALLOCATION -> DiagnosticContent(
            R.string.diagnostic_sdcard_title,
            R.string.diagnostic_sdcard_cause,
            R.string.diagnostic_sdcard_fix,
            AboutLinks.REPOSITORY_URL + DOC_COMMON_QUESTIONS,
        )

        DiagnosticType.F2FS_KERNEL -> DiagnosticContent(
            R.string.diagnostic_f2fs_title,
            R.string.diagnostic_f2fs_cause,
            R.string.diagnostic_f2fs_fix,
            AboutLinks.REPOSITORY_URL + DOC_OPERATION_MODES,
        )

        DiagnosticType.EXTENTS_TOO_FRAGMENTED -> DiagnosticContent(
            R.string.diagnostic_extents_title,
            R.string.diagnostic_extents_cause,
            R.string.diagnostic_extents_fix,
            AboutLinks.REPOSITORY_URL + DOC_OPERATION_MODES,
        )

        DiagnosticType.ALREADY_RUNNING_DSU -> DiagnosticContent(
            R.string.diagnostic_running_dsu_title,
            R.string.diagnostic_running_dsu_cause,
            R.string.diagnostic_running_dsu_fix,
        )

        DiagnosticType.CANCELLED,
        DiagnosticType.UNKNOWN -> null
    }
}

/**
 * @return true when [DiagnosticCard] has a friendly explanation for [type].
 */
fun hasDiagnosticContent(type: DiagnosticType): Boolean = contentFor(type) != null

/**
 * Friendly explanation of a failed installation, with the raw error hidden
 * behind an expandable "Technical details" section.
 */
@Composable
fun DiagnosticCard(
    diagnosticType: DiagnosticType,
    rawError: String,
    modifier: Modifier = Modifier,
) {
    val content = contentFor(diagnosticType) ?: return
    val uriHandler = LocalUriHandler.current
    var showTechnicalDetails by remember { mutableStateOf(false) }

    CardBox(
        modifier = modifier,
        cardTitle = stringResource(id = content.title),
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Icon(
                imageVector = Icons.Outlined.WarningAmber,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(end = 6.dp),
            )
            Text(
                text = stringResource(id = content.cause),
                fontSize = 14.sp,
            )
        }
        Text(
            modifier = Modifier.padding(top = 8.dp),
            text = stringResource(id = R.string.diagnostic_suggested_fix),
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.secondary,
        )
        Text(
            text = stringResource(id = content.fix),
            fontSize = 14.sp,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
        ) {
            ActionButton(
                text =
                if (showTechnicalDetails) {
                    stringResource(id = R.string.diagnostic_hide_technical_details)
                } else {
                    stringResource(id = R.string.diagnostic_technical_details)
                },
                onClick = { showTechnicalDetails = !showTechnicalDetails },
                textButton = true,
            )
            Spacer(modifier = Modifier.weight(1F))
            if (content.docUrl != null) {
                ActionButton(
                    text = stringResource(id = R.string.diagnostic_view_docs),
                    onClick = { uriHandler.openUri(content.docUrl) },
                    textButton = true,
                )
            }
        }
        AnimatedVisibility(
            visible = showTechnicalDetails,
            enter = expandVertically(),
            exit = shrinkVertically(),
        ) {
            Text(
                modifier = Modifier.padding(top = 4.dp),
                text = ReportFormatting.truncate(
                    rawError,
                    placeholder = stringResource(id = R.string.diagnostic_no_error_details),
                ),
                fontSize = 12.sp,
            )
        }
    }
}
