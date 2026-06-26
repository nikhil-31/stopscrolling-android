package com.example.stopscrolling_android.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.stopscrolling_android.data.database.UsageRecord
import com.example.stopscrolling_android.data.device.DeviceInfo
import com.example.stopscrolling_android.data.remote.dto.DeviceRow
import com.example.stopscrolling_android.data.sync.BackendSyncService
import com.example.stopscrolling_android.data.sync.DevicesFetchResult
import com.example.stopscrolling_android.data.sync.SessionsFetchResult
import com.example.stopscrolling_android.data.sync.TodayInsightsFetchResult
import com.example.stopscrolling_android.data.sync.SyncSessionMapper
import com.example.stopscrolling_android.domain.repository.AuthRepository
import com.example.stopscrolling_android.domain.repository.UsageRepository
import com.example.stopscrolling_android.presentation.timeline.TimelineModel
import com.example.stopscrolling_android.presentation.timeline.TimelineSegment
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class UsageViewModel @Inject constructor(
    private val repository: UsageRepository,
    private val backendSyncService: BackendSyncService,
    private val authRepository: AuthRepository
) : ViewModel() {

    /**
     * Pending upload outbox (captured-but-not-yet-uploaded). Rendered as an overlay
     * on top of server data so freshly captured activity is visible immediately, and
     * still used for the "pending" indicator and local export.
     */
    val allRecords: StateFlow<List<UsageRecord>> = repository.getAllRecords()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Today timeline rendered from the backend sessions API. Null until first load;
    // kept (short-lived in-memory cache) across transient fetch failures so a brief
    // disconnect doesn't blank the screen.
    private val _daySegments = MutableStateFlow<List<TimelineSegment>?>(null)
    val daySegments: StateFlow<List<TimelineSegment>?> = _daySegments.asStateFlow()

    // Multi-day history (Timeline screen) rendered from the backend sessions API.
    private val _historyRecords = MutableStateFlow<List<UsageRecord>?>(null)
    val historyRecords: StateFlow<List<UsageRecord>?> = _historyRecords.asStateFlow()

    private val _devices = MutableStateFlow<List<DeviceRow>>(emptyList())
    val devices: StateFlow<List<DeviceRow>> = _devices.asStateFlow()

    private val _timelineStatus = MutableStateFlow<String?>(null)
    val timelineStatus: StateFlow<String?> = _timelineStatus.asStateFlow()

    private val _isLoadingTimeline = MutableStateFlow(false)
    val isLoadingTimeline: StateFlow<Boolean> = _isLoadingTimeline.asStateFlow()

    fun refreshDayTimeline(referenceMs: Long = System.currentTimeMillis()) {
        if (authRepository.currentUser.value == null) {
            _daySegments.value = null
            _timelineStatus.value = null
            return
        }

        viewModelScope.launch {
            _isLoadingTimeline.value = true
            val day = TimelineModel.formatDate(referenceMs)
            val timeZone = DeviceInfo.timeZone()
            when (val result = backendSyncService.fetchTodayInsights(day, timeZone)) {
                is TodayInsightsFetchResult.Success -> {
                    _daySegments.value = BackendSyncService.todayInsightsToSegments(result.response)
                    _timelineStatus.value = null
                }
                is TodayInsightsFetchResult.Skipped -> {
                    _daySegments.value = null
                    _timelineStatus.value = null
                }
                is TodayInsightsFetchResult.Failure -> {
                    // Keep the last successful response (in-memory cache) so brief
                    // offline periods show stale-but-useful data instead of nothing.
                    _timelineStatus.value = result.message
                }
            }
            _isLoadingTimeline.value = false
        }
    }

    fun refreshHistory(days: Int = HISTORY_DAYS) {
        if (authRepository.currentUser.value == null) {
            _historyRecords.value = null
            return
        }

        viewModelScope.launch {
            _isLoadingTimeline.value = true
            refreshDevicesInternal()
            val end = System.currentTimeMillis()
            val start = end - days * MILLIS_PER_DAY
            when (val result = backendSyncService.fetchSessions(start, end)) {
                is SessionsFetchResult.Success -> {
                    _historyRecords.value = result.response.sessions
                        .map(SyncSessionMapper::toUsageRecord)
                        .sortedByDescending { it.startTimeUTC }
                    _timelineStatus.value = null
                }
                is SessionsFetchResult.Skipped -> {
                    _historyRecords.value = null
                    _timelineStatus.value = null
                }
                is SessionsFetchResult.Failure -> {
                    _timelineStatus.value = result.message
                }
            }
            _isLoadingTimeline.value = false
        }
    }

    private suspend fun refreshDevicesInternal() {
        when (val result = backendSyncService.fetchDevices()) {
            is DevicesFetchResult.Success -> {
                _devices.value = result.response.results
            }
            else -> {
                // Keep existing or handle failure
            }
        }
    }

    suspend fun clearAllData() {
        repository.clearData()
    }

    private companion object {
        const val HISTORY_DAYS = 14
        const val MILLIS_PER_DAY = 24L * 60L * 60L * 1000L
    }
}
