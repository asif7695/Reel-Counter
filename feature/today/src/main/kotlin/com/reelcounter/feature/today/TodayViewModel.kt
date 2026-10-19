package com.reelcounter.feature.today

import androidx.lifecycle.ViewModel
import com.reelcounter.core.model.DailyPlatformActivity
import com.reelcounter.core.ui.state.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class TodayViewModel @Inject constructor() : ViewModel() {

    // There's no tracking yet, so today is honestly empty. Phase 4 replaces this with a
    // flow of today's summaries from the tracking repository.
    private val _uiState = MutableStateFlow<UiState<List<DailyPlatformActivity>>>(UiState.Empty())
    val uiState: StateFlow<UiState<List<DailyPlatformActivity>>> = _uiState.asStateFlow()
}
