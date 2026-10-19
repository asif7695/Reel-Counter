package com.reelcounter.feature.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reelcounter.core.domain.tracking.CurrentDateProvider
import com.reelcounter.core.domain.tracking.TrackingRepository
import com.reelcounter.core.domain.tracking.TrackingStatus
import com.reelcounter.core.model.DailyPlatformActivity
import com.reelcounter.core.model.PlatformMetric
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject
import kotlin.time.Duration

sealed interface TodayUiState {
    data object Loading : TodayUiState

    /** Tracking has never been set up (or the disclosure needs accepting). */
    data object NeedsSetup : TodayUiState

    data class Content(
        val date: LocalDate,
        val activities: List<DailyPlatformActivity>,
        /** Sum of measured app time across tracked apps; null when nothing was measured. */
        val totalAppTime: Duration?,
        val lastUpdatedMs: Long?,
        val status: TrackingStatus,
        val isRefreshing: Boolean,
    ) : TodayUiState
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class TodayViewModel @Inject constructor(
    private val trackingRepository: TrackingRepository,
    currentDateProvider: CurrentDateProvider,
) : ViewModel() {

    private val isRefreshing = MutableStateFlow(false)

    // A new date at midnight switches to the new day's (empty) figures without a restart.
    private val todaysActivity = currentDateProvider.today.flatMapLatest { date ->
        trackingRepository.observeDay(date).map { date to it }
    }

    val uiState: StateFlow<TodayUiState> = combine(
        trackingRepository.status,
        todaysActivity,
        trackingRepository.lastUpdatedMs,
        isRefreshing,
    ) { status, (date, activities), lastUpdated, refreshing ->
        // Once anything was ever recorded, keep showing it even if access was later revoked.
        if (status == TrackingStatus.NeedsDisclosure || (status == TrackingStatus.NeedsUsageAccess && lastUpdated == null)) {
            TodayUiState.NeedsSetup
        } else {
            TodayUiState.Content(
                date = date,
                activities = activities,
                totalAppTime = activities.mapNotNull { it.metric.appTimeOrNull() }.takeIf { it.isNotEmpty() }?.reduce(Duration::plus),
                lastUpdatedMs = lastUpdated,
                status = status,
                isRefreshing = refreshing,
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TodayUiState.Loading)

    /** Reads the usage log now. Called when the screen resumes and on pull-to-refresh. */
    fun refresh() {
        if (isRefreshing.value) return
        isRefreshing.value = true
        viewModelScope.launch {
            try {
                trackingRepository.refresh()
            } finally {
                isRefreshing.value = false
            }
        }
    }
}

private fun PlatformMetric.appTimeOrNull(): Duration? = when (this) {
    is PlatformMetric.Estimated -> appTime
    is PlatformMetric.DurationOnly -> appTime
    is PlatformMetric.Verified -> appTime
    is PlatformMetric.Unavailable -> null
}
