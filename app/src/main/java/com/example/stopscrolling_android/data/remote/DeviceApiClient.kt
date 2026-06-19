package com.example.stopscrolling_android.data.remote

import com.example.stopscrolling_android.data.remote.dto.DeviceRegisterRequest
import com.example.stopscrolling_android.data.remote.dto.DeviceRow
import com.example.stopscrolling_android.data.remote.dto.DeviceListResponse
import com.example.stopscrolling_android.data.remote.dto.InsightsSessionsResponse
import com.example.stopscrolling_android.data.remote.dto.InsightsTimelineResponse
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeviceApiClient @Inject constructor(
    private val http: HttpJsonClient
) {
    suspend fun registerDevice(
        baseUrl: String,
        accessToken: String,
        request: DeviceRegisterRequest
    ): DeviceRow {
        return http.post(
            baseUrl = baseUrl,
            path = "api/devices/",
            body = request,
            accessToken = accessToken
        )
    }

    suspend fun fetchDevices(
        baseUrl: String,
        accessToken: String
    ): DeviceListResponse {
        return http.get(
            baseUrl = baseUrl,
            path = "api/devices/",
            accessToken = accessToken
        )
    }

    suspend fun fetchTimeline(
        baseUrl: String,
        accessToken: String,
        deviceId: String,
        day: String
    ): InsightsTimelineResponse {
        return http.get(
            baseUrl = baseUrl,
            path = "api/insights/timeline/",
            accessToken = accessToken,
            queryParams = mapOf(
                "device_id" to deviceId,
                "day" to day
            )
        )
    }

    suspend fun fetchSessions(
        baseUrl: String,
        accessToken: String,
        startIso: String,
        endIso: String
    ): InsightsSessionsResponse {
        return http.get(
            baseUrl = baseUrl,
            path = "api/insights/sessions/",
            accessToken = accessToken,
            queryParams = mapOf(
                "start" to startIso,
                "end" to endIso
            )
        )
    }

    suspend fun fetchSessionsWithRetry(
        baseUrl: String,
        accessToken: String,
        startIso: String,
        endIso: String,
        refreshAccessToken: suspend () -> String?
    ): InsightsSessionsResponse {
        return try {
            fetchSessions(baseUrl, accessToken, startIso, endIso)
        } catch (e: ApiException.HttpError) {
            if (e.code == 401) {
                val refreshed = refreshAccessToken()
                    ?: throw ApiException.MissingAccessToken()
                fetchSessions(baseUrl, refreshed, startIso, endIso)
            } else {
                throw e
            }
        }
    }

    suspend fun fetchDevicesWithRetry(
        baseUrl: String,
        accessToken: String,
        refreshAccessToken: suspend () -> String?
    ): DeviceListResponse {
        return try {
            fetchDevices(baseUrl, accessToken)
        } catch (e: ApiException.HttpError) {
            if (e.code == 401) {
                val refreshed = refreshAccessToken()
                    ?: throw ApiException.MissingAccessToken()
                fetchDevices(baseUrl, refreshed)
            } else {
                throw e
            }
        }
    }

    suspend fun registerDeviceWithRetry(
        baseUrl: String,
        accessToken: String,
        request: DeviceRegisterRequest,
        refreshAccessToken: suspend () -> String?
    ): DeviceRow {
        return try {
            registerDevice(baseUrl, accessToken, request)
        } catch (e: ApiException.HttpError) {
            if (e.code == 401) {
                val refreshed = refreshAccessToken()
                    ?: throw ApiException.MissingAccessToken()
                registerDevice(baseUrl, refreshed, request)
            } else {
                throw e
            }
        }
    }

    suspend fun fetchTimelineWithRetry(
        baseUrl: String,
        accessToken: String,
        deviceId: String,
        day: String,
        refreshAccessToken: suspend () -> String?
    ): InsightsTimelineResponse {
        return try {
            fetchTimeline(baseUrl, accessToken, deviceId, day)
        } catch (e: ApiException.HttpError) {
            if (e.code == 401) {
                val refreshed = refreshAccessToken()
                    ?: throw ApiException.MissingAccessToken()
                fetchTimeline(baseUrl, refreshed, deviceId, day)
            } else {
                throw e
            }
        }
    }
}
