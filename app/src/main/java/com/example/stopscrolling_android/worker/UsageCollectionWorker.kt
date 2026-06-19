package com.example.stopscrolling_android.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.stopscrolling_android.data.sync.BackendSyncService
import com.example.stopscrolling_android.data.sync.SyncResult
import com.example.stopscrolling_android.domain.repository.UsageRepository
import com.example.stopscrolling_android.usage.UsageStatsCollector
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class UsageCollectionWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val usageStatsCollector: UsageStatsCollector,
    private val repository: UsageRepository,
    private val backendSyncService: BackendSyncService
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val endTime = System.currentTimeMillis()
        val startTime = endTime - (60 * 60 * 1000)

        val records = usageStatsCollector.queryUsageEvents(startTime, endTime)
        repository.saveRecords(records)

        return when (backendSyncService.syncUnsyncedRecords()) {
            is SyncResult.Success -> Result.success()
            is SyncResult.Skipped -> Result.success()
            is SyncResult.Failure -> Result.retry()
        }
    }
}
