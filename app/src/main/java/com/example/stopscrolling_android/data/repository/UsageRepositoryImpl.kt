package com.example.stopscrolling_android.data.repository

import com.example.stopscrolling_android.data.database.UsageDao
import com.example.stopscrolling_android.data.database.UsageRecord
import com.example.stopscrolling_android.domain.repository.UsageRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UsageRepositoryImpl @Inject constructor(
    private val usageDao: UsageDao
) : UsageRepository {
    override fun getAllRecords(): Flow<List<UsageRecord>> = usageDao.getAllRecords()

    override fun getRecordsSince(since: Long): Flow<List<UsageRecord>> = usageDao.getRecordsSince(since)

    override fun getRecordsInDateRange(startTime: Long, endTime: Long): Flow<List<UsageRecord>> =
        usageDao.getRecordsInDateRange(startTime, endTime)

    override suspend fun saveRecord(record: UsageRecord) {
        usageDao.insertRecord(record)
    }

    override suspend fun saveRecords(records: List<UsageRecord>) {
        usageDao.insertRecords(records)
    }

    override suspend fun clearData() {
        usageDao.clearAllRecords()
    }
}
