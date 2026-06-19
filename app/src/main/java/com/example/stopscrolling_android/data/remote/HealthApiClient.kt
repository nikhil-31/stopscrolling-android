package com.example.stopscrolling_android.data.remote

import com.example.stopscrolling_android.data.remote.dto.HealthResponse
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HealthApiClient @Inject constructor(
    private val http: HttpJsonClient
) {
    suspend fun checkHealth(baseUrl: String): HealthResponse {
        return http.get(
            baseUrl = baseUrl,
            path = "api/health/"
        )
    }
}
