package com.example.stopscrolling_android.domain.repository

import com.example.stopscrolling_android.data.database.UsageRecord
import kotlinx.coroutines.flow.Flow

interface UsageRepository {
    /** Pending outbox rows (captured but not yet uploaded), newest first. */
    fun getAllRecords(): Flow<List<UsageRecord>>
    suspend fun saveRecord(record: UsageRecord)
    suspend fun saveRecords(records: List<UsageRecord>)
    suspend fun clearData()
}
