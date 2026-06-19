package com.example.stopscrolling_android.data.auth

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.example.stopscrolling_android.data.remote.dto.AuthTokens
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthTokenStore @Inject constructor(
    @ApplicationContext context: Context
) {
    private val json = Json { ignoreUnknownKeys = true }

    private val prefs: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        "auth_tokens",
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun save(tokens: AuthTokens) {
        prefs.edit()
            .putString(KEY_TOKENS, json.encodeToString(tokens))
            .apply()
    }

    fun load(): AuthTokens? {
        val raw = prefs.getString(KEY_TOKENS, null) ?: return null
        return try {
            json.decodeFromString<AuthTokens>(raw)
        } catch (_: Exception) {
            null
        }
    }

    fun clear() {
        prefs.edit().remove(KEY_TOKENS).apply()
    }

    fun hasTokens(): Boolean = load() != null

    companion object {
        private const val KEY_TOKENS = "tokens"
    }
}
