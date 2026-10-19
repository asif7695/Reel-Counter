package com.reelcounter.feature.friends

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reelcounter.core.domain.Outcome
import com.reelcounter.core.domain.social.ConnectionsState
import com.reelcounter.core.domain.social.FriendRequestResult
import com.reelcounter.core.domain.social.FriendsRepository
import com.reelcounter.core.domain.social.PresenceRepository
import com.reelcounter.core.domain.social.PrivacyRepository
import com.reelcounter.core.domain.social.PrivacyState
import com.reelcounter.core.domain.social.SocialError
import com.reelcounter.core.model.Connection
import com.reelcounter.core.model.Visibility
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SearchState(
    val query: String = "",
    val isSearching: Boolean = false,
    /** null until a search for the current query has finished. */
    val results: List<Connection>? = null,
    val error: SocialError? = null,
) {
    val isActive: Boolean get() = query.trim().length >= MIN_QUERY_LENGTH
}

/** One-off feedback after an action, shown as a snackbar. */
sealed interface FriendsMessage {
    data class RequestResult(val result: FriendRequestResult) : FriendsMessage
    data object Blocked : FriendsMessage
    data class Failed(val error: SocialError) : FriendsMessage
}

data class FriendsUiState(
    val search: SearchState = SearchState(),
    val connections: ConnectionsState = ConnectionsState.Loading,
    val onlineIds: Set<String> = emptySet(),
    /** Users with an action in flight; their buttons are disabled. */
    val busyIds: Set<String> = emptySet(),
    /** The user's own activity visibility, shown so they always know what friends can see. */
    val activityVisibility: Visibility? = null,
    val message: FriendsMessage? = null,
)

internal const val MIN_QUERY_LENGTH = 2

@OptIn(FlowPreview::class)
@HiltViewModel
class FriendsViewModel @Inject constructor(
    private val friendsRepository: FriendsRepository,
    privacyRepository: PrivacyRepository,
    presenceRepository: PresenceRepository,
) : ViewModel() {

    private val search = MutableStateFlow(SearchState())
    private val busy = MutableStateFlow<Set<String>>(emptySet())
    private val message = MutableStateFlow<FriendsMessage?>(null)

    val uiState: StateFlow<FriendsUiState> = combine(
        combine(search, friendsRepository.connections, presenceRepository.onlineFriendIds, ::Triple),
        busy,
        privacyRepository.settings.map { (it as? PrivacyState.Loaded)?.settings?.activityVisibility },
        message,
    ) { (search, connections, online), busy, visibility, message ->
        FriendsUiState(search, connections, online, busy, visibility, message)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FriendsUiState())

    init {
        viewModelScope.launch {
            search.map { it.query.trim() }
                .distinctUntilChanged()
                .debounce(SEARCH_DEBOUNCE_MS)
                .collect { query -> if (query.length >= MIN_QUERY_LENGTH) runSearch(query) }
        }
    }

    fun onQueryChange(query: String) = search.update {
        // A new query clears stale results; the debounced search fills them in.
        if (query.trim() == it.query.trim()) it.copy(query = query) else SearchState(query = query)
    }

    fun refresh() {
        viewModelScope.launch { friendsRepository.refresh() }
    }

    fun sendRequest(userId: String) = act(userId) {
        when (val result = friendsRepository.sendRequest(userId)) {
            is Outcome.Success -> FriendsMessage.RequestResult(result.value)
            is Outcome.Failure -> FriendsMessage.Failed(result.error)
        }
    }

    fun accept(userId: String) = act(userId) { friendsRepository.respond(userId, accept = true).failureMessage() }

    fun decline(userId: String) = act(userId) { friendsRepository.respond(userId, accept = false).failureMessage() }

    fun cancel(userId: String) = act(userId) { friendsRepository.cancelRequest(userId).failureMessage() }

    fun remove(userId: String) = act(userId) { friendsRepository.removeFriend(userId).failureMessage() }

    fun block(userId: String) = act(userId) {
        friendsRepository.block(userId).failureMessage() ?: FriendsMessage.Blocked
    }

    fun onMessageShown() = message.update { null }

    private fun act(userId: String, action: suspend () -> FriendsMessage?) {
        if (userId in busy.value) return
        busy.update { it + userId }
        viewModelScope.launch {
            val outcome = action()
            busy.update { it - userId }
            message.value = outcome
            // Relationship labels in the results changed; refresh them.
            search.value.query.trim().takeIf { it.length >= MIN_QUERY_LENGTH }?.let { runSearch(it) }
        }
    }

    private suspend fun runSearch(query: String) {
        search.update { if (it.query.trim() == query) it.copy(isSearching = true, error = null) else it }
        val result = friendsRepository.search(query)
        search.update { current ->
            // Ignore answers for a query the user has already changed.
            if (current.query.trim() != query) return@update current
            when (result) {
                is Outcome.Success -> current.copy(isSearching = false, results = result.value)
                is Outcome.Failure -> current.copy(isSearching = false, error = result.error)
            }
        }
    }

    internal companion object {
        const val SEARCH_DEBOUNCE_MS = 300L
    }
}

private fun Outcome<Unit, SocialError>.failureMessage(): FriendsMessage? =
    (this as? Outcome.Failure)?.let { FriendsMessage.Failed(it.error) }
