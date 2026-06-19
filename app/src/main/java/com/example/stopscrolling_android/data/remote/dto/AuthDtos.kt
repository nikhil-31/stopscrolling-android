package com.example.stopscrolling_android.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AuthTokens(
    val access: String,
    val refresh: String
)

@Serializable
data class MfaPendingResponse(
    @SerialName("mfa_required") val mfaRequired: Boolean,
    @SerialName("mfa_token") val mfaToken: String,
    @SerialName("mfa_method") val mfaMethod: String,
    val message: String,
    val created: Boolean? = null
)

@Serializable
data class AuthenticatedUser(
    val id: Int,
    val email: String,
    @SerialName("tracking_id") val trackingId: String,
    @SerialName("totp_enabled") val totpEnabled: Boolean = false,
    @SerialName("phone_number") val phoneNumber: String = "",
    @SerialName("phone_verified") val phoneVerified: Boolean = false,
    @SerialName("mfa_delivery") val mfaDelivery: String = "",
    val theme: String = "light",
    @SerialName("social_providers") val socialProviders: List<String> = emptyList()
)

@Serializable
data class TokenRefreshResponse(
    val access: String
)

@Serializable
data class DetailResponse(
    val detail: String
)
