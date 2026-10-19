package com.reelcounter.feature.tracking.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.reelcounter.core.designsystem.component.RcButton
import com.reelcounter.core.designsystem.component.RcCard
import com.reelcounter.core.designsystem.component.RcLoadingIndicator
import com.reelcounter.core.designsystem.component.RcTextButton
import com.reelcounter.core.designsystem.component.RcTopAppBar
import com.reelcounter.core.designsystem.icon.RcIcons
import com.reelcounter.core.designsystem.theme.Spacing
import com.reelcounter.core.domain.tracking.PlatformConfiguration
import com.reelcounter.core.domain.tracking.TrackingStatus
import com.reelcounter.feature.tracking.R
import com.reelcounter.feature.tracking.openUsageAccessSettings
import kotlin.math.roundToInt

@Composable
internal fun TrackingSettingsScreen(
    onBack: () -> Unit,
    onSetUpTracking: () -> Unit,
    viewModel: TrackingSettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    TrackingSettingsScreen(
        uiState = uiState,
        actions = TrackingSettingsActions(
            onBack = onBack,
            onSetUpTracking = onSetUpTracking,
            onOpenUsageAccess = { context.openUsageAccessSettings() },
            onPausedChange = viewModel::setPaused,
            onTrackedChange = viewModel::setTracked,
            onEstimationChange = viewModel::setEstimationEnabled,
            onShareChange = viewModel::setShortFormShare,
            onSecondsChange = viewModel::setSecondsPerReel,
            onReset = viewModel::resetEstimation,
        ),
    )
}

internal class TrackingSettingsActions(
    val onBack: () -> Unit = {},
    val onSetUpTracking: () -> Unit = {},
    val onOpenUsageAccess: () -> Unit = {},
    val onPausedChange: (Boolean) -> Unit = {},
    val onTrackedChange: (String, Boolean) -> Unit = { _, _ -> },
    val onEstimationChange: (String, Boolean) -> Unit = { _, _ -> },
    val onShareChange: (String, Double) -> Unit = { _, _ -> },
    val onSecondsChange: (String, Double) -> Unit = { _, _ -> },
    val onReset: (String) -> Unit = {},
)

@Composable
internal fun TrackingSettingsScreen(uiState: TrackingSettingsUiState, actions: TrackingSettingsActions) {
    Scaffold(
        topBar = {
            RcTopAppBar(
                title = stringResource(R.string.feature_tracking_settings_title),
                navigationIcon = RcIcons.Back,
                navigationIconContentDescription = stringResource(R.string.feature_tracking_back),
                onNavigationClick = actions.onBack,
            )
        },
    ) { innerPadding ->
        if (uiState.isLoading) {
            RcLoadingIndicator(modifier = Modifier.padding(innerPadding).fillMaxWidth())
            return@Scaffold
        }
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            StatusCard(uiState.status, actions)
            Text(
                text = stringResource(R.string.feature_tracking_apps_heading),
                modifier = Modifier
                    .padding(top = Spacing.sm)
                    .semantics { heading() },
                style = MaterialTheme.typography.titleMedium,
            )
            uiState.platforms.forEach { PlatformCard(it, actions) }
            Text(
                text = stringResource(R.string.feature_tracking_assumptions_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun StatusCard(status: TrackingStatus, actions: TrackingSettingsActions) {
    RcCard(modifier = Modifier.fillMaxWidth()) {
        when (status) {
            TrackingStatus.NeedsDisclosure -> {
                Text(stringResource(R.string.feature_tracking_status_not_set_up), style = MaterialTheme.typography.bodyLarge)
                RcButton(
                    text = stringResource(R.string.feature_tracking_set_up),
                    onClick = actions.onSetUpTracking,
                    modifier = Modifier.padding(top = Spacing.sm),
                )
            }
            TrackingStatus.NeedsUsageAccess -> {
                Text(stringResource(R.string.feature_tracking_status_no_access), style = MaterialTheme.typography.bodyLarge)
                RcButton(
                    text = stringResource(R.string.feature_tracking_open_settings),
                    onClick = actions.onOpenUsageAccess,
                    modifier = Modifier.padding(top = Spacing.sm),
                )
            }
            TrackingStatus.Paused, TrackingStatus.Active -> SwitchRow(
                title = stringResource(R.string.feature_tracking_pause),
                summary = stringResource(
                    if (status == TrackingStatus.Paused) R.string.feature_tracking_paused_summary else R.string.feature_tracking_active_summary,
                ),
                checked = status == TrackingStatus.Paused,
                onCheckedChange = actions.onPausedChange,
            )
        }
    }
}

@Composable
private fun PlatformCard(config: PlatformConfiguration, actions: TrackingSettingsActions) {
    val id = config.platform.id
    RcCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            SwitchRow(
                title = config.platform.displayName,
                summary = if (config.isInstalled) null else stringResource(R.string.feature_tracking_not_installed),
                checked = config.isTracked,
                onCheckedChange = { actions.onTrackedChange(id, it) },
            )
            if (!config.isTracked) return@Column
            SwitchRow(
                title = stringResource(R.string.feature_tracking_estimate_title),
                summary = stringResource(
                    if (config.estimationEnabled) R.string.feature_tracking_estimate_on else R.string.feature_tracking_estimate_off,
                ),
                checked = config.estimationEnabled,
                onCheckedChange = { actions.onEstimationChange(id, it) },
            )
            if (!config.estimationEnabled) return@Column

            val share = config.shortFormShareOverride ?: config.defaults.shortFormShare
            val seconds = config.secondsPerReelOverride ?: config.defaults.secondsPerReel
            ParameterSlider(
                label = stringResource(R.string.feature_tracking_share_label, (share * 100).roundToInt()),
                value = share.toFloat(),
                range = 0.05f..1f,
                steps = 18,
                onValueChangeFinished = { actions.onShareChange(id, it.toDouble()) },
            )
            ParameterSlider(
                label = stringResource(R.string.feature_tracking_seconds_label, seconds.roundToInt()),
                value = seconds.toFloat(),
                range = 5f..90f,
                steps = 84,
                onValueChangeFinished = { actions.onSecondsChange(id, it.roundToInt().toDouble()) },
            )
            Text(
                text = stringResource(
                    R.string.feature_tracking_defaults,
                    (config.defaults.shortFormShare * 100).roundToInt(),
                    config.defaults.secondsPerReel.roundToInt(),
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (config.shortFormShareOverride != null || config.secondsPerReelOverride != null) {
                RcTextButton(text = stringResource(R.string.feature_tracking_reset), onClick = { actions.onReset(id) })
            }
        }
    }
}

/** Saves only when the user lets go, so dragging doesn't recompute history on every frame. */
@Composable
private fun ParameterSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    steps: Int,
    onValueChangeFinished: (Float) -> Unit,
) {
    var dragging by remember(value) { mutableFloatStateOf(value) }
    Column {
        Text(text = label, style = MaterialTheme.typography.bodyMedium)
        Slider(
            value = dragging,
            onValueChange = { dragging = it },
            onValueChangeFinished = { onValueChangeFinished(dragging) },
            valueRange = range,
            steps = steps,
        )
    }
}

@Composable
private fun SwitchRow(title: String, summary: String?, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange),
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            if (summary != null) {
                Text(text = summary, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Switch(checked = checked, onCheckedChange = null)
    }
}
