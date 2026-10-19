package com.reelcounter.feature.tracking.setup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.reelcounter.core.designsystem.component.RcButton
import com.reelcounter.core.designsystem.component.RcCard
import com.reelcounter.core.designsystem.component.RcLoadingIndicator
import com.reelcounter.core.designsystem.component.RcTextButton
import com.reelcounter.core.designsystem.component.RcTopAppBar
import com.reelcounter.core.designsystem.icon.RcIcons
import com.reelcounter.core.designsystem.theme.Spacing
import com.reelcounter.feature.tracking.R
import com.reelcounter.feature.tracking.openUsageAccessSettings

@Composable
internal fun TrackingSetupScreen(
    onFinished: () -> Unit,
    viewModel: TrackingSetupViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(uiState.step) {
        if (uiState.step == SetupStep.Done) onFinished()
    }

    TrackingSetupScreen(
        uiState = uiState,
        onAcceptDisclosure = viewModel::acceptDisclosure,
        onDecline = onFinished,
        onToggle = viewModel::toggle,
        onConfirmApps = viewModel::confirmApps,
        onOpenSettings = { context.openUsageAccessSettings() },
    )
}

@Composable
internal fun TrackingSetupScreen(
    uiState: TrackingSetupUiState,
    onAcceptDisclosure: () -> Unit,
    onDecline: () -> Unit,
    onToggle: (String) -> Unit,
    onConfirmApps: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    Scaffold(
        topBar = {
            RcTopAppBar(
                title = stringResource(R.string.feature_tracking_setup_title),
                navigationIcon = RcIcons.Back,
                navigationIconContentDescription = stringResource(R.string.feature_tracking_back),
                onNavigationClick = onDecline,
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
                .fillMaxSize(),
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 560.dp)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = Spacing.xl, vertical = Spacing.lg),
                verticalArrangement = Arrangement.spacedBy(Spacing.lg),
            ) {
                when (uiState.step) {
                    null, SetupStep.Done -> RcLoadingIndicator(modifier = Modifier.fillMaxWidth())
                    SetupStep.Disclosure -> DisclosureStep(uiState.isWorking, onAcceptDisclosure, onDecline)
                    SetupStep.ChooseApps -> ChooseAppsStep(uiState, onToggle, onConfirmApps)
                    SetupStep.GrantAccess -> GrantAccessStep(uiState.isWorking, onOpenSettings)
                }
            }
        }
    }
}

/**
 * The prominent disclosure. Shown before usage access is requested, describes exactly what is
 * collected and how it's used, and needs an affirmative tap. Changing what's collected means
 * updating this text AND bumping CURRENT_DISCLOSURE_VERSION, so everyone sees it again.
 */
@Composable
private fun ColumnScope.DisclosureStep(isWorking: Boolean, onAccept: () -> Unit, onDecline: () -> Unit) {
    StepHeading(stringResource(R.string.feature_tracking_disclosure_title))
    Text(text = stringResource(R.string.feature_tracking_disclosure_intro), style = MaterialTheme.typography.bodyLarge)
    RcCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
            listOf(
                R.string.feature_tracking_disclosure_collects,
                R.string.feature_tracking_disclosure_not_collected,
                R.string.feature_tracking_disclosure_other_apps,
                R.string.feature_tracking_disclosure_storage,
                R.string.feature_tracking_disclosure_estimates,
                R.string.feature_tracking_disclosure_control,
            ).forEach { Bullet(stringResource(it)) }
        }
    }
    RcButton(
        text = stringResource(R.string.feature_tracking_disclosure_accept),
        onClick = onAccept,
        loading = isWorking,
        modifier = Modifier.fillMaxWidth(),
    )
    RcTextButton(
        text = stringResource(R.string.feature_tracking_not_now),
        onClick = onDecline,
        enabled = !isWorking,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun ColumnScope.ChooseAppsStep(uiState: TrackingSetupUiState, onToggle: (String) -> Unit, onConfirm: () -> Unit) {
    StepHeading(stringResource(R.string.feature_tracking_choose_title))
    Text(
        text = stringResource(R.string.feature_tracking_choose_body),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    uiState.choices.forEach { choice ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .toggleable(
                    value = choice.selected,
                    enabled = !uiState.isWorking,
                    role = Role.Checkbox,
                    onValueChange = { onToggle(choice.platform.id) },
                )
                .padding(vertical = Spacing.xs),
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(checked = choice.selected, onCheckedChange = null)
            Column {
                Text(text = choice.platform.displayName, style = MaterialTheme.typography.titleMedium)
                if (!choice.isInstalled) {
                    Text(
                        text = stringResource(R.string.feature_tracking_not_installed),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
    RcButton(
        text = stringResource(R.string.feature_tracking_continue),
        onClick = onConfirm,
        enabled = uiState.canContinueFromApps,
        loading = uiState.isWorking,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun ColumnScope.GrantAccessStep(isWorking: Boolean, onOpenSettings: () -> Unit) {
    StepHeading(stringResource(R.string.feature_tracking_access_title))
    Text(text = stringResource(R.string.feature_tracking_access_body), style = MaterialTheme.typography.bodyLarge)
    RcCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            listOf(
                R.string.feature_tracking_access_step_1,
                R.string.feature_tracking_access_step_2,
                R.string.feature_tracking_access_step_3,
            ).forEachIndexed { index, step ->
                Text(text = "${index + 1}. ${stringResource(step)}", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
    RcButton(
        text = stringResource(R.string.feature_tracking_open_settings),
        onClick = onOpenSettings,
        loading = isWorking,
        modifier = Modifier.fillMaxWidth(),
    )
    Text(
        text = stringResource(R.string.feature_tracking_access_auto_continue),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun StepHeading(text: String) {
    Text(text = text, modifier = Modifier.semantics { heading() }, style = MaterialTheme.typography.headlineSmall)
}

@Composable
private fun Bullet(text: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        Icon(imageVector = RcIcons.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Text(text = text, style = MaterialTheme.typography.bodyMedium)
    }
}
