package com.strmr.ai.data.models

import com.google.gson.annotations.SerializedName

// Premiumize OAuth Models
data class PremiumizeTokenResponse(
    @SerializedName("access_token")
    val accessToken: String,
    @SerializedName("token_type")
    val tokenType: String?,
    @SerializedName("expires_in")
    val expiresIn: Int?,
)

data class PremiumizeAuthError(
    val error: String,
    @SerializedName("error_description")
    val errorDescription: String?,
)

// Device code flow models
data class PremiumizeDeviceCodeResponse(
    @SerializedName("device_code")
    val deviceCode: String,
    @SerializedName("user_code")
    val userCode: String,
    @SerializedName("verification_uri")
    val verificationUrl: String = "https://premiumize.me/device",
    @SerializedName("expires_in")
    val expiresIn: Int,
    val interval: Int,
)

// User account models
data class PremiumizeAccount(
    @SerializedName("customer_id")
    val customerId: String?,
    val email: String?,
    @SerializedName("premium_until")
    val premiumUntil: Long,
    val status: String,
    @SerializedName("space_used")
    val spaceUsed: Double,
    @SerializedName("limit_used")
    val limitUsed: Double,
    @SerializedName("space_limit")
    val spaceLimit: Double? = 1024.0, // 1TB default in GB
) {
    // Helper properties to convert GB to bytes for UI consistency
    val spaceUsedBytes: Long
        get() = (spaceUsed * 1073741824).toLong() // Convert GB to bytes

    val limitUsedBytes: Long
        get() = (limitUsed * 1073741824).toLong() // Convert GB to bytes

    val spaceLimitBytes: Long
        get() = ((spaceLimit ?: 1024.0) * 1073741824).toLong() // Convert GB to bytes
}
