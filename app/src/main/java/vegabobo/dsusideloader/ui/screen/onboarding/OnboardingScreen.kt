package vegabobo.dsusideloader.ui.screen.onboarding

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Usb
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import vegabobo.dsusideloader.R
import vegabobo.dsusideloader.access.AccessCheckResult
import vegabobo.dsusideloader.access.AccessGateState
import vegabobo.dsusideloader.access.AccessMethod
import vegabobo.dsusideloader.access.AccessStatus
import vegabobo.dsusideloader.ui.components.AccessMethodCard
import vegabobo.dsusideloader.ui.components.TopBar
import vegabobo.dsusideloader.ui.components.access.accessMethodSummary
import vegabobo.dsusideloader.ui.components.access.accessMethodTitle
import vegabobo.dsusideloader.ui.screen.Destinations
import vegabobo.dsusideloader.ui.util.motionDuration
import vegabobo.dsusideloader.util.collectAsStateWithLifecycle

/**
 * First run (and "change access method") screen.
 *
 * The user picks a method, the pick is checked with the real APIs and
 * "Continue" stays disabled until the selected method actually works.
 */
@OptIn(ExperimentalMaterial3Api::class)
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
    val uriHandler = LocalUriHandler.current

    // First run: the gate flips to "confirmed" and the nav host is rebuilt
    // with the home screen as start destination. When the screen was opened
    // from Settings the home screen is already on the back stack, so pop it.
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

    val selectedResult = selectedMethod?.let { results[it] }
    val canContinue = selectedResult?.status == AccessStatus.AVAILABLE

    Scaffold(
        topBar = {
            TopBar(barTitle = stringResource(id = R.string.choose_access_method))
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Whole screen fade + slide in on entry.
            var isVisible by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) { isVisible = true }

            AnimatedVisibility(
                visible = isVisible,
                enter = fadeIn(tween(motionDuration(260))) +
                    slideInVertically(tween(motionDuration(260))) { it / 12 },
                exit = fadeOut(tween(0)),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = stringResource(id = R.string.choose_access_method),
                        style = MaterialTheme.typography.headlineSmall,
                    )
                    Text(
                        text = stringResource(id = R.string.choose_access_method_description),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    (gateState as? AccessGateState.NeedsOnboarding)
                        ?.previousMethod
                        ?.let { previous ->
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
                            detail = when (method) {
                                AccessMethod.ADB ->
                                    stringResource(id = R.string.access_method_adb_detail)

                                AccessMethod.SHIZUKU ->
                                    stringResource(id = R.string.access_method_shizuku_detail)

                                AccessMethod.ROOT ->
                                    stringResource(id = R.string.access_method_root_detail)
                            },
                            result = results[method]
                                ?: AccessCheckResult.unknown(),
                            isSelected = selectedMethod == method,
                            isEnabled = true,
                            onSelect = { onboardingViewModel.select(method) },
                            onPrimaryAction = {
                                onboardingViewModel.onPrimaryAction(method, it)
                            },
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (!canContinue) {
                            Text(
                                text = stringResource(id = R.string.access_required),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    Button(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        enabled = canContinue,
                        onClick = {
                            selectedMethod?.let { onboardingViewModel.confirm(it) }
                        },
                    ) {
                        Text(text = stringResource(id = R.string.access_check_continue))
                    }

                }
            }
        }
    }

    // Small overlay while the persisted method is validated on cold start.
    val isResolving by onboardingViewModel.isResolving.collectAsStateWithLifecycle()
    if (isResolving) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(
                modifier = Modifier.size(32.dp),
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

private fun AccessMethod.icon(): ImageVector = when (this) {
    AccessMethod.ADB -> Icons.Outlined.Usb
    AccessMethod.SHIZUKU -> Icons.Outlined.Shield
    AccessMethod.ROOT -> Icons.Outlined.Build
}
