package com.example.stopscrolling_android.worker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.AndroidEntryPoint
import java.util.concurrent.TimeUnit

@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            scheduleUsageCollection(context)
        }
    }

        companion object {
        fun scheduleUsageCollection(context: Context) {
            val networkConstraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val collectionRequest = PeriodicWorkRequestBuilder<UsageCollectionWorker>(15, TimeUnit.MINUTES)
                .setConstraints(networkConstraints)
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "UsageCollection",
                ExistingPeriodicWorkPolicy.KEEP,
                collectionRequest
            )

            val syncRequest = PeriodicWorkRequestBuilder<SyncWorker>(15, TimeUnit.MINUTES)
                .setConstraints(networkConstraints)
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "BackendSync",
                ExistingPeriodicWorkPolicy.KEEP,
                syncRequest
            )

            val heartbeatRequest = OneTimeWorkRequestBuilder<HeartbeatWorker>()
                .setConstraints(networkConstraints)
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                "BackendHeartbeat",
                ExistingWorkPolicy.REPLACE,
                heartbeatRequest
            )
        }
    }
}
