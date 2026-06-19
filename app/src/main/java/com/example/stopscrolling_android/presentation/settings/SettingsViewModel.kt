package com.example.stopscrolling_android.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.stopscrolling_android.data.database.UsageDao
import com.example.stopscrolling_android.data.remote.dto.DeviceRow
import com.example.stopscrolling_android.data.settings.BackendSettings
import com.example.stopscrolling_android.data.settings.BackendSettingsStore
import com.example.stopscrolling_android.data.sync.BackendSyncService
import com.example.stopscrolling_android.data.sync.ConnectionResult
import com.example.stopscrolling_android.data.sync.DevicesFetchResult
import com.example.stopscrolling_android.data.sync.SyncResult
import com.example.stopscrolling_android.worker.UploadScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val backendSettingsStore: BackendSettingsStore,
    private val backendSyncService: BackendSyncService,
    private val usageDao: UsageDao,
    private val uploadScheduler: UploadScheduler
) : ViewModel() {

    val backendSettings: StateFlow<BackendSettings> = backendSettingsStore.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), BackendSettings())

    private val _unsyncedCount = MutableStateFlow(0)
    val unsyncedCount: StateFlow<Int> = _unsyncedCount.asStateFlow()

    private val _syncStatus = MutableStateFlow<String?>(null)
    val syncStatus: StateFlow<String?> = _syncStatus.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _isCheckingConnection = MutableStateFlow(false)
    val isCheckingConnection: StateFlow<Boolean> = _isCheckingConnection.asStateFlow()

    private val _devices = MutableStateFlow<List<DeviceRow>>(emptyList())
    val devices: StateFlow<List<DeviceRow>> = _devices.asStateFlow()

    init {
        refreshUnsyncedCount()
        refreshDevices()
    }

    fun updateApiBaseUrl(url: String) {
        viewModelScope.launch {
            val current = backendSettingsStore.current()
            backendSettingsStore.update(current.copy(apiBaseUrl = url))
        }
    }

    fun updateSyncEnabled(enabled: Boolean) {
        viewModelScope.launch {
            val current = backendSettingsStore.current()
            backendSettingsStore.update(current.copy(syncEnabled = enabled))
        }
    }

    fun updateEnhancedTrackingEnabled(enabled: Boolean) {
        viewModelScope.launch {
            val current = backendSettingsStore.current()
            backendSettingsStore.update(current.copy(enhancedTrackingEnabled = enabled))
        }
    }

    fun testConnection() {
        viewModelScope.launch {
            _isCheckingConnection.value = true
            when (val result = backendSyncService.checkConnection()) {
                ConnectionResult.Healthy -> _syncStatus.value = "Backend is reachable."
                is ConnectionResult.Degraded -> _syncStatus.value = result.message
                is ConnectionResult.Unreachable -> _syncStatus.value = result.message
            }
            _isCheckingConnection.value = false
        }
    }

    fun syncNow() {
        viewModelScope.launch {
            _isSyncing.value = true
            uploadScheduler.scheduleUploadImmediate()
            when (val result = backendSyncService.syncAll()) {
                is SyncResult.Success -> {
                    _syncStatus.value = buildSyncMessage(result)
                }
                is SyncResult.Skipped -> _syncStatus.value = result.reason
                is SyncResult.Failure -> _syncStatus.value = result.message
            }
            refreshUnsyncedCount()
            _isSyncing.value = false
        }
    }

    fun refreshUnsyncedCount() {
        viewModelScope.launch {
            _unsyncedCount.value = usageDao.getUnsyncedCount()
        }
    }

    fun refreshDevices() {
        viewModelScope.launch {
            when (val result = backendSyncService.fetchDevices()) {
                is DevicesFetchResult.Success -> {
                    _devices.value = result.response.results
                }
                else -> {
                    // Fail silently or keep existing list
                }
            }
        }
    }

    private fun buildSyncMessage(result: SyncResult.Success): String {
        return when {
            result.uploaded == 0 && result.pulled == 0 && result.remaining == 0 ->
                "Everything is in sync."
            result.uploaded == 0 && result.pulled == 0 ->
                "No changes. ${result.remaining} events still pending upload."
            else -> buildString {
                if (result.uploaded > 0) append("Uploaded ${result.uploaded} events. ")
                if (result.pulled > 0) append("Pulled ${result.pulled} sessions. ")
                if (result.remaining > 0) append("${result.remaining} still pending.")
            }.trim()
        }
    }
}
