package com.reelcounter.feature.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.reelcounter.core.designsystem.component.RcAlertDialog
import com.reelcounter.core.designsystem.component.RcTopAppBar
import com.reelcounter.core.designsystem.icon.RcIcons
import com.reelcounter.core.designsystem.theme.Spacing
import com.reelcounter.core.model.UserProfile
import com.reelcounter.core.ui.profile.Avatar

@Composable
internal fun SettingsScreen(
    onEditProfileClick: () -> Unit,
    onTrackingClick: () -> Unit,
    onPrivacyClick: () -> Unit,
    onHowWeCountClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val profile by viewModel.profile.collectAsStateWithLifecycle()
    SettingsScreen(
        profile = profile,
        onEditProfileClick = onEditProfileClick,
        onTrackingClick = onTrackingClick,
        onPrivacyClick = onPrivacyClick,
        onHowWeCountClick = onHowWeCountClick,
        onSignOut = viewModel::signOut,
        modifier = modifier,
    )
}

// Data export and account deletion join this list in Phase 8.
@Composable
internal fun SettingsScreen(
    profile: UserProfile?,
    onEditProfileClick: () -> Unit,
    onTrackingClick: () -> Unit,
    onPrivacyClick: () -> Unit,
    onHowWeCountClick: () -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var confirmSignOut by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        topBar = { RcTopAppBar(title = stringResource(R.string.feature_settings_title)) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .verticalScroll(rememberScrollState()),
        ) {
            if (profile != null) {
                AccountHeader(profile = profile, onClick = onEditProfileClick)
            }

            SectionHeader(text = stringResource(R.string.feature_settings_section_tracking))
            SettingsRow(
                title = stringResource(R.string.feature_settings_tracking),
                summary = stringResource(R.string.feature_settings_tracking_summary),
                leading = { Icon(imageVector = RcIcons.Today, contentDescription = null) },
                onClick = onTrackingClick,
            )

            SectionHeader(text = stringResource(R.string.feature_settings_section_privacy))
            SettingsRow(
                title = stringResource(R.string.feature_settings_privacy),
                summary = stringResource(R.string.feature_settings_privacy_summary),
                leading = { Icon(imageVector = RcIcons.Privacy, contentDescription = null) },
                onClick = onPrivacyClick,
            )

            SectionHeader(text = stringResource(R.string.feature_settings_section_about_numbers))
            SettingsRow(
                title = stringResource(R.string.feature_settings_how_we_count),
                summary = stringResource(R.string.feature_settings_how_we_count_summary),
                leading = { Icon(imageVector = RcIcons.Info, contentDescription = null) },
                onClick = onHowWeCountClick,
            )

            SectionHeader(text = stringResource(R.string.feature_settings_section_account))
            SettingsRow(
                title = stringResource(R.string.feature_settings_sign_out),
                leading = { Icon(imageVector = RcIcons.SignOut, contentDescription = null) },
                showChevron = false,
                onClick = { confirmSignOut = true },
            )
        }
    }

    if (confirmSignOut) {
        RcAlertDialog(
            title = stringResource(R.string.feature_settings_sign_out_title),
            text = stringResource(R.string.feature_settings_sign_out_message),
            confirmText = stringResource(R.string.feature_settings_sign_out),
            dismissText = stringResource(R.string.feature_settings_cancel),
            onConfirm = {
                confirmSignOut = false
                onSignOut()
            },
            onDismiss = { confirmSignOut = false },
        )
    }
}

@Composable
private fun AccountHeader(profile: UserProfile, onClick: () -> Unit) {
    val name = profile.displayName ?: profile.username.orEmpty()
    ListItem(
        headlineContent = { Text(text = name, style = MaterialTheme.typography.titleMedium) },
        supportingContent = profile.username?.let { { Text(text = "@$it") } },
        leadingContent = { Avatar(image = profile.avatarUrl, name = name, size = 56.dp) },
        trailingContent = {
            Text(
                text = stringResource(R.string.feature_settings_edit_profile),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        },
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .padding(vertical = Spacing.sm)
            .clickable(role = Role.Button, onClick = onClick),
    )
}

@Composable
private fun SettingsRow(
    title: String,
    onClick: () -> Unit,
    leading: @Composable () -> Unit,
    summary: String? = null,
    showChevron: Boolean = true,
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = summary?.let { { Text(it) } },
        leadingContent = leading,
        trailingContent = if (showChevron) {
            { Icon(imageVector = RcIcons.ChevronForward, contentDescription = null) }
        } else {
            null
        },
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.clickable(role = Role.Button, onClick = onClick),
    )
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        modifier = Modifier
            .padding(start = Spacing.lg, end = Spacing.lg, top = Spacing.lg, bottom = Spacing.xs)
            .semantics { heading() },
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
    )
}
