package com.example.stopscrolling_android.startup

import com.example.stopscrolling_android.data.auth.AuthTokenStore
import com.example.stopscrolling_android.data.sync.BackendSyncService
import com.example.stopscrolling_android.domain.repository.AuthRepository
import com.example.stopscrolling_android.worker.UploadScheduler
import com.example.stopscrolling_android.worker.HeartbeatScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppStartupRunner @Inject constructor(
    private val authRepository: AuthRepository,
    private val backendSyncService: BackendSyncService,
    private val tokenStore: AuthTokenStore,
    private val uploadScheduler: UploadScheduler,
    private val heartbeatScheduler: HeartbeatScheduler
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun runOnAppStart() {
        scope.launch {
            registerDeviceIfSignedIn(restoreSession = true)
        }
    }

    suspend fun registerDeviceIfSignedIn(restoreSession: Boolean = false) {
        if (restoreSession) {
            authRepository.restoreSession()
        }
        if (!tokenStore.hasTokens()) return
        backendSyncService.ensureDeviceRegistered()
        backendSyncService.postHeartbeat()
        uploadScheduler.scheduleUploadImmediate()
        heartbeatScheduler.scheduleHeartbeatImmediate()
    }
}
