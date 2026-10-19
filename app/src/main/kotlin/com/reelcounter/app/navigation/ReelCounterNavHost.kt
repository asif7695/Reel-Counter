package com.reelcounter.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import com.reelcounter.app.ui.ReelCounterAppState
import com.reelcounter.feature.friends.navigation.friendsScreen
import com.reelcounter.feature.insights.navigation.insightsScreen
import com.reelcounter.feature.settings.navigation.navigateToHowWeCount
import com.reelcounter.feature.settings.navigation.settingsGraph
import com.reelcounter.feature.today.navigation.TodayRoute
import com.reelcounter.feature.today.navigation.todayScreen

/**
 * The app's navigation graph. Features expose their destinations as NavGraphBuilder extensions and
 * never reference each other; any navigation between features is wired here.
 */
@Composable
fun ReelCounterNavHost(
    appState: ReelCounterAppState,
    modifier: Modifier = Modifier,
) {
    val navController = appState.navController
    NavHost(
        navController = navController,
        startDestination = TodayRoute,
        modifier = modifier,
    ) {
        todayScreen()
        insightsScreen()
        friendsScreen()
        settingsGraph(
            onHowWeCountClick = { navController.navigateToHowWeCount() },
            onBackClick = { navController.popBackStack() },
        )
    }
}
