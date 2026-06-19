package com.example.stopscrolling_android.data.repository

import com.example.stopscrolling_android.data.auth.AuthTokenStore
import com.example.stopscrolling_android.data.remote.AuthApiClient
import com.example.stopscrolling_android.data.remote.ApiException
import com.example.stopscrolling_android.data.remote.dto.AuthTokens
import com.example.stopscrolling_android.data.remote.dto.AuthenticatedUser
import com.example.stopscrolling_android.data.remote.dto.MfaPendingResponse
import com.example.stopscrolling_android.data.settings.BackendSettingsStore
import com.example.stopscrolling_android.domain.repository.AuthRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val authApiClient: AuthApiClient,
    private val tokenStore: AuthTokenStore,
    private val backendSettingsStore: BackendSettingsStore
) : AuthRepository {

    private val _currentUser = MutableStateFlow<AuthenticatedUser?>(null)
    override val currentUser: StateFlow<AuthenticatedUser?> = _currentUser.asStateFlow()

    private val _mfaChallenge = MutableStateFlow<MfaPendingResponse?>(null)
    override val mfaChallenge: StateFlow<MfaPendingResponse?> = _mfaChallenge.asStateFlow()

    override suspend fun restoreSession(): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val tokens = tokenStore.load() ?: return@runCatching
            val baseUrl = backendSettingsStore.current().apiBaseUrl
            val user = authApiClient.me(tokens.access, baseUrl)
            _currentUser.value = user
            _mfaChallenge.value = null
        }.onFailure {
            tokenStore.clear()
            _currentUser.value = null
        }
    }

    override suspend fun register(
        email: String,
        password: String,
        phoneNumber: String?
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val baseUrl = backendSettingsStore.current().apiBaseUrl
            authApiClient.register(email, password, phoneNumber, baseUrl)
            val challenge = authApiClient.login(email, password, baseUrl)
            _mfaChallenge.value = challenge
        }
    }

    override suspend fun login(email: String, password: String): Result<MfaPendingResponse> =
        withContext(Dispatchers.IO) {
            runCatching {
                val baseUrl = backendSettingsStore.current().apiBaseUrl
                val challenge = authApiClient.login(email, password, baseUrl)
                _mfaChallenge.value = challenge
                challenge
            }
        }

    override suspend fun verifyMfa(
        code: String,
        backupCode: String?,
        useBackupCode: Boolean
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val challenge = _mfaChallenge.value ?: error("No active MFA challenge.")
            val baseUrl = backendSettingsStore.current().apiBaseUrl
            val tokens = when {
                useBackupCode -> authApiClient.verifyBackupCode(
                    challenge.mfaToken,
                    backupCode.orEmpty(),
                    baseUrl
                )
                challenge.mfaMethod == "totp" -> authApiClient.verifyTotp(
                    challenge.mfaToken,
                    code,
                    baseUrl
                )
                else -> authApiClient.verifyOtp(challenge.mfaToken, code, baseUrl)
            }
            tokenStore.save(tokens)
            val user = authApiClient.me(tokens.access, baseUrl)
            _currentUser.value = user
            _mfaChallenge.value = null
        }
    }

    override suspend fun resendMfa(): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val challenge = _mfaChallenge.value ?: error("No active MFA challenge.")
            val baseUrl = backendSettingsStore.current().apiBaseUrl
            authApiClient.resendOtp(challenge.mfaToken, baseUrl)
        }
    }

    override fun cancelMfa() {
        _mfaChallenge.value = null
    }

    override fun logout() {
        tokenStore.clear()
        _currentUser.value = null
        _mfaChallenge.value = null
    }

    override suspend fun getValidAccessToken(): String? = withContext(Dispatchers.IO) {
        tokenStore.load()?.access
    }

    override suspend fun refreshAccessTokenIfNeeded(): String? = withContext(Dispatchers.IO) {
        val tokens = tokenStore.load() ?: return@withContext null
        val baseUrl = backendSettingsStore.current().apiBaseUrl
        try {
            val access = authApiClient.refreshTokens(tokens.refresh, baseUrl)
            tokenStore.save(AuthTokens(access = access, refresh = tokens.refresh))
            access
        } catch (e: ApiException.HttpError) {
            if (e.code == 401 || e.code == 403) {
                logout()
                null
            } else {
                tokens.access
            }
        } catch (_: Exception) {
            tokens.access
        }
    }

    override suspend fun verifyPhone(code: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val tokens = tokenStore.load() ?: error("Sign in required.")
            val baseUrl = backendSettingsStore.current().apiBaseUrl
            val user = authApiClient.verifyPhone(code, tokens.access, baseUrl)
            _currentUser.value = user
        }
    }

    override suspend fun setTheme(theme: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val tokens = tokenStore.load() ?: error("Sign in required.")
            val baseUrl = backendSettingsStore.current().apiBaseUrl
            val user = authApiClient.setTheme(theme, tokens.access, baseUrl)
            _currentUser.value = user
        }
    }
}
