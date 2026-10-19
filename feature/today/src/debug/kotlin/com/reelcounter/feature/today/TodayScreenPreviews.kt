package com.reelcounter.feature.today

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import com.reelcounter.core.domain.tracking.TrackingStatus
import com.reelcounter.core.preview.PreviewTheme
import com.reelcounter.core.preview.SampleActivity
import java.time.LocalDate
import kotlin.time.Duration.Companion.minutes

private fun content(status: TrackingStatus = TrackingStatus.Active) = TodayUiState.Content(
    date = LocalDate.of(2026, 10, 20),
    activities = SampleActivity.today,
    totalAppTime = 153.minutes,
    lastUpdatedMs = 1_792_000_000_000,
    status = status,
    isRefreshing = false,
)

@PreviewLightDark
@PreviewScreenSizes
@Composable
private fun TodayScreenWithActivityPreview() = PreviewTheme {
    TodayScreen(uiState = content(), onRefresh = {}, onSetUpTracking = {})
}

@PreviewLightDark
@Composable
private fun TodayScreenPausedPreview() = PreviewTheme {
    TodayScreen(uiState = content(TrackingStatus.Paused), onRefresh = {}, onSetUpTracking = {})
}

@PreviewLightDark
@Composable
private fun TodayScreenNeedsSetupPreview() = PreviewTheme {
    TodayScreen(uiState = TodayUiState.NeedsSetup, onRefresh = {}, onSetUpTracking = {})
}
