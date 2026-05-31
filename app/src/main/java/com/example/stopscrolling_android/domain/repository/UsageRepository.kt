package com.example.stopscrolling_android.domain.repository

import com.example.stopscrolling_android.data.database.UsageRecord
import kotlinx.coroutines.flow.Flow

interface UsageRepository {
    fun getAllRecords(): Flow<List<UsageRecord>>
    fun getRecordsSince(since: Long): Flow<List<UsageRecord>>
    fun getRecordsInDateRange(startTime: Long, endTime: Long): Flow<List<UsageRecord>>
    suspend fun saveRecord(record: UsageRecord)
    suspend fun saveRecords(records: List<UsageRecord>)
    suspend fun clearData()
}
