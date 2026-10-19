package com.reelcounter.feature.friends

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.reelcounter.core.designsystem.component.EmptyState
import com.reelcounter.core.designsystem.component.RcTopAppBar
import com.reelcounter.core.designsystem.icon.RcIcons

// Search, requests and the leaderboard arrive in Phases 6 and 7.
@Composable
internal fun FriendsScreen(modifier: Modifier = Modifier) {
    Scaffold(
        modifier = modifier,
        topBar = { RcTopAppBar(title = stringResource(R.string.feature_friends_title)) },
    ) { innerPadding ->
        EmptyState(
            title = stringResource(R.string.feature_friends_empty_title),
            message = stringResource(R.string.feature_friends_empty_message),
            icon = RcIcons.Friends,
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
        )
    }
}
