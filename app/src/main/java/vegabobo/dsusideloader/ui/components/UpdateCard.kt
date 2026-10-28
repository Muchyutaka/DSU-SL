package vegabobo.dsusideloader.ui.components

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.SystemUpdate
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import vegabobo.dsusideloader.R
import vegabobo.dsusideloader.update.UpdateError
import vegabobo.dsusideloader.update.UpdateState
import vegabobo.dsusideloader.ui.util.motionDuration

/**
 * Update banner/card used on the home screen and in Settings > About.
 *
 * States: idle, checking (spinner), up to date, update available, downloading
 * (determinate progress), ready to install and failed (error tint + reason).
 */
@Composable
fun UpdateCard(
    state: UpdateState,
    modifier: Modifier = Modifier,
    currentVersion: String,
    compact: Boolean = false,
    onCheck: () -> Unit,
    onDownload: () -> Unit,
    onInstall: () -> Unit,
    onOpenReleasePage: () -> Unit,
    onOpenInstallSettings: () -> Unit,
    onDismiss: (() -> Unit)? = null,
    onAcknowledgeConflict: () -> Unit = onCheck,
) {
    val colorScheme = MaterialTheme.colorScheme

    // Android refuses an APK signed with another key (INSTALL_FAILED_UPDATE_
    // INCOMPATIBLE). Explain it instead of leaving a cryptic card behind.
    if (state is UpdateState.Failed && state.error == UpdateError.SIGNATURE_MISMATCH) {
        UpdateConflictDialog(onDismiss = onAcknowledgeConflict)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(tween(motionDuration(220))),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = when (state) {
                is UpdateState.Failed -> colorScheme.errorContainer
                is UpdateState.Available, is UpdateState.ReadyToInstall -> colorScheme.tertiaryContainer
                else -> colorScheme.surfaceVariant
            },
            contentColor = when (state) {
                is UpdateState.Failed -> colorScheme.onErrorContainer
                is UpdateState.Available, is UpdateState.ReadyToInstall -> colorScheme.onTertiaryContainer
                else -> colorScheme.onSurfaceVariant
            },
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = when (state) {
                        is UpdateState.Failed -> Icons.Outlined.ErrorOutline
                        is UpdateState.UpToDate -> Icons.Outlined.CheckCircle
                        else -> Icons.Outlined.SystemUpdate
                    },
                    contentDescription = null,
                    modifier = Modifier.size(22.dp),
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1F)) {
                    Text(
                        text = stringResource(id = R.string.update_title),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Crossfade(
                        targetState = state::class,
                        animationSpec = tween(motionDuration(180)),
                    ) { _ ->
                        Text(
                            text = statusText(state, currentVersion),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
                if (state is UpdateState.Checking || state is UpdateState.Downloading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = colorScheme.primary,
                    )
                }
            }

            if (state is UpdateState.Downloading) {
                Spacer(modifier = Modifier.padding(top = 8.dp))
                LinearProgressIndicator(
                    progress = state.progress,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.padding(top = 4.dp))
                Text(
                    text = stringResource(
                        id = R.string.update_progress,
                        (state.progress * 100).toInt(),
                        state.bytesWritten / (1024 * 1024),
                        state.totalBytes / (1024 * 1024),
                    ),
                    style = MaterialTheme.typography.labelSmall,
                )
            }

            if (!compact && state is UpdateState.Available && state.release.notes.isNotBlank()) {
                Spacer(modifier = Modifier.padding(top = 8.dp))
                Text(
                    text = state.release.notes.lineSequence()
                        .filter { it.isNotBlank() }
                        .take(4)
                        .joinToString("\n"),
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 6,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Spacer(modifier = Modifier.padding(top = 6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End,
            ) {
                if (onDismiss != null && (state is UpdateState.Available || state is UpdateState.Failed)) {
                    TextButton(onClick = onDismiss) {
                        Text(text = stringResource(id = R.string.update_later))
                    }
                }
                Spacer(modifier = Modifier.weight(1F))

                when (state) {
                    is UpdateState.Available -> {
                        TextButton(onClick = onOpenReleasePage) {
                            Text(text = stringResource(id = R.string.update_release_notes))
                        }
                        TextButton(onClick = onDownload) {
                            Text(text = stringResource(id = R.string.update_download))
                        }
                    }

                    is UpdateState.ReadyToInstall -> {
                        TextButton(onClick = onOpenReleasePage) {
                            Text(text = stringResource(id = R.string.update_release_notes))
                        }
                        TextButton(onClick = onInstall) {
                            Text(text = stringResource(id = R.string.update_install))
                        }
                    }

                    is UpdateState.Failed -> {
                        if (state.error == UpdateError.INSTALL_NOT_ALLOWED) {
                            TextButton(onClick = onOpenInstallSettings) {
                                Text(text = stringResource(id = R.string.update_open_settings))
                            }
                        }
                        TextButton(onClick = onCheck) {
                            Text(text = stringResource(id = R.string.update_retry))
                        }
                    }

                    is UpdateState.UpToDate -> Unit

                    else -> Unit
                }
            }
        }
    }
}

@Composable
private fun statusText(state: UpdateState, currentVersion: String): String = when (state) {
    UpdateState.Idle -> stringResource(id = R.string.update_idle)
    UpdateState.Checking -> stringResource(id = R.string.update_checking)
    is UpdateState.UpToDate -> stringResource(id = R.string.update_up_to_date, currentVersion)
    is UpdateState.Available ->
        stringResource(id = R.string.update_available, state.release.tag, currentVersion)

    is UpdateState.Downloading ->
        stringResource(id = R.string.update_downloading, state.release.tag)

    is UpdateState.ReadyToInstall ->
        stringResource(id = R.string.update_ready, state.release.tag)

    is UpdateState.Failed -> when (state.error) {
        UpdateError.NETWORK -> stringResource(id = R.string.update_error_network)
        UpdateError.RATE_LIMITED -> stringResource(id = R.string.update_error_rate_limited)
        UpdateError.PARSE_ERROR -> stringResource(id = R.string.update_error_parse)
        UpdateError.NO_APK -> stringResource(id = R.string.update_error_no_apk)
        UpdateError.DOWNLOAD_FAILED -> stringResource(id = R.string.update_error_download)
        UpdateError.CHECKSUM_MISMATCH -> stringResource(id = R.string.update_error_checksum)
        UpdateError.INSTALL_NOT_ALLOWED -> stringResource(id = R.string.update_error_install)
        UpdateError.SIGNATURE_MISMATCH -> stringResource(id = R.string.update_error_conflict)
    }
}
