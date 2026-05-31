package com.example.stopscrolling_android.worker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.ExistingPeriodicWorkPolicy
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
            val workRequest = PeriodicWorkRequestBuilder<UsageCollectionWorker>(15, TimeUnit.MINUTES)
                .build()
            
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "UsageCollection",
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest
            )
        }
    }
}
