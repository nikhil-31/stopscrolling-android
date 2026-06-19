package com.example.stopscrolling_android.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ScreenTimeEventPayload(
    @SerialName("recorded_at") val recordedAt: String,
    @SerialName("device_platform") val devicePlatform: String,
    @SerialName("device_name") val deviceName: String,
    @SerialName("app_name") val appName: String,
    @SerialName("app_category") val appCategory: String,
    @SerialName("app_bundle_id") val appBundleId: String,
    @SerialName("duration_seconds") val durationSeconds: Int,
    @SerialName("foreground_state") val foregroundState: String = "foreground",
    @SerialName("started_at") val startedAt: String,
    @SerialName("session_title") val sessionTitle: String = "",
    @SerialName("session_url") val sessionUrl: String = "",
    @SerialName("process_name") val processName: String = "",
    @SerialName("time_zone") val timeZone: String = ""
)

@Serializable
data class BulkEventsRequest(
    val events: List<ScreenTimeEventPayload>
)

@Serializable
data class BulkInsertResponse(
    val inserted: Int
)

@Serializable
data class HealthResponse(
    val status: String,
    val clickhouse: Boolean
)
