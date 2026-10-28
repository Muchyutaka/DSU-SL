package vegabobo.dsusideloader.ui.screen.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import vegabobo.dsusideloader.R
import vegabobo.dsusideloader.access.AccessCheckResult
import vegabobo.dsusideloader.access.AccessMethod
import vegabobo.dsusideloader.access.AccessStatus
import vegabobo.dsusideloader.ui.screen.onboarding.accessMethodTitle
import vegabobo.dsusideloader.ui.screen.onboarding.accessReasonText
import vegabobo.dsusideloader.ui.screen.onboarding.accessStatusLabel
import vegabobo.dsusideloader.ui.util.motionDuration
import vegabobo.dsusideloader.ui.util.rememberMotionEnabled

/**
 * Hero area of the home screen: which access method is in use, whether it still
 * works (live dot + label), the current installation stage and the primary
 * action.
 */
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun AccessHeroCard(
    method: AccessMethod?,
    result: AccessCheckResult,
    stage: HomeStage,
    modifier: Modifier = Modifier,
    onChangeMethod: () -> Unit,
    onRecheck: () -> Unit,
    onSelectImage: () -> Unit,
) {
    val colorScheme = MaterialTheme.colorScheme

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = colorScheme.primaryContainer,
            contentColor = colorScheme.onPrimaryContainer,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AccessStatusDot(
                    status = result.status,
                    modifier = Modifier.padding(end = 8.dp),
                )
                Column(modifier = Modifier.weight(1F)) {
                    Text(
                        text = if (method != null) {
                            stringResource(
                                id = R.string.home_access_hero_title,
                                accessMethodTitle(method),
                            )
                        } else {
                            stringResource(id = R.string.access_status_unknown)
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = accessStatusLabel(result.status),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Icon(
                    imageVector = if (result.status.isPositive()) {
                        Icons.Outlined.CheckCircle
                    } else {
                        Icons.Outlined.ErrorOutline
                    },
                    contentDescription = null,
                    modifier = Modifier.size(22.dp),
                )
            }

            Spacer(modifier = Modifier.padding(top = 6.dp))

            Text(
                text = accessReasonText(result.reason),
                style = MaterialTheme.typography.bodySmall,
                color = colorScheme.onPrimaryContainer,
                modifier = Modifier.alpha(0.8f),
            )

            Spacer(modifier = Modifier.padding(top = 10.dp))

            // Stage line: empty -> selected -> installing -> result.
            AnimatedContent(
                targetState = stage,
                transitionSpec = {
                    (fadeIn(tween(motionDuration(200))) +
                        slideInVertically(tween(motionDuration(220))) { it / 4 }) with
                        (fadeOut(tween(motionDuration(120))) +
                            slideOutVertically(tween(motionDuration(160))) { -it / 4 })
                },
            ) { animatedStage ->
                Text(
                    text = when (animatedStage) {
                        HomeStage.EMPTY -> stringResource(id = R.string.home_stage_empty)
                        HomeStage.SELECTED -> stringResource(id = R.string.home_stage_selected)
                        HomeStage.INSTALLING -> stringResource(id = R.string.home_stage_installing)
                        HomeStage.RESULT -> stringResource(id = R.string.home_stage_result)
                    },
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            Spacer(modifier = Modifier.padding(top = 10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start,
            ) {
                if (stage == HomeStage.EMPTY || stage == HomeStage.SELECTED) {
                    TextButton(onClick = onSelectImage) {
                        Icon(
                            imageVector = Icons.Outlined.FolderOpen,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (stage == HomeStage.EMPTY) {
                                stringResource(id = R.string.home_select_image)
                            } else {
                                stringResource(id = R.string.home_change_image)
                            },
                        )
                    }
                }
                Spacer(modifier = Modifier.weight(1F))
                TextButton(onClick = onRecheck) {
                    Icon(
                        imageVector = Icons.Outlined.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = stringResource(id = R.string.access_recheck))
                }
                TextButton(onClick = onChangeMethod) {
                    Text(text = stringResource(id = R.string.access_change_method))
                }
            }
        }
    }
}

/**
 * Small dot showing the live access status. It pulses only while the method is
 * verified and animations are enabled.
 */
@Composable
fun AccessStatusDot(
    status: AccessStatus,
    modifier: Modifier = Modifier,
) {
    val colorScheme = MaterialTheme.colorScheme
    val dotColor = when {
        status.isPositive() -> colorScheme.primary
        status.isActionable() -> colorScheme.tertiary
        status.isNegative() -> colorScheme.error
        else -> colorScheme.onSurfaceVariant
    }

    val motionEnabled = rememberMotionEnabled()
    val pulse: Float = if (motionEnabled && status.isPositive()) {
        val transition = rememberInfiniteTransition()
        val scale by transition.animateFloat(
            initialValue = 0.85f,
            targetValue = 1.2f,
            animationSpec = infiniteRepeatable(
                animation = tween(1200),
                repeatMode = RepeatMode.Reverse,
            ),
        )
        scale
    } else {
        1f
    }

    Box(
        modifier = modifier
            .size(12.dp)
            .scale(pulse)
            .background(dotColor, CircleShape),
    )
}
