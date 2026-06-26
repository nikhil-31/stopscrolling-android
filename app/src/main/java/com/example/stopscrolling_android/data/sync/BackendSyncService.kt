package com.example.stopscrolling_android.data.sync

import com.example.stopscrolling_android.data.auth.AuthTokenStore
import com.example.stopscrolling_android.data.database.UsageDao
import com.example.stopscrolling_android.data.device.DeviceInfo
import com.example.stopscrolling_android.data.remote.DeviceApiClient
import com.example.stopscrolling_android.data.remote.EventsApiClient
import com.example.stopscrolling_android.data.remote.HealthApiClient
import com.example.stopscrolling_android.data.remote.SyncApiClient
import com.example.stopscrolling_android.data.remote.dto.DeviceRegisterRequest
import com.example.stopscrolling_android.data.remote.dto.DeviceRow
import com.example.stopscrolling_android.data.remote.dto.DeviceListResponse
import com.example.stopscrolling_android.data.remote.dto.DeviceStatusListResponse
import com.example.stopscrolling_android.data.remote.dto.DeviceStatusRow
import com.example.stopscrolling_android.data.remote.dto.InsightsSessionsResponse
import com.example.stopscrolling_android.data.remote.dto.InsightsTodayResponse
import com.example.stopscrolling_android.data.remote.dto.SyncSessionRow
import com.example.stopscrolling_android.data.settings.BackendSettingsStore
import com.example.stopscrolling_android.domain.repository.AuthRepository
import com.example.stopscrolling_android.presentation.timeline.TimelineSegment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BackendSyncService @Inject constructor(
    private val usageDao: UsageDao,
    private val eventsApiClient: EventsApiClient,
    private val syncApiClient: SyncApiClient,
    private val deviceApiClient: DeviceApiClient,
    private val healthApiClient: HealthApiClient,
    private val authRepository: AuthRepository,
    private val tokenStore: AuthTokenStore,
    private val backendSettingsStore: BackendSettingsStore
) {
    suspend fun checkConnection(): ConnectionResult = withContext(Dispatchers.IO) {
        val settings = backendSettingsStore.current()
        runCatching {
            val health = healthApiClient.checkHealth(settings.apiBaseUrl)
            if (health.status == "ok") {
                ConnectionResult.Healthy
            } else {
                ConnectionResult.Degraded("Backend status: ${health.status}")
            }
        }.getOrElse {
            ConnectionResult.Unreachable(it.message ?: "Cannot reach backend.")
        }
    }

    suspend fun ensureDeviceRegistered(): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val settings = backendSettingsStore.current()
            if (!tokenStore.hasTokens()) {
                error("Sign in to register this device.")
            }
            val accessToken = authRepository.getValidAccessToken()
                ?: error("Sign in to register this device.")

            val refresh = suspend { authRepository.refreshAccessTokenIfNeeded() }
            val request = DeviceRegisterRequest(
                devicePlatform = DeviceInfo.PLATFORM,
                deviceName = DeviceInfo.deviceName(),
                timeZone = DeviceInfo.timeZone()
            )
            val device = deviceApiClient.registerDeviceWithRetry(
                baseUrl = settings.apiBaseUrl,
                accessToken = accessToken,
                request = request,
                refreshAccessToken = refresh
            )
            backendSettingsStore.updateRegisteredDeviceId(device.deviceId)
            device.deviceId
        }
    }

    /**
     * Posts a heartbeat so the backend marks this device as online (`last_online_at`).
     * Devices are considered online when the latest heartbeat is within five minutes.
     */
    suspend fun postHeartbeat(): HeartbeatResult = withContext(Dispatchers.IO) {
        val settings = backendSettingsStore.current()
        if (!settings.isReadyForSync(tokenStore.hasTokens())) {
            return@withContext HeartbeatResult.Skipped("Sign in and enable backend sync.")
        }

        val accessToken = authRepository.getValidAccessToken()
            ?: return@withContext HeartbeatResult.Skipped("Sign in on the Account screen.")

        val deviceId = settings.registeredDeviceId
            ?: ensureDeviceRegistered().getOrElse {
                return@withContext HeartbeatResult.Skipped("Device not registered.")
            }

        try {
            val refresh = suspend { authRepository.refreshAccessTokenIfNeeded() }
            val status = deviceApiClient.postHeartbeatWithRetry(
                baseUrl = settings.apiBaseUrl,
                accessToken = accessToken,
                deviceId = deviceId,
                refreshAccessToken = refresh
            )
            HeartbeatResult.Success(status)
        } catch (e: Exception) {
            HeartbeatResult.Failure(e.message ?: "Could not post online status.")
        }
    }

    suspend fun fetchDeviceStatus(): DeviceStatusFetchResult = withContext(Dispatchers.IO) {
        val settings = backendSettingsStore.current()
        if (!tokenStore.hasTokens()) {
            return@withContext DeviceStatusFetchResult.Skipped("Sign in to see device status.")
        }

        val accessToken = authRepository.getValidAccessToken()
            ?: return@withContext DeviceStatusFetchResult.Skipped("Sign in to see device status.")

        try {
            val refresh = suspend { authRepository.refreshAccessTokenIfNeeded() }
            val response = deviceApiClient.fetchDeviceStatusWithRetry(
                baseUrl = settings.apiBaseUrl,
                accessToken = accessToken,
                refreshAccessToken = refresh
            )
            DeviceStatusFetchResult.Success(response)
        } catch (e: Exception) {
            DeviceStatusFetchResult.Failure(e.message ?: "Could not load device status.")
        }
    }

    suspend fun fetchDevices(): DevicesFetchResult = withContext(Dispatchers.IO) {
        val settings = backendSettingsStore.current()
        if (!tokenStore.hasTokens()) {
            return@withContext DevicesFetchResult.Skipped("Sign in to see devices.")
        }

        val accessToken = authRepository.getValidAccessToken()
            ?: return@withContext DevicesFetchResult.Skipped("Sign in to see devices.")

        try {
            val refresh = suspend { authRepository.refreshAccessTokenIfNeeded() }
            val response = deviceApiClient.fetchDevicesWithRetry(
                baseUrl = settings.apiBaseUrl,
                accessToken = accessToken,
                refreshAccessToken = refresh
            )
            DevicesFetchResult.Success(response)
        } catch (e: Exception) {
            DevicesFetchResult.Failure(e.message ?: "Could not load devices.")
        }
    }

    /**
     * Fetches every foreground session across all of the user's devices overlapping
     * `[startMs, endMs)` from the backend. This is the single source the app renders
     * timelines/insights from; local rows are only an upload outbox.
     */
    suspend fun fetchSessions(startMs: Long, endMs: Long): SessionsFetchResult =
        withContext(Dispatchers.IO) {
            val settings = backendSettingsStore.current()
            if (!settings.isReadyForSync(tokenStore.hasTokens())) {
                return@withContext SessionsFetchResult.Skipped("Sign in and enable backend sync.")
            }

            val accessToken = authRepository.getValidAccessToken()
                ?: return@withContext SessionsFetchResult.Skipped("Sign in on the Account screen.")

            try {
                val refresh = suspend { authRepository.refreshAccessTokenIfNeeded() }
                val response = deviceApiClient.fetchSessionsWithRetry(
                    baseUrl = settings.apiBaseUrl,
                    accessToken = accessToken,
                    startIso = Instant.ofEpochMilli(startMs).toString(),
                    endIso = Instant.ofEpochMilli(endMs).toString(),
                    refreshAccessToken = refresh
                )
                SessionsFetchResult.Success(response)
            } catch (e: Exception) {
                SessionsFetchResult.Failure(e.message ?: "Could not load timeline.")
            }
        }

    suspend fun fetchTodayInsights(day: String, timeZone: String): TodayInsightsFetchResult =
        withContext(Dispatchers.IO) {
            val settings = backendSettingsStore.current()
            if (!settings.isReadyForSync(tokenStore.hasTokens())) {
                return@withContext TodayInsightsFetchResult.Skipped("Sign in and enable backend sync.")
            }

            val accessToken = authRepository.getValidAccessToken()
                ?: return@withContext TodayInsightsFetchResult.Skipped("Sign in on the Account screen.")

            try {
                val refresh = suspend { authRepository.refreshAccessTokenIfNeeded() }
                val response = deviceApiClient.fetchTodayInsightsWithRetry(
                    baseUrl = settings.apiBaseUrl,
                    accessToken = accessToken,
                    day = day,
                    timeZone = timeZone,
                    refreshAccessToken = refresh
                )
                TodayInsightsFetchResult.Success(response)
            } catch (e: Exception) {
                TodayInsightsFetchResult.Failure(e.message ?: "Could not load today's insights.")
            }
        }

    /**
     * Uploads any pending outbox rows and registers the device. There is no longer a
     * reverse pull into local storage: the backend is authoritative and the UI reads
     * sessions directly via [fetchSessions].
     */
    suspend fun syncAll(): SyncResult = withContext(Dispatchers.IO) {
        val settings = backendSettingsStore.current()
        if (!settings.isReadyForSync(tokenStore.hasTokens())) {
            return@withContext SyncResult.Skipped("Sign in and enable backend sync in Settings.")
        }

        val accessToken = resolveAccessToken()
            ?: return@withContext SyncResult.Skipped("Sign in on the Account screen to sync events.")

        val refresh = suspend { authRepository.refreshAccessTokenIfNeeded() }

        try {
            val uploaded = uploadUnsyncedRecords(settings.apiBaseUrl, accessToken, refresh)
            ensureDeviceRegistered()
            postHeartbeat()
            val remaining = usageDao.getUnsyncedCount()
            SyncResult.Success(uploaded = uploaded, pulled = 0, remaining = remaining)
        } catch (e: Exception) {
            SyncResult.Failure(e.message ?: "Sync failed.")
        }
    }

    suspend fun syncUnsyncedRecords(): SyncResult = withContext(Dispatchers.IO) {
        val settings = backendSettingsStore.current()
        if (!settings.isReadyForSync(tokenStore.hasTokens())) {
            return@withContext SyncResult.Skipped("Sign in and enable backend sync in Settings.")
        }

        val accessToken = resolveAccessToken()
            ?: return@withContext SyncResult.Skipped("Sign in on the Account screen to sync events.")

        try {
            val uploaded = uploadUnsyncedRecords(
                baseUrl = settings.apiBaseUrl,
                accessToken = accessToken,
                refreshAccessToken = { authRepository.refreshAccessTokenIfNeeded() }
            )
            ensureDeviceRegistered()
            postHeartbeat()
            val remaining = usageDao.getUnsyncedCount()
            SyncResult.Success(uploaded = uploaded, pulled = 0, remaining = remaining)
        } catch (e: Exception) {
            SyncResult.Failure(e.message ?: "Sync failed.")
        }
    }

    private suspend fun resolveAccessToken(): String? {
        return authRepository.getValidAccessToken()
    }

    private suspend fun uploadUnsyncedRecords(
        baseUrl: String,
        accessToken: String,
        refreshAccessToken: suspend () -> String?
    ): Int {
        var totalInserted = 0
        var currentToken = accessToken
        while (true) {
            val unsynced = usageDao.getUnsyncedRecords(MAX_UNSYNCED_FETCH)
            if (unsynced.isEmpty()) break

            val batches = unsynced.chunked(BULK_BATCH_SIZE)
            for (batch in batches) {
                val payloads = EventPayloadMapper.fromRecords(batch)
                val response = eventsApiClient.postEventsBulkWithRetry(
                    events = payloads,
                    baseUrl = baseUrl,
                    accessToken = currentToken,
                    refreshAccessToken = refreshAccessToken
                )
                authRepository.getValidAccessToken()?.let { currentToken = it }
                totalInserted += response.inserted
                // Server-authoritative: drop the rows from the outbox the moment the
                // backend confirms the upload. Nothing is kept locally.
                usageDao.deleteRecordsByIds(batch.map { it.id })
            }
        }
        return totalInserted
    }

    companion object {
        const val BULK_BATCH_SIZE = 200
        private const val MAX_UNSYNCED_FETCH = 1000

        /** Maps backend sessions to timeline segments clipped to `[windowStartMs, windowEndMs)`. */
        fun sessionSegments(
            sessions: List<SyncSessionRow>,
            windowStartMs: Long,
            windowEndMs: Long
        ): List<TimelineSegment> {
            return sessions
                .map { session -> sessionToSegment(session, windowStartMs, windowEndMs) }
                .filter { it.durationSeconds > 0 }
                .sortedBy { it.startTimeMs }
        }

        fun todayInsightsToSegments(response: InsightsTodayResponse): List<TimelineSegment> {
            val segments = mutableListOf<TimelineSegment>()
            for (device in response.devices) {
                for (block in device.blocks) {
                    if (block.items.isEmpty()) continue

                    val dominantItem = block.items.maxByOrNull { it.seconds } ?: block.items.first()
                    val startTimeMs = BackendDateTimeParser.parseToEpochMilli(block.start)
                    val endTimeMs = BackendDateTimeParser.parseToEpochMilli(block.end)

                    segments.add(
                        TimelineSegment(
                            id = block.id,
                            startTimeMs = startTimeMs,
                            endTimeMs = endTimeMs,
                            label = dominantItem.appName,
                            subtitle = device.deviceName,
                            category = "Unknown",
                            appName = dominantItem.appName,
                            deviceName = device.deviceName,
                            url = if (dominantItem.isWebsite) dominantItem.id else null,
                            durationSeconds = block.activeSeconds.toLong()
                        )
                    )
                }
            }
            return segments.sortedBy { it.startTimeMs }
        }

        private fun sessionToSegment(
            session: SyncSessionRow,
            dayStartMs: Long,
            dayEndMs: Long
        ): TimelineSegment {
            val record = SyncSessionMapper.toUsageRecord(session)
            val clippedStart = record.startTimeUTC.coerceAtLeast(dayStartMs)
            val clippedEnd = record.endTimeUTC.coerceAtMost(dayEndMs)
            val title = session.title.ifBlank { session.appName }
            return TimelineSegment(
                id = record.id,
                startTimeMs = clippedStart,
                endTimeMs = clippedEnd,
                label = title,
                subtitle = session.processName.ifBlank { session.appName },
                category = session.appCategory.ifBlank { "Unknown" },
                appName = session.processName.ifBlank { session.appName },
                deviceName = session.deviceName.ifBlank { "Unknown Device" },
                url = session.url.ifBlank { null },
                durationSeconds = ((clippedEnd - clippedStart) / 1000).coerceAtLeast(0)
            )
        }
    }
}

