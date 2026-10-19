package com.reelcounter.feature.settings.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import com.reelcounter.feature.settings.HowWeCountScreen
import com.reelcounter.feature.settings.SettingsScreen
import kotlinx.serialization.Serializable

/** The settings tab is a nested graph, so its sub-screens keep the tab selected and restore on return. */
@Serializable
data object SettingsGraph

@Serializable
data object SettingsRoute

@Serializable
data object HowWeCountRoute

fun NavController.navigateToSettings(navOptions: NavOptions? = null) =
    navigate(route = SettingsGraph, navOptions = navOptions)

fun NavController.navigateToHowWeCount(navOptions: NavOptions? = null) =
    navigate(route = HowWeCountRoute, navOptions = navOptions)

fun NavGraphBuilder.settingsGraph(
    onHowWeCountClick: () -> Unit,
    onBackClick: () -> Unit,
) {
    navigation<SettingsGraph>(startDestination = SettingsRoute) {
        composable<SettingsRoute> {
            SettingsScreen(onHowWeCountClick = onHowWeCountClick)
        }
        composable<HowWeCountRoute> {
            HowWeCountScreen(onBackClick = onBackClick)
        }
    }
}
