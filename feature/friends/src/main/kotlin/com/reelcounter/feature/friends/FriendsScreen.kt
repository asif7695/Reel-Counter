package com.reelcounter.feature.friends

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.reelcounter.core.designsystem.component.EmptyState
import com.reelcounter.core.designsystem.component.ErrorState
import com.reelcounter.core.designsystem.component.RcAlertDialog
import com.reelcounter.core.designsystem.component.RcButton
import com.reelcounter.core.designsystem.component.RcCard
import com.reelcounter.core.designsystem.component.RcLoadingIndicator
import com.reelcounter.core.designsystem.component.RcOutlinedButton
import com.reelcounter.core.designsystem.component.RcTextButton
import com.reelcounter.core.designsystem.component.RcTextField
import com.reelcounter.core.designsystem.component.RcTopAppBar
import com.reelcounter.core.designsystem.icon.RcIcons
import com.reelcounter.core.designsystem.theme.Spacing
import com.reelcounter.core.domain.social.ConnectionsState
import com.reelcounter.core.domain.social.FriendRequestResult
import com.reelcounter.core.domain.social.SocialError
import com.reelcounter.core.model.Relationship
import com.reelcounter.core.model.UserSummary
import com.reelcounter.core.model.Visibility
import com.reelcounter.core.ui.profile.Avatar

@Composable
internal fun FriendsScreen(
    onOpenPrivacy: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FriendsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    FriendsScreen(
        uiState = uiState,
        actions = FriendsActions(
            onQueryChange = viewModel::onQueryChange,
            onRetry = viewModel::refresh,
            onAdd = viewModel::sendRequest,
            onAccept = viewModel::accept,
            onDecline = viewModel::decline,
            onCancel = viewModel::cancel,
            onRemove = viewModel::remove,
            onBlock = viewModel::block,
            onOpenPrivacy = onOpenPrivacy,
            onMessageShown = viewModel::onMessageShown,
        ),
        modifier = modifier,
    )
}

internal class FriendsActions(
    val onQueryChange: (String) -> Unit = {},
    val onRetry: () -> Unit = {},
    val onAdd: (String) -> Unit = {},
    val onAccept: (String) -> Unit = {},
    val onDecline: (String) -> Unit = {},
    val onCancel: (String) -> Unit = {},
    val onRemove: (String) -> Unit = {},
    val onBlock: (String) -> Unit = {},
    val onOpenPrivacy: () -> Unit = {},
    val onMessageShown: () -> Unit = {},
)

@Composable
internal fun FriendsScreen(uiState: FriendsUiState, actions: FriendsActions, modifier: Modifier = Modifier) {
    val snackbar = remember { SnackbarHostState() }
    val messageText = uiState.message?.let { messageText(it) }
    LaunchedEffect(uiState.message) {
        if (messageText != null) {
            snackbar.showSnackbar(messageText)
            actions.onMessageShown()
        }
    }
    var confirm by remember { mutableStateOf<PendingConfirmation?>(null) }

    Scaffold(
        modifier = modifier,
        topBar = { RcTopAppBar(title = stringResource(R.string.feature_friends_title)) },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            contentPadding = PaddingValues(Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            item(key = "search") {
                RcTextField(
                    value = uiState.search.query,
                    onValueChange = actions.onQueryChange,
                    label = stringResource(R.string.feature_friends_search_label),
                    leadingIcon = { Icon(imageVector = RcIcons.Search, contentDescription = null) },
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.None,
                        autoCorrectEnabled = false,
                        imeAction = ImeAction.Search,
                    ),
                )
            }
            item(key = "visibility") { VisibilityCard(uiState.activityVisibility, actions.onOpenPrivacy) }

            if (uiState.search.isActive) {
                searchResults(uiState, actions)
            } else {
                connections(uiState, actions) { confirm = it }
            }
        }
    }

    confirm?.let { pending ->
        RcAlertDialog(
            title = stringResource(pending.titleRes, pending.user.nameForDisplay),
            text = stringResource(pending.textRes),
            confirmText = stringResource(pending.confirmRes),
            dismissText = stringResource(R.string.feature_friends_cancel),
            isDestructive = true,
            onConfirm = {
                confirm = null
                if (pending.isBlock) actions.onBlock(pending.user.id) else actions.onRemove(pending.user.id)
            },
            onDismiss = { confirm = null },
        )
    }
}

