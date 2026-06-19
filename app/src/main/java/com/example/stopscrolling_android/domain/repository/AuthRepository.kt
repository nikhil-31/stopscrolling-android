package com.example.stopscrolling_android.domain.repository

import com.example.stopscrolling_android.data.remote.dto.AuthenticatedUser
import com.example.stopscrolling_android.data.remote.dto.MfaPendingResponse
import kotlinx.coroutines.flow.StateFlow

interface AuthRepository {
    val currentUser: StateFlow<AuthenticatedUser?>
    val mfaChallenge: StateFlow<MfaPendingResponse?>

    suspend fun restoreSession(): Result<Unit>
    suspend fun register(email: String, password: String, phoneNumber: String?): Result<Unit>
    suspend fun login(email: String, password: String): Result<MfaPendingResponse>
    suspend fun verifyMfa(code: String, backupCode: String?, useBackupCode: Boolean): Result<Unit>
    suspend fun resendMfa(): Result<String>
    fun cancelMfa()
    fun logout()
    suspend fun getValidAccessToken(): String?
    suspend fun refreshAccessTokenIfNeeded(): String?
    suspend fun verifyPhone(code: String): Result<Unit>
    suspend fun setTheme(theme: String): Result<Unit>
}
