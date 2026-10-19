package com.reelcounter.feature.privacy

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.reelcounter.core.designsystem.component.ErrorState
import com.reelcounter.core.designsystem.component.RcCard
import com.reelcounter.core.designsystem.component.RcLoadingIndicator
import com.reelcounter.core.designsystem.component.RcTextButton
import com.reelcounter.core.designsystem.component.RcTopAppBar
import com.reelcounter.core.designsystem.icon.RcIcons
import com.reelcounter.core.designsystem.theme.Spacing
import com.reelcounter.core.domain.social.PrivacyState
import com.reelcounter.core.domain.social.SocialError
import com.reelcounter.core.model.PrivacySettings
import com.reelcounter.core.model.Visibility
import com.reelcounter.core.ui.profile.Avatar

@Composable
internal fun PrivacyScreen(
    onBack: () -> Unit,
    viewModel: PrivacyViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    PrivacyScreen(
        uiState = uiState,
        actions = PrivacyActions(
            onBack = onBack,
            onVisibilityChange = viewModel::setActivityVisibility,
            onShareOnlineStatusChange = viewModel::setShareOnlineStatus,
            onDiscoverableChange = viewModel::setDiscoverable,
            onAcceptRequestsChange = viewModel::setAcceptFriendRequests,
            onUnblock = viewModel::unblock,
            onRetry = viewModel::retry,
            onErrorShown = viewModel::onErrorShown,
        ),
    )
}

internal class PrivacyActions(
    val onBack: () -> Unit = {},
    val onVisibilityChange: (Visibility) -> Unit = {},
    val onShareOnlineStatusChange: (Boolean) -> Unit = {},
    val onDiscoverableChange: (Boolean) -> Unit = {},
    val onAcceptRequestsChange: (Boolean) -> Unit = {},
    val onUnblock: (String) -> Unit = {},
    val onRetry: () -> Unit = {},
    val onErrorShown: () -> Unit = {},
)

@Composable
internal fun PrivacyScreen(uiState: PrivacyUiState, actions: PrivacyActions) {
    val snackbar = remember { SnackbarHostState() }
    val errorText = uiState.error?.let { stringResource(it.saveErrorText()) }
    LaunchedEffect(uiState.error) {
        if (errorText != null) {
            snackbar.showSnackbar(errorText)
            actions.onErrorShown()
        }
    }

    Scaffold(
        topBar = {
            RcTopAppBar(
                title = stringResource(R.string.feature_privacy_title),
                navigationIcon = RcIcons.Back,
                navigationIconContentDescription = stringResource(R.string.feature_privacy_back),
                onNavigationClick = actions.onBack,
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { innerPadding ->
        val settings = when (val state = uiState.settings) {
            PrivacyState.Loading -> {
                RcLoadingIndicator(modifier = Modifier.padding(innerPadding).fillMaxWidth())
                return@Scaffold
            }
            is PrivacyState.Failed -> {
                ErrorState(
                    title = stringResource(R.string.feature_privacy_load_failed),
                    message = stringResource(state.error.loadErrorText()),
                    retryLabel = stringResource(R.string.feature_privacy_retry),
                    onRetry = actions.onRetry,
                    modifier = Modifier.padding(innerPadding),
                )
                return@Scaffold
            }
            is PrivacyState.Loaded -> state.settings
        }
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            Heading(stringResource(R.string.feature_privacy_activity_heading))
            ActivityVisibilityCard(settings.activityVisibility, actions.onVisibilityChange)

            Heading(stringResource(R.string.feature_privacy_social_heading))
            SocialCard(settings, actions)

            Heading(stringResource(R.string.feature_privacy_blocked_heading))
            BlockedCard(uiState.blocked, uiState.unblockingIds, actions)
        }
    }
}

@Composable
private fun ActivityVisibilityCard(selected: Visibility, onSelect: (Visibility) -> Unit) {
    // "Everyone" isn't offered yet; it only appears if the account already has it, so the
    // current setting is never hidden.
    val options = buildList {
        add(Visibility.Friends)
        add(Visibility.Private)
        if (selected == Visibility.Public) add(Visibility.Public)
    }
    RcCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            options.forEach { option ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(selected = option == selected, role = Role.RadioButton, onClick = { onSelect(option) }),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(selected = option == selected, onClick = null)
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = stringResource(option.titleText()), style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = stringResource(option.summaryText()),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            Text(
                text = stringResource(R.string.feature_privacy_activity_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SocialCard(settings: PrivacySettings, actions: PrivacyActions) {
    RcCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
            SwitchRow(
                title = stringResource(R.string.feature_privacy_online),
                summary = stringResource(R.string.feature_privacy_online_summary),
                checked = settings.shareOnlineStatus,
                onCheckedChange = actions.onShareOnlineStatusChange,
            )
            SwitchRow(
                title = stringResource(R.string.feature_privacy_discoverable),
                summary = stringResource(R.string.feature_privacy_discoverable_summary),
                checked = settings.discoverable,
                onCheckedChange = actions.onDiscoverableChange,
            )
            SwitchRow(
                title = stringResource(R.string.feature_privacy_requests),
                summary = stringResource(R.string.feature_privacy_requests_summary),
                checked = settings.acceptFriendRequests,
                onCheckedChange = actions.onAcceptRequestsChange,
            )
        }
    }
}

@Composable
private fun BlockedCard(blocked: BlockedState, unblockingIds: Set<String>, actions: PrivacyActions) {
    RcCard(modifier = Modifier.fillMaxWidth()) {
        when (blocked) {
            BlockedState.Loading -> RcLoadingIndicator(modifier = Modifier.fillMaxWidth())
            is BlockedState.Failed -> Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(blocked.error.loadErrorText()),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium,
                )
                RcTextButton(text = stringResource(R.string.feature_privacy_retry), onClick = actions.onRetry)
            }
            is BlockedState.Loaded -> if (blocked.users.isEmpty()) {
                Text(
                    text = stringResource(R.string.feature_privacy_blocked_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    blocked.users.forEach { user ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Avatar(image = user.avatarUrl, name = user.nameForDisplay, size = 40.dp)
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = user.nameForDisplay, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    text = "@${user.username}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            RcTextButton(
                                text = stringResource(R.string.feature_privacy_unblock),
                                onClick = { actions.onUnblock(user.id) },
                                enabled = user.id !in unblockingIds,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Heading(text: String) {
    Text(
        text = text,
        modifier = Modifier
            .padding(top = Spacing.sm)
            .semantics { heading() },
        style = MaterialTheme.typography.titleMedium,
    )
}

@Composable
private fun SwitchRow(title: String, summary: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange),
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            Text(text = summary, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = null)
    }
}

private fun Visibility.titleText(): Int = when (this) {
    Visibility.Friends -> R.string.feature_privacy_activity_friends
    Visibility.Private -> R.string.feature_privacy_activity_private
    Visibility.Public -> R.string.feature_privacy_activity_public
}

private fun Visibility.summaryText(): Int = when (this) {
    Visibility.Friends -> R.string.feature_privacy_activity_friends_summary
    Visibility.Private -> R.string.feature_privacy_activity_private_summary
    Visibility.Public -> R.string.feature_privacy_activity_public_summary
}

private fun SocialError.loadErrorText(): Int = when (this) {
    SocialError.Network -> R.string.feature_privacy_error_offline
    else -> R.string.feature_privacy_error_unknown
}

private fun SocialError.saveErrorText(): Int = when (this) {
    SocialError.Network -> R.string.feature_privacy_save_failed_offline
    else -> R.string.feature_privacy_save_failed
}