private fun LazyListScope.searchResults(uiState: FriendsUiState, actions: FriendsActions) {
    val search = uiState.search
    when {
        search.error != null -> item(key = "search-error") {
            Text(text = stringResource(errorText(search.error)), color = MaterialTheme.colorScheme.error)
        }
        search.results == null -> item(key = "searching") { RcLoadingIndicator(modifier = Modifier.fillMaxWidth()) }
        search.results.isEmpty() -> item(key = "no-results") {
            Text(
                text = stringResource(R.string.feature_friends_no_results, search.query.trim()),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        else -> items(items = search.results, key = { "result-${it.user.id}" }) { result ->
            val busy = result.user.id in uiState.busyIds
            PersonRow(user = result.user, online = false) {
                when (result.relationship) {
                    Relationship.None -> RcOutlinedButton(
                        text = stringResource(R.string.feature_friends_add),
                        onClick = { actions.onAdd(result.user.id) },
                        loading = busy,
                        leadingIcon = RcIcons.AddFriend,
                    )
                    Relationship.OutgoingRequest -> RcTextButton(
                        text = stringResource(R.string.feature_friends_cancel_request),
                        onClick = { actions.onCancel(result.user.id) },
                        enabled = !busy,
                    )
                    Relationship.IncomingRequest -> RcButton(
                        text = stringResource(R.string.feature_friends_accept),
                        onClick = { actions.onAccept(result.user.id) },
                        loading = busy,
                    )
                    Relationship.Friend -> Text(
                        text = stringResource(R.string.feature_friends_already_friends),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

private fun LazyListScope.connections(
    uiState: FriendsUiState,
    actions: FriendsActions,
    onConfirm: (PendingConfirmation) -> Unit,
) {
    when (val connections = uiState.connections) {
        ConnectionsState.Loading -> item(key = "loading") { RcLoadingIndicator(modifier = Modifier.fillMaxWidth()) }
        is ConnectionsState.Failed -> item(key = "failed") {
            ErrorState(
                title = stringResource(R.string.feature_friends_load_failed),
                message = stringResource(errorText(connections.error)),
                retryLabel = stringResource(R.string.feature_friends_retry),
                onRetry = actions.onRetry,
            )
        }
        is ConnectionsState.Loaded -> {
            if (connections.friends.isEmpty() && connections.incoming.isEmpty() && connections.outgoing.isEmpty()) {
                item(key = "empty") {
                    EmptyState(
                        title = stringResource(R.string.feature_friends_empty_title),
                        message = stringResource(R.string.feature_friends_empty_message),
                        icon = RcIcons.Friends,
                    )
                }
                return
            }
            section("incoming", R.string.feature_friends_section_requests, connections.incoming) { user ->
                val busy = user.id in uiState.busyIds
                PersonRow(user = user, online = false) {
                    RcTextButton(text = stringResource(R.string.feature_friends_decline), onClick = { actions.onDecline(user.id) }, enabled = !busy)
                    RcButton(text = stringResource(R.string.feature_friends_accept), onClick = { actions.onAccept(user.id) }, loading = busy)
                }
            }
            section("friends", R.string.feature_friends_section_friends, connections.friends) { user ->
                PersonRow(user = user, online = user.id in uiState.onlineIds) {
                    FriendMenu(
                        enabled = user.id !in uiState.busyIds,
                        onRemove = { onConfirm(PendingConfirmation(user, isBlock = false)) },
                        onBlock = { onConfirm(PendingConfirmation(user, isBlock = true)) },
                    )
                }
            }
            section("outgoing", R.string.feature_friends_section_sent, connections.outgoing) { user ->
                PersonRow(user = user, online = false) {
                    RcTextButton(
                        text = stringResource(R.string.feature_friends_cancel_request),
                        onClick = { actions.onCancel(user.id) },
                        enabled = user.id !in uiState.busyIds,
                    )
                }
            }
        }
    }
}

private fun LazyListScope.section(key: String, titleRes: Int, users: List<UserSummary>, row: @Composable (UserSummary) -> Unit) {
    if (users.isEmpty()) return
    item(key = "header-$key") {
        Text(
            text = stringResource(titleRes),
            modifier = Modifier
                .padding(top = Spacing.md)
                .semantics { heading() },
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
        )
    }
    items(items = users, key = { "$key-${it.id}" }) { row(it) }
}

/** Always visible, so users know what friends can see before anyone is added. */
@Composable
private fun VisibilityCard(visibility: Visibility?, onOpenPrivacy: () -> Unit) {
    RcCard(modifier = Modifier.fillMaxWidth()) {
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md), verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = RcIcons.Privacy, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text(
                text = stringResource(
                    when (visibility) {
                        Visibility.Private -> R.string.feature_friends_visibility_private
                        Visibility.Public -> R.string.feature_friends_visibility_public
                        Visibility.Friends, null -> R.string.feature_friends_visibility_friends
                    },
                ),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium,
            )
            RcTextButton(text = stringResource(R.string.feature_friends_change), onClick = onOpenPrivacy)
        }
    }
}

@Composable
private fun PersonRow(user: UserSummary, online: Boolean, trailing: @Composable RowScope.() -> Unit) {
    val onlineLabel = stringResource(R.string.feature_friends_online)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.xs),
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box {
            Avatar(image = user.avatarUrl, name = user.nameForDisplay, size = 44.dp)
            if (online) {
                // Color plus a spoken label, never color alone.
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(14.dp)
                        .background(MaterialTheme.colorScheme.surface, CircleShape)
                        .padding(2.dp)
                        .background(MaterialTheme.colorScheme.tertiary, CircleShape)
                        .semantics { contentDescription = onlineLabel },
                )
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(text = user.nameForDisplay, style = MaterialTheme.typography.titleMedium)
            Text(
                text = if (online) "@${user.username} · $onlineLabel" else "@${user.username}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        trailing()
    }
}

@Composable
private fun FriendMenu(enabled: Boolean, onRemove: () -> Unit, onBlock: () -> Unit) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }, enabled = enabled) {
            Icon(imageVector = RcIcons.More, contentDescription = stringResource(R.string.feature_friends_more))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.feature_friends_remove)) },
                onClick = {
                    expanded = false
                    onRemove()
                },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.feature_friends_block)) },
                onClick = {
                    expanded = false
                    onBlock()
                },
            )
        }
    }
}

