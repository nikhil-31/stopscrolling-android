package com.example.stopscrolling_android.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SyncStatusResponse(
    @SerialName("daily_through") val dailyThrough: String? = null,
    @SerialName("hourly_through") val hourlyThrough: String? = null,
    @SerialName("events_through") val eventsThrough: String? = null,
    @SerialName("daily_fingerprint") val dailyFingerprint: Int = 0,
    @SerialName("hourly_fingerprint") val hourlyFingerprint: Int = 0,
    @SerialName("events_fingerprint") val eventsFingerprint: Int = 0
)

@Serializable
data class SyncCursor(
    @SerialName("daily_through") val dailyThrough: String? = null,
    @SerialName("hourly_through") val hourlyThrough: String? = null,
    @SerialName("events_through") val eventsThrough: String? = null,
    @SerialName("daily_fingerprint") val dailyFingerprint: Int = 0,
    @SerialName("hourly_fingerprint") val hourlyFingerprint: Int = 0,
    @SerialName("events_fingerprint") val eventsFingerprint: Int = 0
)

@Serializable
data class SyncSessionRow(
    @SerialName("started_at") val startedAt: String,
    @SerialName("ended_at") val endedAt: String,
    val title: String = "",
    val url: String = "",
    @SerialName("process_name") val processName: String = "",
    @SerialName("app_name") val appName: String = "",
    @SerialName("app_category") val appCategory: String = "",
    @SerialName("app_bundle_id") val appBundleId: String = "",
    @SerialName("device_platform") val devicePlatform: String = "",
    @SerialName("device_name") val deviceName: String = "",
    @SerialName("duration_seconds") val durationSeconds: Int = 0,
    @SerialName("time_zone") val timeZone: String = ""
)

@Serializable
data class SyncPullResponse(
    val cursor: SyncCursor,
    @SerialName("overlap_day") val overlapDay: String? = null,
    @SerialName("overlap_events_since") val overlapEventsSince: String? = null,
    @SerialName("daily_usage") val dailyUsage: List<SyncDailyUsageRow> = emptyList(),
    @SerialName("hourly_usage") val hourlyUsage: List<SyncHourlyUsageRow> = emptyList(),
    val sessions: List<SyncSessionRow> = emptyList(),
    @SerialName("has_more_sessions") val hasMoreSessions: Boolean = false,
    val profile: SyncProfile? = null
)

@Serializable
data class SyncDailyUsageRow(
    val day: String,
    @SerialName("app_name") val appName: String,
    @SerialName("app_category") val appCategory: String,
    @SerialName("total_seconds") val totalSeconds: Int,
    @SerialName("session_count") val sessionCount: Int
)

@Serializable
data class SyncHourlyUsageRow(
    val hour: String,
    @SerialName("device_platform") val devicePlatform: String,
    @SerialName("total_seconds") val totalSeconds: Int
)

@Serializable
data class SyncProfile(
    val theme: String,
    @SerialName("tracking_id") val trackingId: String
)

@Serializable
data class DeviceRegisterRequest(
    @SerialName("device_platform") val devicePlatform: String,
    @SerialName("device_name") val deviceName: String,
    val label: String = "",
    @SerialName("time_zone") val timeZone: String = ""
)

@Serializable
data class DeviceRow(
    @SerialName("device_id") val deviceId: String,
    @SerialName("device_platform") val devicePlatform: String,
    @SerialName("device_name") val deviceName: String,
    val label: String = "",
    @SerialName("time_zone") val timeZone: String = "",
    @SerialName("registered_at") val registeredAt: String,
    @SerialName("updated_at") val updatedAt: String,
    @SerialName("last_seen_at") val lastSeenAt: String? = null,
    @SerialName("session_count") val sessionCount: Int = 0
)

@Serializable
data class DeviceListResponse(
    val count: Int,
    val results: List<DeviceRow>
)

@Serializable
data class InsightsTimelineResponse(
    @SerialName("device_id") val deviceId: String,
    @SerialName("device_platform") val devicePlatform: String,
    @SerialName("device_name") val deviceName: String,
    val day: String,
    @SerialName("time_zone") val timeZone: String = "",
    val count: Int,
    val sessions: List<SyncSessionRow> = emptyList()
)

/**
 * Response of `GET /api/insights/sessions/?start=&end=` — all foreground sessions
 * across every device of the signed-in user overlapping the requested window. This
 * is the single source the app renders timelines and insights from now that local
 * history is ephemeral.
 */
@Serializable
data class InsightsSessionsResponse(
    val start: String = "",
    val end: String = "",
    val count: Int = 0,
    val sessions: List<SyncSessionRow> = emptyList()
)
