package com.example.stopscrolling_android.data.remote

import com.example.stopscrolling_android.data.remote.dto.DeviceListResponse
import com.example.stopscrolling_android.data.remote.dto.SyncPullResponse
import com.example.stopscrolling_android.data.remote.dto.SyncStatusResponse
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncApiClient @Inject constructor(
    private val http: HttpJsonClient
) {
    suspend fun fetchSyncStatus(
        baseUrl: String,
        accessToken: String,
        deviceId: String? = null
    ): SyncStatusResponse {
        val query = buildDeviceQuery(deviceId)
        return http.get(
            baseUrl = baseUrl,
            path = "api/sync/status/",
            accessToken = accessToken,
            queryParams = query
        )
    }

    suspend fun pullSync(
        baseUrl: String,
        accessToken: String,
        eventsSince: String? = null,
        sessionsLimit: Int = DEFAULT_SESSIONS_LIMIT,
        includeProfile: Boolean = false,
        deviceId: String? = null
    ): SyncPullResponse {
        val query = buildMap {
            put("include_profile", includeProfile.toString())
            put("sessions_limit", sessionsLimit.coerceIn(1, 1000).toString())
            if (!eventsSince.isNullOrBlank()) {
                put("events_since", eventsSince)
            }
            if (!deviceId.isNullOrBlank()) {
                put("device_id", deviceId)
            }
        }
        return http.get(
            baseUrl = baseUrl,
            path = "api/sync/",
            accessToken = accessToken,
            queryParams = query
        )
    }

    suspend fun fetchDevices(baseUrl: String, accessToken: String): DeviceListResponse {
        return http.get(
            baseUrl = baseUrl,
            path = "api/devices/",
            accessToken = accessToken
        )
    }

    suspend fun pullSyncWithRetry(
        baseUrl: String,
        accessToken: String,
        eventsSince: String? = null,
        sessionsLimit: Int = DEFAULT_SESSIONS_LIMIT,
        refreshAccessToken: suspend () -> String?,
        deviceId: String? = null
    ): SyncPullResponse {
        return try {
            pullSync(baseUrl, accessToken, eventsSince, sessionsLimit, deviceId = deviceId)
        } catch (e: ApiException.HttpError) {
            if (e.code == 401) {
                val refreshed = refreshAccessToken()
                    ?: throw ApiException.MissingAccessToken()
                pullSync(baseUrl, refreshed, eventsSince, sessionsLimit, deviceId = deviceId)
            } else {
                throw e
            }
        }
    }

    suspend fun fetchSyncStatusWithRetry(
        baseUrl: String,
        accessToken: String,
        refreshAccessToken: suspend () -> String?,
        deviceId: String? = null
    ): SyncStatusResponse {
        return try {
            fetchSyncStatus(baseUrl, accessToken, deviceId)
        } catch (e: ApiException.HttpError) {
            if (e.code == 401) {
                val refreshed = refreshAccessToken()
                    ?: throw ApiException.MissingAccessToken()
                fetchSyncStatus(baseUrl, refreshed, deviceId)
            } else {
                throw e
            }
        }
    }

    private fun buildDeviceQuery(deviceId: String?): Map<String, String> {
        return if (deviceId.isNullOrBlank()) {
            emptyMap()
        } else {
            mapOf("device_id" to deviceId)
        }
    }

    companion object {
        const val DEFAULT_SESSIONS_LIMIT = 200
    }
}
