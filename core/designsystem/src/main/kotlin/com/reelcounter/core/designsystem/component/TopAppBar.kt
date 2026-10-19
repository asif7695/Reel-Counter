package com.reelcounter.core.designsystem.component

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Screen title bar. Pass [navigationIcon] (with its content description) for screens below the
 * top level; top-level screens have no back arrow.
 *
 * Scroll behavior is left out on purpose: TopAppBarScrollBehavior is still experimental, and
 * exposing it here would force every caller to opt in.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RcTopAppBar(
    title: String,
    modifier: Modifier = Modifier,
    navigationIcon: ImageVector? = null,
    navigationIconContentDescription: String? = null,
    onNavigationClick: () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
) {
    TopAppBar(
        title = { Text(text = title) },
        modifier = modifier,
        navigationIcon = {
            if (navigationIcon != null) {
                IconButton(onClick = onNavigationClick) {
                    Icon(imageVector = navigationIcon, contentDescription = navigationIconContentDescription)
                }
            }
        },
        actions = actions,
    )
}
