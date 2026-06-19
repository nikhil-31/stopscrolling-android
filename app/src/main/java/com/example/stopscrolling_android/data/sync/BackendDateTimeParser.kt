package com.example.stopscrolling_android.data.sync

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

object BackendDateTimeParser {
    private val localDateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME

    fun parseToEpochMilli(value: String): Long {
        val trimmed = value.trim()
        if (trimmed.isEmpty()) return 0L

        return try {
            Instant.parse(trimmed).toEpochMilli()
        } catch (_: DateTimeParseException) {
            LocalDateTime.parse(trimmed, localDateTimeFormatter)
                .atZone(ZoneOffset.UTC)
                .toInstant()
                .toEpochMilli()
        }
    }
}
