package com.reelcounter.feature.friends.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import com.reelcounter.feature.friends.FriendsScreen
import kotlinx.serialization.Serializable

@Serializable
data object FriendsRoute

fun NavController.navigateToFriends(navOptions: NavOptions? = null) =
    navigate(route = FriendsRoute, navOptions = navOptions)

fun NavGraphBuilder.friendsScreen() {
    composable<FriendsRoute> {
        FriendsScreen()
    }
}
