package com.reelcounter.core.network.realtime

import dagger.Lazy
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.realtime.RealtimeChannel
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.presenceDataFlow
import io.github.jan.supabase.realtime.realtime
import io.github.jan.supabase.realtime.track
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject

/**
 * Supabase Realtime. Every flow subscribes when collected and unsubscribes when the collector is
 * cancelled, so callers decide how long connections live (only while the app is in the foreground).
 * All channels are private: joining them is authorized by RLS on realtime.messages.
 */
interface RealtimeRemoteDataSource {
    /**
     * Emits whenever a friendship involving [userId] is created, accepted or deleted. A database
     * trigger broadcasts an empty "changed" message to just the two people involved.
     */
    fun friendshipChanges(userId: String): Flow<Unit>

    /** Shares [userId]'s presence on their own channel for as long as it's collected. */
    fun shareOwnPresence(userId: String): Flow<Unit>

    /** The subset of [userIds] currently present. Only channels the caller may join report anything. */
    fun presenceOf(userIds: Set<String>): Flow<Set<String>>
}

internal class SupabaseRealtimeRemoteDataSource @Inject constructor(
    private val client: Lazy<SupabaseClient>,
) : RealtimeRemoteDataSource {

    override fun friendshipChanges(userId: String): Flow<Unit> = subscribed("friendships:$userId") { channel ->
        channel.broadcastFlow(event = FRIENDSHIP_CHANGED_EVENT).map { }
    }

    override fun shareOwnPresence(userId: String): Flow<Unit> = channelFlow {
        val channel = client.get().channel(presenceTopic(userId)) { isPrivate = true }
        try {
            channel.subscribe(blockUntilSubscribed = true)
            channel.track(PresenceState(online = true))
            send(Unit)
            awaitCancellation()
        } finally {
            withContext(NonCancellable) {
                runCatching { channel.untrack() }
                client.get().realtime.removeChannel(channel)
            }
        }
    }

    override fun presenceOf(userIds: Set<String>): Flow<Set<String>> = channelFlow {
        send(emptySet())
        if (userIds.isEmpty()) awaitCancellation()
        // One collector per friend; they run concurrently, so the shared set must be thread-safe.
        val online = ConcurrentHashMap.newKeySet<String>()
        userIds.forEach { id ->
            launch {
                subscribed(presenceTopic(id)) { it.presenceDataFlow<JsonObject>() }.collect { presences ->
                    val changed = if (presences.isEmpty()) online.remove(id) else online.add(id)
                    if (changed) send(online.toSet())
                }
            }
        }
    }

    /**
     * Creates a private channel, starts listening BEFORE subscribing (Realtime needs bindings and
     * presence callbacks registered first), and removes the channel when cancelled.
     */
    private fun <T> subscribed(topic: String, stream: (RealtimeChannel) -> Flow<T>): Flow<T> = channelFlow {
        val channel = client.get().channel(topic) { isPrivate = true }
        val events = stream(channel)
        launch(start = CoroutineStart.UNDISPATCHED) { events.collect { send(it) } }
        try {
            channel.subscribe(blockUntilSubscribed = true)
            awaitCancellation()
        } finally {
            withContext(NonCancellable) { client.get().realtime.removeChannel(channel) }
        }
    }

    private fun presenceTopic(userId: String) = "presence:$userId"

    private companion object {
        /** Sent by private.notify_friendship_change() in the Phase 6 migration. */
        const val FRIENDSHIP_CHANGED_EVENT = "changed"
    }
}

@Serializable
private data class PresenceState(val online: Boolean)
