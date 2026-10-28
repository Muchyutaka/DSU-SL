package vegabobo.dsusideloader.ui.screen.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Usb
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import vegabobo.dsusideloader.R
import vegabobo.dsusideloader.access.AccessCheckResult
import vegabobo.dsusideloader.access.AccessGateState
import vegabobo.dsusideloader.access.AccessMethod
import vegabobo.dsusideloader.access.AccessStatus
import vegabobo.dsusideloader.ui.components.AccessMethodCard
import vegabobo.dsusideloader.ui.components.access.AccessPrimaryAction
import vegabobo.dsusideloader.ui.components.access.accessMethodSummary
import vegabobo.dsusideloader.ui.components.access.accessMethodTitle
import vegabobo.dsusideloader.ui.screen.Destinations
import vegabobo.dsusideloader.ui.util.motionDuration
import vegabobo.dsusideloader.util.collectAsStateWithLifecycle

/** The three screens the first run goes through. */
private enum class OnboardingStep { METHOD, SETUP, READY }

/**
 * First run (and "change access method") flow: pick a method, follow the setup
 * instructions of that method until the real check passes, then confirm.
 *
 * Every advance animates: the outgoing step fades out while sliding left, the
 * incoming step fades in from the right (and the reverse when going back).
 * The move to the main screen uses the same pair of animations, applied in
 * `MainActivity` around the two navigation graphs.
 */
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun Onboarding(
    navigate: (String) -> Unit,
    onboardingViewModel: OnboardingViewModel = hiltViewModel(),
) {
    val selectedMethod by onboardingViewModel.selectedMethod.collectAsStateWithLifecycle()
    val results by onboardingViewModel.checkResults.collectAsStateWithLifecycle()
    val gateState by onboardingViewModel.gateState.collectAsStateWithLifecycle()
    val isConfirmed by onboardingViewModel.isConfirmed.collectAsStateWithLifecycle()
    val openUrl by onboardingViewModel.openUrl.collectAsStateWithLifecycle()
    val isResolving by onboardingViewModel.isResolving.collectAsStateWithLifecycle()
    val uriHandler = LocalUriHandler.current

    var step by rememberSaveable { mutableStateOf(OnboardingStep.METHOD) }

    LaunchedEffect(isConfirmed) {
        if (isConfirmed) {
            navigate(Destinations.Homepage)
        }
    }

    LaunchedEffect(openUrl) {
        openUrl?.let {
            uriHandler.openUri(it)
            onboardingViewModel.consumeOpenUrl()
        }
    }

    val method = selectedMethod
    val result = method?.let { results[it] } ?: AccessCheckResult.unknown()
    val isVerified = result.status == AccessStatus.AVAILABLE
    // Animation specs are evaluated outside the transitionSpec lambda, which is
    // not a composable scope (motionDuration reads the system animation scale).
    val enterMillis = motionDuration(320)
    val exitMillis = motionDuration(280)

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
        ) {
            StepIndicator(
                current = step.ordinal,
                total = OnboardingStep.values().size,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
            )

            AnimatedContent(
                targetState = step,
                transitionSpec = {
                    val forward = targetState.ordinal > initialState.ordinal
                    val enter = slideInHorizontally(
                        animationSpec = tween(enterMillis),
                        initialOffsetX = { fullWidth -> if (forward) fullWidth else -fullWidth / 4 },
                    ) + fadeIn(tween(enterMillis))
                    val exit = slideOutHorizontally(
                        animationSpec = tween(exitMillis),
                        targetOffsetX = { fullWidth -> if (forward) -fullWidth / 4 else fullWidth },
                    ) + fadeOut(tween(exitMillis))
                    enter with exit
                },
                modifier = Modifier.weight(1F),
            ) { currentStep ->
                when (currentStep) {
                    OnboardingStep.METHOD -> MethodStep(
                        selectedMethod = method,
                        results = results,
                        gateState = gateState,
                        canContinue = method != null && isVerified,
                        onSelect = { onboardingViewModel.select(it) },
                        onPrimaryAction = { selected, action ->
                            onboardingViewModel.onPrimaryAction(selected, action)
                        },
                        onContinue = { step = OnboardingStep.SETUP },
                    )

                    OnboardingStep.SETUP -> SetupStep(
                        method = method,
                        result = result,
                        onBack = { step = OnboardingStep.METHOD },
                        onPrimaryAction = { selected, action ->
                            onboardingViewModel.onPrimaryAction(selected, action)
                        },
                        onContinue = {
                            if (isVerified) {
                                step = OnboardingStep.READY
                            } else {
                                method?.let {
                                    onboardingViewModel.onPrimaryAction(it, AccessPrimaryAction.CHECK)
                                }
                            }
                        },
                    )

                    OnboardingStep.READY -> ReadyStep(
                        method = method,
                        onBack = { step = OnboardingStep.SETUP },
                        onOpenApp = {
                            method?.let { onboardingViewModel.confirm(it) }
                                ?: navigate(Destinations.Homepage)
                        },
                    )
                }
            }
        }
    }

    if (isResolving) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(
                modifier = Modifier.size(32.dp),
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

/** Three dots/short bars showing how far the flow has advanced. */
@Composable
private fun StepIndicator(current: Int, total: Int, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        repeat(total) { index ->
            Surface(
                shape = RoundedCornerShape(50),
                color = if (index <= current) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                },
                modifier = Modifier
                    .weight(if (index == current) 2f else 1f)
                    .height(4.dp),
            ) {}
        }
    }
}

