package com.example.stopscrolling_android.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.stopscrolling_android.domain.repository.UsageRepository
import com.example.stopscrolling_android.usage.UsageStatsCollector
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class UsageCollectionWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val usageStatsCollector: UsageStatsCollector,
    private val repository: UsageRepository
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val endTime = System.currentTimeMillis()
        // Query for the last hour
        val startTime = endTime - (60 * 60 * 1000)
        
        val records = usageStatsCollector.queryUsageEvents(startTime, endTime)
        repository.saveRecords(records)
        
        return Result.success()
    }
}