private data class PendingConfirmation(val user: UserSummary, val isBlock: Boolean) {
    val titleRes get() = if (isBlock) R.string.feature_friends_block_title else R.string.feature_friends_remove_title
    val textRes get() = if (isBlock) R.string.feature_friends_block_text else R.string.feature_friends_remove_text
    val confirmRes get() = if (isBlock) R.string.feature_friends_block else R.string.feature_friends_remove
}

@Composable
private fun messageText(message: FriendsMessage): String = stringResource(
    when (message) {
        is FriendsMessage.RequestResult -> when (message.result) {
            FriendRequestResult.Requested -> R.string.feature_friends_msg_requested
            FriendRequestResult.Accepted -> R.string.feature_friends_msg_now_friends
            FriendRequestResult.AlreadyFriends -> R.string.feature_friends_msg_already_friends
            FriendRequestResult.AlreadyRequested -> R.string.feature_friends_msg_already_requested
            FriendRequestResult.NotAccepting -> R.string.feature_friends_msg_not_accepting
            FriendRequestResult.NotFound -> R.string.feature_friends_msg_not_found
        }
        FriendsMessage.Blocked -> R.string.feature_friends_msg_blocked
        is FriendsMessage.Failed -> errorText(message.error)
    },
)

private fun errorText(error: SocialError): Int = when (error) {
    SocialError.Network -> R.string.feature_friends_error_network
    SocialError.RateLimited -> R.string.feature_friends_error_rate_limited
    SocialError.NotSignedIn, is SocialError.Unknown -> R.string.feature_friends_error_unknown
}
