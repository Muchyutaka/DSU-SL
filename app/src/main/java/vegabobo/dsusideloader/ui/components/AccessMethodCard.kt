package vegabobo.dsusideloader.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import vegabobo.dsusideloader.R
import vegabobo.dsusideloader.access.AccessCheckResult
import vegabobo.dsusideloader.access.AccessMethod
import vegabobo.dsusideloader.access.AccessStatus
import vegabobo.dsusideloader.ui.screen.onboarding.AccessPrimaryAction
import vegabobo.dsusideloader.ui.screen.onboarding.accessPrimaryActionLabel
import vegabobo.dsusideloader.ui.screen.onboarding.accessReasonText
import vegabobo.dsusideloader.ui.screen.onboarding.accessStatusColors
import vegabobo.dsusideloader.ui.screen.onboarding.accessStatusLabel
import vegabobo.dsusideloader.ui.util.motionDuration

/**
 * Selectable card of the onboarding screen.
 *
 * Selection is animated with a scale + border + container color change, the
 * status block cross fades between states and the success checkmark scales in.
 */
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun AccessMethodCard(
    method: AccessMethod,
    icon: ImageVector,
    title: String,
    summary: String,
    detail: String,
    result: AccessCheckResult,
    isSelected: Boolean,
    isEnabled: Boolean,
    onSelect: () -> Unit,
    onPrimaryAction: (AccessPrimaryAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colorScheme = MaterialTheme.colorScheme

    val containerColor by animateColorAsState(
        targetValue = when {
            isSelected -> colorScheme.primaryContainer
            else -> colorScheme.surfaceVariant
        },
        animationSpec = tween(motionDuration(220)),
    )
    val contentColor by animateColorAsState(
        targetValue = if (isSelected) colorScheme.onPrimaryContainer else colorScheme.onSurface,
        animationSpec = tween(motionDuration(220)),
    )
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) colorScheme.primary else colorScheme.outline,
        animationSpec = tween(motionDuration(220)),
    )
    val borderWidth by animateDpAsState(
        targetValue = if (isSelected) 2.dp else 1.dp,
        animationSpec = tween(motionDuration(220)),
    )
    val cardScale by animateFloatAsState(
        targetValue = if (isSelected) 1f else 0.97f,
        animationSpec = tween(motionDuration(220)),
    )

    val action = accessPrimaryAction(method = method, result = result)
    val actionLabel = action?.let { accessPrimaryActionLabel(it, method) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .scale(cardScale)
            .clickable(enabled = isEnabled, onClick = onSelect),
        colors = CardDefaults.cardColors(containerColor = containerColor, contentColor = contentColor),
        border = BorderStroke(borderWidth, borderColor),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 4.dp else 0.dp,
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(28.dp),
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1F)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = summary,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                AccessStatusBadge(result = result)
            }

            Spacer(modifier = Modifier.padding(top = 10.dp))

            AnimatedContent(
                targetState = result,
                transitionSpec = {
                    fadeIn(tween(motionDuration(180))) with fadeOut(tween(motionDuration(120)))
                },
            ) { animatedResult ->
                Column {
                    if (animatedResult.status == AccessStatus.CHECKING) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = colorScheme.primary,
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = stringResource(R.string.access_checking),
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                    Text(
                        text = accessReasonText(animatedResult.reason),
                        style = MaterialTheme.typography.bodySmall,
                        color = when {
                            animatedResult.status == AccessStatus.AVAILABLE -> colorScheme.primary
                            animatedResult.status.isNegative() -> colorScheme.error
                            else -> colorScheme.onSurfaceVariant
                        },
                    )
                    if (animatedResult.detail != null) {
                        Text(
                            text = animatedResult.detail,
                            style = MaterialTheme.typography.labelSmall,
                            color = colorScheme.onSurfaceVariant,
                        )
                    }
                    if (isSelected) {
                        Text(
                            text = detail,
                            style = MaterialTheme.typography.bodySmall,
                            color = colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 6.dp),
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AnimatedVisibility(
                    visible = result.status == AccessStatus.AVAILABLE,
                    enter = fadeIn(tween(motionDuration(200))) +
                        scaleIn(tween(motionDuration(220)), initialScale = 0.6f),
                    exit = fadeOut(tween(motionDuration(120))),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.CheckCircle,
                            contentDescription = stringResource(R.string.access_status_available),
                            tint = colorScheme.primary,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = accessStatusLabel(AccessStatus.AVAILABLE),
                            style = MaterialTheme.typography.labelMedium,
                            color = colorScheme.primary,
                        )
                    }
                }
                Spacer(modifier = Modifier.weight(1F))
                if (actionLabel != null && action != null) {
                    TextButton(
                        onClick = { onPrimaryAction(action) },
                        enabled = isEnabled,
                    ) {
                        Text(text = actionLabel)
                    }
                }
            }
        }
    }
}

/**
 * Small status pill: icon + label, colored with the Material scheme.
 */
@Composable
fun AccessStatusBadge(
    result: AccessCheckResult,
    modifier: Modifier = Modifier,
) {
    val colors = accessStatusColors(result.status)
    val label = accessStatusLabel(result.status)
    Row(
        modifier = modifier
            .background(colors.container, RoundedCornerShape(50))
            .semantics { contentDescription = label }
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (result.status == AccessStatus.CHECKING) {
            CircularProgressIndicator(
                modifier = Modifier.size(12.dp),
                strokeWidth = 1.5.dp,
                color = colors.content,
            )
        } else {
            Icon(
                imageVector = if (result.status.isPositive()) {
                    Icons.Outlined.CheckCircle
                } else {
                    Icons.Outlined.ErrorOutline
                },
                contentDescription = null,
                tint = colors.content,
                modifier = Modifier.size(14.dp),
            )
        }
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = colors.content,
            maxLines = 1,
        )
    }
}
