package com.example.stopscrolling_android.data.sync

import com.example.stopscrolling_android.data.database.UsageRecord
import com.example.stopscrolling_android.data.remote.dto.SyncSessionRow
import java.util.UUID

object SyncSessionMapper {
    fun toUsageRecord(session: SyncSessionRow): UsageRecord {
        val startMs = BackendDateTimeParser.parseToEpochMilli(session.startedAt)
        val endMs = BackendDateTimeParser.parseToEpochMilli(session.endedAt)
        val processName = session.processName.ifBlank { session.appName }
        val title = session.title.ifBlank { null }
        val url = session.url.ifBlank { null }
        return UsageRecord(
            id = stableId(session),
            startTimeUTC = startMs,
            endTimeUTC = endMs,
            title = title,
            url = url,
            appName = processName,
            packageName = session.appBundleId,
            category = session.appCategory.ifBlank { "Unknown" },
            platform = session.devicePlatform.ifBlank { "android" },
            deviceName = session.deviceName.ifBlank { "Unknown Device" },
            durationSeconds = session.durationSeconds.toLong().coerceAtLeast(0),
            source = "BackendSync",
            isSynced = true
        )
    }

    private fun stableId(session: SyncSessionRow): String {
        val key = listOf(
            session.startedAt,
            session.appBundleId,
            session.devicePlatform,
            session.durationSeconds.toString()
        ).joinToString("|")
        return UUID.nameUUIDFromBytes(key.toByteArray()).toString()
    }
}
