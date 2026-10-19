package com.reelcounter.feature.today

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.reelcounter.core.designsystem.component.EmptyState
import com.reelcounter.core.designsystem.component.RcTopAppBar
import com.reelcounter.core.designsystem.icon.RcIcons
import com.reelcounter.core.designsystem.theme.Spacing
import com.reelcounter.core.model.DailyPlatformActivity
import com.reelcounter.core.ui.metric.PlatformActivityCard
import com.reelcounter.core.ui.state.StateContent
import com.reelcounter.core.ui.state.UiState
import com.reelcounter.core.ui.state.asString

@Composable
internal fun TodayScreen(
    modifier: Modifier = Modifier,
    viewModel: TodayViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    TodayScreen(uiState = uiState, modifier = modifier)
}

@Composable
internal fun TodayScreen(
    uiState: UiState<List<DailyPlatformActivity>>,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = { RcTopAppBar(title = stringResource(R.string.feature_today_title)) },
    ) { innerPadding ->
        StateContent(
            state = uiState,
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            empty = { empty ->
                EmptyState(
                    title = stringResource(R.string.feature_today_empty_title),
                    message = empty.message?.asString() ?: stringResource(R.string.feature_today_empty_message),
                    icon = RcIcons.Today,
                    modifier = Modifier.fillMaxSize(),
                )
            },
        ) { activities ->
            LazyColumn(
                contentPadding = PaddingValues(Spacing.lg),
                verticalArrangement = Arrangement.spacedBy(Spacing.md),
            ) {
                items(items = activities, key = { it.platform.id }) { activity ->
                    PlatformActivityCard(activity = activity, modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }
}
