package com.reelcounter.feature.today

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import com.reelcounter.core.preview.PreviewTheme
import com.reelcounter.core.preview.SampleActivity
import com.reelcounter.core.ui.state.UiState

@PreviewLightDark
@PreviewScreenSizes
@Composable
private fun TodayScreenWithActivityPreview() = PreviewTheme {
    TodayScreen(uiState = UiState.Success(SampleActivity.today))
}

@PreviewLightDark
@Composable
private fun TodayScreenLoadingPreview() = PreviewTheme {
    TodayScreen(uiState = UiState.Loading)
}

@PreviewLightDark
@Composable
private fun TodayScreenEmptyPreview() = PreviewTheme {
    TodayScreen(uiState = UiState.Empty())
}
