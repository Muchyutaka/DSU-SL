package vegabobo.dsusideloader.ui.screen.main

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import vegabobo.dsusideloader.R
import vegabobo.dsusideloader.access.AccessCheckResult
import vegabobo.dsusideloader.access.AccessMethod
import vegabobo.dsusideloader.access.AccessStatus
import vegabobo.dsusideloader.ui.components.access.accessMethodTitle
import vegabobo.dsusideloader.ui.components.access.accessReasonText
import vegabobo.dsusideloader.ui.components.access.accessStatusLabel
import vegabobo.dsusideloader.ui.util.motionDuration
import vegabobo.dsusideloader.ui.util.rememberMotionEnabled

/**
 * Prominent status card of the main screen: the confirmed access method, its
 * live state (verified check mark or a warning cross), and a refresh action.
 *
 * The tint is the accent color (`colorScheme.primary`) blended over the
 * surface, so it is teal with the static palette and follows the wallpaper
 * under Material You.
 */
@Composable
fun MainStatusCard(
    method: AccessMethod?,
    result: AccessCheckResult,
    modifier: Modifier = Modifier,
    onRefresh: () -> Unit,
    onChangeMethod: () -> Unit,
) {
    val colorScheme = MaterialTheme.colorScheme
    val isVerified = result.status == AccessStatus.AVAILABLE
    val motionEnabled = rememberMotionEnabled()

    val pulse: Float = if (motionEnabled && isVerified) {
        val transition = rememberInfiniteTransition()
        val scale by transition.animateFloat(
            initialValue = 0.9f,
            targetValue = 1.15f,
            animationSpec = infiniteRepeatable(
                animation = tween(1400),
                repeatMode = RepeatMode.Reverse,
            ),
        )
        scale
    } else {
        1f
    }

    // A translucent accent over the surface: teal with the static palette, a
    // wallpaper tint under Material You, always subtle on the black background.
    val containerColor = colorScheme.primary
        .copy(alpha = 0.16f)
        .compositeOver(colorScheme.surface)

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = containerColor,
            contentColor = colorScheme.onSurface,
        ),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .scale(pulse)
                        .background(
                            if (isVerified) colorScheme.primary else colorScheme.error,
                            CircleShape,
                        ),
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = stringResource(id = R.string.main_status_label),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium,
                )
                Spacer(modifier = Modifier.weight(1F))
                IconButton(onClick = onRefresh) {
                    Icon(
                        imageVector = Icons.Outlined.Refresh,
                        contentDescription = stringResource(id = R.string.access_recheck),
                    )
                }
            }

            Crossfade(
                targetState = result.status,
                animationSpec = tween(motionDuration(200)),
            ) { _ ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isVerified) {
                            Icons.Outlined.CheckCircle
                        } else {
                            Icons.Outlined.WarningAmber
                        },
                        contentDescription = null,
                        tint = if (isVerified) colorScheme.primary else colorScheme.error,
                        modifier = Modifier.size(30.dp),
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1F)) {
                        Text(
                            text = method?.let { accessMethodTitle(it) }
                                ?: stringResource(id = R.string.access_status_unknown),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = if (isVerified) {
                                stringResource(id = R.string.main_status_verified)
                            } else {
                                accessStatusLabel(result.status)
                            },
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.padding(top = 6.dp))

            Text(
                text = accessibleReason(result),
                style = MaterialTheme.typography.bodySmall,
            )

            if (!isVerified) {
                Spacer(modifier = Modifier.padding(top = 8.dp))
                Text(
                    text = stringResource(id = R.string.main_status_how_to_fix),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = colorScheme.error,
                    modifier = Modifier.padding(bottom = 2.dp),
                )
                TextButton(
                    onClick = onChangeMethod,
                    contentPadding = PaddingValues(0.dp),
                ) {
                    Text(text = stringResource(id = R.string.access_change_method))
                }
            }
        }
    }
}

@Composable
private fun accessibleReason(result: AccessCheckResult): String = accessReasonText(result.reason)
