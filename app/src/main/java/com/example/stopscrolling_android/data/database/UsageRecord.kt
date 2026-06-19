package com.example.stopscrolling_android.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "usage_records")
data class UsageRecord(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val startTimeUTC: Long, // Storing as Long for simplicity with Room, can use Converter for Instant
    val endTimeUTC: Long,
    val title: String?,
    val url: String?,
    val appName: String,
    val packageName: String,
    val category: String,
    val platform: String = "Android",
    val deviceName: String = "This Device",
    val durationSeconds: Long,
    val source: String, // "UsageStats" or "Accessibility"
    val isSynced: Boolean = false
)
