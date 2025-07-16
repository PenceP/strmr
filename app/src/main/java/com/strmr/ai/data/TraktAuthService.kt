package com.strmr.ai.data

import android.util.Log
import com.strmr.ai.BuildConfig
import kotlinx.coroutines.delay
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import retrofit2.http.*

interface TraktAuthService {
    @POST("oauth/device/code")
    @Headers("Content-Type: application/json")
    suspend fun getDeviceCode(
        @Body request: DeviceCodeRequest
    ): DeviceCodeResponse

    @POST("oauth/device/token")
    @Headers("Content-Type: application/json")
    suspend fun getAccessToken(
        @Body request: TokenRequest
    ): TokenResponse

    @POST("oauth/token")
    @Headers("Content-Type: application/json")
    suspend fun refreshToken(
        @Body request: RefreshTokenRequest
    ): TokenResponse
}

data class DeviceCodeRequest(
    val client_id: String = BuildConfig.TRAKT_API_KEY,
    val response_type: String = "device_code"
)

data class DeviceCodeResponse(
    val device_code: String,
    val user_code: String,
    val verification_url: String,
    val expires_in: Int,
    val interval: Int
)

data class TokenRequest(
    val code: String,
    val client_id: String = BuildConfig.TRAKT_API_KEY,
    val client_secret: String = "d1096df8b09eca9acb9c6bda2593833743b8106ce437b4ae6498a7d0036300ce",
    val grant_type: String = "device_code"
)

data class RefreshTokenRequest(
    val refresh_token: String,
    val client_id: String = BuildConfig.TRAKT_API_KEY,
    val client_secret: String = "d1096df8b09eca9acb9c6bda2593833743b8106ce437b4ae6498a7d0036300ce",
    val grant_type: String = "refresh_token"
)

data class TokenResponse(
    val access_token: String,
    val refresh_token: String,
    val expires_in: Int,
    val scope: String,
    val created_at: Long
)

class TraktAuthManager(
    private val authService: TraktAuthService
) {
    suspend fun startDeviceAuth(): DeviceCodeResponse {
        return authService.getDeviceCode(DeviceCodeRequest())
    }

    suspend fun pollForToken(deviceCode: String, interval: Int): TokenResponse? {
        var attempts = 0
        val maxAttempts = 60 // 5 minutes with 5-second intervals
        
        Log.d("TraktAuthManager", "🔄 Starting polling for device code: $deviceCode")
        
        while (attempts < maxAttempts && currentCoroutineContext().isActive) {
            try {
                Log.d("TraktAuthManager", "📡 Polling attempt ${attempts + 1}/$maxAttempts")
                val tokenResponse = authService.getAccessToken(
                    TokenRequest(code = deviceCode)
                )
                Log.d("TraktAuthManager", "✅ Authorization successful!")
                return tokenResponse
            } catch (e: Exception) {
                if (!currentCoroutineContext().isActive) {
                    Log.d("TraktAuthManager", "🛑 Polling cancelled")
                    break
                }
                Log.d("TraktAuthManager", "⏳ Waiting for user authorization... (attempt ${attempts + 1})")
                attempts++
                delay((interval * 1000).toLong()) // Convert to milliseconds
            }
        }
        
        if (!currentCoroutineContext().isActive) {
            Log.d("TraktAuthManager", "🛑 Polling was cancelled")
        } else {
            Log.w("TraktAuthManager", "❌ Authorization timed out after $maxAttempts attempts")
        }
        return null
    }

    suspend fun refreshAccessToken(refreshToken: String): TokenResponse? {
        return try {
            authService.refreshToken(RefreshTokenRequest(refresh_token = refreshToken))
        } catch (e: Exception) {
            Log.e("TraktAuthManager", "❌ Failed to refresh token", e)
            null
        }
    }
} 