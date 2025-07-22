package com.strmr.ai.data

import com.strmr.ai.data.models.*
import retrofit2.http.*

// Torrentio API Service
interface TorrentioApiService {
    companion object {
        const val BASE_URL = "https://torrentio.strem.fun/"
    }
    
    @GET("manifest.json")
    suspend fun getManifest(): ScraperManifest
    
    @GET("{config}/manifest.json")
    suspend fun getConfiguredManifest(
        @Path("config") config: String
    ): ScraperManifest
    
    @GET("{config}/stream/{type}/{id}.json")
    suspend fun getStreams(
        @Path("config") config: String,
        @Path("type") type: String, // "movie" or "series"
        @Path("id") id: String // IMDb ID with prefix (e.g., "tt1234567" or "tt1234567:1:1" for series)
    ): StreamResponse
    
    @GET("stream/{type}/{id}.json")
    suspend fun getDefaultStreams(
        @Path("type") type: String,
        @Path("id") id: String
    ): StreamResponse
}

// Comet API Service
interface CometApiService {
    companion object {
        const val BASE_URL = "https://comet.elfhosted.com/"
    }
    
    @GET("{config}/manifest.json")
    suspend fun getManifest(
        @Path("config") config: String
    ): ScraperManifest
    
    @GET("{config}/stream/{type}/{id}.json")
    suspend fun getStreams(
        @Path("config") config: String,
        @Path("type") type: String,
        @Path("id") id: String
    ): StreamResponse
}

// Premiumize API Service
interface PremiumizeApiService {
    companion object {
        const val BASE_URL = "https://www.premiumize.me/api/"
    }
    
    @GET("account/info")
    suspend fun getAccountInfo(
        @Header("Authorization") apiKey: String
    ): PremiumizeUser
    
    @POST("transfer/create")
    @FormUrlEncoded
    suspend fun createTransfer(
        @Header("Authorization") apiKey: String,
        @Field("src") magnetLink: String
    ): PremiumizeTransfer
    
    @GET("transfer/list")
    suspend fun getTransfers(
        @Header("Authorization") apiKey: String
    ): Map<String, Any>
    
    @POST("cache/check")
    @FormUrlEncoded
    suspend fun checkCache(
        @Header("Authorization") apiKey: String,
        @Field("items[]") hashes: List<String>
    ): PremiumizeCache
    
    @POST("transfer/directdl")
    @FormUrlEncoded
    suspend fun getDirectDownloadLink(
        @Header("Authorization") apiKey: String,
        @Field("src") magnetLink: String
    ): Map<String, Any>
}

// Real-Debrid API Service (for future implementation)
interface RealDebridApiService {
    companion object {
        const val BASE_URL = "https://api.real-debrid.com/rest/1.0/"
    }
    
    @GET("user")
    suspend fun getUserInfo(
        @Header("Authorization") token: String
    ): Map<String, Any>
    
    @POST("torrents/addMagnet")
    @FormUrlEncoded
    suspend fun addMagnet(
        @Header("Authorization") token: String,
        @Field("magnet") magnetLink: String
    ): Map<String, Any>
    
    @GET("torrents/instantAvailability/{hash}")
    suspend fun checkInstantAvailability(
        @Header("Authorization") token: String,
        @Path("hash") hash: String
    ): Map<String, Any>
}