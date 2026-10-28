package vegabobo.dsusideloader.ui.cards.installation.content

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import vegabobo.dsusideloader.R
import vegabobo.dsusideloader.diagnostics.ChecksumUtils
import vegabobo.dsusideloader.ui.components.buttons.ActionButton
import vegabobo.dsusideloader.ui.screen.home.ChecksumStatus
import vegabobo.dsusideloader.ui.screen.home.InstallationCardState

/**
 * Shows the SHA-256 and security patch level information of the selected image,
 * plus the warnings and the action to set an expected checksum.
 */
@Composable
fun VerificationInfo(
    uiState: InstallationCardState,
    modifier: Modifier = Modifier,
    onClickSetExpectedChecksum: () -> Unit = {},
) {
    val hasChecksum = uiState.checksum.status != ChecksumStatus.NONE
    if (!hasChecksum && !uiState.patchLevelChecked) {
        return
    }

    val smallTextSize = 13.sp

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 10.dp),
    ) {
        if (hasChecksum) {
            when (uiState.checksum.status) {
                ChecksumStatus.COMPUTING -> {
                    Text(
                        text = stringResource(
                            R.string.checksum_computing,
                            (uiState.checksum.progress * 100).toInt(),
                        ),
                        fontSize = smallTextSize,
                    )
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp),
                        progress = uiState.checksum.progress,
                    )
                }

                ChecksumStatus.READY -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "${stringResource(R.string.checksum_sha256)}: ",
                            fontSize = smallTextSize,
                        )
                        Text(
                            text = ChecksumUtils.abbreviate(uiState.checksum.value),
                            fontSize = smallTextSize,
                            color = MaterialTheme.colorScheme.secondary,
                        )
                    }
                    if (uiState.expectedChecksum.isNotEmpty()) {
                        Text(
                            text = "${stringResource(R.string.checksum_expected)}: " +
                                ChecksumUtils.abbreviate(uiState.expectedChecksum),
                            fontSize = smallTextSize,
                            color = if (uiState.checksumMismatch) {
                                MaterialTheme.colorScheme.error
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                        )
                    }
                    if (uiState.checksumMismatch) {
                        Text(
                            text = stringResource(R.string.checksum_mismatch_warning),
                            fontSize = smallTextSize,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                }

                ChecksumStatus.SKIPPED -> Text(
                    text = stringResource(R.string.checksum_skipped_warning),
                    fontSize = smallTextSize,
                    color = MaterialTheme.colorScheme.error,
                )

                ChecksumStatus.FAILED -> Text(
                    text = stringResource(R.string.checksum_unavailable),
                    fontSize = smallTextSize,
                    color = MaterialTheme.colorScheme.error,
                )

                ChecksumStatus.NONE -> {}
            }

            if (uiState.checksum.status == ChecksumStatus.READY ||
                uiState.checksum.status == ChecksumStatus.FAILED
            ) {
                ActionButton(
                    text = stringResource(R.string.checksum_set_expected),
                    onClick = onClickSetExpectedChecksum,
                    textButton = true,
                )
            }
        }

        if (uiState.patchLevelChecked) {
            Spacer(modifier = Modifier.padding(top = 4.dp))
            Text(
                text = stringResource(R.string.patch_level_title),
                fontSize = smallTextSize,
                color = MaterialTheme.colorScheme.secondary,
            )
            if (uiState.devicePatchLevel.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.patch_level_device, uiState.devicePatchLevel),
                    fontSize = smallTextSize,
                )
            }
            Text(
                text =
                if (uiState.gsiPatchLevel.isEmpty()) {
                    stringResource(R.string.patch_level_image_unknown)
                } else {
                    stringResource(R.string.patch_level_image, uiState.gsiPatchLevel)
                },
                fontSize = smallTextSize,
            )
            if (uiState.patchLevelWarning) {
                Text(
                    text = stringResource(R.string.patch_level_warning),
                    fontSize = smallTextSize,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            if (uiState.gsiPatchLevel.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.patch_level_estimate_note),
                    fontSize = smallTextSize,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }

        if (uiState.isInstallBlocked() && uiState.checksumMismatch) {
            Text(
                text = stringResource(R.string.checksum_mismatch_blocked),
                fontSize = smallTextSize,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
    }
}
