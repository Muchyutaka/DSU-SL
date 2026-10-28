package vegabobo.dsusideloader.ui.screen.main

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import vegabobo.dsusideloader.R
import vegabobo.dsusideloader.ui.components.FileSelectionBox
import vegabobo.dsusideloader.ui.screen.home.ImageSizeCardState
import vegabobo.dsusideloader.ui.screen.home.UserDataCardState
import vegabobo.dsusideloader.ui.util.motionDuration

/**
 * "Additional Settings" card: a row per tweak with an icon, the current value
 * and a switch. Enabling a row reveals the numeric field, which is the same
 * logic the previous separate cards used.
 */
@Composable
fun AdditionalSettingsCard(
    userdataState: UserDataCardState,
    imageSizeState: ImageSizeCardState,
    isInstalling: Boolean,
    modifier: Modifier = Modifier,
    onToggleUserdata: () -> Unit,
    onUserdataChange: (String) -> Unit,
    onToggleImageSize: () -> Unit,
    onImageSizeChange: (String) -> Unit,
) {
    val colorScheme = MaterialTheme.colorScheme

    Card(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(tween(motionDuration(240))),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = colorScheme.surfaceVariant,
            contentColor = colorScheme.onSurface,
        ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.Tune,
                    contentDescription = null,
                    tint = colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(id = R.string.additional_settings_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Start,
                    modifier = Modifier.weight(1F),
                )
            }

            // Userdata size
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Storage,
                    contentDescription = null,
                    tint = colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1F)) {
                    Text(
                        text = stringResource(id = R.string.userdata_size),
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Start,
                    )
                    Text(
                        text = stringResource(id = R.string.userdata_size_info),
                        style = MaterialTheme.typography.bodySmall,
                        color = colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Start,
                    )
                }
                Switch(
                    checked = userdataState.isSelected,
                    onCheckedChange = { onToggleUserdata() },
                    enabled = !isInstalling,
                )
            }

            AnimatedVisibility(
                visible = userdataState.isSelected,
                enter = expandVertically(),
                exit = shrinkVertically(),
            ) {
                Column {
                    FileSelectionBox(
                        modifier = Modifier.padding(bottom = 4.dp),
                        isEnabled = !isInstalling,
                        isError = userdataState.isError,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        textFieldValue = userdataState.text,
                        textFieldTitle = stringResource(id = R.string.userdata_size_info),
                        onValueChange = onUserdataChange,
                    )
                    if (userdataState.isError) {
                        Text(
                            modifier = Modifier.padding(start = 1.dp),
                            text = stringResource(
                                id = R.string.allowed_userdata_allocation,
                                userdataState.maximumAllowed,
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = colorScheme.error,
                            textAlign = TextAlign.Start,
                        )
                    }
                }
            }

            // Image size
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Storage,
                    contentDescription = null,
                    tint = colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1F)) {
                    Text(
                        text = stringResource(id = R.string.image_size),
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Start,
                    )
                    Text(
                        text = stringResource(id = R.string.image_size_info),
                        style = MaterialTheme.typography.bodySmall,
                        color = colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Start,
                    )
                }
                Switch(
                    checked = imageSizeState.isSelected,
                    onCheckedChange = { onToggleImageSize() },
                    enabled = !isInstalling,
                )
            }

            AnimatedVisibility(
                visible = imageSizeState.isSelected,
                enter = expandVertically(),
                exit = shrinkVertically(),
            ) {
                Column {
                    FileSelectionBox(
                        modifier = Modifier.padding(bottom = 4.dp),
                        isEnabled = !isInstalling,
                        isError = false,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        textFieldValue = imageSizeState.text,
                        textFieldTitle = stringResource(id = R.string.image_size_info),
                        onValueChange = onImageSizeChange,
                    )
                    if (imageSizeState.text.isEmpty()) {
                        Text(
                            modifier = Modifier.padding(start = 1.dp),
                            text = stringResource(id = R.string.not_recommended_option),
                            style = MaterialTheme.typography.bodySmall,
                            color = colorScheme.error,
                            textAlign = TextAlign.Start,
                        )
                    }
                }
            }
        }
    }
}
