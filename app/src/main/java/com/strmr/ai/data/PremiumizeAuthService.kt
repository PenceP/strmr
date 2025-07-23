package com.strmr.ai.data

import com.strmr.ai.data.models.*
import retrofit2.http.*

interface PremiumizeAuthService {
    companion object {
        const val BASE_URL = "https://www.premiumize.me/"
        const val CLIENT_ID = "784355782"
        const val REDIRECT_URI = "urn:ietf:wg:oauth:2.0:oob"
    }
    
    @POST("token")
    @FormUrlEncoded
    suspend fun getDeviceCode(
        @Field("client_id") clientId: String = CLIENT_ID,
        @Field("response_type") responseType: String = "device_code"
    ): PremiumizeDeviceCodeResponse
    
    @POST("token")
    @FormUrlEncoded
    suspend fun getAccessToken(
        @Field("client_id") clientId: String = CLIENT_ID,
        @Field("grant_type") grantType: String = "device_code",
        @Field("code") deviceCode: String
    ): PremiumizeTokenResponse
    
    @GET("api/account/info")
    suspend fun getAccountInfo(
        @Header("Authorization") authorization: String
    ): PremiumizeAccount
}