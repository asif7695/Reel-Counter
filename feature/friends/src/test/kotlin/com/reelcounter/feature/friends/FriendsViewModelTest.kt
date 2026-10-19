package com.reelcounter.feature.friends

import com.reelcounter.core.domain.Outcome
import com.reelcounter.core.domain.social.FriendRequestResult
import com.reelcounter.core.domain.social.PrivacyState
import com.reelcounter.core.domain.social.SocialError
import com.reelcounter.core.model.Connection
import com.reelcounter.core.model.Relationship
import com.reelcounter.core.model.UserSummary
import com.reelcounter.core.model.Visibility
import com.reelcounter.core.testing.FakeFriendsRepository
import com.reelcounter.core.testing.FakePresenceRepository
import com.reelcounter.core.testing.FakePrivacyRepository
import com.reelcounter.core.testing.MainDispatcherRule
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class FriendsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val friends = FakeFriendsRepository()
    private val privacy = FakePrivacyRepository()
    private val presence = FakePresenceRepository()

    private val sam = UserSummary(id = "2", username = "sam_r", displayName = null, avatarUrl = null)

    /** Creates the view model with a subscriber, as the screen would. */
    private fun TestScope.viewModel(): FriendsViewModel {
        val viewModel = FriendsViewModel(friends, privacy, presence)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
        return viewModel
    }

    @Test
    fun `search waits for two characters and a pause in typing`() = runTest {
        val viewModel = viewModel()

        viewModel.onQueryChange("s")
        advanceTimeBy(1_000)
        assertTrue(friends.searches.isEmpty())

        viewModel.onQueryChange("sa")
        advanceTimeBy(FriendsViewModel.SEARCH_DEBOUNCE_MS - 1)
        assertTrue(friends.searches.isEmpty())
        advanceTimeBy(2)
        assertEquals(listOf("sa"), friends.searches)
    }

    @Test
    fun `fast typing searches only the final query`() = runTest {
        friends.searchResults["sam"] = listOf(Connection(sam, Relationship.None))
        val viewModel = viewModel()

        viewModel.onQueryChange("sa")
        advanceTimeBy(100)
        viewModel.onQueryChange("sam")
        advanceTimeBy(FriendsViewModel.SEARCH_DEBOUNCE_MS + 1)

        assertEquals(listOf("sam"), friends.searches)
        assertEquals(listOf(Connection(sam, Relationship.None)), viewModel.uiState.value.search.results)
    }

    @Test
    fun `an answer for an old query is ignored`() = runTest {
        friends.searchResults["sa"] = listOf(Connection(sam, Relationship.None))
        val gate = CompletableDeferred<Unit>().also { friends.gate = it }
        val viewModel = viewModel()

        viewModel.onQueryChange("sa")
        advanceTimeBy(FriendsViewModel.SEARCH_DEBOUNCE_MS + 1)
        viewModel.onQueryChange("zz")
        gate.complete(Unit)

        val search = viewModel.uiState.value.search
        assertEquals("zz", search.query)
        assertNull(search.results)
    }

    @Test
    fun `a failed search shows the error`() = runTest {
        friends.searchError = SocialError.RateLimited
        val viewModel = viewModel()

        viewModel.onQueryChange("sam")
        advanceTimeBy(FriendsViewModel.SEARCH_DEBOUNCE_MS + 1)

        assertEquals(SocialError.RateLimited, viewModel.uiState.value.search.error)
    }

    @Test
    fun `sending a request reports the result and refreshes the search`() = runTest {
        friends.searchResults["sam"] = listOf(Connection(sam, Relationship.None))
        friends.requestResult = Outcome.Success(FriendRequestResult.Accepted)
        val viewModel = viewModel()
        viewModel.onQueryChange("sam")
        advanceTimeBy(FriendsViewModel.SEARCH_DEBOUNCE_MS + 1)

        viewModel.sendRequest(sam.id)

        assertEquals(listOf("send:2"), friends.actions)
        assertEquals(FriendsMessage.RequestResult(FriendRequestResult.Accepted), viewModel.uiState.value.message)
        assertEquals(listOf("sam", "sam"), friends.searches)

        viewModel.onMessageShown()
        assertNull(viewModel.uiState.value.message)
    }

    @Test
    fun `a user with an action in flight is busy and can't be acted on twice`() = runTest {
        val gate = CompletableDeferred<Unit>().also { friends.gate = it }
        val viewModel = viewModel()

        viewModel.accept("3")
        viewModel.accept("3")
        assertEquals(setOf("3"), viewModel.uiState.value.busyIds)
        assertEquals(listOf("accept:3"), friends.actions)

        gate.complete(Unit)
        assertTrue(viewModel.uiState.value.busyIds.isEmpty())
        assertNull(viewModel.uiState.value.message)
    }

    @Test
    fun `each action reaches the repository`() = runTest {
        val viewModel = viewModel()

        viewModel.decline("3")
        viewModel.cancel("4")
        viewModel.remove("1")

        assertEquals(listOf("decline:3", "cancel:4", "remove:1"), friends.actions)
    }

    @Test
    fun `blocking confirms, and failures are reported`() = runTest {
        val viewModel = viewModel()

        viewModel.block("1")
        assertEquals(FriendsMessage.Blocked, viewModel.uiState.value.message)

        friends.actionError = SocialError.Network
        viewModel.remove("2")
        assertEquals(FriendsMessage.Failed(SocialError.Network), viewModel.uiState.value.message)
    }

    @Test
    fun `shows the user's own visibility and which friends are online`() = runTest {
        val viewModel = viewModel()
        assertEquals(Visibility.Friends, viewModel.uiState.value.activityVisibility)

        privacy.state.value = PrivacyState.Loaded(FakePrivacyRepository.DEFAULT_PRIVACY.copy(activityVisibility = Visibility.Private))
        presence.online.value = setOf("1")

        assertEquals(Visibility.Private, viewModel.uiState.value.activityVisibility)
        assertEquals(setOf("1"), viewModel.uiState.value.onlineIds)
    }
}
