package com.reelcounter.core.designsystem.icon

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Today
import androidx.compose.material.icons.rounded.Calculate
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.Inbox
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Today
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.ui.graphics.vector.ImageVector

/** Every icon the app uses, in one place, so swapping the icon set later is a single-file change. */
object RcIcons {
    // Navigation: filled when selected, outlined otherwise.
    val Today: ImageVector = Icons.Rounded.Today
    val TodayOutlined: ImageVector = Icons.Outlined.Today
    val Insights: ImageVector = Icons.Rounded.Insights
    val InsightsOutlined: ImageVector = Icons.Outlined.Insights
    val Friends: ImageVector = Icons.Rounded.Group
    val FriendsOutlined: ImageVector = Icons.Outlined.Group
    val Settings: ImageVector = Icons.Rounded.Settings
    val SettingsOutlined: ImageVector = Icons.Outlined.Settings

    // Metric categories
    val Verified: ImageVector = Icons.Rounded.Verified
    val Estimated: ImageVector = Icons.Rounded.Calculate
    val DurationOnly: ImageVector = Icons.Rounded.Schedule
    val Unavailable: ImageVector = Icons.Rounded.VisibilityOff
    val UserAdjusted: ImageVector = Icons.Rounded.Edit

    // States and actions
    val Empty: ImageVector = Icons.Rounded.Inbox
    val Error: ImageVector = Icons.Rounded.ErrorOutline
    val Offline: ImageVector = Icons.Rounded.CloudOff
    val Retry: ImageVector = Icons.Rounded.Refresh
    val Info: ImageVector = Icons.Rounded.Info
    val Back: ImageVector = Icons.AutoMirrored.Rounded.ArrowBack
    val ChevronForward: ImageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight
    val Success: ImageVector = Icons.Rounded.CheckCircle

    // Account
    val ShowPassword: ImageVector = Icons.Rounded.Visibility
    val HidePassword: ImageVector = Icons.Rounded.VisibilityOff
    val ChangePhoto: ImageVector = Icons.Rounded.PhotoCamera
    val SignOut: ImageVector = Icons.AutoMirrored.Rounded.Logout

    // Social and privacy
    val Search: ImageVector = Icons.Rounded.Search
    val AddFriend: ImageVector = Icons.Rounded.PersonAdd
    val More: ImageVector = Icons.Rounded.MoreVert
    val Privacy: ImageVector = Icons.Rounded.Lock
}
