package com.example.stopscrolling_android.data.settings

import com.example.stopscrolling_android.data.remote.BackendApiUrl

data class BackendSettings(
    val apiBaseUrl: String = BackendApiUrl.DEFAULT_API_BASE_URL,
    val syncEnabled: Boolean = true,
    val enhancedTrackingEnabled: Boolean = true,
    val syncEventsThrough: String? = null,
    val registeredDeviceId: String? = null
) {
    val isConfigured: Boolean = apiBaseUrl.trim().isNotEmpty()

    fun isReadyForSync(hasAuthTokens: Boolean): Boolean {
        return syncEnabled && isConfigured && hasAuthTokens
    }
}
