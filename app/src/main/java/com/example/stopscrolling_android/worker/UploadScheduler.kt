package com.example.stopscrolling_android.worker

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UploadScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun scheduleUpload() {
        enqueueUpload(delaySeconds = UPLOAD_DEBOUNCE_SECONDS)
    }

    fun scheduleUploadImmediate() {
        enqueueUpload(delaySeconds = 0)
    }

    private fun enqueueUpload(delaySeconds: Long) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val requestBuilder = OneTimeWorkRequestBuilder<UploadWorker>()
            .setConstraints(constraints)

        if (delaySeconds > 0) {
            requestBuilder.setInitialDelay(delaySeconds, TimeUnit.SECONDS)
        }

        WorkManager.getInstance(context).enqueueUniqueWork(
            UPLOAD_WORK_NAME,
            ExistingWorkPolicy.KEEP,
            requestBuilder.build()
        )
    }

    companion object {
        private const val UPLOAD_WORK_NAME = "BackendUpload"
        private const val UPLOAD_DEBOUNCE_SECONDS = 5L
    }
}
