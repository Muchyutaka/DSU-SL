package vegabobo.dsusideloader.ui.screen.main

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
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import vegabobo.dsusideloader.R
import vegabobo.dsusideloader.access.AccessCheckResult
import vegabobo.dsusideloader.access.AccessMethod
import vegabobo.dsusideloader.access.AccessStatus
import vegabobo.dsusideloader.ui.components.access.accessMethodTitle
import vegabobo.dsusideloader.ui.components.access.accessReasonText
import vegabobo.dsusideloader.ui.components.access.accessStatusLabel

/**
 * Access status card: the confirmed method, its live state (verified check mark
 * or a warning icon) and a refresh action.
 *
 * The tint is the accent color blended over the surface, so it is teal with the
 * static palette and follows the wallpaper under Material You.
 */
@Composable
fun MainStatusCard(
    method: AccessMethod?,
    result: AccessCheckResult,
    modifier: Modifier = Modifier,
    onRefresh: () -> Unit,
) {
    val colorScheme = MaterialTheme.colorScheme
    val isVerified = result.status == AccessStatus.AVAILABLE
    val isChecking = result.status == AccessStatus.CHECKING

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = colorScheme.primary.copy(alpha = 0.16f).compositeOver(colorScheme.surfaceVariant),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = if (isVerified) {
                        Icons.Outlined.CheckCircle
                    } else {
                        Icons.Outlined.WarningAmber
                    },
                    contentDescription = null,
                    tint = if (isVerified) colorScheme.primary else colorScheme.error,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = method?.let { accessMethodTitle(it) }
                        ?: stringResource(id = R.string.access_status_unknown),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Start,
                    color = colorScheme.onSurface,
                    modifier = Modifier.weight(1F),
                )
                if (isChecking) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = colorScheme.primary,
                    )
                } else {
                    IconButton(onClick = onRefresh) {
                        Icon(
                            imageVector = Icons.Outlined.Refresh,
                            contentDescription = stringResource(id = R.string.access_recheck),
                            tint = colorScheme.onSurface,
                        )
                    }
                }
            }

            Text(
                text = if (isVerified) {
                    stringResource(id = R.string.main_status_verified)
                } else {
                    accessStatusLabel(result.status)
                },
                style = MaterialTheme.typography.bodySmall,
                color = colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Start,
            )

            Text(
                text = if (isVerified) {
                    accessReasonText(result.reason)
                } else {
                    stringResource(id = R.string.main_status_how_to_fix)
                },
                style = MaterialTheme.typography.bodySmall,
                color = if (isVerified) colorScheme.onSurfaceVariant else colorScheme.error,
                textAlign = TextAlign.Start,
            )
        }
    }
}
