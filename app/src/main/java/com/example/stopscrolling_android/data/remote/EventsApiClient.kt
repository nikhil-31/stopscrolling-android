package com.example.stopscrolling_android.data.remote

import com.example.stopscrolling_android.data.remote.dto.BulkEventsRequest
import com.example.stopscrolling_android.data.remote.dto.BulkInsertResponse
import com.example.stopscrolling_android.data.remote.dto.ScreenTimeEventPayload
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EventsApiClient @Inject constructor(
    private val http: HttpJsonClient
) {
    suspend fun postEventsBulk(
        events: List<ScreenTimeEventPayload>,
        baseUrl: String,
        accessToken: String
    ): BulkInsertResponse {
        if (events.isEmpty()) return BulkInsertResponse(inserted = 0)
        return http.post(
            baseUrl = baseUrl,
            path = "api/events/bulk/",
            body = BulkEventsRequest(events),
            accessToken = accessToken
        )
    }

    suspend fun postEventsBulkWithRetry(
        events: List<ScreenTimeEventPayload>,
        baseUrl: String,
        accessToken: String,
        refreshAccessToken: suspend () -> String?
    ): BulkInsertResponse {
        return try {
            postEventsBulk(events, baseUrl, accessToken)
        } catch (e: ApiException.HttpError) {
            if (e.code == 401) {
                val refreshed = refreshAccessToken()
                    ?: throw ApiException.MissingAccessToken()
                postEventsBulk(events, baseUrl, refreshed)
            } else {
                throw e
            }
        }
    }
}
