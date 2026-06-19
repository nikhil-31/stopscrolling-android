package com.example.stopscrolling_android.data.remote

import com.example.stopscrolling_android.data.remote.dto.AuthTokens
import com.example.stopscrolling_android.data.remote.dto.AuthenticatedUser
import com.example.stopscrolling_android.data.remote.dto.DetailResponse
import com.example.stopscrolling_android.data.remote.dto.MfaPendingResponse
import com.example.stopscrolling_android.data.remote.dto.TokenRefreshResponse
import kotlinx.serialization.Serializable
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthApiClient @Inject constructor(
    private val http: HttpJsonClient
) {
    suspend fun register(
        email: String,
        password: String,
        phoneNumber: String?,
        baseUrl: String
    ): AuthenticatedUser {
        return http.post(
            baseUrl = baseUrl,
            path = "api/auth/register/",
            body = RegisterRequest(email, password, phoneNumber)
        )
    }

    suspend fun login(email: String, password: String, baseUrl: String): MfaPendingResponse {
        return http.post(
            baseUrl = baseUrl,
            path = "api/auth/login/",
            body = LoginRequest(email, password)
        )
    }

    suspend fun verifyOtp(mfaToken: String, code: String, baseUrl: String): AuthTokens {
        return http.post(
            baseUrl = baseUrl,
            path = "api/auth/mfa/otp/verify/",
            body = MfaVerifyRequest(mfaToken, code)
        )
    }

    suspend fun verifyTotp(mfaToken: String, code: String, baseUrl: String): AuthTokens {
        return http.post(
            baseUrl = baseUrl,
            path = "api/auth/mfa/totp/verify/",
            body = MfaVerifyRequest(mfaToken, code)
        )
    }

    suspend fun verifyBackupCode(mfaToken: String, backupCode: String, baseUrl: String): AuthTokens {
        return http.post(
            baseUrl = baseUrl,
            path = "api/auth/mfa/backup-code/verify/",
            body = BackupCodeRequest(mfaToken, backupCode)
        )
    }

    suspend fun resendOtp(mfaToken: String, baseUrl: String): String {
        val response: DetailResponse = http.post(
            baseUrl = baseUrl,
            path = "api/auth/mfa/otp/resend/",
            body = MfaTokenRequest(mfaToken)
        )
        return response.detail
    }

    suspend fun refreshTokens(refreshToken: String, baseUrl: String): String {
        val response: TokenRefreshResponse = http.post(
            baseUrl = baseUrl,
            path = "api/auth/token/refresh/",
            body = RefreshRequest(refreshToken)
        )
        return response.access
    }

    suspend fun me(accessToken: String, baseUrl: String): AuthenticatedUser {
        return http.get(
            baseUrl = baseUrl,
            path = "api/auth/me/",
            accessToken = accessToken
        )
    }

    suspend fun verifyPhone(code: String, accessToken: String, baseUrl: String): AuthenticatedUser {
        return http.post(
            baseUrl = baseUrl,
            path = "api/auth/phone/verify/",
            body = PhoneVerifyRequest(code),
            accessToken = accessToken
        )
    }

    suspend fun setupPhone(phoneNumber: String, accessToken: String, baseUrl: String): String {
        val response: DetailResponse = http.post(
            baseUrl = baseUrl,
            path = "api/auth/phone/",
            body = PhoneSetupRequest(phoneNumber),
            accessToken = accessToken
        )
        return response.detail
    }

    suspend fun setMfaDelivery(delivery: String, accessToken: String, baseUrl: String): AuthenticatedUser {
        return http.post(
            baseUrl = baseUrl,
            path = "api/auth/mfa/delivery/",
            body = MfaDeliveryRequest(delivery),
            accessToken = accessToken
        )
    }

    suspend fun setTheme(theme: String, accessToken: String, baseUrl: String): AuthenticatedUser {
        return http.post(
            baseUrl = baseUrl,
            path = "api/auth/theme/",
            body = ThemeRequest(theme),
            accessToken = accessToken
        )
    }
}

@Serializable
private data class RegisterRequest(
    val email: String,
    val password: String,
    @kotlinx.serialization.SerialName("phone_number") val phoneNumber: String? = null
)

@Serializable
private data class LoginRequest(
    val email: String,
    val password: String
)

@Serializable
private data class MfaVerifyRequest(
    @kotlinx.serialization.SerialName("mfa_token") val mfaToken: String,
    val code: String
)

@Serializable
private data class BackupCodeRequest(
    @kotlinx.serialization.SerialName("mfa_token") val mfaToken: String,
    @kotlinx.serialization.SerialName("backup_code") val backupCode: String
)

@Serializable
private data class MfaTokenRequest(
    @kotlinx.serialization.SerialName("mfa_token") val mfaToken: String
)

@Serializable
private data class RefreshRequest(
    val refresh: String
)

@Serializable
private data class PhoneSetupRequest(
    @kotlinx.serialization.SerialName("phone_number") val phoneNumber: String
)

@Serializable
private data class PhoneVerifyRequest(
    val code: String
)

@Serializable
private data class MfaDeliveryRequest(
    val delivery: String
)

@Serializable
private data class ThemeRequest(
    val theme: String
)
