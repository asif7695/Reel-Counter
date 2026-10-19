package com.reelcounter.feature.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.PreviewFontScale
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.reelcounter.core.preview.PreviewTheme

@PreviewLightDark
@Composable
private fun SettingsScreenPreview() = PreviewTheme {
    SettingsScreen(onHowWeCountClick = {})
}

@PreviewLightDark
@PreviewFontScale
@Composable
private fun HowWeCountScreenPreview() = PreviewTheme {
    HowWeCountScreen(onBackClick = {})
}