sealed class SyncResult {
    data class Success(val uploaded: Int, val pulled: Int, val remaining: Int) : SyncResult()
    data class Skipped(val reason: String) : SyncResult()
    data class Failure(val message: String) : SyncResult()
}

sealed class ConnectionResult {
    data object Healthy : ConnectionResult()
    data class Degraded(val message: String) : ConnectionResult()
    data class Unreachable(val message: String) : ConnectionResult()
}

sealed class SessionsFetchResult {
    data class Success(val response: InsightsSessionsResponse) : SessionsFetchResult()
    data class Skipped(val reason: String) : SessionsFetchResult()
    data class Failure(val message: String) : SessionsFetchResult()
}

sealed class DevicesFetchResult {
    data class Success(val response: DeviceListResponse) : DevicesFetchResult()
    data class Skipped(val reason: String) : DevicesFetchResult()
    data class Failure(val message: String) : DevicesFetchResult()
}

sealed class TodayInsightsFetchResult {
    data class Success(val response: InsightsTodayResponse) : TodayInsightsFetchResult()
    data class Skipped(val reason: String) : TodayInsightsFetchResult()
    data class Failure(val message: String) : TodayInsightsFetchResult()
}

sealed class HeartbeatResult {
    data class Success(val status: DeviceStatusRow) : HeartbeatResult()
    data class Skipped(val reason: String) : HeartbeatResult()
    data class Failure(val message: String) : HeartbeatResult()
}

sealed class DeviceStatusFetchResult {
    data class Success(val response: DeviceStatusListResponse) : DeviceStatusFetchResult()
    data class Skipped(val reason: String) : DeviceStatusFetchResult()
    data class Failure(val message: String) : DeviceStatusFetchResult()
}
