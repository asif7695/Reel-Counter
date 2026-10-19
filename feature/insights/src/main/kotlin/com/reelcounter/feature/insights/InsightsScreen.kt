package com.reelcounter.feature.insights

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.reelcounter.core.designsystem.component.EmptyState
import com.reelcounter.core.designsystem.component.RcTopAppBar
import com.reelcounter.core.designsystem.icon.RcIcons

// Weekly and monthly dashboards arrive in Phase 7, with a ViewModel once there is data to load.
@Composable
internal fun InsightsScreen(modifier: Modifier = Modifier) {
    Scaffold(
        modifier = modifier,
        topBar = { RcTopAppBar(title = stringResource(R.string.feature_insights_title)) },
    ) { innerPadding ->
        EmptyState(
            title = stringResource(R.string.feature_insights_empty_title),
            message = stringResource(R.string.feature_insights_empty_message),
            icon = RcIcons.Insights,
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
        )
    }
}
