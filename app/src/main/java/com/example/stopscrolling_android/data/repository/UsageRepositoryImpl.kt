package com.example.stopscrolling_android.data.repository

import com.example.stopscrolling_android.data.database.UsageDao
import com.example.stopscrolling_android.data.database.UsageRecord
import com.example.stopscrolling_android.domain.repository.UsageRepository
import com.example.stopscrolling_android.worker.UploadScheduler
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UsageRepositoryImpl @Inject constructor(
    private val usageDao: UsageDao,
    private val uploadScheduler: UploadScheduler
) : UsageRepository {
    override fun getAllRecords(): Flow<List<UsageRecord>> = usageDao.getAllRecords()

    override suspend fun saveRecord(record: UsageRecord) {
        usageDao.insertRecord(record)
        uploadScheduler.scheduleUpload()
    }

    override suspend fun saveRecords(records: List<UsageRecord>) {
        if (records.isEmpty()) return
        usageDao.insertRecords(records)
        uploadScheduler.scheduleUpload()
    }

    override suspend fun clearData() {
        usageDao.clearAllRecords()
    }
}
