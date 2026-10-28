package vegabobo.dsusideloader.ui.screen.main

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import vegabobo.dsusideloader.R
import vegabobo.dsusideloader.ui.screen.home.ChecksumStatus
import vegabobo.dsusideloader.ui.screen.home.InstallationCardState
import vegabobo.dsusideloader.ui.util.motionDuration

/**
 * SHA-256 verification and security patch level of the selected image.
 *
 * Verification is opt-in (the switch is off by default): as long as it is off
 * the image is not hashed and nothing is compared. Turning it on reveals the
 * field for the expected checksum; the computed hash is shown under the image
 * name in [MainScreen].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChecksumCard(
    uiState: InstallationCardState,
    expectedChecksumInput: String,
    expectedChecksumInputError: Boolean,
    onToggleVerification: (Boolean) -> Unit,
    onExpectedChecksumChange: (String) -> Unit,
    onApplyExpectedChecksum: () -> Unit,
    onClearExpectedChecksum: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colorScheme = MaterialTheme.colorScheme
    val enabled = uiState.checksumVerificationEnabled

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(tween(motionDuration(240))),
        shape = RoundedCornerShape(12.dp),
        color = colorScheme.surfaceVariant,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(id = R.string.checksum_verify_toggle),
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Start,
                    modifier = Modifier.weight(1F),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Switch(
                    checked = enabled,
                    onCheckedChange = onToggleVerification,
                )
            }

            AnimatedVisibility(
                visible = enabled,
                enter = expandVertically(tween(motionDuration(220))) +
                    fadeIn(tween(motionDuration(180))),
                exit = shrinkVertically(tween(motionDuration(180))) +
                    fadeOut(tween(motionDuration(120))),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = expectedChecksumInput,
                        onValueChange = onExpectedChecksumChange,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(text = stringResource(R.string.checksum_expected_hint)) },
                        singleLine = true,
                        isError = expectedChecksumInputError,
                        textStyle = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace,
                        ),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Ascii,
                            imeAction = ImeAction.Done,
                        ),
                        keyboardActions = KeyboardActions(onDone = { onApplyExpectedChecksum() }),
                        supportingText = {
                            when {
                                expectedChecksumInputError -> Text(
                                    text = stringResource(R.string.checksum_input_invalid),
                                    style = MaterialTheme.typography.bodySmall,
                                )

                                uiState.expectedChecksum.isNotEmpty() -> Text(
                                    text = stringResource(R.string.checksum_expected_set),
                                    style = MaterialTheme.typography.bodySmall,
                                )

                                else -> Text(
                                    text = stringResource(R.string.checksum_expected_hint_help),
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                        },
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (uiState.expectedChecksum.isNotEmpty()) {
                            TextButton(onClick = onClearExpectedChecksum) {
                                Text(
                                    text = stringResource(R.string.checksum_remove),
                                    style = MaterialTheme.typography.labelLarge,
                                )
                            }
                        }
                        TextButton(
                            onClick = onApplyExpectedChecksum,
                            enabled = expectedChecksumInput.isNotBlank(),
                        ) {
                            Text(
                                text = stringResource(R.string.checksum_verify),
                                style = MaterialTheme.typography.labelLarge,
                            )
                        }
                    }
                }
            }

            when (uiState.checksum.status) {
                ChecksumStatus.COMPUTING -> {
                    Text(
                        text = stringResource(
                            R.string.checksum_computing,
                            (uiState.checksum.progress * 100).toInt(),
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = colorScheme.onSurfaceVariant,
                    )
                    LinearProgressIndicator(
                        progress = uiState.checksum.progress,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                ChecksumStatus.READY -> {
                    if (uiState.expectedChecksum.isNotEmpty()) {
                        if (uiState.checksumMismatch) {
                            Text(
                                text = stringResource(R.string.checksum_mismatch_warning),
                                style = MaterialTheme.typography.bodySmall,
                                color = colorScheme.error,
                            )
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Outlined.CheckCircle,
                                    contentDescription = null,
                                    tint = colorScheme.primary,
                                    modifier = Modifier.size(18.dp),
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = stringResource(R.string.checksum_matches),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colorScheme.primary,
                                )
                            }
                        }
                    }
                }

                ChecksumStatus.FAILED -> Text(
                    text = stringResource(R.string.checksum_unavailable),
                    style = MaterialTheme.typography.bodySmall,
                    color = colorScheme.error,
                )

                ChecksumStatus.SKIPPED, ChecksumStatus.NONE -> {}
            }

            if (uiState.isInstallBlocked() && uiState.checksumMismatch) {
                Text(
                    text = stringResource(R.string.checksum_mismatch_blocked),
                    style = MaterialTheme.typography.bodySmall,
                    color = colorScheme.error,
                )
            }

            if (uiState.patchLevelChecked) {
                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = stringResource(R.string.patch_level_title),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = colorScheme.onSurfaceVariant,
                )
                if (uiState.devicePatchLevel.isNotEmpty()) {
                    PatchLevelRow(
                        label = stringResource(R.string.patch_level_device_label),
                        value = uiState.devicePatchLevel,
                    )
                }
                PatchLevelRow(
                    label = stringResource(R.string.patch_level_image_label),
                    value = uiState.gsiPatchLevel.ifEmpty {
                        stringResource(R.string.patch_level_unknown)
                    },
                )

                if (uiState.patchLevelWarning) {
                    PatchLevelWarningCard()
                } else if (uiState.gsiPatchLevel.isNotEmpty()) {
                    Text(
                        text = stringResource(R.string.patch_level_estimate_note),
                        style = MaterialTheme.typography.bodySmall,
                        color = colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun PatchLevelRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1F),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Proper card (not floating text) for an image older than the device patch. */
@Composable
private fun PatchLevelWarningCard() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
        ),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Icon(
                imageVector = Icons.Outlined.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = stringResource(R.string.patch_level_warning_title),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = stringResource(R.string.patch_level_warning),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}
