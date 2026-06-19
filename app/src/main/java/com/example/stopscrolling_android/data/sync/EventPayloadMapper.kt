package com.example.stopscrolling_android.data.sync

import com.example.stopscrolling_android.data.device.DeviceInfo
import com.example.stopscrolling_android.data.database.UsageRecord
import com.example.stopscrolling_android.data.remote.dto.ScreenTimeEventPayload
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.TimeZone

object EventPayloadMapper {
    private val isoFormatter = DateTimeFormatter.ISO_INSTANT
    private const val TRACKING_TEXT_MAX_LENGTH = 4096

    fun fromRecord(record: UsageRecord): ScreenTimeEventPayload {
        val startedAt = isoFormatter.format(
            Instant.ofEpochMilli(record.startTimeUTC).atOffset(ZoneOffset.UTC)
        )
        val recordedAt = isoFormatter.format(
            Instant.ofEpochMilli(record.endTimeUTC).atOffset(ZoneOffset.UTC)
        )
        val sessionTitle = record.title?.trim().orEmpty()
        val sessionUrl = record.url?.trim().orEmpty()
        return ScreenTimeEventPayload(
            recordedAt = recordedAt,
            devicePlatform = clip(DeviceInfo.PLATFORM, 64),
            deviceName = clip(DeviceInfo.deviceName(), TRACKING_TEXT_MAX_LENGTH),
            appName = clip(displayAppName(record), TRACKING_TEXT_MAX_LENGTH),
            appCategory = clip(record.category, TRACKING_TEXT_MAX_LENGTH),
            appBundleId = clip(record.packageName, 255),
            durationSeconds = record.durationSeconds.coerceAtMost(Int.MAX_VALUE.toLong()).toInt(),
            foregroundState = "foreground",
            startedAt = startedAt,
            sessionTitle = clip(sessionTitle, TRACKING_TEXT_MAX_LENGTH),
            sessionUrl = clip(sessionUrl, TRACKING_TEXT_MAX_LENGTH),
            processName = clip(record.appName, TRACKING_TEXT_MAX_LENGTH),
            timeZone = clip(DeviceInfo.timeZone(), 64)
        )
    }

    fun fromRecords(records: List<UsageRecord>): List<ScreenTimeEventPayload> {
        return records.map(::fromRecord)
    }

    private fun displayAppName(record: UsageRecord): String {
        val title = record.title?.trim().orEmpty()
        if (title.isNotEmpty() && title != record.appName) return title
        return record.appName
    }

    private fun clip(value: String, maxLength: Int): String {
        return if (value.length <= maxLength) value else value.take(maxLength)
    }
}
