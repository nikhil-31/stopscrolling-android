package com.example.stopscrolling_android.presentation.timeline

import com.example.stopscrolling_android.data.database.UsageRecord

data class TimelineSegment(
    val id: String,
    val startTimeMs: Long,
    val endTimeMs: Long,
    val label: String,
    val subtitle: String,
    val category: String,
    val appName: String,
    val deviceName: String,
    val url: String? = null,
    val durationSeconds: Long
) {
    fun fraction(dayStartMs: Long, dayEndMs: Long): SegmentFraction {
        val dayDuration = (dayEndMs - dayStartMs).toFloat()
        if (dayDuration <= 0f) return SegmentFraction(0f, 0f)
        val x = ((startTimeMs - dayStartMs) / dayDuration).coerceIn(0f, 1f)
        val width = ((endTimeMs - startTimeMs) / dayDuration).coerceIn(0f, 1f - x)
        return SegmentFraction(x, width)
    }
}

data class SegmentFraction(val x: Float, val width: Float)

object TimelineModel {
    val dayGridHours = listOf(0, 6, 12, 18, 24)

    fun dayBounds(referenceMs: Long = System.currentTimeMillis()): Pair<Long, Long> {
        val calendar = java.util.Calendar.getInstance()
        calendar.timeInMillis = referenceMs
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
        calendar.set(java.util.Calendar.MINUTE, 0)
        calendar.set(java.util.Calendar.SECOND, 0)
        calendar.set(java.util.Calendar.MILLISECOND, 0)
        val start = calendar.timeInMillis
        calendar.add(java.util.Calendar.DAY_OF_MONTH, 1)
        val end = calendar.timeInMillis
        return start to end
    }

    fun formatDate(referenceMs: Long = System.currentTimeMillis()): String {
        val formatter = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
        return formatter.format(java.util.Date(referenceMs))
    }

    fun recordsForDay(records: List<UsageRecord>, dayStartMs: Long, dayEndMs: Long): List<UsageRecord> {
        return records.filter { record ->
            record.endTimeUTC > dayStartMs && record.startTimeUTC < dayEndMs
        }
    }

    fun toSegments(records: List<UsageRecord>, dayStartMs: Long, dayEndMs: Long): List<TimelineSegment> {
        return recordsForDay(records, dayStartMs, dayEndMs)
            .sortedBy { it.startTimeUTC }
            .map { record ->
                val clippedStart = record.startTimeUTC.coerceAtLeast(dayStartMs)
                val clippedEnd = record.endTimeUTC.coerceAtMost(dayEndMs)
                TimelineSegment(
                    id = record.id,
                    startTimeMs = clippedStart,
                    endTimeMs = clippedEnd,
                    label = displayLabel(record),
                    subtitle = record.appName,
                    category = record.category,
                    appName = record.appName,
                    deviceName = record.deviceName,
                    url = record.url,
                    durationSeconds = ((clippedEnd - clippedStart) / 1000).coerceAtLeast(0)
                )
            }
            .filter { it.durationSeconds > 0 }
    }

    /**
     * Merges server segments with locally-buffered (outbox) segments, preferring the
     * server copy when the same session appears in both. Lets freshly captured
     * activity show before it has been uploaded without double-counting.
     */
    fun mergeSegments(
        primary: List<TimelineSegment>,
        secondary: List<TimelineSegment>
    ): List<TimelineSegment> {
        val seen = HashSet<String>()
        val merged = ArrayList<TimelineSegment>(primary.size + secondary.size)
        for (segment in primary + secondary) {
            val key = "${segment.startTimeMs / 1000}|${segment.appName}|${segment.deviceName}"
            if (seen.add(key)) merged.add(segment)
        }
        return merged.sortedBy { it.startTimeMs }
    }

    /** Same de-duplication as [mergeSegments] but for raw records (Timeline list). */
    fun mergeRecords(
        primary: List<UsageRecord>,
        secondary: List<UsageRecord>
    ): List<UsageRecord> {
        val seen = HashSet<String>()
        val merged = ArrayList<UsageRecord>(primary.size + secondary.size)
        for (record in primary + secondary) {
            val key = "${record.startTimeUTC / 1000}|${record.appName}|${record.deviceName}"
            if (seen.add(key)) merged.add(record)
        }
        return merged.sortedByDescending { it.startTimeUTC }
    }

    fun hourLabel(hour: Int): String {
        return when {
            hour == 0 || hour == 24 -> "12a"
            hour < 12 -> "${hour}a"
            hour == 12 -> "12p"
            else -> "${hour - 12}p"
        }
    }

    fun formatTime(timeMs: Long): String {
        val formatter = java.text.SimpleDateFormat("h:mm a", java.util.Locale.getDefault())
        return formatter.format(java.util.Date(timeMs))
    }

    fun formatDuration(seconds: Long): String {
        val h = seconds / 3600
        val m = (seconds % 3600) / 60
        val s = seconds % 60
        return when {
            h > 0 -> "${h}h ${m}m"
            m > 0 -> "${m}m ${s}s"
            else -> "${s}s"
        }
    }

    private fun displayLabel(record: UsageRecord): String {
        val title = record.title?.trim().orEmpty()
        return if (title.isNotEmpty() && title != record.appName) title else record.appName
    }
}
