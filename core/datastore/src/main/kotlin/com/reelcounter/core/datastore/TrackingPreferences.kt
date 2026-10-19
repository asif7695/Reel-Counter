package com.reelcounter.core.datastore

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

data class TrackingPreferences(
    /** The disclosure version the user agreed to; 0 = never. */
    val disclosureAcceptedVersion: Int = 0,
    val disclosureAcceptedAtMs: Long? = null,
    val paused: Boolean = false,
    /**
     * When tracking last resumed after a pause. Usage before this moment is never read, so time
     * spent while paused is never recorded, even though Android still logs it.
     */
    val resumedAtMs: Long? = null,
)

interface TrackingPreferencesStore {
    val preferences: Flow<TrackingPreferences>
    suspend fun acceptDisclosure(version: Int, atMs: Long)
    suspend fun setPaused(paused: Boolean, atMs: Long)
}

private val Context.trackingDataStore by preferencesDataStore(name = "tracking_preferences")

@Singleton
internal class DataStoreTrackingPreferencesStore @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : TrackingPreferencesStore {

    override val preferences: Flow<TrackingPreferences> = context.trackingDataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map {
            TrackingPreferences(
                disclosureAcceptedVersion = it[DISCLOSURE_VERSION] ?: 0,
                disclosureAcceptedAtMs = it[DISCLOSURE_AT],
                paused = it[PAUSED] ?: false,
                resumedAtMs = it[RESUMED_AT],
            )
        }

    override suspend fun acceptDisclosure(version: Int, atMs: Long) {
        context.trackingDataStore.edit {
            it[DISCLOSURE_VERSION] = version
            it[DISCLOSURE_AT] = atMs
        }
    }

    override suspend fun setPaused(paused: Boolean, atMs: Long) {
        context.trackingDataStore.edit {
            it[PAUSED] = paused
            if (!paused) it[RESUMED_AT] = atMs
        }
    }

    private companion object {
        val DISCLOSURE_VERSION = intPreferencesKey("disclosure_version")
        val DISCLOSURE_AT = longPreferencesKey("disclosure_accepted_at")
        val PAUSED = booleanPreferencesKey("paused")
        val RESUMED_AT = longPreferencesKey("resumed_at")
    }
}

@Module
@InstallIn(SingletonComponent::class)
internal abstract class DataStoreModule {
    @Binds
    abstract fun bindTrackingPreferencesStore(impl: DataStoreTrackingPreferencesStore): TrackingPreferencesStore
}
