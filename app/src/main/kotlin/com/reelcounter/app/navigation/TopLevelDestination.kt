package com.reelcounter.app.navigation

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.vector.ImageVector
import com.reelcounter.app.R
import com.reelcounter.core.designsystem.icon.RcIcons
import com.reelcounter.feature.friends.navigation.FriendsRoute
import com.reelcounter.feature.insights.navigation.InsightsRoute
import com.reelcounter.feature.settings.navigation.SettingsGraph
import com.reelcounter.feature.today.navigation.TodayRoute
import kotlin.reflect.KClass

/**
 * The tabs of the navigation bar or rail. [baseRoute] is what marks a tab as selected: the route
 * itself, or the nested graph for tabs that have sub-screens (Settings).
 */
enum class TopLevelDestination(
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    @param:StringRes val labelRes: Int,
    val baseRoute: KClass<*>,
) {
    TODAY(
        selectedIcon = RcIcons.Today,
        unselectedIcon = RcIcons.TodayOutlined,
        labelRes = R.string.nav_today,
        baseRoute = TodayRoute::class,
    ),
    INSIGHTS(
        selectedIcon = RcIcons.Insights,
        unselectedIcon = RcIcons.InsightsOutlined,
        labelRes = R.string.nav_insights,
        baseRoute = InsightsRoute::class,
    ),
    FRIENDS(
        selectedIcon = RcIcons.Friends,
        unselectedIcon = RcIcons.FriendsOutlined,
        labelRes = R.string.nav_friends,
        baseRoute = FriendsRoute::class,
    ),
    SETTINGS(
        selectedIcon = RcIcons.Settings,
        unselectedIcon = RcIcons.SettingsOutlined,
        labelRes = R.string.nav_settings,
        baseRoute = SettingsGraph::class,
    ),
}