@Composable
private fun MethodStep(
    selectedMethod: AccessMethod?,
    results: Map<AccessMethod, AccessCheckResult>,
    gateState: AccessGateState,
    canContinue: Boolean,
    onSelect: (AccessMethod) -> Unit,
    onPrimaryAction: (AccessMethod, AccessPrimaryAction) -> Unit,
    onContinue: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(id = R.string.choose_access_method),
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 26.sp,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            text = stringResource(id = R.string.choose_access_method_description),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        (gateState as? AccessGateState.NeedsOnboarding)?.previousMethod?.let { previous ->
            Text(
                text = stringResource(
                    id = R.string.access_previous_method_failed,
                    accessMethodTitle(previous),
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }

        AccessMethod.values().forEach { method ->
            AccessMethodCard(
                method = method,
                icon = method.icon(),
                title = accessMethodTitle(method),
                summary = accessMethodSummary(method),
                detail = method.detail(),
                result = results[method] ?: AccessCheckResult.unknown(),
                isSelected = selectedMethod == method,
                isEnabled = true,
                onSelect = { onSelect(method) },
                onPrimaryAction = { onPrimaryAction(method, it) },
            )
        }

        if (!canContinue) {
            Text(
                text = stringResource(id = R.string.access_required),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Button(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            enabled = canContinue,
            shape = RoundedCornerShape(50),
            onClick = onContinue,
        ) {
            Text(text = stringResource(id = R.string.access_check_continue))
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun SetupStep(
    method: AccessMethod?,
    result: AccessCheckResult,
    onBack: () -> Unit,
    onPrimaryAction: (AccessMethod, AccessPrimaryAction) -> Unit,
    onContinue: () -> Unit,
) {
    val colorScheme = MaterialTheme.colorScheme
    val isVerified = result.status == AccessStatus.AVAILABLE

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(id = R.string.onboarding_setup_title),
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 26.sp,
            color = colorScheme.onBackground,
        )
        Text(
            text = method?.let {
                stringResource(id = R.string.onboarding_setup_subtitle, accessMethodTitle(it))
            } ?: stringResource(id = R.string.access_required),
            style = MaterialTheme.typography.bodyMedium,
            color = colorScheme.onSurfaceVariant,
        )

        if (method == null) {
            OutlinedButton(onClick = onBack, shape = RoundedCornerShape(50)) {
                Text(text = stringResource(id = R.string.onboarding_back))
            }
            return@Column
        }

        method.setupSteps().forEachIndexed { index, stepText ->
            Row(modifier = Modifier.fillMaxWidth()) {
                Surface(
                    shape = CircleShape,
                    color = colorScheme.secondaryContainer,
                    modifier = Modifier.size(24.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = (index + 1).toString(),
                            style = MaterialTheme.typography.labelMedium,
                            color = colorScheme.onSecondaryContainer,
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = stepText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colorScheme.onBackground,
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        AccessMethodCard(
            method = method,
            icon = method.icon(),
            title = accessMethodTitle(method),
            summary = accessMethodSummary(method),
            detail = method.detail(),
            result = result,
            isSelected = true,
            isEnabled = true,
            onSelect = {},
            onPrimaryAction = { onPrimaryAction(method, it) },
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedButton(
                onClick = onBack,
                shape = RoundedCornerShape(50),
                modifier = Modifier.weight(1f),
            ) {
                Text(text = stringResource(id = R.string.onboarding_back))
            }
            Button(
                onClick = onContinue,
                shape = RoundedCornerShape(50),
                modifier = Modifier
                    .weight(1.4f)
                    .height(48.dp),
            ) {
                Text(
                    text = if (isVerified) {
                        stringResource(id = R.string.access_check_continue)
                    } else {
                        stringResource(id = R.string.access_check_button)
                    },
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun ReadyStep(
    method: AccessMethod?,
    onBack: () -> Unit,
    onOpenApp: () -> Unit,
) {
    val colorScheme = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Surface(
            shape = CircleShape,
            color = colorScheme.primaryContainer,
            modifier = Modifier.size(96.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Outlined.Check,
                    contentDescription = null,
                    tint = colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(48.dp),
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = stringResource(id = R.string.onboarding_ready_title),
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 24.sp,
            textAlign = TextAlign.Center,
            color = colorScheme.onBackground,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(
                id = R.string.onboarding_ready_subtitle,
                method?.let { accessMethodTitle(it) } ?: "",
            ),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(28.dp))

        Button(
            onClick = onOpenApp,
            shape = RoundedCornerShape(50),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
        ) {
            Text(
                text = stringResource(id = R.string.onboarding_ready_open),
                fontWeight = FontWeight.SemiBold,
            )
        }

        TextButton(onClick = onBack) {
            Text(text = stringResource(id = R.string.onboarding_back))
        }
    }
}

private fun AccessMethod.icon(): ImageVector = when (this) {
    AccessMethod.ADB -> Icons.Outlined.Usb
    AccessMethod.SHIZUKU -> Icons.Outlined.Shield
    AccessMethod.ROOT -> Icons.Outlined.Build
}

@Composable
private fun AccessMethod.detail(): String = when (this) {
    AccessMethod.ADB -> stringResource(id = R.string.access_method_adb_detail)
    AccessMethod.SHIZUKU -> stringResource(id = R.string.access_method_shizuku_detail)
    AccessMethod.ROOT -> stringResource(id = R.string.access_method_root_detail)
}

/** Numbered instructions of the setup step, one screen per method. */
@Composable
private fun AccessMethod.setupSteps(): List<String> = when (this) {
    AccessMethod.ADB -> listOf(
        stringResource(id = R.string.onboarding_setup_adb_step_1),
        stringResource(id = R.string.onboarding_setup_adb_step_2),
        stringResource(id = R.string.onboarding_setup_adb_step_3),
    )

    AccessMethod.SHIZUKU -> listOf(
        stringResource(id = R.string.onboarding_setup_shizuku_step_1),
        stringResource(id = R.string.onboarding_setup_shizuku_step_2),
        stringResource(id = R.string.onboarding_setup_shizuku_step_3),
    )

    AccessMethod.ROOT -> listOf(
        stringResource(id = R.string.onboarding_setup_root_step_1),
        stringResource(id = R.string.onboarding_setup_root_step_2),
    )
}
