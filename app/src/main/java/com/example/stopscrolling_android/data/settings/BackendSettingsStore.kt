package com.example.stopscrolling_android.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.backendDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "backend_settings"
)

@Singleton
class BackendSettingsStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore = context.backendDataStore

    val settings: Flow<BackendSettings> = dataStore.data.map { prefs ->
        BackendSettings(
            apiBaseUrl = prefs[KEY_API_BASE_URL] ?: BackendSettings().apiBaseUrl,
            syncEnabled = prefs[KEY_SYNC_ENABLED] ?: true,
            enhancedTrackingEnabled = prefs[KEY_ENHANCED_TRACKING_ENABLED] ?: true,
            syncEventsThrough = prefs[KEY_SYNC_EVENTS_THROUGH],
            registeredDeviceId = prefs[KEY_REGISTERED_DEVICE_ID]
        )
    }

    suspend fun current(): BackendSettings = settings.first()

    suspend fun update(settings: BackendSettings) {
        dataStore.edit { prefs ->
            prefs[KEY_API_BASE_URL] = settings.apiBaseUrl.trim()
            prefs[KEY_SYNC_ENABLED] = settings.syncEnabled
            prefs[KEY_ENHANCED_TRACKING_ENABLED] = settings.enhancedTrackingEnabled
            if (settings.syncEventsThrough != null) {
                prefs[KEY_SYNC_EVENTS_THROUGH] = settings.syncEventsThrough
            } else {
                prefs.remove(KEY_SYNC_EVENTS_THROUGH)
            }
            if (settings.registeredDeviceId != null) {
                prefs[KEY_REGISTERED_DEVICE_ID] = settings.registeredDeviceId
            } else {
                prefs.remove(KEY_REGISTERED_DEVICE_ID)
            }
        }
    }

    suspend fun updateRegisteredDeviceId(deviceId: String?) {
        val current = current()
        update(current.copy(registeredDeviceId = deviceId))
    }

    suspend fun clearRegisteredDeviceId() {
        updateRegisteredDeviceId(null)
    }

    suspend fun updateSyncEventsThrough(eventsThrough: String?) {
        val current = current()
        update(current.copy(syncEventsThrough = eventsThrough))
    }

    companion object {
        private val KEY_API_BASE_URL = stringPreferencesKey("api_base_url")
        private val KEY_SYNC_ENABLED = booleanPreferencesKey("sync_enabled")
        private val KEY_ENHANCED_TRACKING_ENABLED = booleanPreferencesKey("enhanced_tracking_enabled")
        private val KEY_SYNC_EVENTS_THROUGH = stringPreferencesKey("sync_events_through")
        private val KEY_REGISTERED_DEVICE_ID = stringPreferencesKey("registered_device_id")
    }
}
