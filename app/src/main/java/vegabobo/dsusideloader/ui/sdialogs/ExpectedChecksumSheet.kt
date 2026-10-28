package vegabobo.dsusideloader.ui.sdialogs

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import vegabobo.dsusideloader.R
import vegabobo.dsusideloader.ui.components.CustomBottomSheet
import vegabobo.dsusideloader.ui.components.FileSelectionBox
import vegabobo.dsusideloader.ui.components.buttons.PrimaryButton
import vegabobo.dsusideloader.ui.components.buttons.SecondaryButton

/**
 * Lets the user paste an expected SHA-256 checksum for the selected image.
 * Verification is optional and only warns, unless strict mode is enabled.
 */
@Composable
fun ExpectedChecksumSheet(
    input: String,
    isError: Boolean,
    hasExpectedChecksum: Boolean,
    onValueChange: (String) -> Unit,
    onClickConfirm: () -> Unit,
    onClickRemove: () -> Unit,
    onDismiss: () -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()

    CustomBottomSheet(
        title = stringResource(id = R.string.checksum_input_title),
        icon = Icons.Outlined.Security,
        onDismiss = onDismiss,
    ) { hideSheet ->
        Text(
            modifier = Modifier.fillMaxWidth(),
            text = stringResource(id = R.string.checksum_input_description),
            textAlign = TextAlign.Justify,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(modifier = Modifier.height(10.dp))
        FileSelectionBox(
            isEnabled = true,
            isError = isError,
            textFieldValue = input,
            textFieldTitle = stringResource(id = R.string.checksum_input_label),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Ascii,
                autoCorrect = false,
            ),
            onValueChange = onValueChange,
        )
        if (isError) {
            Text(
                modifier = Modifier.padding(top = 4.dp),
                text = stringResource(id = R.string.checksum_input_invalid),
                color = MaterialTheme.colorScheme.error,
                fontSize = 14.sp,
            )
        }
        Row(modifier = Modifier.padding(top = 16.dp)) {
            if (hasExpectedChecksum) {
                SecondaryButton(
                    text = stringResource(id = R.string.checksum_remove),
                    onClick = {
                        coroutineScope.launch {
                            hideSheet()
                            onClickRemove()
                        }
                    },
                )
            }
            Spacer(modifier = Modifier.weight(1F))
            PrimaryButton(
                text = stringResource(id = R.string.checksum_verify),
                onClick = {
                    coroutineScope.launch {
                        hideSheet()
                        onClickConfirm()
                    }
                },
            )
        }
    }
}
