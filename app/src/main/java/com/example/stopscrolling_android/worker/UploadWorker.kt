package com.example.stopscrolling_android.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.stopscrolling_android.data.sync.BackendSyncService
import com.example.stopscrolling_android.data.sync.SyncResult
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class UploadWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val backendSyncService: BackendSyncService
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return when (val syncResult = backendSyncService.syncUnsyncedRecords()) {
            is SyncResult.Success -> Result.success()
            is SyncResult.Skipped -> Result.success()
            is SyncResult.Failure -> Result.retry()
        }
    }
}
