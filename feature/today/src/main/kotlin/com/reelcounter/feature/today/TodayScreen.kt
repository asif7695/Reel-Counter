package com.reelcounter.feature.today

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.reelcounter.core.designsystem.component.EmptyState
import com.reelcounter.core.designsystem.component.RcButton
import com.reelcounter.core.designsystem.component.RcCard
import com.reelcounter.core.designsystem.component.RcLoadingIndicator
import com.reelcounter.core.designsystem.component.RcStatCard
import com.reelcounter.core.designsystem.component.RcTopAppBar
import com.reelcounter.core.designsystem.icon.RcIcons
import com.reelcounter.core.designsystem.theme.Spacing
import com.reelcounter.core.domain.tracking.TrackingStatus
import com.reelcounter.core.ui.metric.PlatformActivityCard
import com.reelcounter.core.ui.metric.formatAppTime
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
internal fun TodayScreen(
    onSetUpTracking: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TodayViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Read the usage log every time the screen comes back, so figures are fresh even when
    // background work was delayed by the system (docs/phase-1/04-technical-limitations.md L2, L5).
    LifecycleResumeEffect(Unit) {
        viewModel.refresh()
        onPauseOrDispose { }
    }

    TodayScreen(uiState = uiState, onRefresh = viewModel::refresh, onSetUpTracking = onSetUpTracking, modifier = modifier)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TodayScreen(
    uiState: TodayUiState,
    onRefresh: () -> Unit,
    onSetUpTracking: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = { RcTopAppBar(title = stringResource(R.string.feature_today_title)) },
    ) { innerPadding ->
        val contentModifier = Modifier
            .padding(innerPadding)
            .fillMaxSize()
        when (uiState) {
            TodayUiState.Loading -> RcLoadingIndicator(modifier = contentModifier)
            TodayUiState.NeedsSetup -> EmptyState(
                title = stringResource(R.string.feature_today_setup_title),
                message = stringResource(R.string.feature_today_setup_message),
                icon = RcIcons.Today,
                modifier = contentModifier,
                action = { RcButton(text = stringResource(R.string.feature_today_setup_action), onClick = onSetUpTracking) },
            )
            is TodayUiState.Content -> PullToRefreshBox(
                isRefreshing = uiState.isRefreshing,
                onRefresh = onRefresh,
                modifier = contentModifier,
            ) {
                TodayContent(uiState, onSetUpTracking)
            }
        }
    }
}

@Composable
private fun TodayContent(state: TodayUiState.Content, onSetUpTracking: () -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        if (state.status == TrackingStatus.NeedsUsageAccess || state.status == TrackingStatus.Paused) {
            item(key = "status") { StatusNotice(state.status, onSetUpTracking) }
        }
        item(key = "total") {
            RcStatCard(
                label = stringResource(R.string.feature_today_total_label),
                value = state.totalAppTime?.let { formatAppTime(it) } ?: stringResource(R.string.feature_today_no_data),
                supportingText = lastUpdatedText(state.lastUpdatedMs),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        items(items = state.activities, key = { it.platform.id }) { activity ->
            PlatformActivityCard(activity = activity, modifier = Modifier.fillMaxWidth())
        }
        item(key = "note") {
            Text(
                text = stringResource(R.string.feature_today_estimate_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun StatusNotice(status: TrackingStatus, onSetUpTracking: () -> Unit) {
    RcCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(
                if (status == TrackingStatus.Paused) R.string.feature_today_paused else R.string.feature_today_access_off,
            ),
            style = MaterialTheme.typography.bodyMedium,
        )
        if (status == TrackingStatus.NeedsUsageAccess) {
            RcButton(
                text = stringResource(R.string.feature_today_turn_on),
                onClick = onSetUpTracking,
                modifier = Modifier.padding(top = Spacing.sm),
            )
        }
    }
}

@Composable
private fun lastUpdatedText(lastUpdatedMs: Long?): String {
    if (lastUpdatedMs == null) return stringResource(R.string.feature_today_not_updated)
    val locale = LocalConfiguration.current.locales[0]
    val time = remember(lastUpdatedMs, locale) {
        DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale)
            .format(Instant.ofEpochMilli(lastUpdatedMs).atZone(ZoneId.systemDefault()))
    }
    return stringResource(R.string.feature_today_updated_at, time)
}
