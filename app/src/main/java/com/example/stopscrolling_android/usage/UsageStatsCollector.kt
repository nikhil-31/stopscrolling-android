package com.example.stopscrolling_android.usage

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import com.example.stopscrolling_android.data.database.UsageRecord
import com.example.stopscrolling_android.data.device.DeviceInfo
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UsageStatsCollector @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

    fun queryUsageEvents(startTime: Long, endTime: Long): List<UsageRecord> {
        val events = usageStatsManager.queryEvents(startTime, endTime)
        val event = UsageEvents.Event()
        val records = mutableListOf<UsageRecord>()
        
        val appSessions = mutableMapOf<String, Long>() // PackageName to StartTime
        val excludedPackages = setOf("com.android.systemui", "android", context.packageName)

        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            val packageName = event.packageName
            if (packageName in excludedPackages) continue

            val timestamp = event.timeStamp

            when (event.eventType) {
                UsageEvents.Event.ACTIVITY_RESUMED -> {
                    appSessions[packageName] = timestamp
                }
                UsageEvents.Event.ACTIVITY_PAUSED -> {
                    val start = appSessions.remove(packageName)
                    if (start != null && timestamp > start) {
                        records.add(createRecord(packageName, start, timestamp))
                    }
                }
            }
        }
        return records
    }

    private fun createRecord(packageName: String, startTime: Long, endTime: Long): UsageRecord {
        val pm = context.packageManager
        val appLabel = try {
            val info = pm.getApplicationInfo(packageName, 0)
            pm.getApplicationLabel(info).toString()
        } catch (e: Exception) {
            packageName
        }

        return UsageRecord(
            startTimeUTC = startTime,
            endTimeUTC = endTime,
            title = null,
            url = null,
            appName = appLabel,
            packageName = packageName,
            category = CategoryMapper.getCategory(context, packageName),
            deviceName = DeviceInfo.deviceName(),
            durationSeconds = (endTime - startTime) / 1000,
            source = "UsageStats"
        )
    }
}
